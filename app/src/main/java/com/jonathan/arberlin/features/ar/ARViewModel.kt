package com.jonathan.arberlin.features.ar

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.ar.core.Earth
import com.google.ar.core.TrackingState
import com.jonathan.arberlin.ARBerlinApp
import com.jonathan.arberlin.data.local.entity.PoiWithDiscovery
import com.jonathan.arberlin.data.repository.LocationRepository
import com.jonathan.arberlin.data.repository.PoiRepository
import com.jonathan.arberlin.domain.ARScanManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ARUiState(
    val nearbyPois: List<PoiWithDiscovery> = emptyList(),
    val userLocation: Location? = null,
    val scannedPoiId: Long? = null,
    val scanMessage: String? = null
)

class ARViewModel(
    private val poiRepository: PoiRepository,
    private val locationRepository: LocationRepository,
    private val scanManager: ARScanManager
) : ViewModel() {
    private val VISIBILITY_RADIUS_METERS = 50.0

    private var lastUpdateTimestamp = 0L

    private val _selectedPoi = MutableStateFlow<PoiWithDiscovery?>(null)
    val selectedPoi = _selectedPoi.asStateFlow()

    private val _discoveryEvents = MutableSharedFlow<String>()
    val discoveryEvents = _discoveryEvents.asSharedFlow()

    val uiState: StateFlow<ARUiState> = combine(
        poiRepository.getPoisWithStatus(),
        locationRepository.getLocationUpdates()
    ) { allPois, location ->
        val nearby = if (location != null) {
            allPois.filter { item ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    item.poi.latitude, item.poi.longitude,
                    results
                )
                results[0] < VISIBILITY_RADIUS_METERS

            }
        } else {
            emptyList()
        }

        ARUiState(
            nearbyPois = nearby,
            userLocation = location
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ARUiState()
    )

    fun onARFrame(frameTime: Long, earth: Earth?, hitPoiId: Long?) {

        if (earth?.trackingState == TrackingState.TRACKING) {
            val pose = earth.cameraGeospatialPose



            if (pose.horizontalAccuracy < 5.0) {
                if (System.currentTimeMillis() - lastUpdateTimestamp > 1000) {
                    val fixedLocation = Location("AR_VPS_PROVIDER").apply {
                        latitude = pose.latitude
                        longitude = pose.longitude
                        altitude = pose.altitude
                        accuracy = pose.horizontalAccuracy.toFloat()
                        time = System.currentTimeMillis()

                    }

                    locationRepository.provideARLocation(fixedLocation)
                    lastUpdateTimestamp = System.currentTimeMillis()
                }
            }
        }

        if (hitPoiId == null) {
            scanManager.processFrame(null)
            return
        }

        val poi = uiState.value.nearbyPois.find { it.poi.id == hitPoiId } ?: return

        if (poi.discovery != null) {
            val isScanComplete = scanManager.processFrame(hitPoiId)

            if (isScanComplete) {
                selectPoi(poi)
                scanManager.processFrame(null)
            }
        } else {
            val isScanComplete = scanManager.processFrame(hitPoiId)

            if (isScanComplete) {
                markAsDiscovered(hitPoiId)
            }
        }
    }

    fun selectPoi(poi: PoiWithDiscovery) {
        _selectedPoi.value = poi
    }

    fun dismissBottomSheet() {
        _selectedPoi.value = null
    }

    private fun markAsDiscovered(id: Long) {
        viewModelScope.launch {
            poiRepository.markAsDiscovered(id)

            val foundPoi = uiState.value.nearbyPois.find { it.poi.id == id }
            if (foundPoi != null) {
                _discoveryEvents.emit("${foundPoi.poi.name} discovered!")
                selectPoi(foundPoi)
            }
        }
    }

    val scanProgress = scanManager.scanProgress

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ARBerlinApp)
                val container = application.container

                ARViewModel(
                    poiRepository = container.poiRepository,
                    locationRepository = container.locationRepository,
                    scanManager = container.arScanManager
                )
            }
        }
    }
}