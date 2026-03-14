package com.example.lostfound.utils

/**
 * Constants — Central place for all app-wide constant values.
 *
 * Keeps Firestore collection names, ImgBB config, and intent extra keys
 * in one location so they're easy to update.
 */
object Constants {

    // ── Firestore ───────────────────────────────────────────────
    /** Name of the Firestore collection that stores all items */
    const val COLLECTION_ITEMS = "items"

    // ── ImgBB (Free Image Hosting) ─────────────────────────────
    // SETUP:
    // 1. Go to https://imgbb.com/ and create a free account
    // 2. Go to https://api.imgbb.com/ and get your free API key
    // 3. Replace the placeholder below with your API key
    const val IMGBB_API_KEY = "29690ef6bbc9e0b7b452015dca85c9ab"
    const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload"

    // ── Item Status Values ──────────────────────────────────────
    const val STATUS_LOST = "lost"
    const val STATUS_FOUND = "found"

    // ── Intent Extras ───────────────────────────────────────────
    /** Key used to pass an ItemModel ID between activities */
    const val EXTRA_ITEM_ID = "extra_item_id"

    // ── Request Codes ───────────────────────────────────────────
    const val REQUEST_CAMERA_PERMISSION = 1001
}
