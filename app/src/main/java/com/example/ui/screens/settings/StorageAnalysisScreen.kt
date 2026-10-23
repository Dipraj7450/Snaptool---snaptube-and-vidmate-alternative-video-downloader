package com.example.ui.screens.settings

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalysisDialog(
    downloads: List<DownloadEntity>,
    onDismiss: () -> Unit,
    onDeleteFile: (Long) -> Unit,
    onPlayMedia: (String, String) -> Unit
) {
    val context = LocalContext.current

    val stat = remember {
        val path = Environment.getDataDirectory()
        val statFs = StatFs(path.path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong
        Pair(totalBlocks * blockSize, availableBlocks * blockSize)
    }

    val totalDeviceStorage = stat.first
    val freeDeviceStorage = stat.second

    val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
    val videoBytes = completed.filter { !it.format.isAudio && !it.isPrivate }.sumOf { it.fileSize }
    val audioBytes = completed.filter { it.format.isAudio && !it.isPrivate }.sumOf { it.fileSize }
    val vaultBytes = completed.filter { it.isPrivate }.sumOf { it.fileSize }
    val totalAppMediaBytes = videoBytes + audioBytes + vaultBytes

    // Cache calculation
    var cacheBytes by remember {
        mutableStateOf(context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum())
    }

    val largestFiles = remember(completed) {
        completed.sortedByDescending { it.fileSize }.take(8)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Storage Breakdown & Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("App Media Used", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatBytes(totalAppMediaBytes), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Device Free Space", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatBytes(freeDeviceStorage), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Multi-segment bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            val totalApp = totalAppMediaBytes.coerceAtLeast(1L).toFloat()
                            val vRatio = (videoBytes.toFloat() / totalApp).coerceIn(0f, 1f)
                            val aRatio = (audioBytes.toFloat() / totalApp).coerceIn(0f, 1f)
                            val vaultRatio = (vaultBytes.toFloat() / totalApp).coerceIn(0f, 1f)

                            if (videoBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(vRatio.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF9800))
                                )
                            }
                            if (audioBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(aRatio.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .background(Color(0xFF00E676))
                                )
                            }
                            if (vaultBytes > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(vaultRatio.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .background(Color(0xFFD500F9))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryLegendItem("🎬 Videos", formatBytes(videoBytes), Color(0xFFFF9800))
                            CategoryLegendItem("🎵 Music", formatBytes(audioBytes), Color(0xFF00E676))
                            CategoryLegendItem("🔒 Vault", formatBytes(vaultBytes), Color(0xFFD500F9))
                        }
                    }
                }
            }

            // Cache & Temp cleaner card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Temporary Chunks & Cache", fontWeight = FontWeight.SemiBold)
                            Text(formatBytes(cacheBytes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                try {
                                    context.cacheDir.deleteRecursively()
                                    context.cacheDir.mkdirs()
                                    cacheBytes = 0L
                                    Toast.makeText(context, "Temporary cache cleared!", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean")
                        }
                    }
                }
            }

            // Largest Files List
            item {
                Text(
                    "Largest Files on Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(largestFiles) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPlayMedia(file.filePath, file.title) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isPrivate) Icons.Default.Lock else if (file.format.isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${formatBytes(file.fileSize)} • ${file.format.extension.uppercase()} ${if (file.isPrivate) "• Vault" else ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteFile(file.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CategoryLegendItem(title: String, sizeText: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(sizeText, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
