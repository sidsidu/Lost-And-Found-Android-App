package com.example.lostfound.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lostfound.R
import com.example.lostfound.databinding.ItemCardBinding
import com.example.lostfound.models.ItemModel
import com.example.lostfound.utils.Constants

/**
 * ItemAdapter — RecyclerView adapter for displaying lost/found items.
 *
 * Uses ListAdapter with DiffUtil for efficient list updates (animated inserts,
 * removals, and moves). Loads images with Glide.
 *
 * @param onItemClick Lambda invoked when a card is tapped, passes the ItemModel
 */
class ItemAdapter(
    private val onItemClick: (ItemModel) -> Unit
) : ListAdapter<ItemModel, ItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    // ═══════════════════════════════════════════════════════════════
    // VIEW HOLDER
    // ═══════════════════════════════════════════════════════════════

    inner class ItemViewHolder(
        private val binding: ItemCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemModel) {
            binding.apply {
                // Item name
                tvItemName.text = item.itemName

                // Location — show whichever is available
                tvLocation.text = item.lostLocation.ifBlank { item.foundLocation }

                // Date
                tvDate.text = item.date

                // Status badge — red for lost, green for found
                if (item.status == Constants.STATUS_LOST) {
                    tvStatus.text = root.context.getString(R.string.status_lost)
                    tvStatus.setBackgroundResource(R.drawable.bg_status_lost)
                } else {
                    tvStatus.text = root.context.getString(R.string.status_found)
                    tvStatus.setBackgroundResource(R.drawable.bg_status_found)
                }

                // Load image with Glide — matching the 20dp corner radius from XML
                Glide.with(root.context)
                    .load(item.imageUrl)
                    .transform(CenterCrop()) // Corners are handled by the MaterialCardView
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(ivItemImage)

                // Card click → navigate to detail screen
                cardItem.setOnClickListener { onItemClick(item) }

                // Add tactile touch animation (scale down slightly on press)
                cardItem.setOnTouchListener { view, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(root.context, R.anim.card_press))
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(root.context, R.anim.card_release))
                        }
                    }
                    false // Return false so onClick still fires
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADAPTER OVERRIDES
    // ═══════════════════════════════════════════════════════════════

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ═══════════════════════════════════════════════════════════════
    // DIFF CALLBACK — Efficient RecyclerView updates
    // ═══════════════════════════════════════════════════════════════

    class ItemDiffCallback : DiffUtil.ItemCallback<ItemModel>() {
        override fun areItemsTheSame(old: ItemModel, new: ItemModel): Boolean {
            return old.id == new.id
        }

        override fun areContentsTheSame(old: ItemModel, new: ItemModel): Boolean {
            return old == new
        }
    }
}
