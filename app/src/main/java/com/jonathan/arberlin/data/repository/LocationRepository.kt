package com.jonathan.arberlin.data.repository


import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform


interface LocationRepository {
    fun getLocationUpdates(): Flow<Location?>
    fun provideARLocation(location: Location)
}

class LocationRepositoryImpl(private val context: Context): LocationRepository {

    private val fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val arLocationSource = MutableSharedFlow<Location>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun getLocationUpdates(): Flow<Location?> {

        val gpsFlow = callbackFlow {

            val locationRequest = LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    trySend(result.locations.lastOrNull())
                }
            }


            try {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                close(e)
            }

            awaitClose {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }

        return merge(gpsFlow,arLocationSource)
            .transform { location ->
                emit(location)
            }
            .scan<Location?, Pair<Location?, Long>>(null to 0L) { state,newLocation ->
                val (lastEmitted, lastARTime) = state
                if (newLocation == null) return@scan state
                val isARUpdate = newLocation.provider == "AR_VPS_PROVIDER"
                val currentTime = System.currentTimeMillis()

                if (isARUpdate) {
                    newLocation to currentTime
                } else {
                    val isARStale = (currentTime - lastARTime) > 3000
                    if (isARStale) {
                        newLocation to lastARTime
                    } else {
                        lastEmitted to lastARTime
                    }
                }
            }
            .map { it.first }
            .filterNotNull()
            .distinctUntilChanged()
    }

    override fun provideARLocation(location: Location) {
        arLocationSource.tryEmit(location)
    }
}