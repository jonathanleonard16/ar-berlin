package com.jonathan.arberlin.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jonathan.arberlin.data.local.entity.Discovery
import com.jonathan.arberlin.data.local.entity.PoiWithDiscovery
import com.jonathan.arberlin.data.local.entity.PointOfInterest
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {
    @Query("SELECT * FROM points_of_interest")
    fun getAllPois(): Flow<List<PointOfInterest>>

    @Query("SELECT * FROM points_of_interest WHERE id = :id")
    suspend fun getPoiById(id: Long): PointOfInterest?

    @Transaction
    @Query("SELECT * FROM points_of_interest")
    fun getPoisWithStatus(): Flow<List<PoiWithDiscovery>>

    @Insert
    suspend fun insertDiscovery(discovery: Discovery)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pois: List<PointOfInterest>)
}