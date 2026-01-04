package com.jonathan.arberlin.features.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jonathan.arberlin.data.local.entity.PoiWithDiscovery
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.jonathan.arberlin.ARBerlinApp
import com.jonathan.arberlin.data.repository.PoiRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiscoveredPoiUiState(
    val id: Long,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val formattedDate: String
)

//class CollectionViewModel(
//    private val repository: PoiRepository
//) : ViewModel() {
//
//    val collectionState: StateFlow<List<DiscoveredPoiUiState>> = repository.getDiscoveredPois()
//        .map { list ->
//            list.map { item -> mapToUiState(item) }
//        }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList()
//        )
//
//
//    fun getPoiDetail(poiId: Long): DiscoveredPoiUiState? {
//        return collectionState.value.find { it.id == poiId }
//    }
//
//
//
//    private fun mapToUiState(item: PoiWithDiscovery): DiscoveredPoiUiState {
//        val discoveryDate = item.discovery?.discoveredAt ?: System.currentTimeMillis()
//
//        return DiscoveredPoiUiState(
//            id = item.poi.id,
//            name = item.poi.name,
//            category = item.poi.category,
//            description = item.poi.description,
//            imageUrl = parseFirstImage(item.poi.imageFiles),
//            formattedDate = formatDate(discoveryDate)
//        )
//    }
//
//    private fun parseFirstImage(imageFiles: String): String {
//        return imageFiles.split(",").firstOrNull()?.trim() ?: ""
//    }
//
//    private fun formatDate(timestamp: Long): String {
//        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
//        return sdf.format(Date(timestamp))
//    }
//}

class CollectionViewModel(
    private val repository: PoiRepository
) : ViewModel() {

    val collectionState: StateFlow<List<DiscoveredPoiUiState>> = repository.getDiscoveredPois()
        .map { list -> list.map { mapToUiState(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPoiDetail(poiId: Long): DiscoveredPoiUiState? {
        return collectionState.value.find { it.id == poiId }
    }

    private fun mapToUiState(item: PoiWithDiscovery): DiscoveredPoiUiState {
        val discoveryDate = item.discovery?.discoveredAt ?: System.currentTimeMillis()

        return DiscoveredPoiUiState(
            id = item.poi.id,
            name = item.poi.name,
            category = item.poi.category,
            description = item.poi.description,
            imageUrl = parseFirstImage(item.poi.imageFiles),
            formattedDate = formatDate(discoveryDate)
        )
    }

    private fun parseFirstImage(imageFiles: String): String {
        val filename = imageFiles.split(",").firstOrNull()?.trim() ?: ""

        if (filename.startsWith("http")) return filename

        return "file:///android_asset/poi_images/$filename"
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ARBerlinApp)
                val repository = application.container.poiRepository

                CollectionViewModel(repository = repository)
            }
        }
    }
}
