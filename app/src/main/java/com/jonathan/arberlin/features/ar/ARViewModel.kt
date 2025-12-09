package com.jonathan.arberlin.features.ar

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jonathan.arberlin.ARBerlinApp
import com.jonathan.arberlin.data.local.entity.PoiWithDiscovery
import com.jonathan.arberlin.data.repository.LocationRepository
import com.jonathan.arberlin.data.repository.PoiRepository
import com.jonathan.arberlin.domain.ARScanManager
import com.jonathan.arberlin.features.map.MapViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val VISIBILITY_RADIUS_METERS = 20.0

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

    fun onARFrame(hitPoiId: Long?) {
        val isScanComplete = scanManager.processFrame(hitPoiId)

        if (isScanComplete && hitPoiId != null) {
            markAsDiscovered(hitPoiId)
        }
    }

    private fun markAsDiscovered(id: Long) {
        viewModelScope.launch {
            poiRepository.markAsDiscovered(id)
            // TODO: Show success message with Toast
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