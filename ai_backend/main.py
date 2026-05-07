import os
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import requests
import io
from PIL import Image
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Import local modules
from ai_engine import ai_engine
import firebase_service

from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Read the backend ngrok URL from .env and push it to Firestore
    backend_url = os.getenv("BACKEND_NGROK_URL", "")
    if backend_url and not backend_url.startswith("https://your-"):
        print(f"Backend ngrok URL from .env: {backend_url}")
        firebase_service.update_backend_url(backend_url)
    else:
        print("WARNING: BACKEND_NGROK_URL not set in .env — Android app won't be able to find this server.")
    yield
    # Shutdown

app = FastAPI(title="Lost & Found AI Backend", lifespan=lifespan)

class ItemSubmitRequest(BaseModel):
    itemName: str
    description: str = ""
    status: str
    lostLocation: str = ""
    foundLocation: str = ""
    dropOffLocation: str = ""
    contactPhone: str = ""
    date: str = ""
    fcmToken: str = ""
    imageUrl: str = ""

class ItemAsyncRequest(BaseModel):
    documentId: str
    itemName: str
    description: str = ""
    status: str
    imageUrl: str = ""
    fcmToken: str = ""


@app.get("/")
def read_root():
    return {"status": "AI Backend is running!"}

def process_item_background_task(req: ItemAsyncRequest):
    try:
        print(f"Background Task: Downloading image for {req.itemName} from {req.imageUrl}...")
        response = requests.get(req.imageUrl)
        response.raise_for_status()
        pil_image = Image.open(io.BytesIO(response.content)).convert("RGB")
        
        print(f"Background Task: Processing image for {req.itemName}...")
        ai_data = ai_engine.analyze_image(pil_image)
        
        combined_text = f"Name: {req.itemName}. User Desc: {req.description}. AI Desc: {ai_data.get('description', '')}. Category: {ai_data.get('category', '')}."
        
        print("Background Task: Generating embedding...")
        embedding = ai_engine.generate_embedding(combined_text)
        
        item_data = req.dict()
        
        print(f"Background Task: Updating Firestore for {req.documentId}...")
        firebase_service.process_and_update_item(req.documentId, item_data, ai_data, embedding)
        
    except Exception as e:
        print(f"Background Task Error processing {req.itemName}:", e)

@app.post("/process_item_async")
async def process_item_async(req: ItemAsyncRequest, background_tasks: BackgroundTasks):
    # Enqueue the background task
    background_tasks.add_task(process_item_background_task, req)
    
    # Return immediately to the Android app
    return JSONResponse(status_code=202, content={
        "success": True,
        "message": "Item processing started in the background."
    })

@app.post("/submit_item")
async def submit_item(req: ItemSubmitRequest):
    try:
        # Download the image from the provided URL
        print(f"Downloading image for {req.itemName} from {req.imageUrl}...")
        response = requests.get(req.imageUrl)
        response.raise_for_status()
        pil_image = Image.open(io.BytesIO(response.content)).convert("RGB")
        
        # 1. Analyze image with LLaVA
        print(f"Processing image for {req.itemName}...")
        ai_data = ai_engine.analyze_image(pil_image)
        
        # We will use the user's description + AI description to generate the embedding
        combined_text = f"Name: {req.itemName}. User Desc: {req.description}. AI Desc: {ai_data.get('description', '')}. Category: {ai_data.get('category', '')}."
        
        # 2. Generate embedding
        print("Generating embedding...")
        embedding = ai_engine.generate_embedding(combined_text)
        
        # Construct item dictionary
        item_data = req.dict()
        
        # AI Generated fields
        item_data["aiDescription"] = ai_data.get("description", "")
        item_data["category"] = ai_data.get("category", "")
        item_data["dominantColors"] = ai_data.get("dominant_colors", [])
        item_data["detectedText"] = ai_data.get("detected_text", "None")

        # 3. Save to Firestore & Check for matches
        match_result = firebase_service.save_item_and_check_matches(item_data, embedding)
        
        return JSONResponse(status_code=200, content={
            "success": True,
            "message": "Item processed and saved.",
            "ai_data": ai_data,
            "match": match_result
        })

    except Exception as e:
        print("Error processing item:", e)
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
