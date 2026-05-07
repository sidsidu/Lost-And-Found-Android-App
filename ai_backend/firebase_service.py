import os
import firebase_admin
from firebase_admin import credentials, firestore, messaging
import numpy as np

# Initialize Firebase Admin
# Requires setting GOOGLE_APPLICATION_CREDENTIALS environment variable or placing serviceAccountKey.json in this folder
try:
    if os.path.exists("serviceAccountKey.json"):
        cred = credentials.Certificate("serviceAccountKey.json")
        firebase_admin.initialize_app(cred)
    else:
        # Tries to use default credentials if available
        firebase_admin.initialize_app()
    db = firestore.client()
    print("Firebase Admin initialized successfully.")
except Exception as e:
    print("Warning: Firebase Admin initialization failed. Ensure service account key is available.", e)
    db = None

def update_backend_url(url: str):
    if not db:
        print("Firebase DB not available. Skipping url update.")
        return
    try:
        db.collection("config").document("backend").set({"url": url})
        print(f"Successfully updated backend URL in Firestore to: {url}")
    except Exception as e:
        print("Error updating backend URL in Firestore:", e)

def cosine_similarity(vec1, vec2):
    v1 = np.array(vec1)
    v2 = np.array(vec2)
    return np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))

def save_item_and_check_matches(item_data: dict, embedding: list):
    """
    Saves the new item to Firestore and searches for potential matches.
    """
    if not db:
        print("Firebase DB not available. Skipping DB operations.")
        return None

    # 1. Save to Firestore
    doc_ref = db.collection("items").document()
    
    # CRASH FIX: Do not set "id" in the document explicitly if Android uses @DocumentId.
    item_data_copy = item_data.copy()
    item_data_copy.pop("id", None)
    
    # CRASH FIX: Ensure dominantColors is a list of strings
    colors = item_data_copy.get("dominantColors", [])
    if isinstance(colors, str):
        colors = [c.strip() for c in colors.split(",")]
    elif not isinstance(colors, list):
        colors = []
    item_data_copy["dominantColors"] = colors

    item_data_copy["embedding"] = embedding  # Save the embedding for future matches
    item_data_copy["timestamp"] = firestore.SERVER_TIMESTAMP
    
    doc_ref.set(item_data_copy)
    print(f"Saved item {doc_ref.id} to Firestore.")

    # 2. Check for matches
    target_status = "found" if item_data.get("status") == "lost" else "lost"
    
    docs = db.collection("items").where("status", "==", target_status).limit(50).stream()

    best_match = None
    highest_score = 0.0

    for doc in docs:
        other_item = doc.to_dict()
        other_embedding = other_item.get("embedding")
        if other_embedding:
            score = cosine_similarity(embedding, other_embedding)
            if score > highest_score:
                highest_score = score
                best_match = other_item
                best_match["id"] = doc.id # attach ID from doc snapshot

    # Threshold for a match (e.g., 0.75 or 75% similarity)
    if highest_score > 0.75 and best_match:
        print(f"Found a match! Score: {highest_score:.2f} -> {best_match.get('itemName')}")
        send_match_notification(item_data_copy, best_match, new_item_id=doc_ref.id)
        return {"match_found": True, "match_score": float(highest_score), "match_id": best_match.get("id")}
    
    return {"match_found": False}

def process_and_update_item(document_id: str, item_data: dict, ai_data: dict, embedding: list):
    """
    Updates an existing item with AI data and searches for matches.
    """
    if not db:
        print("Firebase DB not available.")
        return None

    # CRASH FIX: Ensure dominantColors is a list of strings
    colors = ai_data.get("dominant_colors", [])
    if isinstance(colors, str):
        colors = [c.strip() for c in colors.split(",")]
    elif not isinstance(colors, list):
        colors = []

    # 1. Update the document in Firestore
    updates = {
        "aiDescription": str(ai_data.get("description", "")),
        "category": str(ai_data.get("category", "")),
        "dominantColors": colors,
        "detectedText": str(ai_data.get("detected_text", "None")),
        "embedding": embedding,
    }
    
    # We update instead of set to keep any data written by Android
    db.collection("items").document(document_id).update(updates)
    print(f"Updated item {document_id} with AI data.")

    # 2. Check for matches
    target_status = "found" if item_data.get("status") == "lost" else "lost"
    docs = db.collection("items").where("status", "==", target_status).limit(50).stream()

    best_match = None
    highest_score = 0.0

    for doc in docs:
        other_item = doc.to_dict()
        other_embedding = other_item.get("embedding")
        if other_embedding:
            score = cosine_similarity(embedding, other_embedding)
            if score > highest_score:
                highest_score = score
                best_match = other_item
                best_match["id"] = doc.id

    if highest_score > 0.75 and best_match:
        print(f"Found a match! Score: {highest_score:.2f} -> {best_match.get('itemName')}")
        send_match_notification(item_data, best_match, new_item_id=document_id)
        return {"match_found": True, "match_score": float(highest_score), "match_id": best_match.get("id")}
    
    return {"match_found": False}


def send_match_notification(new_item: dict, matched_item: dict, new_item_id: str = None):
    """
    Sends an FCM notification to the user of the matched item.
    """
    token = matched_item.get("fcmToken")
    if not token:
        print("Match found, but the other user has no FCM token saved. Cannot notify.")
        return

    title = "Potential Match Found!"
    body = f"We found an item similar to your {matched_item.get('itemName')}. Check the app!"

    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body
        ),
        data={
            "matched_item_id": new_item_id or new_item.get("id", "")
        },
        token=token
    )

    try:
        response = messaging.send(message)
        print("Successfully sent message:", response)
    except Exception as e:
        print("Error sending message:", e)
