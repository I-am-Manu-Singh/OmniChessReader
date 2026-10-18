# OmniChess Reader ♟️📸

<p align="center">
  <img src="playstore/icon_512.png" alt="OmniChess Reader Logo" width="128" height="128" style="border-radius: 20%;" />
</p>

<p align="center">
  <b>Instant AI Chessboard Camera Scanner, Neural PGN Digitize & Stockfish Analysis Engine</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-blue.svg?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?logo=jetpackcompose" alt="Compose Multiplatform" />
  <img src="https://img.shields.io/badge/Engine-Stockfish%20NNUE-D32F2F.svg" alt="Stockfish" />
  <img src="https://img.shields.io/badge/Vision-Camera%20%2B%20MediaPipe-FF6F00.svg?logo=google" alt="MediaPipe" />
  <img src="https://img.shields.io/badge/Room-KMP%202.7.1-green.svg" alt="Room" />
  <img src="https://img.shields.io/badge/Target-Android%20%7C%20iOS%20%7C%20Desktop-lightgrey.svg" alt="Targets" />
</p>

---

## 🌟 Overview

**OmniChess Reader** bridges physical chess with modern digital chess analysis. By utilizing real-time computer vision and neural diagram recognition, players can point their phone camera at any physical wooden/plastic chessboard, printed chess book, or screen diagram, and convert it instantly into interactive **FEN (Forsyth-Edwards Notation)** and **PGN (Portable Game Notation)**.

Once digitized, positions feed directly into an embedded **Stockfish engine** for live evaluation bars, best move recommendations, opening book lookups, and blunder detection.

---

---

## 📱 App Screenshots

<p align="center">
  <table>
    <tr>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_1.png" alt="Screenshot 1" width="100%" /><br />
        <b>1. Main Screen</b>
      </td>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_2.png" alt="Screenshot 2" width="100%" /><br />
        <b>2. Core Experience</b>
      </td>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_3.png" alt="Screenshot 3" width="100%" /><br />
        <b>3. Interactive Mode</b>
      </td>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_4.png" alt="Screenshot 4" width="100%" /><br />
        <b>4. Advanced Features</b>
      </td>
    </tr>
    <tr>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_5.png" alt="Screenshot 5" width="100%" /><br />
        <b>5. Workflow / Details</b>
      </td>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_6.png" alt="Screenshot 6" width="100%" /><br />
        <b>6. Studio Console</b>
      </td>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_7.png" alt="Screenshot 7" width="100%" /><br />
        <b>7. Settings & Config</b>
      </td>
      <td align="center" width="25%">
        <img src="screenshots/screenshot_8.png" alt="Screenshot 8" width="100%" /><br />
        <b>8. Dark & Light Theme</b>
      </td>
    </tr>
  </table>
</p>

---

## ✨ Key Features

- 📸 **Instant Board Recognition**: Ultra-fast camera capture of 2D diagrams and 3D physical boards with perspective warping and corner alignment.
- ♟️ **Piece Classification Neural Net**: Accurate identification of pieces under challenging lighting conditions and varying piece sets.
- ⚡ **Deep Stockfish Evaluation**:
  - Live multithreaded Stockfish evaluation bar.
  - Principal variation lines (best continuation moves).
  - Mistake, inaccuracy, and blunder detection with corrective suggestions.
- 📖 **Opening Book Explorer**: Interactive TSV-backed opening database providing ECO codes, variation names, and win-rate statistics.
- 🌐 **Export & Play Integration**:
  - One-tap export to **Lichess** and **Chess.com**.
  - Local PGN library manager with tag editing and move branching.
- 👥 **Local & Network Relay**: Embedded chess socket relay service for syncing boards across devices during study sessions.

---

## 🏗️ Architecture & Modules

The application is structured into high-performance, modular Kotlin Multiplatform layers:

```mermaid
graph TD
    Camera[Camera Preview / Book Diagram] --> Vision[Neural Vision & Perspective Warp]
    Vision --> FEN[FEN / Position Generator]
    FEN --> Engine[Stockfish Evaluation Engine]
    FEN --> UI[Compose Multiplatform Interactive Board]
    Engine --> UI
    OpeningRepo[Opening TSV DB] --> UI
    UI --> RoomDB[Room KMP PGN Library]
    UI --> Cloud[Lichess / Chess.com API]
```

### Modules:
- `:app`: Android, iOS, and Desktop entry points, CameraX integrations, and platform lifecycle hooks.
- `:chess-engine`: Stockfish communication protocols, UCI command pipeline, and evaluation algorithms.
- `:chess-reader`: Neural OCR, contour detection, square grid segmentation, and piece classifier.
- `:chess-shared`: Core chess rules, legal move validator, FEN parser, PGN serialization, and Compose UI boards.
- `:core`: Dependency injection via Koin, persistence with Room KMP, and network clients.

---

## 📂 Project Structure

```text
OmniChessReader/
├── app/                        # Android application runner & CameraX hooks
├── chess-engine/               # UCI Stockfish interface & depth solvers
├── chess-reader/               # Camera perception, diagram & book scanner
├── chess-shared/               # Common KMP UI, board renderers, SVG piece sets
├── core/                       # Koin DI, Room KMP database, network sockets
├── playstore/                  # High-res icon_512.png, feature graphics
└── gradle/                     # Version catalog & build scripts
```

---

## 🚀 Building & Running

### Requirements
- Android Studio Ladybug+ / IntelliJ IDEA
- JDK 17 or JDK 21
- Android NDK (for native engine compilation)

### Commands
```bash
# Build Android APK
./gradlew :app:assembleDebug

# Run Unit & Chess Logic Tests
./gradlew check
```

---


---
## 📄 License
Designed and developed by **CreativeBliss Labs**. All rights reserved.
