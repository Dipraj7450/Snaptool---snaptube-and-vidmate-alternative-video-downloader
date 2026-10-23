package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (_: Exception) {
        DownloadStatus.QUEUED
    }

    @TypeConverter
    fun fromFormat(format: MediaFormat): String = format.name

    @TypeConverter
    fun toFormat(value: String): MediaFormat = try {
        MediaFormat.valueOf(value)
    } catch (_: Exception) {
        MediaFormat.MP4
    }
}

@Database(
    entities = [DownloadEntity::class, FolderEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun folderDao(): FolderDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "snapload_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default folders and top video site bookmarks
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.folderDao().insertFolder(FolderEntity(name = "Movies"))
                                database.folderDao().insertFolder(FolderEntity(name = "Music & Podcasts"))
                                database.folderDao().insertFolder(FolderEntity(name = "Social Clips"))
                                database.folderDao().insertFolder(FolderEntity(name = "Lectures & Courses"))

                                // Popular sites
                                val bookmarks = listOf(
                                    BookmarkEntity(title = "YouTube", url = "https://m.youtube.com", iconUrl = "https://www.youtube.com/s/desktop/f71d5b32/img/favicon.ico"),
                                    BookmarkEntity(title = "Instagram", url = "https://www.instagram.com", iconUrl = "https://static.cdninstagram.com/rsrc.php/v3/yI/r/VsNE-OHk_8a.png"),
                                    BookmarkEntity(title = "TikTok", url = "https://www.tiktok.com", iconUrl = "https://sf16-website-login.neutral.ttwstatic.com/obj/tiktok_web_login_static/favicon.ico"),
                                    BookmarkEntity(title = "Reddit", url = "https://www.reddit.com", iconUrl = "https://www.redditstatic.com/shreddit/assets/favicon/192x192.png"),
                                    BookmarkEntity(title = "Vimeo", url = "https://vimeo.com/watch", iconUrl = "https://f.vimeocdn.com/images_v6/favicon.ico"),
                                    BookmarkEntity(title = "Archive.org", url = "https://archive.org/details/movies", iconUrl = "https://archive.org/images/glogo.png"),
                                    BookmarkEntity(title = "Pexels Free Video", url = "https://www.pexels.com/videos/", iconUrl = "https://www.pexels.com/assets/favicon.ico"),
                                    BookmarkEntity(title = "Pixabay Videos", url = "https://pixabay.com/videos/", iconUrl = "https://pixabay.com/favicon-32x32.png")
                                )
                                bookmarks.forEach { database.bookmarkDao().insertBookmark(it) }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
