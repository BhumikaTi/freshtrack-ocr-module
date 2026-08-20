# FreshTrack — Camera & OCR Module (Phase 2 PoC)

Proof-of-concept for the Camera & OCR module of FreshTrack, an Android app
that scans packaged products and extracts expiry dates using on-device OCR.

This repo covers **Member 1's scope only**: camera integration, OCR text
extraction, and image preprocessing. Parsing (turning raw OCR text into a
structured expiry date) and the production UI are handled by other modules.

## What this PoC demonstrates

- Live camera capture (CameraX) with flashlight toggle
- Gallery image upload as an alternate input path
- Adjustable crop box (Google Lens-style) so the user can select just the
  date region before OCR runs
- Image preprocessing (crop, upscale, contrast enhancement) to improve OCR
  accuracy on small or worn print
- EXIF-based orientation correction so sideways/rotated photos are handled
- On-device text extraction via Google ML Kit Text Recognition v2
- Manual text entry fallback, for testing the hand-off point without
  relying on OCR

## Tech stack

- Kotlin, Android Views (XML layouts) — this PoC uses Views for speed of
  prototyping; the production app uses Jetpack Compose, so this module's
  logic (not its UI) is what gets ported over
- CameraX 1.4.0
- Google ML Kit Text Recognition v2 (on-device, no network required)
- Min SDK 26, target/compile SDK 36

## Setup — for teammates testing this on their own device

### 1. Get the code
- Clone this repo, or download it as a ZIP from GitHub and extract it
- Open **Android Studio** → **Open** → select the extracted/cloned
  `OcrTest` folder

### 2. Let Gradle sync
- Android Studio will show a banner: *"Gradle files have changed since
  last sync"* → click **Sync Now**
- This downloads all dependencies automatically (CameraX, ML Kit, etc.) —
  can take a few minutes on first open, especially over slower internet
- If it fails, click **Try Again** once — first attempts sometimes time
  out on slow connections

### 3. Enable Developer Options on your phone
- Go to **Settings → About phone**
- Find **Build number**, tap it **7 times** — you'll see a countdown
  message, then "Developer mode enabled"

### 4. Turn on USB debugging
- Go to **Settings → Developer options** (now visible, usually under
  "System")
- Turn on **USB debugging**

### 5. Connect your phone
- Plug your phone into your laptop via USB
- On your phone, a popup will ask about connection type — choose
  **File Transfer** (not "Charging only")
- Another popup will ask **"Allow USB debugging?"** — tap **Allow**

### 6. Select your device and run
- In Android Studio's top toolbar, click the device dropdown (near the
  green ▶ Run button) — your phone's name should now appear
- Select it, then click the green ▶ **Run** button
- First install can take a minute or two

### 7. Grant camera permission
- On first launch, the app will ask for camera access — tap **Allow**

### Note: must use a physical device
This app **will not work correctly on an emulator** — emulator cameras
either don't work or produce unusable OCR results. A real phone is
required for testing.

### Troubleshooting
- **Build fails on first sync:** try **File → Sync Project with Gradle
  Files** again, or check your internet connection — Gradle needs to
  download several libraries the first time
- **Phone doesn't appear in device dropdown:** unplug and replug the USB
  cable, and check your phone screen for a missed "Allow USB debugging?"
  popup
- **"INSTALL_FAILED" error:** make sure you tapped Allow on the USB
  debugging popup, and that your phone's screen is unlocked during install

## How to test it

1. Launch the app → two options: **Live OCR Scanner** or **Upload Image**
2. **Live scan:** point camera at a product label, tap capture
3. **Upload:** pick an existing photo from gallery
4. Either path opens a crop screen — drag the green box corners to frame
   just the expiry/mfg date text, tap the send button (top right)
5. Extracted text appears in a bottom sheet — tap it to copy to clipboard
6. Alternatively, type text directly into the bottom bar and tap "Use" to
   test the hand-off without relying on OCR

## Known limitations (found during testing)

- OCR sometimes drops thin symbols (`/`, `@`, `#`) on low-quality dot-matrix
  print — this is a model-level limitation, not fixable via preprocessing
- Character confusion on certain fonts: `8`↔`B`, `1`↔`l`/`I`, `0`↔`O`
- Some packaging uses `@` to mark expiry date and `#` to mark manufacture
  date — this convention needs to be handled in parsing, not here
- Date formats vary: `DD/MM/YYYY`, `MM/YY`, or spelled-out months
  (e.g. `25/NOV/27`) — parsing needs to handle multiple patterns
- Accuracy depends heavily on lighting, focus, and packaging condition —
  this matches the risk already identified in Phase 1

See `TESTING.md` for detailed test results.

## Hand-off point for the parsing module

`OcrResultListener.onTextExtracted(rawText: String, source: OcrSource)` is
where raw, unprocessed OCR text becomes available. This is completely
unfiltered — no trimming, regex, or cleanup happens on this side. See
`OcrResultListener.kt` for the interface.

## Project structure

- `ImagePreprocessor.kt` — cropping, upscaling, contrast enhancement,
  EXIF rotation correction
- `OcrTextExtractor.kt` — the actual ML Kit OCR call, shared by both the
  camera and gallery paths
- `ScanTarget.kt` — defines the default crop region
- `ui/CameraScanActivity.kt` — live camera capture screen
- `ui/CropConfirmActivity.kt` — adjustable crop + confirm + manual entry
  screen (both input paths route through here)
- `ui/MainActivity.kt` — entry screen (camera vs. gallery choice)

## Demo video

[Link to be added]