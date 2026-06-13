package com.example.ui

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ParkingLocation
import com.example.location.AddressHelper
import com.example.location.GeofenceManager
import com.example.location.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ParkingViewModel(
    private val db: AppDatabase,
    private val locationTracker: LocationTracker,
    private val geofenceManager: GeofenceManager,
    private val addressHelper: AddressHelper
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val parkingLocations: StateFlow<List<ParkingLocation>> = db.parkingLocationDao().getAllLocations()
        .combine(searchQuery) { locations, query ->
            if (query.isBlank()) {
                locations
            } else {
                locations.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true) ||
                    it.streetName.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    
    fun getSearchQuery(): String = searchQuery.value

    suspend fun getCurrentLocation(): Location? {
        return locationTracker.getCurrentLocation()
    }

    fun saveLocationWithDetails(lat: Double, lng: Double, titleOverride: String? = null, snapshotPath: String = "") {
        viewModelScope.launch {
            val address = addressHelper.getAddressFromLocation(lat, lng)
            val newLoc = ParkingLocation(
                title = titleOverride ?: "موقف محدد",
                latitude = lat,
                longitude = lng,
                streetName = address?.thoroughfare ?: "",
                streetNumber = address?.subThoroughfare ?: "",
                buildingNumber = address?.featureName ?: "",
                mapSnapshotPath = snapshotPath,
                isGeofenceEnabled = false
            )
            db.parkingLocationDao().insertLocation(newLoc)
        }
    }

    fun saveCurrentLocation(titleOverride: String? = null) {
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                saveLocationWithDetails(location.latitude, location.longitude, titleOverride ?: "موقف حالي", "")
            }
        }
    }

    fun updateLocation(location: ParkingLocation) {
        viewModelScope.launch {
            db.parkingLocationDao().updateLocation(location)
            if (location.isGeofenceEnabled) {
                geofenceManager.addGeofence(
                    id = location.id.toString(),
                    lat = location.latitude,
                    lng = location.longitude
                )
            } else {
                geofenceManager.removeGeofence(location.id.toString())
            }
        }
    }

    fun deleteLocation(location: ParkingLocation) {
        viewModelScope.launch {
            db.parkingLocationDao().deleteLocationById(location.id)
            geofenceManager.removeGeofence(location.id.toString())
        }
    }
}

class ParkingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(context)
        val locationTracker = LocationTracker(context)
        val geofenceManager = GeofenceManager(context)
        val addressHelper = AddressHelper(context)
        @Suppress("UNCHECKED_CAST")
        return ParkingViewModel(db, locationTracker, geofenceManager, addressHelper) as T
    }
}
