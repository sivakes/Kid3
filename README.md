# ChromaCut

A desktop app for professional-grade photo editing, video editing, and color grading, with an interface that is easy to learn.

Built with **Kotlin** (desktop UI) and **Python** (processing engine).

> **Status:** Early development. The features below are project goals, not all implemented yet.

## Goals

- **Professional quality:** non-destructive editing, high bit-depth color, accurate color management.
- **Easy interface:** clean layout, sensible defaults, and guided workflows for beginners, with full control for professionals.
- **One app for photo, video, and color:** no jumping between separate tools.

## Planned Features

### Photo Editing
- Non-destructive editing with an adjustment stack
- RAW file support
- Exposure, contrast, white balance, curves, HSL, sharpening, noise reduction
- Crop, rotate, perspective correction
- Masks (brush, gradient)
- Presets and batch export

### Video Editing
- Multi-track timeline
- Cut, trim, split, speed changes
- Transitions and titles
- Audio waveform view
- Proxy workflow for smooth editing of 4K footage
- Export to H.264, H.265, ProRes

### Color Grading
- Primary wheels (lift, gamma, gain, offset)
- Curves and HSL secondaries
- LUT import/export (.cube)
- Scopes: waveform, vectorscope, histogram, RGB parade
- Color management (Rec.709, sRGB, P3, Rec.2020, log profiles)
- Reusable grades and shot matching

### Interface
- Dark, distraction-free theme
- Workspaces: Photo, Edit, Color, Export
- Keyboard shortcuts and a searchable command palette
- Beginner mode with guided tooltips

## Architecture

```
┌──────────────────────────┐        local HTTP / JSON        ┌──────────────────────────┐
│  Kotlin Desktop App      │  <---------------------------> │  Python Engine           │
│  (Compose Multiplatform) │        127.0.0.1:8765          │  (FastAPI + NumPy/OpenCV)│
│  UI, timeline, scopes,   │                                │  Image ops, color math,  │
│  project management      │                                │  LUTs, RAW, FFmpeg       │
└──────────────────────────┘                                └──────────────────────────┘
```

The Kotlin app starts the Python engine as a background process and sends it edit instructions. The engine returns rendered frames, previews, and scope data.

## Tech Stack

| Area | Technology |
|------|-----------|
| Desktop UI | Kotlin + Compose Multiplatform (Desktop) |
| Build system | Gradle (Kotlin DSL) |
| Engine API | Python 3.11+ with FastAPI + Uvicorn |
| Image processing | NumPy, OpenCV, Pillow |
| RAW decoding | rawpy (LibRaw) |
| Color science / LUTs | colour-science, PyOpenColorIO |
| Video decode/encode | FFmpeg (via PyAV or ffmpeg-python) |

## Getting Started

### Requirements
- Windows 10/11, macOS 12+, or Linux
- JDK 17 or newer
- Python 3.11 or newer
- FFmpeg installed and available on your PATH
- Git

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/chromacut.git
cd chromacut
```

### 2. Set up the Python engine

```bash
cd engine
python -m venv .venv

# Windows
.venv\Scripts\activate
# macOS / Linux
source .venv/bin/activate

pip install -r requirements.txt
```

Test the engine on its own:

```bash
uvicorn chromacut_engine.main:app --host 127.0.0.1 --port 8765
```

### 3. Run the Kotlin app

In a new terminal, from the project root:

```bash
cd app
./gradlew run          # macOS / Linux
gradlew.bat run        # Windows
```

### 4. Package an installer

```bash
cd app
./gradlew packageDistributionForCurrentOS
```

## Project Structure

```
chromacut/
├── app/                          # Kotlin desktop application
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/kotlin/
│       ├── Main.kt               # App entry point
│       ├── ui/                   # Screens, timeline, color panels, scopes
│       ├── engine/               # Engine launcher and API client
│       └── model/                # Project and edit data classes
├── engine/                       # Python processing engine
│   ├── requirements.txt
│   └── chromacut_engine/
│       ├── main.py               # FastAPI app
│       ├── image_ops.py          # Exposure, curves, HSL, sharpening
│       ├── color.py              # Color wheels, LUTs, color spaces
│       ├── video.py              # Decode, encode, proxies
│       ├── scopes.py             # Waveform, vectorscope, histogram
│       └── raw.py                # RAW decoding
├── assets/                       # Icons, LUTs, presets
├── docs/                         # Documentation
├── README.md
└── LICENSE
```

## Roadmap

- [ ] Project setup: Kotlin app shell and Python engine skeleton
- [ ] Engine connection (launch, health check, shutdown)
- [ ] Image import, viewer, and basic adjustments
- [ ] Non-destructive edit stack
- [ ] Color wheels, curves, and scopes
- [ ] LUT support and color management
- [ ] Video import, proxies, and timeline
- [ ] Export presets
- [ ] Performance work (caching, GPU acceleration, multiprocessing)
- [ ] Beta release

## Performance Notes

Python is used for flexibility and its strong imaging libraries. For smooth real-time playback of high-resolution video, the project relies on proxy files, frame caching, and vectorized NumPy/OpenCV operations. Heavy hot paths can later be moved to GPU code or native extensions if needed.

## Contributing

Contributions are welcome. Please open an issue to discuss major changes before submitting a pull request.

1. Fork the repository
2. Create a branch: `git checkout -b feature/your-feature`
3. Commit your changes
4. Push and open a pull request

## License

Choose a license (for example MIT) and add a `LICENSE` file to the repository.
