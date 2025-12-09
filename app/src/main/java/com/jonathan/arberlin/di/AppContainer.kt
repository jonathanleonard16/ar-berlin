package com.jonathan.arberlin.di

import android.content.Context
import com.jonathan.arberlin.data.local.database.AppDatabase
import com.jonathan.arberlin.data.repository.LocationRepository
import com.jonathan.arberlin.data.repository.LocationRepositoryImpl
import com.jonathan.arberlin.data.repository.PoiRepository
import com.jonathan.arberlin.data.repository.PoiRepositoryImpl
import com.jonathan.arberlin.domain.ARScanManager

interface AppContainer {
    val poiRepository: PoiRepository
    val locationRepository: LocationRepository
    val arScanManager: ARScanManager
}

class DefaultAppContainer(private val context: Context): AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabaseInstance(context)
    }

    override val poiRepository: PoiRepository by lazy {
        PoiRepositoryImpl(database.poiDao())
    }

    override val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(context)
    }

    override val arScanManager: ARScanManager by lazy {
        ARScanManager()
    }

}