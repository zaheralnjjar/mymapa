package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ParkingLocation
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationScreen(
    viewModel: ParkingViewModel,
    locationId: Int,
    onNavigateBack: () -> Unit
) {
    val locations by viewModel.parkingLocations.collectAsStateWithLifecycle()
    val location = locations.find { it.id == locationId }
    val context = LocalContext.current

    if (location == null) {
        onNavigateBack()
        return
    }

    var title by remember { mutableStateOf(location.title) }
    var notes by remember { mutableStateOf(location.notes) }
    var geofenceEnabled by remember { mutableStateOf(location.isGeofenceEnabled) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            geofenceEnabled = true
        } else {
            Toast.makeText(context, "يجب تفعيل إذن الموقع في الخلفية لاستخدام التنبيهات", Toast.LENGTH_LONG).show()
            geofenceEnabled = false
        }
    }

    // Audio recording state
    val audioFile = remember { File(context.filesDir, "audio_${locationId}.3gp") }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تعديل الموقع") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteLocation(location)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الموقف") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات (مثال: الطابق الثاني، موقف 4B)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ملاحظة صوتية للموقف", style = MaterialTheme.typography.titleMedium)
                    Row {
                        if (isRecording) {
                            IconButton(onClick = {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                                isRecording = false
                            }) {
                                Icon(Icons.Filled.Stop, contentDescription = "ايقاف التسجيل", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            IconButton(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        MediaRecorder(context)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaRecorder()
                                    }.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                        setOutputFile(audioFile.absolutePath)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                        try {
                                            prepare()
                                            start()
                                            isRecording = true
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "الرجاء السماح بصلاحية الميكروفون", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Mic, contentDescription = "تسجيل", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (audioFile.exists() && !isRecording) {
                            IconButton(onClick = {
                                if (isPlaying) {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    isPlaying = false
                                } else {
                                    mediaPlayer = MediaPlayer().apply {
                                        try {
                                            setDataSource(audioFile.absolutePath)
                                            prepare()
                                            start()
                                            isPlaying = true
                                            setOnCompletionListener {
                                                isPlaying = false
                                                release()
                                                mediaPlayer = null
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = "تشغيل الملاحظة",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("انذار عند الاقتراب", style = MaterialTheme.typography.titleMedium)
                    Text("يصلك اشعار عند الاقتراب من هذا الموقف", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = geofenceEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                } else {
                                    geofenceEnabled = true
                                }
                            } else {
                                geofenceEnabled = true
                            }
                        } else {
                            geofenceEnabled = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updateLocation(
                        location.copy(
                            title = title,
                            notes = notes,
                            isGeofenceEnabled = geofenceEnabled
                        )
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ التعديلات")
            }
        }
    }
}
