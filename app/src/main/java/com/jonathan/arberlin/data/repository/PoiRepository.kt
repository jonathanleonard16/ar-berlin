package com.jonathan.arberlin.data.repository

import com.jonathan.arberlin.data.local.database.PoiDao
import com.jonathan.arberlin.data.local.entity.Discovery
import com.jonathan.arberlin.data.local.entity.PoiWithDiscovery
import com.jonathan.arberlin.data.local.entity.PointOfInterest
import kotlinx.coroutines.flow.Flow

interface PoiRepository {
    fun getPoisWithStatus():  Flow<List<PoiWithDiscovery>>
    suspend fun getPoiById(id:Long): PointOfInterest?

    suspend fun markAsDiscovered(poiId: Long)

    fun getDiscoveredPois(): Flow<List<PoiWithDiscovery>>

}

class PoiRepositoryImpl(private val dao: PoiDao): PoiRepository {

    override fun getPoisWithStatus(): Flow<List<PoiWithDiscovery>> {
        return dao.getPoisWithStatus()
    }

    override suspend fun getPoiById(id: Long): PointOfInterest? {
        return dao.getPoiById(id)
    }

    override suspend fun markAsDiscovered(poiId: Long) {
        val discovery = Discovery(
            poiId = poiId,
            discoveredAt = System.currentTimeMillis()
        )
        dao.insertDiscovery(discovery)
    }

    override fun getDiscoveredPois(): Flow<List<PoiWithDiscovery>> {
        return dao.getDiscoveredPois()
    }
}