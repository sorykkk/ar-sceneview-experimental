package com.example.earthanchordemo

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

class MainActivity : AppCompatActivity() {

    private var arSceneView: ARSceneView? = null
    private lateinit var arContainer: FrameLayout
    private lateinit var placeButton: Button
    private lateinit var debugOverlay: TextView

    private val PERMISSION_REQUEST_CODE = 100
    private val modelPath = "models/chair.glb"
    private val altitudeAboveTerrain = 0.0
    private val PLACEMENT_DISTANCE_M = 1.0 // metres ahead of camera

    private var isEarthTracking = false
    private var lastDebugUpdate = 0L
    private var placedCount = 0
    private var lastPlacedLat = 0.0
    private var lastPlacedLon = 0.0

    // Real GPS from LocationManager
    private lateinit var locationManager: LocationManager
    private var lastGpsLocation: Location? = null

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            lastGpsLocation = loc
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arContainer = findViewById(R.id.arContainer)
        placeButton = findViewById(R.id.placeButton)
        debugOverlay = findViewById(R.id.debugOverlay)
        placeButton.isEnabled = false

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        requestPermissionsIfNeeded()
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
            val pose = earth.cameraGeospatialPose

            // Update debug overlay
            if (now - lastDebugUpdate > 500) {
                lastDebugUpdate = now
                val gps = lastGpsLocation
                val debugText = buildString {
                    append("General debug Info:\n")
                    append("Real GPS: ")
                    if (gps != null) {
                        append("%.6f, %.6f (%.0fm)\n".format(gps.latitude, gps.longitude, gps.accuracy))
                    } else {
                        append("no fix\n")
                    }
                    append("ARCore:   %.6f, %.6f\n".format(pose.latitude, pose.longitude))
                    append("Earth: $trackingState\n")
                    append("H-acc: %.1fm  Alt: %.1fm\n".format(pose.horizontalAccuracy, pose.altitude))
                    if (placedCount > 0) {
                        append("Models placed: $placedCount\n")
                        append("Last: %.6f, %.6f".format(lastPlacedLat, lastPlacedLon))
                    }
                }
                runOnUiThread { debugOverlay.text = debugText }
            }

            // Enable place button when tracking with good accuracy
            val ready = trackingState == TrackingState.TRACKING && pose.horizontalAccuracy < 10
            if (ready != isEarthTracking) {
                isEarthTracking = ready
                runOnUiThread { placeButton.isEnabled = ready }
            }
        }

        placeButton.setOnClickListener { placeModelOnTerrain() }

        arContainer.addView(sceneView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        arSceneView = sceneView
    }

    private fun placeModelOnTerrain() {
        val sceneView = arSceneView ?: return
        val earth = sceneView.session?.earth ?: return

        if (earth.trackingState != TrackingState.TRACKING) {
            Toast.makeText(this, "Earth not tracking yet", Toast.LENGTH_SHORT).show()
            return
        }

        val pose = earth.cameraGeospatialPose
        val (lat, lon) = Pair(pose.latitude, pose.longitude) //offsetByHeading(pose.latitude, pose.longitude, pose.heading, PLACEMENT_DISTANCE_M)

        earth.resolveAnchorOnTerrainAsync(lat, lon, altitudeAboveTerrain, 0f, 0f, 0f, 1f) { anchor, state ->
            runOnUiThread {
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    val anchorNode = AnchorNode(sceneView.engine, anchor)
                    sceneView.modelLoader.loadModelAsync(modelPath) { model ->
                        model?.let {
                            val modelNode = ModelNode(modelInstance = it.instance, scaleToUnits = 2.0f)
                            anchorNode.addChildNode(modelNode)
                            sceneView.addChildNode(anchorNode)
                            placedCount++
                            lastPlacedLat = lat
                            lastPlacedLon = lon
                        }
                    }
                } else {
                    debugOverlay.text = "Terrain error: $state"
                }
            }
        }
    }

    // private fun offsetByHeading(lat: Double, lon: Double, headingDeg: Double, distanceM: Double): Pair<Double, Double> {
    //     val R = 6371000.0
    //     val d = distanceM / R
    //     val bearing = Math.toRadians(headingDeg)
    //     val lat1 = Math.toRadians(lat)
    //     val lon1 = Math.toRadians(lon)
    //     val lat2 = Math.asin(Math.sin(lat1) * Math.cos(d) + Math.cos(lat1) * Math.sin(d) * Math.cos(bearing))
    //     val lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(d) * Math.cos(lat1), Math.cos(d) - Math.sin(lat1) * Math.sin(lat2))
    //     return Pair(Math.toDegrees(lat2), Math.toDegrees(lon2))
    // }
}