import os
import json
import base64
import requests
from io import BytesIO
from sentence_transformers import SentenceTransformer
from PIL import Image
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

class AIEngine:
    def __init__(self, llama_server_url=None):
        self.llama_server_url = llama_server_url or os.getenv("LLAMA_SERVER_URL", "http://localhost:8080")
        print(f"Initializing AI Engine with Llama.cpp server at {self.llama_server_url}")
        
        print("Loading Embedding model...")
        # A tiny fast embedding model for semantic similarity matching
        self.embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
        print("AI Engine initialized successfully!")

    def _image_to_base64(self, image: Image.Image) -> str:
        buffered = BytesIO()
        image.save(buffered, format="JPEG")
        return base64.b64encode(buffered.getvalue()).decode("utf-8")

    def analyze_image(self, image: Image.Image) -> dict:
        """
        Uses an external llama.cpp server to extract description, category, colors, and text.
        """
        base64_image = self._image_to_base64(image)
        
        prompt = (
            "Analyze this image for a Lost & Found application. "
            "Return ONLY a valid JSON object with the following keys: "
            "'description' (detailed description of the item), "
            "'category' (e.g. Electronics, Keys, Wallet, Clothing, Pet), "
            "'dominant_colors' (a list of main colors), "
            "'detected_text' (any readable text or OCR, or 'None'). "
            "Do not include any other text."
        )

        headers = {
            "Content-Type": "application/json",
            "Authorization": "Bearer no-key"
        }
        
        payload = {
            "model": "llava", # Model name is usually ignored by llama.cpp but required by schema
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}"
                            }
                        },
                        {
                            "type": "text",
                            "text": prompt
                        }
                    ]
                }
            ],
            "max_tokens": 300,
            "temperature": 0.2
        }

        print(f"Sending image to Llama.cpp server at {self.llama_server_url}...")
        try:
            url = f"{self.llama_server_url.rstrip('/')}/v1/chat/completions"
            response = requests.post(url, headers=headers, json=payload, timeout=120)
            response.raise_for_status()
            
            response_json = response.json()
            generated_text = response_json["choices"][0]["message"]["content"].strip()
            
            # Clean up in case the model includes markdown blocks
            if generated_text.startswith("```json"):
                generated_text = generated_text[7:]
            if generated_text.startswith("```"):
                generated_text = generated_text[3:]
            if generated_text.endswith("```"):
                generated_text = generated_text[:-3]

            generated_text = generated_text.strip()
            
            try:
                result_json = json.loads(generated_text)
            except json.JSONDecodeError:
                print("Failed to parse JSON. Raw output:", generated_text)
                # Fallback
                result_json = {
                    "description": generated_text,
                    "category": "Unknown",
                    "dominant_colors": [],
                    "detected_text": "None"
                }
            
            return result_json
            
        except Exception as e:
            print("Failed to analyze image with Llama.cpp. Error:", e)
            return {
                "description": "Error during analysis",
                "category": "Unknown",
                "dominant_colors": [],
                "detected_text": "None"
            }

    def generate_embedding(self, text: str) -> list:
        """
        Generates an embedding vector for the text to allow for fast cosine similarity search.
        """
        embedding = self.embedding_model.encode(text)
        return embedding.tolist()

# Singleton instance
ai_engine = AIEngine()
