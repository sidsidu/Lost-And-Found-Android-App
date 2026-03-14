package com.example.lostfound.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.lostfound.R
import com.example.lostfound.databinding.ActivityItemDetailBinding
import com.example.lostfound.utils.Constants
import com.example.lostfound.viewmodels.ItemViewModel

/**
 * ItemDetailActivity — Displays full details of a lost or found item.
 *
 * Features:
 * • Full-bleed hero image with gradient scrim
 * • Floating back button
 * • Status badge overlay ("LOST" red / "FOUND" green)
 * • Elevated card with description, location, date, contact
 * • "Call Contact" button that opens the phone dialer
 *
 * Receives the item ID via Intent extra and fetches it from Firestore.
 */
class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private val viewModel: ItemViewModel by viewModels()

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.fabBack.setOnClickListener { finish() }

        // Get item ID from intent and fetch data
        val itemId = intent.getStringExtra(Constants.EXTRA_ITEM_ID)
        if (itemId.isNullOrEmpty()) {
            finish()
            return
        }

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE

        // Fetch item data
        viewModel.fetchItemById(itemId)

        // Observe the fetched item
        viewModel.selectedItem.observe(this) { item ->
            binding.progressBar.visibility = View.GONE

            if (item == null) {
                finish()
                return@observe
            }

            // ─── Populate UI ─────────────────────────────────────────

            // Animate card entrance
            binding.cardDetail.startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
            )

            // Hero image
            Glide.with(this)
                .load(item.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_image_placeholder)
                .into(binding.ivItemImage)

            // Item name
            binding.tvItemName.text = item.itemName

            // Status badge
            if (item.status == Constants.STATUS_LOST) {
                binding.tvStatusBadge.text = getString(R.string.status_lost)
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_lost)
            } else {
                binding.tvStatusBadge.text = getString(R.string.status_found)
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_found)
            }

            // Description
            binding.tvDescription.text = item.description.ifBlank { "No description provided." }

            // Location — show lost or found location
            val locationText = item.lostLocation.ifBlank { item.foundLocation }
            binding.tvLocation.text = locationText

            // Update location label based on type
            binding.tvLocationLabel.text = if (item.status == Constants.STATUS_LOST) {
                getString(R.string.location_lost)
            } else {
                getString(R.string.location_found)
            }

            // Drop-off location (only for found items)
            if (item.dropOffLocation.isNotBlank()) {
                binding.layoutDropOff.visibility = View.VISIBLE
                binding.tvDropOff.text = item.dropOffLocation
            } else {
                binding.layoutDropOff.visibility = View.GONE
            }

            // Date
            binding.tvDate.text = item.date

            // Contact phone
            binding.tvContact.text = item.contactPhone

            // ─── Call Contact Button ─────────────────────────────────
            binding.btnCallContact.setOnClickListener {
                // Opens the phone dialer with the contact number pre-filled
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${item.contactPhone}")
                }
                startActivity(dialIntent)
            }
        }
    }
}
