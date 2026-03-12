# EarthAnchorDemo

An Android AR demo that lets you **interactively place a 3D model** on the terrain at your current location. It uses ARCore's Geospatial API with terrain anchoring to resolve a stable anchor, and provides a real-time debug overlay comparing raw GPS data with ARCore's geospatial pose.

## Features

- **Interactive placement** - press a button to drop a model at your current ARCore-estimated position
- **Terrain-aware anchoring** - uses `resolveAnchorOnTerrainAsync` so the model sits on the ground surface
- **Live debug overlay** showing:
  - Raw GPS coordinates and accuracy
  - ARCore geospatial pose (latitude, longitude, altitude)
  - Earth tracking state and horizontal accuracy
  - Number of models placed and their coordinates
- **Geospatial readiness gate** - the placement button is enabled only when Earth tracking is active and horizontal accuracy is below 10 m

## How It Works

1. The app requests Camera and Location permissions on launch.
2. An `ARSceneView` is created with `GeospatialMode.ENABLED` and HDR light estimation.
3. A `LocationManager` listener provides raw GPS fixes for comparison.
4. On each AR frame update, the debug overlay refreshes with the latest pose and GPS data.
5. When the user taps **Place**, the app calls `resolveAnchorOnTerrainAsync` at the camera's geospatial position, loads `chair.glb`, and attaches it to the resolved anchor.

## Prerequisites

- Android Studio Ladybug or newer
- JDK 21
- An ARCore-supported physical Android device
- A Google Cloud project with the [ARCore API](https://console.cloud.google.com/apis/library/arcore.googleapis.com) enabled

## Setup

1. Clone the repository and open the `EarthAnchorDemo` folder in Android Studio.
2. Add your ARCore API key to `local.properties`:
   ```properties
   ARCORE_API_KEY=YOUR_API_KEY_HERE
   ```
3. Sync Gradle, connect your device, and run the app.

## Project Structure

```
EarthAnchorDemo/
├── app/src/main/
│   ├── AndroidManifest.xml       # Permissions & ARCore metadata
│   ├── assets/models/chair.glb   # 3D model
│   ├── java/.../MainActivity.kt  # AR session, placement logic, debug overlay
│   └── res/                      # Layouts & resources
├── build.gradle.kts
├── gradle/libs.versions.toml
└── local.properties              # ARCORE_API_KEY (not committed)
```

## Dependencies

| Library | Purpose |
|---|---|
| SceneView AR (`arsceneview:2.3.3`) | AR rendering and model loading |
| Google Play Services Location | Raw GPS access |
| Jetpack Compose + Material 3 | UI toolkit |