package com.example.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaFormat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager private constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val prefs: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeSpeeds = ConcurrentHashMap<Long, Long>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
        startSchedulerLoop()
        startQueueLoop()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress of video and audio downloads"
                enableVibration(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when media downloads complete"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    private fun startSchedulerLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val dueDownloads = database.downloadDao().getDueScheduledDownloads(now)
                    for (task in dueDownloads) {
                        database.downloadDao().updateStatus(task.id, DownloadStatus.QUEUED)
                    }
                } catch (_: Exception) {}
                delay(10000)
            }
        }
    }

    private fun startQueueLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val maxParallel = if (prefs.lowPowerMode.value) 1 else prefs.maxParallelDownloads.value
                    val currentRunning = activeJobs.size
                    if (currentRunning < maxParallel) {
                        val needed = maxParallel - currentRunning
                        // Fetch QUEUED items
                        val queued = withContext(Dispatchers.IO) {
                            database.downloadDao().getDownloadsByStatus(DownloadStatus.QUEUED)
                        }
                        // Launch downloads
                        queued.collect { list ->
                            val toStart = list.take(needed)
                            for (item in toStart) {
                                if (!activeJobs.containsKey(item.id)) {
                                    startDownload(item.id)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    fun enqueueDownload(
        title: String,
        url: String,
        format: MediaFormat,
        quality: String,
        thumbnailUrl: String? = null,
        scheduledTime: Long? = null,
        folderId: Long? = null,
        isPrivate: Boolean = false
    ): Long {
        val safeTitle = title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(50)
        val fileName = "${safeTitle}_${System.currentTimeMillis()}.${format.extension}"
        
        val downloadsDir = if (isPrivate) {
            File(context.filesDir, ".vault").apply { mkdirs() }
        } else if (format.isAudio) {
            File(prefs.audioPath.value).apply { mkdirs() }
        } else {
            File(prefs.videoPath.value).apply { mkdirs() }
        }
        val targetFile = File(downloadsDir, fileName)

        val status = if (scheduledTime != null && scheduledTime > System.currentTimeMillis()) {
            DownloadStatus.SCHEDULED
        } else {
            DownloadStatus.QUEUED
        }

        val entity = DownloadEntity(
            title = title,
            url = url,
            filePath = targetFile.absolutePath,
            fileSize = 0L,
            downloadedBytes = 0L,
            status = status,
            format = format,
            quality = quality,
            thumbnailUrl = thumbnailUrl,
            scheduledTime = scheduledTime,
            folderId = folderId,
            isPrivate = isPrivate
        )

        var newId = 0L
        runBlocking(Dispatchers.IO) {
            newId = database.downloadDao().insertDownload(entity)
        }

        if (status == DownloadStatus.QUEUED) {
            scope.launch {
                triggerQueueCheck()
            }
        }
        return newId
    }

    fun triggerQueueCheck() {
        scope.launch {
            val maxParallel = if (prefs.lowPowerMode.value) 1 else prefs.maxParallelDownloads.value
            val currentRunning = activeJobs.size
            if (currentRunning < maxParallel) {
                val item = database.downloadDao().getDownloadsByStatus(DownloadStatus.QUEUED)
                // Just let startQueueLoop pick it up or direct fetch
            }
        }
    }

    fun startDownload(downloadId: Long) {
        if (activeJobs.containsKey(downloadId)) return

        val job = scope.launch {
            executeDownload(downloadId)
        }
        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        activeSpeeds.remove(downloadId)
        cancelNotification(downloadId.toInt())
        scope.launch(Dispatchers.IO) {
            database.downloadDao().updateStatus(downloadId, DownloadStatus.PAUSED, 0L)
        }
    }

    fun resumeDownload(downloadId: Long) {
        scope.launch(Dispatchers.IO) {
            database.downloadDao().updateStatus(downloadId, DownloadStatus.QUEUED, 0L)
            startDownload(downloadId)
        }
    }

    fun cancelOrDeleteDownload(downloadId: Long, deleteFile: Boolean = true) {
        pauseDownload(downloadId)
        scope.launch(Dispatchers.IO) {
            val entity = database.downloadDao().getDownloadById(downloadId)
            if (entity != null && deleteFile) {
                try {
                    File(entity.filePath).delete()
                } catch (_: Exception) {}
            }
            database.downloadDao().deleteById(downloadId)
        }
    }

    fun batchPauseAll() {
        val keys = activeJobs.keys().toList()
        for (id in keys) {
            pauseDownload(id)
        }
    }

    fun batchResumeAll() {
        scope.launch(Dispatchers.IO) {
            // Pick all paused
            val paused = database.downloadDao().getDownloadsByStatus(DownloadStatus.PAUSED)
            // Or get direct
        }
    }

    private suspend fun executeDownload(downloadId: Long) {
        val download = database.downloadDao().getDownloadById(downloadId) ?: run {
            activeJobs.remove(downloadId)
            return
        }

        val targetFile = File(download.filePath)
        var existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        // If file already finished or bytes exceed recorded
        if (existingBytes > download.downloadedBytes) {
            existingBytes = download.downloadedBytes
        }

        database.downloadDao().updateStatus(downloadId, DownloadStatus.DOWNLOADING)

        try {
            val policy = prefs.networkPolicy.value
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val isWifi = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true

            if (policy == com.example.data.local.NetworkPolicy.WIFI_ONLY && !isWifi) {
                throw Exception("Paused: Waiting for Wi-Fi network")
            }
            if (policy == com.example.data.local.NetworkPolicy.MOBILE_DATA_ONLY && !isCellular) {
                throw Exception("Paused: Waiting for mobile data network")
            }

            val requestBuilder = Request.Builder()
                .url(download.url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")

            // HTTP Range request for resume
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful && response.code != 206) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val responseBody = response.body ?: throw Exception("Empty response body")
            val isPartial = response.code == 206
            val totalLength = if (isPartial) {
                existingBytes + responseBody.contentLength()
            } else {
                existingBytes = 0L // Server ignored range, restart cleanly
                responseBody.contentLength()
            }

            val append = isPartial && existingBytes > 0
            val fos = FileOutputStream(targetFile, append)
            val inputStream: InputStream = responseBody.byteStream()

            val bufferSize = if (prefs.lowPowerMode.value) 32 * 1024 else 64 * 1024
            val buffer = ByteArray(bufferSize)
            var bytesRead = 0
            var totalDownloaded = existingBytes
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            inputStream.use { input ->
                fos.use { output ->
                    while (currentCoroutineContext().isActive) {
                        bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 750) { // update progress every 750ms
                            val speed = (bytesSinceLastUpdate * 1000) / elapsed
                            activeSpeeds[downloadId] = speed
                            database.downloadDao().updateProgress(
                                downloadId,
                                totalDownloaded,
                                totalLength,
                                speed
                            )
                            showProgressNotification(
                                downloadId.toInt(),
                                download.title,
                                totalDownloaded,
                                totalLength,
                                speed
                            )
                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                }
            }

            if (currentCoroutineContext().isActive) {
                // Completed!
                database.downloadDao().updateProgress(downloadId, totalDownloaded, totalLength, 0L)
                val updated = download.copy(
                    downloadedBytes = totalDownloaded,
                    fileSize = totalLength,
                    status = DownloadStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    downloadSpeed = 0L
                )
                database.downloadDao().updateDownload(updated)
                activeSpeeds.remove(downloadId)
                showCompletedNotification(downloadId.toInt(), download.title, totalLength)
            }

        } catch (e: CancellationException) {
            // Paused or cancelled
        } catch (e: Exception) {
            database.downloadDao().updateStatus(downloadId, DownloadStatus.FAILED, 0L)
            val updated = database.downloadDao().getDownloadById(downloadId)?.copy(
                errorMessage = e.localizedMessage ?: "Network error"
            )
            if (updated != null) database.downloadDao().updateDownload(updated)
            activeSpeeds.remove(downloadId)
            cancelNotification(downloadId.toInt())
        } finally {
            activeJobs.remove(downloadId)
        }
    }

    private fun showProgressNotification(
        notificationId: Int,
        title: String,
        progress: Long,
        total: Long,
        speed: Long
    ) {
        if (!prefs.notifyDownloading.value || !hasNotificationPermission()) return
        val percent = if (total > 0) ((progress * 100) / total).toInt() else 0
        val speedStr = formatSpeed(speed)
        val progressStr = "${formatBytes(progress)} / ${formatBytes(total)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$progressStr ($percent%) • $speedStr")
            .setProgress(100, percent, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    private fun showCompletedNotification(notificationId: Int, title: String, totalBytes: Long) {
        if (!prefs.notifyCompleted.value || !hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Finished")
            .setContentText("$title (${formatBytes(totalBytes)})")
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.cancel(notificationId)
            notificationManager.notify(notificationId + 10000, notification)
        } catch (_: SecurityException) {}
    }

    private fun cancelNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
        } catch (_: Exception) {}
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        if (digitGroups >= units.size) digitGroups = units.size - 1
        return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return "${formatBytes(bytesPerSec)}/s"
    }

    companion object {
        const val CHANNEL_PROGRESS = "snapload_progress_channel"
        const val CHANNEL_COMPLETED = "snapload_completed_channel"

        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context, database: AppDatabase, prefs: PreferencesManager): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(
                    context.applicationContext,
                    database,
                    prefs
                ).also { INSTANCE = it }
            }
        }
    }
}
