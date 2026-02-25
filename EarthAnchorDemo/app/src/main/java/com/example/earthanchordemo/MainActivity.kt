package com.example.earthanchordemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
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

    private lateinit var arSceneView: ARSceneView
    private lateinit var placeButton: Button

//    private val PERMISSION_REQUEST_CODE = 100

    // Replace with your coordinates
    private val targetLat = 45.766728
    private val targetLon = 21.226138

    private val modelPath = "models/chair.glb"
    private val altitudeAboveTerrain = 0.0

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        arSceneView = findViewById(R.id.arSceneView)
//        placeButton = findViewById(R.id.placeButton)
//
//        // Configure geospatial BEFORE the session is created (before onResume)
//        arSceneView.configureSession { session, config ->
//            val supported =
//                session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)
//            Log.d("AR_DEBUG", "Geospatial supported: $supported")
//
//            if (supported) {
//                config.geospatialMode = Config.GeospatialMode.ENABLED
//                Log.d("AR_DEBUG", "Geospatial ENABLED in config")
//            }
//        }
//
//        requestPermissionsIfNeeded()
//    }

    // ----------------------------------------------------
    // Permission Handling
    // ----------------------------------------------------

//    private fun requestPermissionsIfNeeded() {
//
//        val requiredPermissions = arrayOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        )
//
//        if (requiredPermissions.all {
//                ContextCompat.checkSelfPermission(this, it) ==
//                        PackageManager.PERMISSION_GRANTED
//            }) {
//
//            startAR()
//
//        } else {
//            ActivityCompat.requestPermissions(
//                this,
//                requiredPermissions,
//                PERMISSION_REQUEST_CODE
//            )
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        // Wait for session to be created
//        arSceneView.postDelayed({
//            arSceneView.session?.let { session ->
//                if (session.config.geospatialMode != Config.GeospatialMode.ENABLED) {
//                    try {
//                        session.pause()
//                        val config = Config(session)
//                        if (session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
//                            config.geospatialMode = Config.GeospatialMode.ENABLED
//                            session.configure(config)
//                            Log.d("AR_DEBUG", "Geospatial configured in onResume")
//                        }
//                        session.resume()
//                    } catch (e: Exception) {
//                        Log.e("AR_DEBUG", "Error configuring geospatial: ${e.message}")
//                    }
//                }
//            }
//        }, 100)
//    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == PERMISSION_REQUEST_CODE &&
//            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
//        ) {
//            startAR()
//        } else {
//            Toast.makeText(
//                this,
//                "Camera & Location permissions are required.",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//    }

    // ----------------------------------------------------
    // AR Setup (ONLY after permissions granted)
    // ----------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arSceneView = findViewById(R.id.arSceneView)
        placeButton = findViewById(R.id.placeButton)

        // Configure geospatial BEFORE the session is created
        arSceneView.configureSession { session, config ->
            val supported = session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)
            Log.d("AR_DEBUG", "Geospatial supported: $supported")

            if (supported) {
                config.geospatialMode = Config.GeospatialMode.ENABLED
                Log.d("AR_DEBUG", "Geospatial ENABLED in config")
            }
            // IMPORTANT: The lambda should implicitly return Unit, config is modified by reference
        }

//        requestPermissionsIfNeeded()
        startAR();
    }

    private fun startAR() {
        Log.d("AR_DEBUG", "Starting AR...")

        // Just monitor the session, DON'T reconfigure here
        arSceneView.onSessionUpdated = { session, _ ->
            Log.d("AR_DEBUG", "Geospatial actual mode: ${session.config.geospatialMode}")

            val earth = session.earth
            if (earth != null) {
                Log.d("AR_DEBUG", "Earth tracking: ${earth.trackingState}")
            } else {
                Log.d("AR_DEBUG", "Earth object is null")
            }
        }

        placeButton.setOnClickListener {
            placeModelOnTerrain()
        }
    }

    // ----------------------------------------------------
    // Place Terrain Anchor
    // ----------------------------------------------------

    private fun placeModelOnTerrain() {

        val session = arSceneView.session
        val earth = session?.earth

        Log.d("AR_DEBUG", "Button pressed")
        Log.d("AR_DEBUG", "Earth: $earth")
        Log.d("AR_DEBUG", "Tracking: ${earth?.trackingState}")

        if (earth?.trackingState == TrackingState.TRACKING) {

            Toast.makeText(this, "Resolving terrain anchor...", Toast.LENGTH_SHORT).show()

            earth.resolveAnchorOnTerrainAsync(
                targetLat,
                targetLon,
                altitudeAboveTerrain,
                0f, 0f, 0f, 1f
            ) { anchor, state ->

                Log.d("AR_DEBUG", "Terrain resolve state: $state")

                when (state) {

                    Anchor.TerrainAnchorState.SUCCESS -> {

                        val anchorNode =
                            AnchorNode(arSceneView.engine, anchor)

                        arSceneView.modelLoader.loadModelAsync(modelPath) { model ->

                            model?.let {

                                val modelNode = ModelNode(
                                    modelInstance = it.instance,
                                    scaleToUnits = 2.0f
                                )

                                anchorNode.addChildNode(modelNode)
                                arSceneView.addChildNode(anchorNode)

                                Toast.makeText(
                                    this,
                                    "Model placed successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    Anchor.TerrainAnchorState.ERROR_UNSUPPORTED_LOCATION ->
                        Toast.makeText(
                            this,
                            "Unsupported location for Geospatial API.",
                            Toast.LENGTH_LONG
                        ).show()

                    Anchor.TerrainAnchorState.ERROR_NOT_AUTHORIZED ->
                        Toast.makeText(
                            this,
                            "Geospatial API not authorized.",
                            Toast.LENGTH_LONG
                        ).show()

                    Anchor.TerrainAnchorState.ERROR_INTERNAL ->
                        Toast.makeText(
                            this,
                            "Internal terrain resolution error.",
                            Toast.LENGTH_LONG
                        ).show()

                    else ->
                        Toast.makeText(
                            this,
                            "Unknown terrain error.",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }

        } else {

            Toast.makeText(
                this,
                "Earth not tracking yet: ${earth?.trackingState}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}