package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DetectedMedia
import com.example.data.model.MediaFormat
import com.example.data.model.MediaQuality
import com.example.ui.i18n.StringsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionDialog(
    detectedMedia: DetectedMedia,
    strings: StringsProvider.AppStrings,
    onDismiss: () -> Unit,
    onDownload: (quality: MediaQuality, isPrivate: Boolean, scheduleTime: Long?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Video, 1: Audio
    var isPrivateVault by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    // Pre-calculated default qualities if detectedMedia has none
    val videoQualities = remember(detectedMedia) {
        if (detectedMedia.qualities.filter { !it.isAudioOnly }.isNotEmpty()) {
            detectedMedia.qualities.filter { !it.isAudioOnly }
        } else {
            listOf(
                MediaQuality("1080p Full HD", "1920x1080", MediaFormat.MP4, 42.5, false, detectedMedia.directUrl),
                MediaQuality("720p HD", "1280x720", MediaFormat.MP4, 21.0, false, detectedMedia.directUrl),
                MediaQuality("480p SD", "854x480", MediaFormat.MP4, 12.4, false, detectedMedia.directUrl),
                MediaQuality("360p Mobile", "640x360", MediaFormat.MP4, 7.8, false, detectedMedia.directUrl),
                MediaQuality("WebM 720p", "1280x720", MediaFormat.WEBM, 18.2, false, detectedMedia.directUrl),
                MediaQuality("MKV 1080p", "1920x1080", MediaFormat.MKV, 44.0, false, detectedMedia.directUrl)
            )
        }
    }

    val audioQualities = remember(detectedMedia) {
        if (detectedMedia.qualities.filter { it.isAudioOnly }.isNotEmpty()) {
            detectedMedia.qualities.filter { it.isAudioOnly }
        } else {
            listOf(
                MediaQuality("MP3 High Quality", "320 kbps", MediaFormat.MP3, 8.4, true, detectedMedia.directUrl),
                MediaQuality("MP3 Standard", "128 kbps", MediaFormat.MP3, 4.1, true, detectedMedia.directUrl),
                MediaQuality("M4A Audio", "256 kbps", MediaFormat.M4A, 6.2, true, detectedMedia.directUrl)
            )
        }
    }

    var selectedQuality by remember {
        mutableStateOf<MediaQuality?>(videoQualities.firstOrNull())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header: thumbnail + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                detectedMedia.thumbnailUrl?.let { thumb ->
                    AsyncImage(
                        model = thumb,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp, 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detectedMedia.title.ifBlank { "Media Stream" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detectedMedia.sourceUrl.take(45) + if (detectedMedia.sourceUrl.length > 45) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video vs Music Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedQuality = videoQualities.firstOrNull()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video (${videoQualities.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedQuality = audioQualities.firstOrNull()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audio (${audioQualities.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options List
            val currentList = if (selectedTab == 0) videoQualities else audioQualities
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(currentList) { item ->
                    val isSelected = selectedQuality == item
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedQuality = item },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedQuality = item }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${item.format.extension.uppercase()} • ${item.resolution}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "≈ ${item.estimatedSizeMb} MB",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vault toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPrivateVault) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isPrivateVault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.moveToVault,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isPrivateVault,
                    onCheckedChange = { isPrivateVault = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Download Now & Schedule for Later)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showScheduleDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        selectedQuality?.let { q ->
                            onDownload(q, isPrivateVault, null)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.downloadNow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            title = { Text(strings.scheduleLater) },
            text = {
                Text("Schedule this file to download automatically during off-peak data hours (e.g. 2:00 AM tonight) or in 2 hours?")
            },
            confirmButton = {
                Button(onClick = {
                    val twoHoursLater = System.currentTimeMillis() + (2 * 60 * 60 * 1000)
                    selectedQuality?.let { q ->
                        onDownload(q, isPrivateVault, twoHoursLater)
                    }
                    showScheduleDialog = false
                    onDismiss()
                }) {
                    Text("Start in 2 Hours")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
