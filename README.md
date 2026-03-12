# ARCore & SceneView Experimental

An experimental repository for exploring the capabilities of [ARCore](https://developers.google.com/ar) and [SceneView](https://github.com/SceneView/sceneview-android) on Android. The focus is on placing 3D models in the real world using geospatial anchors and terrain-aware placement.

## Projects

| Project | Description |
|---|---|
| [EarthAnchorDemo](EarthAnchorDemo) | Interactive placement of a 3D model on a detected surface. Useful for studying ARCore Earth tracking accuracy and model placement stability. |
| [FixedEarthAnchorDemo](FixedEarthAnchorDemo) | Automatic placement of a 3D model at fixed GPS coordinates using the ARCore Geospatial API with terrain anchoring. Includes a real-time debug overlay showing GPS vs. ARCore distance to the target. |

## Tech Stack

| Technology | Version |
|---|---|
| Kotlin | 2.0.21 |
| AGP | 9.0.1 |
| SceneView (AR) | 2.3.3 |
| Min SDK | 24 |
| Target SDK | 36 |
| Jetpack Compose BOM | 2024.09.00 |

## Prerequisites

- **Android Studio** Ladybug or newer
- **JDK 21**
- **An ARCore-supported Android device** — the emulator does not support ARCore geospatial features
- **A Google Cloud project** with the [ARCore API](https://console.cloud.google.com/apis/library/arcore.googleapis.com) enabled and an API key generated

## Repository Structure

```
ar-sceneview-experimental/
├── EarthAnchorDemo/          # Interactive surface-placement demo
│   ├── app/src/main/
│   │   ├── assets/models/    # 3D model (chair.glb)
│   │   └── java/…            # Kotlin source
│   └── local.properties      # API key (not committed)
├── FixedEarthAnchorDemo/     # Fixed-coordinate placement demo
│   ├── app/src/main/
│   │   ├── assets/
│   │   │   ├── config.properties  # Target latitude & longitude
│   │   │   └── models/            # 3D models (coloana.glb, chair.glb)
│   │   └── java/…                 # Kotlin source
│   └── local.properties           # API key (not committed)
└── README.md
```