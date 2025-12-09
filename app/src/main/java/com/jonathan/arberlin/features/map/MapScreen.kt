package com.jonathan.arberlin.features.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jonathan.arberlin.R

@Composable
fun MapRoute(
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (isGranted) {
            viewModel.onPermissionGranted()
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    MapScreen(
        uiState = uiState
    )
}

@Composable
fun MapScreen(
    uiState: MapUiState
) {
        Box (
            modifier = Modifier
                .fillMaxSize()
        ) {

            val berlin = LatLng(52.5200, 13.4050)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(berlin, 12f)
            }

            val context = LocalContext.current

            val showUserLocation = uiState.userLocation != null

            val mapProperties = remember(showUserLocation) {
                MapProperties(
                    mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style),
                    isMyLocationEnabled = showUserLocation
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties
            ) {
                uiState.pois.forEach {

                    item -> val poi = item.poi

                    val iconRes = if (item.discovery != null) R.drawable.marker_done else R.drawable.marker_open

                    Marker(
                        state = MarkerState(position = LatLng(poi.latitude, poi.longitude)),
//                        title = poi.name,
//                        snippet = poi.description,
                        icon = BitmapDescriptorFactory.fromResource(iconRes)
                    )
                }
            }

            if (uiState.isLoading) {
                Text("Loading Berlin...")
            }
//            else {
//                Text(
//                    "Found ${uiState.pois.size} places. \nUser at: " +
//                            "${uiState.userLocation?.latitude}, " +
//                            "${uiState.userLocation?.longitude}"
//                )
//            }
        }
}