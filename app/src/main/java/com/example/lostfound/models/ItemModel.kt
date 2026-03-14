package com.example.lostfound.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * ItemModel — Represents a lost or found item stored in Firestore.
 *
 * Each field maps directly to a Firestore document field.
 * [DocumentId] auto-populates from the Firestore document ID.
 * [ServerTimestamp] auto-generates on the server when the doc is created.
 *
 * Firestore collection: "items"
 */
data class ItemModel(
    @DocumentId
    val id: String = "",                  // Auto-filled by Firestore document ID
    val itemName: String = "",            // Name of the lost/found item
    val description: String = "",         // Detailed description
    val status: String = "",              // "lost" or "found"
    val lostLocation: String = "",        // Where the item was lost
    val foundLocation: String = "",       // Where the item was found
    val dropOffLocation: String = "",     // Drop-off location (found items, optional)
    val contactPhone: String = "",        // Contact phone number
    val imageUrl: String = "",            // Firebase Storage image URL
    val date: String = "",                // User-selected date string
    @ServerTimestamp
    val timestamp: Timestamp? = null      // Server-generated timestamp for ordering
)
