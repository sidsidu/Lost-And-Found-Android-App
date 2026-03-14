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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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

    // OkHttp client for ImgBB API requests
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ═══════════════════════════════════════════════════════════════
    // IMAGE UPLOAD (via ImgBB — completely FREE)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Uploads an image to ImgBB and returns the public display URL.
     *
     * How it works:
     * 1. Read the image from the content URI
     * 2. Encode the image bytes to Base64
     * 3. POST the Base64 string to ImgBB's API
     * 4. Parse the JSON response to get the image URL
     *
     * ImgBB is completely FREE — no credit card or billing needed.
     * Just sign up at https://imgbb.com/ and get your API key
     * from https://api.imgbb.com/
     *
     * @param context Android context to access the ContentResolver
     * @param imageUri Local URI of the image file
     * @return Public URL of the uploaded image
     * @throws Exception if the upload fails
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            // Step 1: Read image bytes from the content URI
            val inputStream: InputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Cannot read the selected image")

            val byteArray = inputStream.use { stream ->
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(4096)
                var bytesRead: Int
                while (stream.read(data).also { bytesRead = it } != -1) {
                    buffer.write(data, 0, bytesRead)
                }
                buffer.toByteArray()
            }

            // Step 2: Encode to Base64
            val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // Step 3: Build the POST request to ImgBB
            val requestBody = FormBody.Builder()
                .add("key", Constants.IMGBB_API_KEY)
                .add("image", base64Image)
                .build()

            val request = Request.Builder()
                .url(Constants.IMGBB_UPLOAD_URL)
                .post(requestBody)
                .build()

            // Step 4: Execute the request
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Image upload failed (HTTP ${response.code})")
            }

            // Step 5: Parse JSON response to extract image URL
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from ImgBB")

            val imgbbResponse = Gson().fromJson(responseBody, ImgBBResponse::class.java)

            if (imgbbResponse.success) {
                imgbbResponse.data.displayUrl
            } else {
                throw Exception("ImgBB upload failed")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ImgBB API Response Models
    // ═══════════════════════════════════════════════════════════════

    /** Top-level response from ImgBB API */
    private data class ImgBBResponse(
        val success: Boolean,
        val data: ImgBBData
    )

    /** Image data inside the ImgBB response */
    private data class ImgBBData(
        val url: String,
        @SerializedName("display_url")
        val displayUrl: String
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
