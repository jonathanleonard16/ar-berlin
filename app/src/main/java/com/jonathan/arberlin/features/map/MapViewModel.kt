package com.jonathan.arberlin.features.map

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MapUiState(
    val pois: List<PoiWithDiscovery> = emptyList(),
    val userLocation: Location? = null,
    val isLoading: Boolean = true
)


class MapViewModel(
    private val poiRepository: PoiRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val permissionGranted = MutableStateFlow(false)
    fun onPermissionGranted() {
        permissionGranted.value = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MapUiState> = permissionGranted
        .flatMapLatest { hasPermission ->
            if (hasPermission) {
                combine(
                    poiRepository.getPoisWithStatus(),
                    locationRepository.getLocationUpdates()
                ) { poisList, location ->
                    MapUiState(
                        pois = poisList,
                        userLocation = location,
                        isLoading = false
                    )
                }

            } else {
                poiRepository.getPoisWithStatus().map { poisList ->
                    MapUiState(
                        pois = poisList,
                        userLocation = null,
                        isLoading = true // Still waiting for GPS
                    )
                }

            }

        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MapUiState()
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ARBerlinApp)
                val container = application.container

                MapViewModel(
                    poiRepository = container.poiRepository,
                    locationRepository = container.locationRepository
                )
            }
        }
    }
}
