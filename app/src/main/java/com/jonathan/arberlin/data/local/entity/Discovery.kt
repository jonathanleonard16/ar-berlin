package com.jonathan.arberlin.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "discovery",
    foreignKeys = [
        ForeignKey(
            entity = PointOfInterest::class,
            parentColumns = ["id"],
            childColumns = ["poiId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Discovery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val discoveredAt: Long,
    val poiId: Long,
)
