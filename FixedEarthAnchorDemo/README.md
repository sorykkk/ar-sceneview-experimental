# FixedEarthAnchorDemo

An Android AR demo that places a **3D model at fixed GPS coordinates** using the ARCore Geospatial API with terrain anchoring. The app shows a live debug overlay with real-time distance to the target (from both raw GPS and ARCore), making it useful for studying ARCore geospatial accuracy and terrain anchor behaviour.

## Features

- **Fixed-coordinate placement** - the model is anchored at predefined latitude/longitude, not at the user's position
- **Terrain-aware anchoring** - uses `resolveAnchorOnTerrainAsync` so the model sits on the actual ground surface
- **Configurable coordinates** - target location is stored in a plain-text config file, easy to change without recompiling
- **Distance overlay** showing GPS distance and ARCore distance to the target in real time
- **Accuracy gate** - placement is only allowed when horizontal accuracy < 1.5 m and yaw accuracy < 10°

## How It Works

1. On launch, coordinates are loaded from `app/src/main/assets/config.properties`.
2. Camera and Location permissions are requested.
3. An `ARSceneView` is created with `GeospatialMode.ENABLED` and HDR light estimation.
4. The debug overlay continuously updates with GPS/ARCore positions and distance to the target.
5. When the user taps **Place** and tracking accuracy is sufficient, `resolveAnchorOnTerrainAsync` is called at the configured coordinates, and the `coloana.glb` model is loaded and attached to the anchor.

---

## Getting Started (Android Studio)

### Prerequisites

| Requirement | Details |
|---|---|
| Android Studio | Ladybug (2024.3) or newer |
| JDK | 21 |
| Android device | Physical device with [ARCore support](https://developers.google.com/ar/devices) - the emulator does not support geospatial features |
| Google Cloud | A project with the [ARCore API](https://console.cloud.google.com/apis/library/arcore.googleapis.com) enabled and an API key |

### Step 1 - Open the project

1. Open Android Studio.
2. Select **File -> Open** and navigate to the `FixedEarthAnchorDemo` folder inside this repository.
3. Wait for Gradle sync to complete (Android Studio will prompt you automatically).

### Step 2 - Set your ARCore API key

The API key is read from `local.properties` in the project root and injected into the Android manifest at build time.

1. Open (or create) the file `FixedEarthAnchorDemo/local.properties`.
2. Add the following line (replace the placeholder with your actual key):
   ```properties
   ARCORE_API_KEY=YOUR_API_KEY_HERE
   ```
3. Save the file. **Do not commit this file to version control** - it is already listed in `.gitignore`.

> **How to get an API key:**
> 1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
> 2. Create or select a project.
> 3. Enable the **ARCore API** under **APIs & Services -> Library**.
> 4. Go to **APIs & Services -> Credentials** and create an **API key**.
> 5. (Recommended) Restrict the key to the ARCore API and your app's package name.

### Step 3 - Set the target coordinates

The GPS coordinates where the 3D model will be placed are stored in:

```
FixedEarthAnchorDemo/app/src/main/assets/config.properties
```

Edit this file and set `CONST_LAT` and `CONST_LON` to your desired location:

```properties
# Target location for model placement
CONST_LAT=45.749006201233456
CONST_LON=21.241457366533247
```

Use decimal degrees (WGS 84). You can get coordinates from [Google Maps](https://maps.google.com) by right-clicking any point and selecting the coordinates.

### Step 4 - Build and run

1. Connect your ARCore-supported Android device via USB (enable **USB Debugging** in Developer Options).
2. Select your device in the toolbar device selector.
3. Click **Run** (or press `Shift + F10`).
4. On the device, grant Camera and Location permissions when prompted.
5. Point the camera at your surroundings and wait for the debug overlay to show **Earth: TRACKING** with low horizontal accuracy.
6. Tap the **Place** button - the model will appear at the configured coordinates.

---

## Project Structure

```
FixedEarthAnchorDemo/
├── app/src/main/
│   ├── AndroidManifest.xml            # Permissions & ARCore metadata
│   ├── assets/
│   │   ├── config.properties          # Target latitude & longitude
│   │   └── models/
│   │       ├── coloana.glb            # Default 3D model
│   │       └── chair.glb              # Alternative 3D model
│   ├── java/.../MainActivity.kt       # AR session, placement, debug overlay
│   └── res/                           # Layouts & resources
├── build.gradle.kts
├── gradle/libs.versions.toml
└── local.properties                   # ARCORE_API_KEY (not committed)
```

## Dependencies

| Library | Purpose |
|---|---|
| SceneView AR (`arsceneview:2.3.3`) | AR rendering and model loading |
| Google Play Services Location | Raw GPS access |
| Jetpack Compose + Material 3 | UI toolkit |