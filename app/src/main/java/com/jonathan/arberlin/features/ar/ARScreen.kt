package com.jonathan.arberlin.features.ar

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.acos
import kotlin.math.sqrt


fun findFocusedPoiId(
    cameraPose: Pose,
    anchors: Map<Long, Node>,
    maxAngleDegrees: Float = 8f,
): Long? {
    val camPos  = Position(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())

    val zAxis = cameraPose.zAxis
    val camForward = Position(-zAxis[0], -zAxis[1], -zAxis[2])

    var closestDist = Float.MAX_VALUE
    var bestMatchId: Long? = null

    anchors.forEach { (id, node) ->
        val nodePos = node.worldPosition
        val toNode = Position(
            nodePos.x - camPos.x,
            nodePos.y - camPos.y,
            nodePos.z - camPos.z
        )

        val dist = sqrt(toNode.x * toNode.x + toNode.y * toNode.y + toNode.z * toNode.z)

        val toNodeNorm = Position(toNode.x / dist, toNode.y / dist, toNode.z / dist)

        val dotProduct = (toNodeNorm.x * camForward.x) +
                (toNodeNorm.y * camForward.y) +
                (toNodeNorm.z * camForward.z)

        val clampedDot = dotProduct.coerceIn(-1f, 1f)
        val angleRad = acos(clampedDot)
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

        if (angleDeg < maxAngleDegrees && dist < closestDist) {
            closestDist = dist
            bestMatchId = id
        }
    }

    return bestMatchId
}


@Composable
fun ARRoute(
    viewModel: ARViewModel = viewModel(factory = ARViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (cameraGranted && locationGranted) {

        }

    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera || !hasLocation) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        viewModel.discoveryEvents.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    ARScreen(
        uiState = uiState,
        scanProgress = scanProgress,
        onFrame = viewModel::onARFrame
    )
}

@Composable
fun ARScreen(
    uiState: ARUiState,
    scanProgress: Float,
    onFrame: (Long, Earth?, Long?) -> Unit
) {

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes = rememberNodes()
    val createdAnchors = remember { mutableStateMapOf<Long, AnchorNode>() }

    var focusedPoiName by remember { mutableStateOf<String?>(null)}

    var vpsStatusMessage by remember { mutableStateOf("Waiting for VPS...") }
    var isLocalized by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            sessionConfiguration = { session, config ->
                config.geospatialMode = Config.GeospatialMode.ENABLED
                config.focusMode = Config.FocusMode.AUTO
                config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
            },
            onSessionUpdated = { session, frame ->
                val earth = session.earth

                val focusedId = findFocusedPoiId(
                    cameraPose = frame.camera.pose,
                    anchors = createdAnchors
                )

                if (focusedId != null) {
                    val poi = uiState.nearbyPois.find {it.poi.id == focusedId}
                    focusedPoiName = poi?.poi?.name
                } else {
                    focusedPoiName = null
                }

                if (earth?.trackingState == TrackingState.TRACKING && earth.earthState == Earth.EarthState.ENABLED) {
                    val pose = earth.cameraGeospatialPose
                    val accuracy = pose.horizontalAccuracy
                    val orientationAccuracy = pose.orientationYawAccuracy

                    val isPrecise = accuracy < 5.0 && orientationAccuracy < 10.0



                    if (isPrecise) {

                        isLocalized = true
                        vpsStatusMessage = "VPS Locked! (Err: ${accuracy.toInt()}m)"



                        uiState.nearbyPois.forEach { item ->
                            val poiId = item.poi.id

                            if (!createdAnchors.containsKey(poiId)) {
                                Log.d(
                                    "AR_DEBUG",
                                    "Attempting to create anchor for ${item.poi.name}"
                                )
                                try {

                                    earth.resolveAnchorOnTerrainAsync(
                                        item.poi.latitude,
                                        item.poi.longitude,
                                        0.0,
                                        0f, 0f, 0f, 1f
                                    ) { anchor, state ->
                                        if (state == Anchor.TerrainAnchorState.SUCCESS && anchor != null) {
                                            val anchorNode =
                                                AnchorNode(engine = engine, anchor = anchor)
                                            Log.d("AR_DEBUG", "Anchor Created!")
                                            val modelNode = ModelNode(
                                                modelInstance = modelLoader.createModelInstance(
                                                    assetFileLocation = "Box.glb"
                                                ),
                                                scaleToUnits = 1.0f
                                            )
                                            anchorNode.addChildNode(modelNode)
                                            childNodes.add(anchorNode)
                                            createdAnchors[poiId] = anchorNode
                                        }

                                    }

                                    Log.d("AR_DEBUG", "Node added to scene")
                                } catch (e: Exception) {
                                    Log.e("AR_DEBUG", "Error creating anchor", e)
                                }
                            }

//                        if (!createdAnchors.containsKey(9999L)) { // Unique ID
//                            val cameraPose = earth.cameraGeospatialPose
//                            // Create anchor 0.0001 degrees lat away (~10 meters) or just use camera position?
//                            // Actually, let's use a standard AR plane anchor for a quick test,
//                            // OR just offset the lat/lng slightly.
//
//                            val testAnchor = earth.createAnchor(
//                                52.5574944, // ~5 meters North
//                                13.3591937,
//                               87.3,
//                                0f, 0f, 0f, 1f
//                            )
//
//                            val testNode = AnchorNode(engine, anchor = testAnchor)
//                            val boxNode = ModelNode(
//                                modelInstance = modelLoader.createModelInstance("Box.glb"),
//                                scaleToUnits = 1.0f
//                            ).apply { position = Position(0f, 0f, 0f) }
//
//                            testNode.addChildNode(boxNode)
//                            childNodes.add(testNode)
//                            createdAnchors[9999L] = testNode
//
//                            Log.d("AR_DEBUG", "TEST BOX CREATED AT YOUR FEET!")
//                        }
                        }

                    } else {
                        isLocalized = false
                        vpsStatusMessage = "Scan buildings to refine location...\\n(Current Err: ${accuracy.toInt()}m)"

                    }
                }
                onFrame(frame.timestamp, session.earth, focusedId)
            }

        )

        Column(modifier = Modifier
            .padding(16.dp)
            .align(Alignment.TopStart)
            .background(if (isLocalized) Color.Green.copy(alpha=0.6f) else Color.Red.copy(alpha=0.6f), RoundedCornerShape(8.dp))
            .padding(8.dp))
        {


            // Status Box
//            Surface (
//                color = if (isLocalized) Color.Green.copy(alpha=0.6f) else Color.Red.copy(alpha=0.6f),
//                shape = RoundedCornerShape(8.dp)
//            ) {
                Text(
                    text = vpsStatusMessage,
                    color = Color.White,

                )
                val loc = uiState.userLocation
                if (loc != null) {

                    Text("Source: ${loc.provider}", color = Color.White)
                    Text("Lat: ${loc.latitude}", color = Color.White)
                    Text("Lng: ${loc.longitude}", color = Color.White)
                    Text("Alt: ${loc.altitude}m", color = Color.White)
                    Text("Acc: ${loc.accuracy}m", color = Color.White)
                } else {
                    Text("Location: NULL (Waiting...)", color = Color.Yellow)
                }
                Text(
                    text = "Nearby POIs: ${uiState.nearbyPois.size}",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
                uiState.nearbyPois.forEach { item ->
                    // Calculate distance for debug
                    val dist = if (loc != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            loc.latitude, loc.longitude,
                            item.poi.latitude, item.poi.longitude,
                            results
                        )
                        "${results[0].toInt()}m"
                    } else "?"

                    Text(
                        text = "- ${item.poi.name} ($dist)",
                        color = Color.White,
                        fontSize = 12.sp
                    )
//                }
                }
            Spacer(modifier = Modifier.height(8.dp))
            if (focusedPoiName != null) {
                Text(
                    text = "Target Acquired",
                    color = Color.Green,

                )
                Text(
                    text = "Focusing: $focusedPoiName",
                    color = Color.White
                )
            } else {
                Text(
                    text = "Scanning...",
                    color = Color.Gray
                )
            }
        }


    }
}