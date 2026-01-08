package com.jonathan.arberlin.features.ar

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.filament.LightManager
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.jonathan.arberlin.data.local.entity.PoiWithDiscovery
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.acos
import kotlin.math.sqrt


fun findFocusedPoiId(
    cameraPose: Pose,
    anchors: Map<Long, Node>,
    maxAngleDegrees: Float = 8f,
): Long? {
    val camPos = Position(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())

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
    val selectedPoi by viewModel.selectedPoi.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val context = LocalContext.current

    var toastMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (cameraGranted && locationGranted) {

        }

    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

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
            toastMessage = message
            delay(2000)
            toastMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {


        ARScreen(
            uiState = uiState,
            selectedPoi = selectedPoi,
            onDismissSheet = viewModel::dismissBottomSheet,
            scanProgress = scanProgress,
            onFrame = viewModel::onARFrame
        )

        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(50),
                shadowElevation = 6.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {


                    Text(
                        text = toastMessage ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    uiState: ARUiState,
    selectedPoi: PoiWithDiscovery?,
    onDismissSheet: () -> Unit,
    scanProgress: Float,
    onFrame: (Long, Earth?, Long?) -> Unit
) {

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes = rememberNodes()
    val createdAnchors = remember { mutableStateMapOf<Long, AnchorNode>() }
    val pendingAnchors = remember { mutableSetOf<Long>() }
    val currentModels = remember { mutableMapOf<Long, String>() }

    var isLocalized by remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    LaunchedEffect(selectedPoi) {
        if (selectedPoi != null) {
            scaffoldState.bottomSheetState.expand()
        } else {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    LaunchedEffect(Unit) {
        val light1 = LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                color(1.0f, 1.0f, 1.0f)
                intensity(110_000.0f)
                direction(0.0f, -1.0f, 0.5f)
                castShadows(true)
            }
        )
        val light2 = LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                color(1.0f, 1.0f, 1.0f)
                intensity(110_000.0f)
                direction(0.0f, -1.0f, -0.5f)
                castShadows(true)
            }
        )
        childNodes.add(light1)
        childNodes.add(light2)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        containerColor = Color(0xFFFDFAFF),
        sheetPeekHeight = 50.dp,
        sheetContainerColor = Color(0xFFFDFAFF),
        sheetContentColor = Color(0xFF8527B8),
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFF1D8FF),
            )
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (selectedPoi != null) {
                    PoiDetailsContent(poi = selectedPoi)
                } else {
                    ScanHintContent(vpsStatus = if (isLocalized) "Find the object and scan it (by pointing the camera towards the object)." else "Scan Buildings to refine location.")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                childNodes = childNodes,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                sessionConfiguration = { _, config ->
                    config.geospatialMode = Config.GeospatialMode.ENABLED
                    config.focusMode = Config.FocusMode.AUTO
                    config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },
                onSessionUpdated = { session, frame ->
                    val earth = session.earth

                    val focusedId = findFocusedPoiId(
                        cameraPose = frame.camera.pose,
                        anchors = createdAnchors
                    )

                    if (earth?.trackingState == TrackingState.TRACKING && earth.earthState == Earth.EarthState.ENABLED) {
                        val pose = earth.cameraGeospatialPose
                        val accuracy = pose.horizontalAccuracy
                        val orientationAccuracy = pose.orientationYawAccuracy

                        val isPrecise = accuracy < 5.0 && orientationAccuracy < 10.0



                        if (isPrecise) {

                            isLocalized = true



                            uiState.nearbyPois.forEach { item ->
                                val poiId = item.poi.id

                                val targetModelFile =
                                    if (item.discovery != null) "Checked.glb" else "Open.glb"

                                if (createdAnchors.containsKey(poiId)) {
                                    val currentFile = currentModels[poiId]

                                    if (currentFile != targetModelFile) {
                                        Log.d("AR_DEBUG", "Swapping model file to $targetModelFile")
                                        val anchorNode = createdAnchors[poiId]!!

                                        val oldModelNode =
                                            anchorNode.childNodes.firstOrNull { it is ModelNode } as? ModelNode

                                        if (oldModelNode != null) {

                                            anchorNode.removeChildNode(oldModelNode)
                                            oldModelNode.destroy()

                                            val newModelNode = ModelNode(
                                                modelInstance = modelLoader.createModelInstance(targetModelFile),
                                                scaleToUnits = 1.0f
                                            )
                                            anchorNode.addChildNode(newModelNode)

                                            currentModels[poiId] = targetModelFile
                                        }
                                    }
                                } else {

                                    if (!pendingAnchors.contains(poiId)) {
                                        pendingAnchors.add(poiId)

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
                                                pendingAnchors.remove(poiId)
                                                if (state == Anchor.TerrainAnchorState.SUCCESS && anchor != null) {
                                                    val anchorNode =
                                                        AnchorNode(engine = engine, anchor = anchor)
                                                    Log.d("AR_DEBUG", "Anchor Created!")

                                                    val modelFile = if (item.discovery != null) {
                                                        "Checked.glb"
                                                    } else {
                                                        "Open.glb"
                                                    }


                                                    val modelNode = ModelNode(
                                                        modelInstance = modelLoader.createModelInstance(
                                                            assetFileLocation = modelFile
                                                        ),
                                                        scaleToUnits = 1.0f
                                                    )

                                                    anchorNode.addChildNode(modelNode)
                                                    childNodes.add(anchorNode)
                                                    createdAnchors[poiId] = anchorNode
                                                    currentModels[poiId] = modelFile
                                                }

                                            }

                                            Log.d("AR_DEBUG", "Node added to scene")
                                        } catch (e: Exception) {
                                            Log.e("AR_DEBUG", "Error creating anchor", e)
                                            pendingAnchors.remove(poiId)
                                        }
                                    }
                                }
                            }


                        } else {
                            isLocalized = false

                        }
                    }
                    onFrame(frame.timestamp, session.earth, focusedId)
                }

            )
        }
    }
}


@Composable
fun PoiDetailsContent(poi: PoiWithDiscovery) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = poi.poi.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = poi.poi.description,
            style = MaterialTheme.typography.bodyLarge
        )

    }
}

@Composable
fun ScanHintContent(vpsStatus: String) {
    Column {
        Text(
            "AR Scanner Active",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Text(vpsStatus)
        Spacer(Modifier.height(32.dp))
    }
}