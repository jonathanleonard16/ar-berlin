package com.jonathan.arberlin.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PoiWithDiscovery(
    @Embedded val poi: PointOfInterest,

    @Relation(
        parentColumn = "id",
        entityColumn = "poiId"
    )
    val discovery: Discovery?
)
