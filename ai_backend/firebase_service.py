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
    item_data["id"] = doc_ref.id
    item_data["embedding"] = embedding  # Save the embedding for future matches
    item_data["timestamp"] = firestore.SERVER_TIMESTAMP
    
    doc_ref.set(item_data)
    print(f"Saved item {doc_ref.id} to Firestore.")

    # 2. Check for matches
    target_status = "found" if item_data.get("status") == "lost" else "lost"
    
    # Get recent items of the opposite status
    # Note: In a large production app, you would use a Vector DB (like Pinecone) 
    # or Firestore's new vector search. We will do local cosine similarity for simplicity.
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

    # Threshold for a match (e.g., 0.75 or 75% similarity)
    if highest_score > 0.75 and best_match:
        print(f"Found a match! Score: {highest_score:.2f} -> {best_match.get('itemName')}")
        send_match_notification(item_data, best_match)
        return {"match_found": True, "match_score": float(highest_score), "match_id": best_match.get("id")}
    
    return {"match_found": False}

def send_match_notification(new_item: dict, matched_item: dict):
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
            "matched_item_id": new_item.get("id")
        },
        token=token
    )

    try:
        response = messaging.send(message)
        print("Successfully sent message:", response)
    except Exception as e:
        print("Error sending message:", e)
