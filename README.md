# 🔍 Lost & Found — AI-Powered Android App

> **An intelligent lost-and-found platform that uses computer vision and semantic matching to automatically reunite people with their lost belongings.**

Lost & Found is a full-stack Android application backed by an AI-powered Python server. Users report lost or found items with photos, and the system automatically analyzes images using a **LLaVA vision model**, generates **semantic embeddings**, and matches lost items with found items using **cosine similarity** — notifying users in real-time via **Firebase Cloud Messaging (FCM)** when a potential match is discovered.

---

## 📑 Table of Contents

- [Features](#-features)
- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [How It Works — End-to-End Flow](#-how-it-works--end-to-end-flow)
- [Android App Architecture (MVVM)](#-android-app-architecture-mvvm)
- [AI Backend Architecture](#-ai-backend-architecture)
- [Data Model](#-data-model)
- [UI/UX Design](#-uiux-design)
- [Setup & Installation](#-setup--installation)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)

---

## ✨ Features

### Core Features
| Feature | Description |
|---|---|
| **Report Lost Items** | Submit a lost item with photo, name, description, location, date, and contact info |
| **Report Found Items** | Submit a found item with photo, drop-off location, and contact details |
| **AI Image Analysis** | Automatic image analysis via LLaVA vision model — extracts description, category, dominant colors, and OCR text |
| **Smart Matching** | Semantic similarity matching between lost and found items using sentence embeddings and cosine similarity |
| **Push Notifications** | Real-time FCM notifications when a potential match (>75% similarity) is found |
| **Real-Time Feed** | Live-updating item feed powered by Firestore snapshot listeners |
| **Search & Filter** | Search items by name and filter by status (Lost / Found) using Material chip filters |
| **Item Details** | Full detail view with hero image, status badge, location info, and one-tap call button |
| **Image Upload** | Camera capture or gallery selection with EXIF rotation correction, compression, and Cloudinary hosting |

### UI/UX Features
| Feature | Description |
|---|---|
| **Material Design 3** | Modern Material You theming with dynamic color support |
| **Dark Mode** | Full dark theme support with dedicated night color palette |
| **Expandable FAB Menu** | Animated floating action button with expand/collapse for report options |
| **Spotlight Ripple Card** | Custom `CardView` with physics-based radial gradient ripple effect |
| **Micro-Animations** | Card press/release animations, fall-down layout animations, fade-in transitions |
| **Skeleton Loading** | Facebook Shimmer for loading placeholders |
| **Lottie Animations** | Rich animated loading indicators |
| **Form Validation** | Real-time inline error messages with Material TextInputLayout |
| **Empty State** | Illustrated empty state when no items are available |

---

## 🏗 Architecture Overview

The project follows a **two-tier client-server architecture**:

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID APP (Client)                     │
│  ┌───────────┐   ┌──────────┐   ┌────────────┐   ┌──────────┐ │
│  │ Activities │──▶│ ViewModel│──▶│ Repository  │──▶│ Firestore│ │
│  │  (Views)   │◀──│ (LiveData│◀──│ (Data Layer)│   │ (Cloud)  │ │
│  └───────────┘   └──────────┘   └─────┬──────┘   └──────────┘ │
│                                       │                         │
│                                  ┌────▼─────┐                  │
│                                  │Cloudinary │                  │
│                                  │ (Images)  │                  │
│                                  └──────────┘                   │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP POST (via ngrok tunnel)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                     AI BACKEND (Python Server)                   │
│  ┌──────────┐   ┌───────────┐   ┌───────────────┐              │
│  │  FastAPI  │──▶│ AI Engine │──▶│ Firebase Admin│              │
│  │ (Routes)  │   │           │   │   (Service)   │              │
│  └──────────┘   │ ┌────────┐│   │ ┌───────────┐ │              │
│                 │ │ LLaVA  ││   │ │ Firestore  │ │              │
│                 │ │ (Vision)││   │ │  Update    │ │              │
│                 │ └────────┘│   │ ├───────────┤ │              │
│                 │ ┌────────┐│   │ │  Cosine    │ │              │
│                 │ │MiniLM  ││   │ │ Similarity │ │              │
│                 │ │(Embed) ││   │ │  Matching  │ │              │
│                 │ └────────┘│   │ ├───────────┤ │              │
│                 └───────────┘   │ │   FCM      │ │              │
│                                 │ │  Notify    │ │              │
│                                 │ └───────────┘ │              │
│                                 └───────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where | Purpose |
|---|---|---|
| **MVVM** | Android App | Separates UI (Activities) from business logic (ViewModel) and data (Repository) |
| **Repository** | `FirebaseRepository` | Single source of truth abstracting Firestore and Cloudinary API calls |
| **Observer (LiveData)** | ViewModel ↔ Activities | Reactive UI updates that respect lifecycle |
| **Singleton** | `AIEngine` | Single instance of the AI engine shared across the backend |
| **Background Task** | FastAPI `BackgroundTasks` | Async AI processing that doesn't block the HTTP response |
| **Callback Flow** | `FirebaseRepository` | Converts Firestore snapshot listeners into Kotlin coroutine Flows |
| **ViewBinding** | All Activities | Type-safe view access replacing `findViewById` |
| **DiffUtil** | `ItemAdapter` | Efficient RecyclerView updates with minimal redraws |
| **FileProvider** | Camera capture | Secure file URI sharing between apps |

---

## 🛠 Tech Stack

### Android App
| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 1.9.22 | Primary language |
| Android SDK | API 26–34 | Min SDK 26 (Android 8.0), Target SDK 34 |
| Gradle | 8.2.2 | Build system |
| Material Design 3 | 1.11.0 | UI component library |
| Firebase Firestore | BOM 32.7.1 | Real-time NoSQL cloud database |
| Firebase Cloud Messaging | BOM 32.7.1 | Push notifications |
| OkHttp | 4.12.0 | HTTP client for Cloudinary & backend API |
| Gson | 2.10.1 | JSON serialization/deserialization |
| Glide | 4.16.0 | Image loading and caching |
| Kotlin Coroutines | 1.7.3 | Asynchronous programming |
| Lottie | 6.3.0 | Rich JSON-based animations |
| Facebook Shimmer | 0.5.0 | Skeleton loading placeholders |
| AndroidX Lifecycle | 2.7.0 | ViewModel + LiveData |

### AI Backend (Python)
| Technology | Purpose |
|---|---|
| FastAPI | High-performance async REST API framework |
| Uvicorn | ASGI server for FastAPI |
| Llama.cpp (LLaVA) | Multimodal vision model for image analysis |
| Sentence-Transformers | `all-MiniLM-L6-v2` embedding model for semantic similarity |
| Firebase Admin SDK | Server-side Firestore access and FCM notifications |
| NumPy | Cosine similarity vector math |
| Pillow (PIL) | Image processing and format conversion |
| python-dotenv | Environment variable management |
| ngrok | HTTP tunneling to expose local server to the internet |

### Cloud Services
| Service | Purpose |
|---|---|
| Firebase Firestore | Document database for items, config, and embeddings |
| Firebase Cloud Messaging | Push notifications to mobile devices |
| Cloudinary | Image hosting CDN with upload presets |
| ngrok | Tunnel local AI backend to a public HTTPS URL |

---

## 📁 Project Structure

```
Lost And Found Android App/
├── app/                                    # Android Application
│   ├── build.gradle                        # App-level dependencies
│   ├── src/main/
│   │   ├── AndroidManifest.xml             # Permissions, activities, services
│   │   ├── java/com/example/lostfound/
│   │   │   ├── activities/                 # UI Screens
│   │   │   │   ├── MainActivity.kt         # Home feed with search & filters
│   │   │   │   ├── ReportLostActivity.kt   # Form to report lost items
│   │   │   │   ├── ReportFoundActivity.kt  # Form to report found items
│   │   │   │   ├── ItemDetailActivity.kt   # Full item detail view
│   │   │   │   └── SpotlightDemoActivity.kt# Custom ripple card demo
│   │   │   ├── adapters/
│   │   │   │   └── ItemAdapter.kt          # RecyclerView adapter with DiffUtil
│   │   │   ├── models/
│   │   │   │   └── ItemModel.kt            # Firestore data class
│   │   │   ├── repository/
│   │   │   │   └── FirebaseRepository.kt   # Data layer (Firestore + Cloudinary)
│   │   │   ├── services/
│   │   │   │   └── MyFirebaseMessagingService.kt  # FCM push handler
│   │   │   ├── utils/
│   │   │   │   ├── Constants.kt            # App-wide constants
│   │   │   │   └── ImagePicker.kt          # Camera/gallery helper
│   │   │   ├── viewmodels/
│   │   │   │   └── ItemViewModel.kt        # Business logic + state
│   │   │   └── views/
│   │   │       └── SpotlightRippleCard.kt  # Custom physics-based ripple CardView
│   │   └── res/
│   │       ├── layout/                     # XML layouts (7 files)
│   │       ├── anim/                       # Animations (8 files)
│   │       ├── drawable/                   # Backgrounds, gradients, icons (17 files)
│   │       ├── values/                     # Colors, strings, themes, dimens
│   │       └── values-night/               # Dark mode color overrides
│
├── ai_backend/                             # Python AI Server
│   ├── main.py                             # FastAPI app with routes
│   ├── ai_engine.py                        # LLaVA vision + MiniLM embeddings
│   ├── firebase_service.py                 # Firestore CRUD + matching + FCM
│   ├── requirements.txt                    # Python dependencies
│   ├── .env                                # ngrok URLs configuration
│   └── serviceAccountKey.json              # Firebase Admin credentials
│
├── build.gradle                            # Root Gradle config
├── settings.gradle                         # Project settings
└── .gitignore                              # Git exclusions
```

---

## 🔄 How It Works — End-to-End Flow

### Reporting an Item (Lost or Found)

```
User taps "Report"
       │
       ▼
┌──────────────┐
│ Select Image │──── Camera or Gallery (with EXIF rotation fix)
│ Fill Form    │──── Name, Description, Location, Date, Phone
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│ Upload to         │
│ Cloudinary (CDN)  │──── Image compressed to 1024px max, JPEG 80% quality
└──────┬───────────┘
       │ Returns imageUrl
       ▼
┌──────────────────┐
│ Save to Firestore │──── Immediate save for instant UI feedback
└──────┬───────────┘
       │ Returns documentId
       ▼
┌──────────────────┐
│ POST to AI Backend│──── /process_item_async (non-blocking)
│ via ngrok tunnel  │     Sends: documentId, imageUrl, itemName, status
└──────┬───────────┘
       │ Returns HTTP 202 (Accepted) immediately
       ▼
   User sees success ✅
```

### Background AI Processing

```
Backend receives request
       │
       ▼
┌──────────────────┐
│ Download Image    │──── From Cloudinary URL
│ from URL          │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ LLaVA Vision      │──── Analyzes image via llama.cpp server
│ Analysis          │     Extracts: description, category, colors, OCR text
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Generate Embedding│──── all-MiniLM-L6-v2 sentence transformer
│ Vector            │     Combines: itemName + userDesc + aiDesc + category
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Update Firestore  │──── Writes AI fields + embedding to the document
│ Document          │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Match Search      │──── Query opposite status items (lost↔found)
│ (Cosine Similarity│     Compare embedding vectors
│  > 0.75 threshold)│     Limit: top 50 candidates
└──────┬───────────┘
       │ Match found?
       ▼
┌──────────────────┐
│ FCM Push Notify   │──── "Potential Match Found!" notification
│ Matched User      │     Opens ItemDetailActivity on tap
└──────────────────┘
```

---

## 📱 Android App Architecture (MVVM)

The app strictly follows the **Model-View-ViewModel (MVVM)** pattern:

```
┌─────────────────────────────────────────────────┐
│                    VIEW LAYER                    │
│  Activities (UI) observe LiveData from ViewModel │
│                                                  │
│  MainActivity ─── Item feed, search, filters     │
│  ReportLostActivity ─── Lost item form           │
│  ReportFoundActivity ─── Found item form         │
│  ItemDetailActivity ─── Item detail view         │
│  ItemAdapter ─── RecyclerView list rendering     │
└──────────────────┬──────────────────────────────┘
                   │ observes LiveData
                   ▼
┌─────────────────────────────────────────────────┐
│                 VIEWMODEL LAYER                  │
│  ItemViewModel                                   │
│  ├── items: LiveData<List<ItemModel>>            │
│  ├── isLoading: LiveData<Boolean>                │
│  ├── isUploading: LiveData<Boolean>              │
│  ├── uploadSuccess: LiveData<Boolean>            │
│  ├── errorMessage: LiveData<String?>             │
│  ├── selectedItem: LiveData<ItemModel?>          │
│  ├── loadAllItems()                              │
│  ├── loadItemsByStatus(status)                   │
│  ├── searchItems(query)                          │
│  ├── submitItem(...)                             │
│  └── fetchItemById(id)                           │
└──────────────────┬──────────────────────────────┘
                   │ calls
                   ▼
┌─────────────────────────────────────────────────┐
│                  DATA LAYER                      │
│  FirebaseRepository                              │
│  ├── getItemsRealTime(): Flow<List<ItemModel>>   │
│  ├── getItemsByStatus(): Flow<List<ItemModel>>   │
│  ├── saveItem(item): String (documentId)         │
│  ├── getItemById(id): ItemModel?                 │
│  ├── uploadImage(context, uri): String (url)     │
│  └── getBackendUrl(): String? (from Firestore)   │
│                                                  │
│  External APIs:                                  │
│  ├── Cloudinary (image upload via OkHttp)         │
│  ├── Firestore (real-time snapshot listeners)    │
│  └── AI Backend (HTTP POST via ngrok)            │
└─────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Kotlin Coroutines + Flow** — Firestore snapshot listeners are wrapped in `callbackFlow` for reactive, lifecycle-safe data streaming
- **ViewBinding** — Type-safe view references generated from XML layouts (no `findViewById`)
- **DiffUtil in ListAdapter** — Efficient RecyclerView updates that only re-render changed items
- **Asynchronous Upload Pipeline** — Image upload → Firestore save → AI processing are decoupled; the user sees instant success while AI runs in the background
- **Dynamic Backend URL** — The ngrok URL is stored in Firestore (`config/backend`) and fetched at runtime, eliminating hardcoded URLs

---

## 🤖 AI Backend Architecture

The Python backend is a **FastAPI** application with three core modules:

### `main.py` — API Server
- Defines FastAPI routes and request schemas
- On startup, pushes the ngrok URL to Firestore so the Android app can discover it
- Provides both synchronous (`/submit_item`) and asynchronous (`/process_item_async`) endpoints
- Uses FastAPI `BackgroundTasks` for non-blocking AI processing

### `ai_engine.py` — AI Processing Engine
- **LLaVA Vision Model** (via llama.cpp): Analyzes item images and returns structured JSON with description, category, dominant colors, and detected text (OCR)
- **Sentence Embeddings** (`all-MiniLM-L6-v2`): Generates 384-dimensional embedding vectors from combined text (item name + user description + AI description + category)
- Uses the OpenAI-compatible `/v1/chat/completions` API format to communicate with the llama.cpp server

### `firebase_service.py` — Data & Matching Service
- **Firestore Operations**: Save new items, update existing documents with AI data
- **Cosine Similarity Matching**: Compares the new item's embedding against all items of the opposite status (lost↔found) using NumPy vector math
- **Match Threshold**: Items with >75% similarity score trigger a match
- **FCM Notifications**: Sends push notifications to the matched user's device via Firebase Admin SDK

---

## 📦 Data Model

### Firestore Collection: `items`

| Field | Type | Source | Description |
|---|---|---|---|
| `id` | `string` | Firestore | Auto-generated document ID (`@DocumentId`) |
| `itemName` | `string` | User | Name of the item |
| `description` | `string` | User | User-provided description |
| `status` | `string` | User | `"lost"` or `"found"` |
| `lostLocation` | `string` | User | Where the item was lost |
| `foundLocation` | `string` | User | Where the item was found |
| `dropOffLocation` | `string` | User | Drop-off point (found items) |
| `contactPhone` | `string` | User | Contact phone number |
| `imageUrl` | `string` | Cloudinary | Hosted image URL |
| `date` | `string` | User | Date the item was lost/found |
| `fcmToken` | `string` | Device | FCM token for push notifications |
| `aiDescription` | `string` | AI | AI-generated image description |
| `category` | `string` | AI | AI-assigned category (e.g., Electronics, Keys) |
| `dominantColors` | `list<string>` | AI | Dominant colors detected in the image |
| `detectedText` | `string` | AI | OCR text extracted from the image |
| `embedding` | `list<float>` | AI | 384-dim sentence embedding vector |
| `timestamp` | `Timestamp` | Server | Server-generated timestamp for ordering |

### Firestore Collection: `config`

| Document | Field | Purpose |
|---|---|---|
| `backend` | `url` | Current ngrok URL for the AI backend |

---

## 🎨 UI/UX Design

### Theme System
- **Material Design 3** with custom theme (`Theme.LostAndFound`)
- **Full dark mode support** via `values-night/colors.xml`
- Custom gradient headers, scrim overlays, and glow effects

### Custom Components
- **SpotlightRippleCard** — A custom `CardView` with physics-based touch ripple animation using `RadialGradient` shaders and `ValueAnimator`. Mimics React's Material ripple with soft-edge container ratios and dynamic duration calculations
- **Expandable FAB** — Extended FAB that shrinks + rotates 45° into an "X" close button, revealing sub-action buttons with slide-up animations

### Animations (8 animation files)
| Animation | Purpose |
|---|---|
| `fade_in_up` | Detail card entrance |
| `item_fall_down` | List item cascade |
| `layout_animation_fall_down` | RecyclerView stagger |
| `card_press` / `card_release` | Tactile touch feedback |
| `slide_in_right` / `slide_out_left` | Screen transitions |
| `btn_state_list_anim` | Button state animator |

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio (Hedgehog or later)
- Python 3.9+
- [ngrok](https://ngrok.com/) account (free tier works)
- [llama.cpp](https://github.com/ggerganov/llama.cpp) with a LLaVA model (e.g., `llava-v1.5-7b`)
- Firebase project with Firestore and FCM enabled
- Cloudinary account (free tier)

### 1. Android App Setup

```bash
# Clone the repository
git clone https://github.com/your-username/Lost-And-Found-Android-App.git
cd Lost-And-Found-Android-App
```

1. Open the project in **Android Studio**
2. Go to [Firebase Console](https://console.firebase.google.com/) → Create project → Add Android app (`com.example.lostfound`)
3. Download `google-services.json` → Place in `app/` directory
4. Enable **Firestore Database** (test mode) and **Cloud Messaging** in Firebase Console
5. Build and run on device/emulator (API 26+)

### 2. AI Backend Setup

```bash
cd ai_backend

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # Linux/Mac
# or
.venv\Scripts\activate     # Windows

# Install dependencies
pip install -r requirements.txt
```

### 3. Start llama.cpp Server

```bash
# In a separate terminal, start the LLaVA model server
./llama-server -m models/llava-v1.5-7b-Q4_K_M.gguf \
  --mmproj models/mmproj-model-f16.gguf \
  -c 2048 --port 8080
```

### 4. Start ngrok Tunnels

```bash
# Terminal 1: Tunnel for the FastAPI backend
ngrok http 8000

# Terminal 2 (if llama.cpp is on a different machine): Tunnel for llama.cpp
ngrok http 8080
```

### 5. Configure & Run Backend

```bash
# Edit .env with your ngrok URLs
# BACKEND_NGROK_URL="https://your-backend-url.ngrok-free.dev"
# LLAMA_SERVER_URL="http://127.0.0.1:8080"

# Place your Firebase serviceAccountKey.json in ai_backend/

# Start the server
python main.py
# or
uvicorn main:app --host 0.0.0.0 --port 8000
```

---

## ⚙ Configuration

### Environment Variables (`ai_backend/.env`)

| Variable | Description | Example |
|---|---|---|
| `BACKEND_NGROK_URL` | Public ngrok URL for the FastAPI server | `https://abc123.ngrok-free.dev` |
| `LLAMA_SERVER_URL` | URL of the llama.cpp server | `http://127.0.0.1:8080` |

### Android Constants (`Constants.kt`)

| Constant | Description |
|---|---|
| `CLOUDINARY_CLOUD_NAME` | Your Cloudinary cloud name |
| `CLOUDINARY_UPLOAD_PRESET` | Unsigned upload preset name |
| `NGROK_BACKEND_URL` | Fallback backend URL (primary is fetched from Firestore) |

---

## 📡 API Endpoints

### `GET /`
Health check — returns `{"status": "AI Backend is running!"}`

### `POST /process_item_async` *(Primary — used by Android app)*
Accepts an item and processes it asynchronously in the background.

**Request Body:**
```json
{
  "documentId": "firestore_doc_id",
  "itemName": "Blue Backpack",
  "description": "Nike blue backpack with zipper",
  "status": "lost",
  "imageUrl": "https://res.cloudinary.com/...",
  "fcmToken": "device_fcm_token"
}
```

**Response (HTTP 202):**
```json
{
  "success": true,
  "message": "Item processing started in the background."
}
```

### `POST /submit_item` *(Synchronous alternative)*
Processes the item synchronously and returns AI analysis results and match info.

**Response (HTTP 200):**
```json
{
  "success": true,
  "message": "Item processed and saved.",
  "ai_data": {
    "description": "A blue Nike backpack with...",
    "category": "Bag",
    "dominant_colors": ["blue", "black"],
    "detected_text": "Nike"
  },
  "match": {
    "match_found": true,
    "match_score": 0.82,
    "match_id": "firestore_doc_id"
  }
}
```

---

## 📄 License

This project is for educational and personal use.

---

*Built with ❤️ using Kotlin, Python, Firebase, and AI*
