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

// main screen showing items and search
class MainActivity : AppCompatActivity() {

    // ViewBinding — generated from activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // ViewModel — survives configuration changes
    private val viewModel: ItemViewModel by viewModels()

    // RecyclerView adapter
    private lateinit var itemAdapter: ItemAdapter

    // Expandable FAB state tracker
    private var isFabExpanded = false

    // setup activity

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

    // setup list
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

    // handle clicks

    private fun setupClickListeners() {
        // FAB toggles Expandable Menu
        binding.fabReport.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabOverlay.setOnClickListener {
            if (isFabExpanded) toggleFabMenu()
        }

        binding.fabReportLost.setOnClickListener {
            toggleFabMenu()
            startActivity(Intent(this, ReportLostActivity::class.java))
        }

        binding.fabReportFound.setOnClickListener {
            toggleFabMenu()
            startActivity(Intent(this, ReportFoundActivity::class.java))
        }

        // Hide UI on scroll
        binding.rvItems.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                // Collapse menu if user starts scrolling while it is open
                if (dy != 0 && isFabExpanded) {
                    toggleFabMenu()
                }
                
                if (dy > 0 && binding.fabReport.isExtended) {
                    binding.fabReport.shrink()
                } else if (dy < 0 && !binding.fabReport.isExtended && !isFabExpanded) {
                    binding.fabReport.extend()
                }
            }
        })
    }

    // show or hide the fab menu
    private fun toggleFabMenu() {
        isFabExpanded = !isFabExpanded

        if (isFabExpanded) {
            // 1. Expand Mode
            
            // Shrink the main FAB down to just a circle, then rotate 45 deg to make an 'x'
            binding.fabReport.shrink()
            binding.fabReport.animate().rotation(45f).setDuration(250).start()
            
            // Fade in dim overlay
            binding.fabOverlay.visibility = View.VISIBLE
            binding.fabOverlay.animate().alpha(1f).setDuration(250).start()

            // Pop up the buttons
            binding.fabReportLost.visibility = View.VISIBLE
            binding.fabReportLost.animate().translationY(0f).alpha(1f).setDuration(250).start()

            binding.fabReportFound.visibility = View.VISIBLE
            binding.fabReportFound.animate().translationY(0f).alpha(1f).setDuration(250).start()
        } else {
            // 2. Collapse Mode
            
            // Restore main FAB text and un-rotate back to '+'
            binding.fabReport.extend()
            binding.fabReport.animate().rotation(0f).setDuration(250).start()

            // Fade out overlay
            binding.fabOverlay.animate().alpha(0f).setDuration(250).withEndAction { 
                binding.fabOverlay.visibility = View.GONE 
            }.start()

            // Slide the buttons back down
            binding.fabReportLost.animate().translationY(50f).alpha(0f).setDuration(250).withEndAction { 
                binding.fabReportLost.visibility = View.GONE 
            }.start()

            binding.fabReportFound.animate().translationY(50f).alpha(0f).setDuration(250).withEndAction { 
                binding.fabReportFound.visibility = View.GONE 
            }.start()
        }
    }

    // handle search
    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchItems(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // handle filters
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

    // update ui from viewmodel
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
