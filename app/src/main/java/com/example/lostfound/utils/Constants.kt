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

    // ── Cloudinary (Free Image Hosting) ─────────────────────────
    // SETUP:
    // 1. Create a free account at https://cloudinary.com/
    // 2. Go to Dashboard and copy your Cloud Name
    // 3. Go to Settings > Upload > Add an Upload Preset (Signing Mode: Unsigned)
    const val CLOUDINARY_CLOUD_NAME = "dlwimmxrk"
    const val CLOUDINARY_UPLOAD_PRESET = "lostandfound"
    const val CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"

    // ── Item Status Values ──────────────────────────────────────
    const val STATUS_LOST = "lost"
    const val STATUS_FOUND = "found"

    // ── Intent Extras ───────────────────────────────────────────
    /** Key used to pass an ItemModel ID between activities */
    const val EXTRA_ITEM_ID = "extra_item_id"

    // ── Request Codes ───────────────────────────────────────────
    const val REQUEST_CAMERA_PERMISSION = 1001
}
