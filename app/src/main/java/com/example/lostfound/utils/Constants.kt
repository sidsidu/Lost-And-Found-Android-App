package com.example.lostfound.utils

object Constants {


    const val COLLECTION_ITEMS = "items"


    const val CLOUDINARY_CLOUD_NAME = "dlwimmxrk"
    const val CLOUDINARY_UPLOAD_PRESET = "lostandfound"
    const val CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"


    const val STATUS_LOST = "lost"
    const val STATUS_FOUND = "found"


    /** Key used to pass an ItemModel ID between activities */
    const val EXTRA_ITEM_ID = "extra_item_id"

    const val REQUEST_CAMERA_PERMISSION = 1001
}
