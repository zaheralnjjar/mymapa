package com.example.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class AddressHelper(private val context: Context) {
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Address? {
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // We need to use suspendCancellableCoroutine for the listener API on T+
                    // but to keep it simple and synchronous in flow we can rely on standard synchronous blocking under IO dispatcher for older and newer? 
                    // Actually, Geocoder.getFromLocation blocking version is deprecated in API 33 but still works.
                    // Let's use the blocking one since it's within Dispatchers.IO
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}
