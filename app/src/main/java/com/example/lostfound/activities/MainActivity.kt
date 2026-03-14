package com.example.lostfound.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostfound.R
import com.example.lostfound.adapters.ItemAdapter
import com.example.lostfound.databinding.ActivityMainBinding
import com.example.lostfound.utils.Constants
import com.example.lostfound.viewmodels.ItemViewModel

/**
 * MainActivity — Home Screen of the Lost & Found app.
 *
 * Displays:
 * • Gradient header with app title and search bar
 * • Filter chips (All / Lost / Found)
 * • Two large report buttons (Lost & Found)
 * • RecyclerView showing all posted items
 * • Empty state when no items exist
 *
 * Uses ViewBinding for layout access and ItemViewModel for data.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding — generated from activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // ViewModel — survives configuration changes
    private val viewModel: ItemViewModel by viewModels()

    // RecyclerView adapter
    private lateinit var itemAdapter: ItemAdapter

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        setupSearchBar()
        setupFilterChips()
        observeViewModel()
    }

    // ═══════════════════════════════════════════════════════════════
    // RECYCLERVIEW SETUP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initializes the RecyclerView with a LinearLayoutManager and
     * the ItemAdapter. Tapping a card opens the detail screen.
     */
    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter { item ->
            // Navigate to Item Detail screen, passing the item ID
            val intent = Intent(this, ItemDetailActivity::class.java).apply {
                putExtra(Constants.EXTRA_ITEM_ID, item.id)
            }
            startActivity(intent)
        }

        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = itemAdapter
            // Subtle fade-in animation for list items
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                this@MainActivity,
                R.anim.layout_animation_fall_down
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CLICK LISTENERS & SCROLL
    // ═══════════════════════════════════════════════════════════════

    private fun setupClickListeners() {
        // FAB opens the Bottom Sheet Menu
        binding.fabReport.setOnClickListener {
            showReportBottomSheet()
        }

        // Hide FAB on scroll down, show on scroll up for cleaner UI
        binding.rvItems.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.fabReport.isExtended) {
                    binding.fabReport.shrink()
                } else if (dy < 0 && !binding.fabReport.isExtended) {
                    binding.fabReport.extend()
                }
            }
        })
    }

    /**
     * Shows a modern bottom sheet menu with options to Report Lost or Found items.
     */
    private fun showReportBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.Widget_LostFound_BottomSheet)
        val sheetBinding = com.example.lostfound.databinding.BottomSheetReportBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.btnSheetReportLost.setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivity(Intent(this, ReportLostActivity::class.java))
        }

        sheetBinding.btnSheetReportFound.setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivity(Intent(this, ReportFoundActivity::class.java))
        }

        bottomSheetDialog.show()
    }

    // ═══════════════════════════════════════════════════════════════
    // SEARCH BAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Attaches a TextWatcher to the search input that filters
     * items by name in real-time as the user types.
     */
    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchItems(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTER CHIPS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sets up the Material Chip group so tapping a chip reloads
     * items filtered by status (or all items).
     */
    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipLost) ->
                    viewModel.loadItemsByStatus(Constants.STATUS_LOST)
                checkedIds.contains(R.id.chipFound) ->
                    viewModel.loadItemsByStatus(Constants.STATUS_FOUND)
                else ->
                    viewModel.loadAllItems()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // OBSERVE VIEWMODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Observes LiveData from the ViewModel:
     * • items → submits list to adapter
     * • isLoading → shows/hides progress bar
     * • Shows empty state when list is empty and not loading
     */
    private fun observeViewModel() {
        // Items list
        viewModel.items.observe(this) { items ->
            itemAdapter.submitList(items)
            // Replay layout animation when data changes
            binding.rvItems.scheduleLayoutAnimation()

            // Toggle empty state visibility
            val isEmpty = items.isNullOrEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}
