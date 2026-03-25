package com.example.lostfound.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.lostfound.R
import com.example.lostfound.databinding.ActivityReportLostBinding
import com.example.lostfound.utils.Constants
import com.example.lostfound.utils.ImagePicker
import com.example.lostfound.viewmodels.ItemViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ReportLostActivity — Form to report a lost item.
 *
 * User flow:
 * 1. Tap image area → choose Camera or Gallery
 * 2. Fill in item name, description, location, phone, date
 * 3. Tap Submit → image uploads to Storage, item saved to Firestore
 * 4. Activity finishes and returns to Home screen
 *
 * Validates all required fields before submission.
 */
class ReportLostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportLostBinding
    private val viewModel: ItemViewModel by viewModels()
    private val imagePicker by lazy { ImagePicker(this) }

    // URI of the selected/captured image
    private var selectedImageUri: Uri? = null

    // Calendar for date picker
    private val calendar = Calendar.getInstance()

    // ═══════════════════════════════════════════════════════════════
    // ACTIVITY RESULT LAUNCHERS (Modern replacement for onActivityResult)
    // ═══════════════════════════════════════════════════════════════

    /** Handles the result from the camera intent */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = imagePicker.currentPhotoUri
            showImagePreview()
        }
    }

    /** Handles the result from the gallery picker */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            showImagePreview()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportLostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    // ═══════════════════════════════════════════════════════════════
    // CLICK LISTENERS
    // ═══════════════════════════════════════════════════════════════

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Image upload area → show camera/gallery dialog
        binding.layoutImageUpload.setOnClickListener { showImageSourceDialog() }

        // Date picker
        binding.etDate.setOnClickListener { showDatePicker() }

        // Submit button
        binding.btnSubmit.setOnClickListener { validateAndSubmit() }
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE SELECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Shows a Material dialog letting the user choose between
     * camera and gallery as the image source.
     */
    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Camera — check permission first
                        if (imagePicker.checkCameraPermission(this)) {
                            imagePicker.launchCamera(cameraLauncher)
                        }
                    }
                    1 -> {
                        // Gallery
                        imagePicker.launchGallery(galleryLauncher)
                    }
                }
            }
            .show()
    }

    /**
     * Shows the selected image in the preview ImageView
     * and hides the dashed placeholder.
     */
    private fun showImagePreview() {
        selectedImageUri?.let { uri ->
            binding.ivPreview.visibility = View.VISIBLE
            binding.layoutPlaceholder.visibility = View.GONE
            binding.tvImageError.visibility = View.GONE

            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivPreview)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DATE PICKER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens a Material DatePickerDialog and sets the selected date
     * into the date text field.
     */
    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.etDate.setText(format.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION & SUBMISSION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates all required fields and submits the item if valid.
     * Shows inline errors on TextInputLayouts for missing fields.
     */
    private fun validateAndSubmit() {
        var isValid = true

        // Image validation
        if (selectedImageUri == null) {
            binding.tvImageError.visibility = View.VISIBLE
            isValid = false
        }

        // Item name
        val itemName = binding.etItemName.text.toString().trim()
        if (itemName.isEmpty()) {
            binding.tilItemName.error = getString(R.string.error_item_name_required)
            isValid = false
        } else {
            binding.tilItemName.error = null
        }

        // Location
        val location = binding.etLocation.text.toString().trim()
        if (location.isEmpty()) {
            binding.tilLocation.error = getString(R.string.error_location_required)
            isValid = false
        } else {
            binding.tilLocation.error = null
        }

        // Phone (Optional)
        val phone = binding.etPhone.text.toString().trim()
        binding.tilPhone.error = null

        // Date
        val date = binding.etDate.text.toString().trim()
        if (date.isEmpty()) {
            binding.tilDate.error = getString(R.string.error_date_required)
            isValid = false
        } else {
            binding.tilDate.error = null
        }

        if (!isValid) return

        // All valid — submit via ViewModel
        val description = binding.etDescription.text.toString().trim()

        viewModel.submitItem(
            context = this,
            imageUri = selectedImageUri!!,
            itemName = itemName,
            description = description,
            status = Constants.STATUS_LOST,
            lostLocation = location,
            foundLocation = "",
            dropOffLocation = "",
            contactPhone = phone,
            date = date
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // OBSERVE VIEWMODEL
    // ═══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        // Upload progress
        viewModel.isUploading.observe(this) { isUploading ->
            binding.progressUpload.visibility = if (isUploading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !isUploading
        }

        // Upload success → finish activity
        viewModel.uploadSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.success), Toast.LENGTH_SHORT).show()
                viewModel.clearUploadSuccess()
                finish()
            }
        }

        // Error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Upload Error")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show()
                viewModel.clearError()
            }
        }
    }

    // Handle camera permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            imagePicker.launchCamera(cameraLauncher)
        }
    }
}
