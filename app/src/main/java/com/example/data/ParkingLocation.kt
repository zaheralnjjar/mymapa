package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_locations")
data class ParkingLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val notes: String = "",
    val latitude: Double,
    val longitude: Double,
    val streetNumber: String = "",
    val streetName: String = "",
    val buildingNumber: String = "",
    val mapSnapshotPath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isGeofenceEnabled: Boolean = false
)
