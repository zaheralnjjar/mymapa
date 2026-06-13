package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingLocationDao {
    @Query("SELECT * FROM parking_locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<ParkingLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: ParkingLocation): Long

    @Update
    suspend fun updateLocation(location: ParkingLocation)

    @Query("DELETE FROM parking_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    @Query("SELECT * FROM parking_locations WHERE id = :id")
    suspend fun getLocationById(id: Int): ParkingLocation?
}
