package com.example.lostfound.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ImagePicker — Helper class for launching camera or gallery image selection.
 *
 * Uses the modern ActivityResult API launchers passed in from the Activity.
 * Handles FileProvider URI creation for camera images and permission checks.
 */
class ImagePicker(private val context: Context) {

    /** Stores the URI of the image captured by the camera */
    var currentPhotoUri: Uri? = null
        private set

    /**
     * Launch the camera to take a photo.
     * Creates a temporary file and gives its URI to the camera intent.
     *
     * @param launcher ActivityResultLauncher for the camera intent
     */
    fun launchCamera(launcher: ActivityResultLauncher<Intent>) {
        val photoFile = createImageFile()
        currentPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
        }
        launcher.launch(intent)
    }

    /**
     * Launch the gallery to pick an image.
     *
     * @param launcher ActivityResultLauncher for the gallery pick intent
     */
    fun launchGallery(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        launcher.launch(intent)
    }

    /**
     * Check and request camera permission if not already granted.
     *
     * @return true if permission is already granted, false if a request was made
     */
    fun checkCameraPermission(activity: Activity): Boolean {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                Constants.REQUEST_CAMERA_PERMISSION
            )
            false
        }
    }

    /**
     * Creates a temporary image file in the app's external cache directory.
     * Uses a timestamp-based filename to avoid collisions.
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }
}
