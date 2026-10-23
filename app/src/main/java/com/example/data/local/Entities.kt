package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaFormat

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val format: MediaFormat = MediaFormat.MP4,
    val quality: String = "720p",
    val thumbnailUrl: String? = null,
    val downloadSpeed: Long = 0L, // bytes per second
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val scheduledTime: Long? = null,
    val folderId: Long? = null,
    val isPrivate: Boolean = false,
    val errorMessage: String? = null
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isProtected: Boolean = false,
    val passwordHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
