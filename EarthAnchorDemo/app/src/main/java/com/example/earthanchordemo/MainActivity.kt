package com.example.earthanchordemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

    // Replace with your specific real-world coordinates
    private val targetLat = 45.7489
    private val targetLon = 21.2087

    private val modelPath = "models/chair.glb"

    // 0.0 means the model will sit exactly on the ground.
    // 1.5 would make it float 1.5 meters above the ground.
    private val altitudeAboveTerrain = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arSceneView = findViewById(R.id.arSceneView)
        placeButton = findViewById(R.id.placeButton)

        // 1. Request Location Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        // 2. Enable Geospatial Mode
        arSceneView.configureSession { _, config ->
            config.geospatialMode = Config.GeospatialMode.ENABLED
        }

        // 3. Set up button interaction
        placeButton.setOnClickListener {
            placeModelOnTerrain()
        }
    }

    private fun placeModelOnTerrain() {
        val earth = arSceneView.session?.earth

        // Ensure ARCore is actively tracking the real world
        if (earth?.trackingState == TrackingState.TRACKING) {

            Toast.makeText(this, "Finding ground level...", Toast.LENGTH_SHORT).show()

            // 4. Asynchronously resolve the Terrain Anchor
            // The last four numbers (0f, 0f, 0f, 1f) are a Quaternion representing no rotation
            earth.resolveAnchorOnTerrainAsync(
                targetLat,
                targetLon,
                altitudeAboveTerrain,
                0f, 0f, 0f, 1f
            ) { anchor, state ->

                // 5. This block runs when ARCore finishes calculating the terrain
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    // Create an AnchorNode attached to the Geospatial Terrain Anchor
                    val anchorNode = AnchorNode(arSceneView.engine, anchor);
                    // Asynchronously load the .glb model
                    arSceneView.modelLoader.loadModelAsync(modelPath) { model ->
                        model?.let {
                            // Create the ModelNode with the loaded 3D instance
                            val modelNode = ModelNode(
                                modelInstance = it.instance,
                                scaleToUnits = 2.0f // Adjust if it is too big/small
                            )
                            // Attach the Model to the Anchor, and the Anchor to the Scene
                            anchorNode.addChildNode(modelNode)
                            arSceneView.addChildNode(anchorNode)

                            Toast.makeText(
                                this@MainActivity,
                                "Model placed on the ground!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to resolve terrain: $state", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Calibrating Location... Look around at buildings.", Toast.LENGTH_LONG).show()
        }
    }
}