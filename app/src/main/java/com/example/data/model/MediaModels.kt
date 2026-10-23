package com.example.data.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    SCHEDULED
}

enum class MediaFormat(val extension: String, val mimeType: String, val isAudio: Boolean) {
    MP4("mp4", "video/mp4", false),
    MKV("mkv", "video/x-matroska", false),
    WEBM("webm", "video/webm", false),
    FLV("flv", "video/x-flv", false),
    THREE_GP("3gp", "video/3gpp", false),
    MP3("mp3", "audio/mpeg", true),
    M4A("m4a", "audio/mp4", true)
}

data class MediaQuality(
    val label: String,
    val resolution: String,
    val format: MediaFormat,
    val estimatedSizeMb: Double,
    val isAudioOnly: Boolean = false,
    val url: String = ""
)

data class DetectedMedia(
    val title: String,
    val sourceUrl: String,
    val directUrl: String,
    val mimeType: String,
    val thumbnailUrl: String? = null,
    val qualities: List<MediaQuality> = emptyList()
)
