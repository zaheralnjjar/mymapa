package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ParkingLocation
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ParkingViewModel,
    onEditLocation: (ParkingLocation) -> Unit
) {
    val locations by viewModel.parkingLocations.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var locationActionDialog by remember { mutableStateOf<ParkingLocation?>(null) }

    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var currentLocationLatLng by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(mapViewInstance) {
        if (mapViewInstance != null) {
            val loc = viewModel.getCurrentLocation()
            if (loc != null) {
                currentLocationLatLng = LatLng(loc.latitude, loc.longitude)
                mapViewInstance?.controller?.setCenter(GeoPoint(loc.latitude, loc.longitude))
                mapViewInstance?.controller?.setZoom(15.0)
            }
        }
    }

    if (selectedLatLng != null) {
        AlertDialog(
            onDismissRequest = { selectedLatLng = null },
            title = { Text("موقع جديد") },
            text = { Text("هل تود حفظ هذا الموقع أم الملاحة إليه؟") },
            confirmButton = {
                Button(onClick = {
                    val lat = selectedLatLng!!.latitude
                    val lng = selectedLatLng!!.longitude
                    
                    // Save the location with a customized title
                    viewModel.saveLocationWithDetails(lat, lng, "موقع مخصص", "")
                    Toast.makeText(context, "تم حفظ الموقع", Toast.LENGTH_SHORT).show()
                    selectedLatLng = null
                }) {
                    Text("حفظ الموقع")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    val uri = Uri.parse("google.navigation:q=${selectedLatLng!!.latitude},${selectedLatLng!!.longitude}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    selectedLatLng = null
                }) {
                    Text("ملاحة")
                }
            }
        )
    }

    if (locationActionDialog != null) {
        val loc = locationActionDialog!!
        AlertDialog(
            onDismissRequest = { locationActionDialog = null },
            title = { Text(loc.title) },
            text = { Text("ماذا تود أن تفعل؟") },
            confirmButton = {
                Button(onClick = {
                    val uri = Uri.parse("google.navigation:q=${loc.latitude},${loc.longitude}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    locationActionDialog = null
                }) {
                    Text("ملاحة")
                }
            },
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "موقعي: https://maps.google.com/?q=${loc.latitude},${loc.longitude}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الموقع"))
                            locationActionDialog = null
                        }
                    ) {
                        Text("مشاركة")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = {
                            onEditLocation(loc)
                            locationActionDialog = null
                        }
                    ) {
                        Text("تعديل")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = {
                            viewModel.deleteLocation(loc)
                            locationActionDialog = null
                            Toast.makeText(context, "تم حذف الموقع", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("مواقفي") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.setSearchQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ابحث في المواقف باسم أو شارع أو منطقة...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveCurrentLocation("موقف جديد") }) {
                Icon(Icons.Filled.Add, contentDescription = "حفظ الموقع الحالي")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                // Using OsmDroid open source map inside AndroidView!
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setMultiTouchControls(true)
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(12.0)
                            
                            val centerPoint = if (locations.isNotEmpty()) {
                                GeoPoint(locations.first().latitude, locations.first().longitude)
                            } else {
                                GeoPoint(24.7136, 46.6753) // Riyadh coordinates as default fallback
                            }
                            controller.setCenter(centerPoint)

                            val mReceive = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        selectedLatLng = LatLng(it.latitude, it.longitude)
                                    }
                                    return true
                                }
                            }
                            val overlayEvents = MapEventsOverlay(mReceive)
                            overlays.add(0, overlayEvents)

                            mapViewInstance = this
                        }
                    },
                    update = { mapView ->
                        mapViewInstance = mapView
                        // Keep click action overlay at index 0, clear everything else
                        val clickOverlay = mapView.overlays.firstOrNull()
                        mapView.overlays.clear()
                        if (clickOverlay != null) {
                            mapView.overlays.add(clickOverlay)
                        }

                        // Render markers
                        locations.forEach { loc ->
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(loc.latitude, loc.longitude)
                                title = loc.title
                                snippet = loc.streetName
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { _, _ ->
                                    locationActionDialog = loc
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                        }

                        // Render CURRENT location marker overlay
                        currentLocationLatLng?.let { currentLoc ->
                            val currentMarker = Marker(mapView).apply {
                                position = GeoPoint(currentLoc.latitude, currentLoc.longitude)
                                title = "موقعي الحالي"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(currentMarker)
                        }
                        
                        mapView.invalidate()
                    }
                )

                // Locate Me Floating Action Button inside Map Container Box
                val coroutineScope = rememberCoroutineScope()
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val loc = viewModel.getCurrentLocation()
                            if (loc != null) {
                                currentLocationLatLng = LatLng(loc.latitude, loc.longitude)
                                mapViewInstance?.let { map ->
                                    val geo = GeoPoint(loc.latitude, loc.longitude)
                                    map.controller.animateTo(geo)
                                    map.controller.setZoom(16.0)
                                }
                            } else {
                                Toast.makeText(context, "فشل في الحصول على الموقع الحالي. يرجى تفعيل الـ GPS وصلاحية الموقع.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = "تحديد موقعي الحالي",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) {
                if (locations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد مواقف محفوظة حالياً.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(locations) { location ->
                            LocationGridCard(
                                location = location,
                                onClick = { locationActionDialog = location }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationGridCard(location: ParkingLocation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "موقع",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "موقع محفوظ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = location.title.ifEmpty { "بدون عنوان" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                val streetDisplay = listOf(location.streetNumber, location.streetName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                
                if (streetDisplay.isNotEmpty()) {
                    Text(
                        text = "شارع: $streetDisplay",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (location.buildingNumber.isNotEmpty()) {
                    Text(
                        text = "مبنى: ${location.buildingNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
