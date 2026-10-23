package com.example.ui.screens.downloads

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.download.DownloadManager
import com.example.data.local.DownloadEntity
import com.example.data.local.LayoutMode
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaFormat
import com.example.data.model.MediaQuality
import com.example.ui.i18n.StringsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsDashboardScreen(
    downloads: List<DownloadEntity>,
    downloadManager: DownloadManager,
    strings: StringsProvider.AppStrings,
    layoutMode: LayoutMode,
    onPlayMedia: (filePath: String, title: String) -> Unit,
    onAddNewDownload: (url: String, quality: MediaQuality, isPrivate: Boolean, scheduleTime: Long?) -> Unit,
    showSpeedMeter: Boolean = true,
    showStorageBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Active, 2: Completed, 3: Paused, 4: Scheduled
    var showAddUrlDialog by remember { mutableStateOf(false) }

    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            1 -> downloads.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
            2 -> downloads.filter { it.status == DownloadStatus.COMPLETED }
            3 -> downloads.filter { it.status == DownloadStatus.PAUSED }
            4 -> downloads.filter { it.status == DownloadStatus.SCHEDULED }
            else -> downloads
        }
    }

    val activeCount = downloads.count { it.status == DownloadStatus.DOWNLOADING }
    val totalSpeed = downloads.filter { it.status == DownloadStatus.DOWNLOADING }.sumOf { it.downloadSpeed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            strings.navDownloads,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeCount > 0) {
                            Text(
                                "$activeCount active • ${downloadManager.formatSpeed(totalSpeed)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "${downloads.size} tasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddUrlDialog = true }) {
                        Icon(Icons.Default.AddLink, contentDescription = "Add Download URL")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Speed & Storage Card
            if (showSpeedMeter || showStorageBar) {
                StorageDashboardCard(
                    downloads = downloads,
                    totalSpeed = totalSpeed,
                    downloadManager = downloadManager,
                    strings = strings,
                    showSpeedMeter = showSpeedMeter,
                    showStorageBar = showStorageBar
                )
            }

            // Batch Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { downloadManager.batchPauseAll() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.pauseAll, fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            downloads.filter { it.status == DownloadStatus.PAUSED }.forEach {
                                downloadManager.resumeDownload(it.id)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.resumeAll, fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = {
                        downloads.filter { it.status == DownloadStatus.COMPLETED }.forEach {
                            downloadManager.cancelOrDeleteDownload(it.id, deleteFile = false)
                        }
                    }
                ) {
                    Text(strings.deleteAll, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedFilter,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                val filters = listOf(
                    strings.tabAll to downloads.size,
                    strings.tabActive to downloads.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED },
                    strings.tabCompleted to downloads.count { it.status == DownloadStatus.COMPLETED },
                    strings.tabPaused to downloads.count { it.status == DownloadStatus.PAUSED },
                    strings.tabScheduled to downloads.count { it.status == DownloadStatus.SCHEDULED }
                )
                filters.forEachIndexed { index, (label, count) ->
                    Tab(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        text = {
                            Text(
                                "$label ($count)",
                                fontWeight = if (selectedFilter == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Downloads List or Grid
            if (filteredDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No downloads in this section",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (layoutMode == LayoutMode.COMPACT_GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredDownloads, key = { it.id }) { item ->
                            DownloadGridItem(item, downloadManager, strings, onPlayMedia)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredDownloads, key = { it.id }) { item ->
                            DownloadListItem(item, downloadManager, strings, onPlayMedia)
                        }
                    }
                }
            }
        }
    }

    if (showAddUrlDialog) {
        AddUrlDownloadDialog(
            strings = strings,
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url, quality, isPrivate, scheduleTime ->
                onAddNewDownload(url, quality, isPrivate, scheduleTime)
                showAddUrlDialog = false
            }
        )
    }
}

@Composable
fun StorageDashboardCard(
    downloads: List<DownloadEntity>,
    totalSpeed: Long,
    downloadManager: DownloadManager,
    strings: StringsProvider.AppStrings,
    showSpeedMeter: Boolean = true,
    showStorageBar: Boolean = true
) {
    if (!showSpeedMeter && !showStorageBar) return

    val totalDownloadedBytes = downloads.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.fileSize }
    val videoBytes = downloads.filter { it.status == DownloadStatus.COMPLETED && !it.format.isAudio }.sumOf { it.fileSize }
    val audioBytes = downloads.filter { it.status == DownloadStatus.COMPLETED && it.format.isAudio }.sumOf { it.fileSize }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (showSpeedMeter) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Speed: ${downloadManager.formatSpeed(totalSpeed)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        "Total Saved: ${downloadManager.formatBytes(totalDownloadedBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showSpeedMeter && showStorageBar) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (showStorageBar) {
                // Multi-segment progress bar for storage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    val total = (videoBytes + audioBytes).coerceAtLeast(1L).toFloat()
                    val videoWeight = (videoBytes.toFloat() / total).coerceIn(0f, 1f)
                    val audioWeight = (audioBytes.toFloat() / total).coerceIn(0f, 1f)

                    if (videoBytes > 0) {
                        Box(
                            modifier = Modifier
                                .weight(videoWeight.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (audioBytes > 0) {
                        Box(
                            modifier = Modifier
                                .weight(audioWeight.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFF00E676))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🎬 Videos: ${downloadManager.formatBytes(videoBytes)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "🎵 Music: ${downloadManager.formatBytes(audioBytes)}",
                        fontSize = 11.sp,
                        color = Color(0xFF00E676)
                    )
                    Text(
                        "💾 Free: 18.4 GB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadListItem(
    item: DownloadEntity,
    downloadManager: DownloadManager,
    strings: StringsProvider.AppStrings,
    onPlayMedia: (filePath: String, title: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = item.status == DownloadStatus.COMPLETED) {
                onPlayMedia(item.filePath, item.title)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail or Format Icon
                Box(
                    modifier = Modifier
                        .size(54.dp, 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnailUrl != null) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (item.format.isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.quality,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.format.extension.uppercase(),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (item.status == DownloadStatus.SCHEDULED) {
                            Text(
                                "⏰ Scheduled",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Action buttons based on status
                Row {
                    when (item.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = { downloadManager.pauseDownload(item.id) }) {
                                Icon(Icons.Default.PauseCircle, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = { downloadManager.resumeDownload(item.id) }) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = { onPlayMedia(item.filePath, item.title) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF00E676))
                            }
                        }
                        else -> {
                            IconButton(onClick = { downloadManager.startDownload(item.id) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            }
                        }
                    }

                    IconButton(onClick = { downloadManager.cancelOrDeleteDownload(item.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Progress Bar & Stats
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = if (item.fileSize > 0) item.downloadedBytes.toFloat() / item.fileSize.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (item.status == DownloadStatus.PAUSED) Color.Gray else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val percent = (progress * 100).toInt()
                    Text(
                        "${downloadManager.formatBytes(item.downloadedBytes)} / ${downloadManager.formatBytes(item.fileSize)} ($percent%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        Text(
                            downloadManager.formatSpeed(item.downloadSpeed),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("Paused", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadGridItem(
    item: DownloadEntity,
    downloadManager: DownloadManager,
    strings: StringsProvider.AppStrings,
    onPlayMedia: (filePath: String, title: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = item.status == DownloadStatus.COMPLETED) {
                onPlayMedia(item.filePath, item.title)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.format.isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.quality} • ${item.format.extension.uppercase()}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            if (item.status == DownloadStatus.DOWNLOADING) {
                val progress = if (item.fileSize > 0) item.downloadedBytes.toFloat() / item.fileSize.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
        }
    }
}

@Composable
fun AddUrlDownloadDialog(
    strings: StringsProvider.AppStrings,
    onDismiss: () -> Unit,
    onConfirm: (url: String, quality: MediaQuality, isPrivate: Boolean, scheduleTime: Long?) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(MediaFormat.MP4) }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Video / Audio URL") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com/video.mp4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == MediaFormat.MP4,
                        onClick = { selectedFormat = MediaFormat.MP4 },
                        label = { Text("MP4 Video") }
                    )
                    FilterChip(
                        selected = selectedFormat == MediaFormat.MP3,
                        onClick = { selectedFormat = MediaFormat.MP3 },
                        label = { Text("MP3 Audio") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Save in Private Vault", fontSize = 13.sp)
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = urlText.isNotBlank(),
                onClick = {
                    val quality = MediaQuality(
                        label = if (selectedFormat == MediaFormat.MP3) "MP3 Audio" else "1080p Video",
                        resolution = if (selectedFormat == MediaFormat.MP3) "320kbps" else "1920x1080",
                        format = selectedFormat,
                        estimatedSizeMb = 25.0,
                        isAudioOnly = selectedFormat.isAudio,
                        url = urlText.trim()
                    )
                    onConfirm(urlText.trim(), quality, isPrivate, null)
                }
            ) {
                Text(strings.download)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
