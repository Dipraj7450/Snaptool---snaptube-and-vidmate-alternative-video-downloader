package com.example.data.local

import androidx.room.*
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isPrivate = 0 ORDER BY createdAt DESC")
    fun getPublicDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isPrivate = 1 ORDER BY createdAt DESC")
    fun getPrivateDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status AND isPrivate = 0 ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE folderId = :folderId AND isPrivate = 0 ORDER BY createdAt DESC")
    fun getDownloadsInFolder(folderId: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeDownloadById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE status = 'SCHEDULED' AND scheduledTime <= :currentTime")
    suspend fun getDueScheduledDownloads(currentTime: Long): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<DownloadEntity>): List<Long>

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE downloads SET status = :status, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, speed: Long = 0L)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, fileSize = :totalBytes, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, totalBytes: Long, speed: Long)

    @Query("UPDATE downloads SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?)

    @Query("UPDATE downloads SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setPrivate(id: Long, isPrivate: Boolean)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY id ASC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
}
