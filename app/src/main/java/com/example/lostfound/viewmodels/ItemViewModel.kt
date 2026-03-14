package com.example.lostfound.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostfound.models.ItemModel
import com.example.lostfound.repository.FirebaseRepository
import com.example.lostfound.utils.Constants
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ItemViewModel — ViewModel for the Lost & Found app.
 *
 * Acts as a bridge between the UI (Activities) and the data layer
 * (FirebaseRepository). Exposes LiveData that the UI observes.
 *
 * Uses Kotlin Coroutines + Flow for asynchronous operations.
 */
class ItemViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    // ═══════════════════════════════════════════════════════════════
    // ITEMS LIST
    // ═══════════════════════════════════════════════════════════════

    /** All items (or filtered items) observed by the RecyclerView */
    private val _items = MutableLiveData<List<ItemModel>>()
    val items: LiveData<List<ItemModel>> = _items

    /** Loading state for showing/hiding progress indicators */
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Error messages for Snackbar / Toast display */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // ═══════════════════════════════════════════════════════════════
    // UPLOAD STATE
    // ═══════════════════════════════════════════════════════════════

    /** Whether an upload operation is in progress */
    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> = _isUploading

    /** Emits true once when upload succeeds (consumed by the Activity) */
    private val _uploadSuccess = MutableLiveData<Boolean>()
    val uploadSuccess: LiveData<Boolean> = _uploadSuccess

    // ═══════════════════════════════════════════════════════════════
    // SINGLE ITEM (Detail Screen)
    // ═══════════════════════════════════════════════════════════════

    private val _selectedItem = MutableLiveData<ItemModel?>()
    val selectedItem: LiveData<ItemModel?> = _selectedItem

    // ═══════════════════════════════════════════════════════════════
    // CURRENT FILTER STATE
    // ═══════════════════════════════════════════════════════════════

    private var currentFilter: String? = null // null = "all"
    private var currentSearchQuery: String = ""

    init {
        // Start by loading all items in real-time
        loadAllItems()
    }

    // ═══════════════════════════════════════════════════════════════
    // LOAD ITEMS (Real-time via Flow)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Subscribes to the real-time stream of ALL items from Firestore.
     * Results are filtered locally by the current search query.
     */
    fun loadAllItems() {
        currentFilter = null
        _isLoading.value = true

        viewModelScope.launch {
            repository.getItemsRealTime()
                .catch { e ->
                    _errorMessage.value = e.message
                    _isLoading.value = false
                }
                .collect { itemsList ->
                    _items.value = applySearchFilter(itemsList)
                    _isLoading.value = false
                }
        }
    }

    /**
     * Subscribes to items filtered by status ("lost" or "found").
     */
    fun loadItemsByStatus(status: String) {
        currentFilter = status
        _isLoading.value = true

        viewModelScope.launch {
            repository.getItemsByStatus(status)
                .catch { e ->
                    _errorMessage.value = e.message
                    _isLoading.value = false
                }
                .collect { itemsList ->
                    _items.value = applySearchFilter(itemsList)
                    _isLoading.value = false
                }
        }
    }

    /**
     * Apply the current search query as a local filter on item names.
     */
    private fun applySearchFilter(items: List<ItemModel>): List<ItemModel> {
        if (currentSearchQuery.isBlank()) return items
        return items.filter {
            it.itemName.contains(currentSearchQuery, ignoreCase = true)
        }
    }

    /**
     * Update the search query and re-filter the current items list.
     */
    fun searchItems(query: String) {
        currentSearchQuery = query
        // Re-apply filter on existing data if available
        _items.value?.let { /* trigger reload */ }
        // Reload from the current filter to apply search
        if (currentFilter == null) {
            loadAllItems()
        } else {
            loadItemsByStatus(currentFilter!!)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FETCH SINGLE ITEM (Detail Screen)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Loads a single item from Firestore by its document ID.
     */
    fun fetchItemById(itemId: String) {
        viewModelScope.launch {
            try {
                _selectedItem.value = repository.getItemById(itemId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SUBMIT ITEM (Upload Image → Save Document)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Uploads the image to ImgBB (free), then saves the item to Firestore.
     *
     * @param context Android context (needed to read image bytes)
     * @param imageUri Local image URI to upload
     * @param itemName Name of the item
     * @param description Description of the item
     * @param status "lost" or "found"
     * @param lostLocation Location where it was lost (empty for found items)
     * @param foundLocation Location where it was found (empty for lost items)
     * @param dropOffLocation Drop-off location (optional, found items only)
     * @param contactPhone Contact phone number
     * @param date User-selected date string
     */
    fun submitItem(
        context: Context,
        imageUri: Uri,
        itemName: String,
        description: String,
        status: String,
        lostLocation: String,
        foundLocation: String,
        dropOffLocation: String,
        contactPhone: String,
        date: String
    ) {
        _isUploading.value = true

        viewModelScope.launch {
            try {
                // Step 1: Upload image to ImgBB (free) → get public URL
                val imageUrl = repository.uploadImage(context, imageUri)

                // Step 2: Create the ItemModel
                val item = ItemModel(
                    itemName = itemName,
                    description = description,
                    status = status,
                    lostLocation = lostLocation,
                    foundLocation = foundLocation,
                    dropOffLocation = dropOffLocation,
                    contactPhone = contactPhone,
                    imageUrl = imageUrl,
                    date = date
                )

                // Step 3: Save to Firestore
                repository.addItem(item)

                // Notify UI of success
                _isUploading.value = false
                _uploadSuccess.value = true

            } catch (e: Exception) {
                _isUploading.value = false
                _errorMessage.value = e.message ?: "Upload failed"
            }
        }
    }

    /**
     * Clears the error message after it has been shown.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Resets the upload success flag after the Activity has consumed it.
     */
    fun clearUploadSuccess() {
        _uploadSuccess.value = false
    }
}
