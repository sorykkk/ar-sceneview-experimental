package com.example.fixedearthanchordemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import java.util.Properties

class MainActivity : AppCompatActivity() {

    private var arSceneView: ARSceneView? = null
    private lateinit var arContainer: FrameLayout
    private lateinit var debugOverlay: TextView

    private val PERMISSION_REQUEST_CODE = 100
    private val modelPath = "models/coloana.glb"
    private val altitudeAboveTerrain = 0.0
    private var lastDebugUpdate = 0L

    private var constLat = 0.0
    private var constLon = 0.0

    // Real GPS from LocationManager
    private lateinit var locationManager: LocationManager
    private var lastGpsLocation: Location? = null

    // Target location for distance calculation
    private lateinit var targetLocation: Location

    // Track if model has been placed
    private var modelPlaced = false
    private var readyToPlace = false

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            lastGpsLocation = loc
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadConfig()

        arContainer = findViewById(R.id.arContainer)
        debugOverlay = findViewById(R.id.debugOverlay)

        findViewById<Button>(R.id.placeButton).setOnClickListener {
            if (modelPlaced) {
                Toast.makeText(this, "Model already placed", Toast.LENGTH_SHORT).show()
            } else if (!readyToPlace) {
                Toast.makeText(this, "Waiting for high accuracy tracking...", Toast.LENGTH_SHORT).show()
            } else {
                modelPlaced = true
                placeModelOnTerrain()
            }
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        requestPermissionsIfNeeded()
    }

    private fun loadConfig() {
        val props = Properties()
        assets.open("config.properties").use { props.load(it) }
        constLat = props.getProperty("CONST_LAT", "0.0").toDouble()
        constLon = props.getProperty("CONST_LON", "0.0").toDouble()
        targetLocation = Location("target").apply {
            latitude = constLat
            longitude = constLon
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { locationManager.removeUpdates(gpsListener) } catch (_: Exception) {}
        arSceneView?.destroy()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            onPermissionsReady()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionsReady()
            } else {
                Toast.makeText(this, "Camera and Location permissions are required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onPermissionsReady() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 0f, gpsListener, Looper.getMainLooper()
            )
        }
        createAR()
    }

    private fun createAR() {
        val sceneView = ARSceneView(this)

        sceneView.configureSession { session, config ->
            if (session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
                config.geospatialMode = Config.GeospatialMode.ENABLED
            }
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        }

        sceneView.onSessionUpdated = onSessionUpdated@{ session, frame ->
            val earth = session.earth ?: return@onSessionUpdated
            val now = System.currentTimeMillis()
            val trackingState = earth.trackingState

            // Update readiness once Earth is tracking with high accuracy
            if (!modelPlaced && trackingState == TrackingState.TRACKING) {
                val pose = earth.cameraGeospatialPose
                readyToPlace = pose.horizontalAccuracy < 1.0 && pose.orientationYawAccuracy < 10.0
            }

            // Update debug overlay with distance calculation
            if (now - lastDebugUpdate > 500) {
                lastDebugUpdate = now
                val gps = lastGpsLocation

                // Calculate distance from GPS location
                val gpsDistance = gps?.let {
                    val currentLocation = Location("current").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                    currentLocation.distanceTo(targetLocation)
                }

                // Only access pose when Earth is tracking
                val pose = if (trackingState == TrackingState.TRACKING) earth.cameraGeospatialPose else null

                val arCoreDistance = pose?.let {
                    val arCoreLocation = Location("arcore").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                    arCoreLocation.distanceTo(targetLocation)
                }

                val debugText = buildString {
                    append("=== DISTANCE TO MODEL ===\n")
                    if (gpsDistance != null) {
                        append("GPS Distance:    ${formatDistance(gpsDistance)}\n")
                    } else {
                        append("GPS Distance:    waiting...\n")
                    }
                    if (arCoreDistance != null) {
                        append("ARCore Distance: ${formatDistance(arCoreDistance)}\n")
                    } else {
                        append("ARCore Distance: waiting...\n")
                    }
                    append("\n=== LOCATION DATA ===\n")
                    append("Real GPS: ")
                    if (gps != null) {
                        append("%.6f, %.6f\n".format(gps.latitude, gps.longitude))
                        append("GPS Accuracy: ±%.0fm\n".format(gps.accuracy))
                    } else {
                        append("no fix\n")
                    }
                    if (pose != null) {
                        append("ARCore:   %.6f, %.6f\n".format(pose.latitude, pose.longitude))
                    } else {
                        append("ARCore:   waiting...\n")
                    }
                    append("Target:   %.6f, %.6f\n".format(constLat, constLon))
                    append("\n=== AR STATUS ===\n")
                    append("Earth: $trackingState\n")
                    if (pose != null) {
                        append("H-acc: %.1fm  Alt: %.1fm\n".format(pose.horizontalAccuracy, pose.altitude))
                    }
                    append("Model placed: $modelPlaced\n")
                }
                runOnUiThread { debugOverlay.text = debugText }
            }
        }

        arContainer.addView(sceneView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        arSceneView = sceneView
    }

    private fun formatDistance(meters: Float): String {
        return when {
            meters < 1 -> "%.0f cm".format(meters * 100)
            meters < 1000 -> "%.1f m".format(meters)
            else -> "%.2f km".format(meters / 1000)
        }
    }

    private fun placeModelOnTerrain() {
        val sceneView = arSceneView ?: return
        val earth = sceneView.session?.earth ?: return

        if (earth.trackingState != TrackingState.TRACKING) {
            Log.w("ARCORE", "Earth not tracking yet")
            return
        }

        earth.resolveAnchorOnTerrainAsync(constLat, constLon, altitudeAboveTerrain, 0f, 0f, 0f, 1f) { anchor, state ->
            runOnUiThread {
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    val anchorNode = AnchorNode(sceneView.engine, anchor)
                    sceneView.modelLoader.loadModelAsync(modelPath) { model ->
                        runOnUiThread {
                            model?.let {
                                val modelNode = ModelNode(modelInstance = it.instance, scaleToUnits = 4.0f)
                                anchorNode.addChildNode(modelNode)
                                sceneView.addChildNode(anchorNode)
                                Toast.makeText(this, "Model placed at target location!", Toast.LENGTH_SHORT).show()
                                Log.i("ARCORE", "Model successfully placed at $constLat, $constLon")
                            }
                        }
                    }
                } else {
                    val errorMsg = "Terrain anchor failed: $state"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("ARCORE", errorMsg)
                }
            }
        }
    }
}