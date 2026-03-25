package com.example.lostfound.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.lostfound.models.ItemModel
import com.example.lostfound.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * FirebaseRepository — Single source of truth for all data operations.
 *
 * Handles:
 * • Uploading images to ImgBB (FREE image hosting — no billing required)
 * • Creating items in Firestore
 * • Real-time streaming of items via Kotlin Flow
 * • Fetching a single item by ID
 * • Filtering items by status
 *
 * ─────────────────────────────────────────────────────────────────
 * SETUP INSTRUCTIONS:
 * 1. Place google-services.json in the app/ directory.
 * 2. In Firebase Console → Firestore → Create database → Start in TEST mode.
 * 3. Get a FREE ImgBB API key from https://api.imgbb.com/
 *    and paste it into Constants.IMGBB_API_KEY
 * ─────────────────────────────────────────────────────────────────
 */
class FirebaseRepository {

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()

    // Reference to the "items" collection
    private val itemsCollection = firestore.collection(Constants.COLLECTION_ITEMS)

    // OkHttp client for Cloudinary API requests
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ═══════════════════════════════════════════════════════════════
    // IMAGE UPLOAD (Cloudinary Free API)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Uploads an image to Cloudinary and returns the public download URL.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            // Step 1: Read image and compress it
            // Parse EXIF for rotation first
            var rotationDegrees = 0f
            context.contentResolver.openInputStream(imageUri)?.use { exifStream ->
                val exif = android.media.ExifInterface(exifStream)
                rotationDegrees = when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }

            val inputStream: InputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Cannot read the selected image")

            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) throw Exception("Failed to decode image")

            // Scale down to max 1024x1024
            val maxDim = 1024f
            val scale = java.lang.Math.min(maxDim / originalBitmap.width, maxDim / originalBitmap.height)
            
            var finalBitmap = if (scale < 1) {
                android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            // Apply EXIF rotation if necessary
            if (rotationDegrees != 0f) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotationDegrees)
                val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                    finalBitmap, 0, 0, finalBitmap.width, finalBitmap.height, matrix, true
                )
                if (rotatedBitmap != finalBitmap) {
                    if (finalBitmap != originalBitmap) finalBitmap.recycle()
                    finalBitmap = rotatedBitmap
                }
            }

            val buffer = ByteArrayOutputStream()
            // Compress to JPEG with 80% quality
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, buffer)
            val byteArray = buffer.toByteArray()

            if (finalBitmap != originalBitmap) finalBitmap.recycle()
            originalBitmap.recycle()

            // Step 2: Build the POST request to Cloudinary
            // Sending raw bytes via MultipartBody completely avoids filename and base64 parsing issues.
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestFile = byteArray.toRequestBody(mediaType)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "upload.jpg", requestFile)
                .addFormDataPart("upload_preset", Constants.CLOUDINARY_UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url(Constants.CLOUDINARY_UPLOAD_URL)
                .post(requestBody)
                .build()

            // Step 4: Execute the request
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                throw Exception("Cloudinary Error HTTP ${response.code}: $errorBody")
            }

            // Step 5: Parse JSON response to extract image URL
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Cloudinary")

            val cloudinaryResponse = Gson().fromJson(responseBody, CloudinaryResponse::class.java)

            if (cloudinaryResponse.secureUrl != null) {
                cloudinaryResponse.secureUrl
            } else {
                throw Exception("Cloudinary upload failed")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Cloudinary API Response Models
    // ═══════════════════════════════════════════════════════════════

    /** Cloudinary API Response */
    private data class CloudinaryResponse(
        @SerializedName("secure_url")
        val secureUrl: String?
    )

    // ═══════════════════════════════════════════════════════════════
    // CREATE ITEM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Adds a new lost/found item to Firestore.
     *
     * The document ID is auto-generated by Firestore.
     * The [ItemModel.timestamp] field is filled by the server via @ServerTimestamp.
     *
     * @param item The ItemModel to store
     */
    suspend fun addItem(item: ItemModel) {
        itemsCollection.add(item).await()
    }

    // ═══════════════════════════════════════════════════════════════
    // REAL-TIME ITEM STREAM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns a Flow that emits the full list of items every time
     * the Firestore "items" collection changes (real-time updates).
     *
     * Items are ordered by timestamp descending (newest first).
     */
    fun getItemsRealTime(): Flow<List<ItemModel>> = callbackFlow {
        val listener = itemsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.toObjects(ItemModel::class.java) ?: emptyList()
                trySend(items)
            }

        // Remove the listener when the Flow collector is cancelled
        awaitClose { listener.remove() }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET SINGLE ITEM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Fetches a single item by its Firestore document ID.
     *
     * @param itemId The document ID
     * @return The ItemModel, or null if not found
     */
    suspend fun getItemById(itemId: String): ItemModel? {
        val snapshot = itemsCollection.document(itemId).get().await()
        return snapshot.toObject(ItemModel::class.java)
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTERED & SEARCH QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns a Flow of items filtered by status ("lost" or "found").
     */
    fun getItemsByStatus(status: String): Flow<List<ItemModel>> = callbackFlow {
        val listener = itemsCollection
            .whereEqualTo("status", status)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.toObjects(ItemModel::class.java) ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }
}
