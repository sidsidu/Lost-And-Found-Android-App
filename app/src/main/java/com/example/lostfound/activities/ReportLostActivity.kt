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

// form screen where users post lost items
class ReportLostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportLostBinding
    private val viewModel: ItemViewModel by viewModels()
    private val imagePicker by lazy { ImagePicker(this) }

    // URI of the selected/captured image
    private var selectedImageUri: Uri? = null

    // Calendar for date picker
    private val calendar = Calendar.getInstance()

    // handles camera result
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = imagePicker.currentPhotoUri
            showImagePreview()
        }
    }

    // handles gallery result
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            showImagePreview()
        }
    }

    // sets up the screen layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportLostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    // connects buttons to actions

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

    // shows pop-up asking user to pick camera or gallery
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

    // places the selected image on screen
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

    // opens calendar so user can pick a date
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

    // checks if user filled the form correctly before sending
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

    // updates screen when data changes

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
