package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.download.DownloadManager
import com.example.data.local.*
import com.example.data.model.DetectedMedia
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaFormat
import com.example.data.model.MediaQuality
import com.example.player.MXPlayerView
import com.example.ui.components.LiquidGlassBottomBar
import com.example.ui.components.QualitySelectionDialog
import com.example.ui.i18n.StringsProvider
import com.example.ui.navigation.Screen
import com.example.ui.screens.browser.BrowserScreen
import com.example.ui.screens.downloads.DownloadsDashboardScreen
import com.example.ui.screens.filemanager.FileManagerScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.SnapLoadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var downloadManager: DownloadManager

    // Shared URL from external apps
    private val incomingSharedUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        downloadManager = DownloadManager.getInstance(this, database, preferencesManager)

        handleShareIntent(intent)
        seedInitialSampleIfEmpty()

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsStateWithLifecycle()
            val accentTheme by preferencesManager.accentTheme.collectAsStateWithLifecycle()
            val layoutMode by preferencesManager.layoutMode.collectAsStateWithLifecycle()
            val languageCode by preferencesManager.languageCode.collectAsStateWithLifecycle()
            val adBlockEnabled by preferencesManager.adBlockEnabled.collectAsStateWithLifecycle()
            val wallpaperStyle by preferencesManager.wallpaperStyle.collectAsStateWithLifecycle()

            val strings = remember(languageCode) { StringsProvider.getStrings(languageCode) }

            // Ask for Notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {}
                )
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            SnapLoadTheme(themeMode = themeMode, accentTheme = accentTheme) {
                MainAppContent(
                    database = database,
                    preferencesManager = preferencesManager,
                    downloadManager = downloadManager,
                    strings = strings,
                    layoutMode = layoutMode,
                    adBlockEnabled = adBlockEnabled,
                    wallpaperStyle = wallpaperStyle,
                    incomingUrl = incomingSharedUrl.value,
                    onClearIncomingUrl = { incomingSharedUrl.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val url = Regex("""https?://[^\s]+""").find(text)?.value
            if (url != null) {
                incomingSharedUrl.value = url
            } else if (text.isNotBlank()) {
                incomingSharedUrl.value = text.trim()
            }
        }
    }

    private fun seedInitialSampleIfEmpty() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = database.downloadDao().getAllDownloads().first().size
            if (count == 0) {
                val extDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                val file1 = File(extDir, "BigBuckBunny_Demo.mp4")
                database.downloadDao().insertDownload(
                    DownloadEntity(
                        title = "Big Buck Bunny (HD 720p)",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        filePath = file1.absolutePath,
                        fileSize = 158008374L,
                        downloadedBytes = 0L,
                        status = DownloadStatus.QUEUED,
                        format = MediaFormat.MP4,
                        quality = "720p HD",
                        thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg"
                    )
                )

                val file2 = File(extDir, "Sintel_Animated_Demo.mp4")
                database.downloadDao().insertDownload(
                    DownloadEntity(
                        title = "Sintel Animated Film",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        filePath = file2.absolutePath,
                        fileSize = 42500000L,
                        downloadedBytes = 0L,
                        status = DownloadStatus.PAUSED,
                        format = MediaFormat.MP4,
                        quality = "1080p Full HD",
                        thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg"
                    )
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    database: AppDatabase,
    preferencesManager: PreferencesManager,
    downloadManager: DownloadManager,
    strings: StringsProvider.AppStrings,
    layoutMode: LayoutMode,
    adBlockEnabled: Boolean,
    wallpaperStyle: WallpaperStyle,
    incomingUrl: String?,
    onClearIncomingUrl: () -> Unit
) {
    // Modular UI Toggles
    val showSpeedMeter by preferencesManager.showDashboardSpeedMeter.collectAsStateWithLifecycle()
    val showStorageBar by preferencesManager.showDashboardStorageBar.collectAsStateWithLifecycle()
    val showBrowserBanner by preferencesManager.showBrowserBanner.collectAsStateWithLifecycle()
    val showBrowserBookmarks by preferencesManager.showBrowserBookmarks.collectAsStateWithLifecycle()
    val showBrowserNews by preferencesManager.showBrowserNews.collectAsStateWithLifecycle()
    val showBrowserSamples by preferencesManager.showBrowserSamples.collectAsStateWithLifecycle()
    val clipboardAutoDetect by preferencesManager.clipboardAutoDetect.collectAsStateWithLifecycle()

    // Bottom Bar & Swipe Navigation Settings
    val bottomBarTransparency by preferencesManager.bottomBarTransparency.collectAsStateWithLifecycle()
    val bottomBarBlurLevel by preferencesManager.bottomBarBlurLevel.collectAsStateWithLifecycle()
    val bottomBarLabelStyle by preferencesManager.bottomBarLabelStyle.collectAsStateWithLifecycle()
    val isSwipeNavigationEnabled by preferencesManager.isSwipeNavigationEnabled.collectAsStateWithLifecycle()
    val isInfiniteSwipeEnabled by preferencesManager.isInfiniteSwipeEnabled.collectAsStateWithLifecycle()
    val showBottomFloatingSearch by preferencesManager.showBottomFloatingSearch.collectAsStateWithLifecycle()
    val enabledBottomTabs by preferencesManager.enabledBottomTabs.collectAsStateWithLifecycle()

    val allScreenMap = remember {
        mapOf(
            "DOWNLOADS" to Screen.DOWNLOADS,
            "BROWSER" to Screen.BROWSER,
            "FILE_MANAGER" to Screen.FILE_MANAGER,
            "SETTINGS" to Screen.SETTINGS
        )
    }

    val visibleScreens = remember(enabledBottomTabs) {
        val list = enabledBottomTabs.mapNotNull { allScreenMap[it] }
        if (list.size < 2) listOf(Screen.DOWNLOADS, Screen.BROWSER, Screen.FILE_MANAGER, Screen.SETTINGS) else list
    }

    val pagerState = rememberPagerState(initialPage = 0) { visibleScreens.size }
    val scope = rememberCoroutineScope()

    // Clipboard Auto-Detection State
    val clipboardManager = LocalClipboardManager.current
    var clipboardDetectedUrl by remember { mutableStateOf<String?>(null) }
    var dismissedClipboardUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clipboardAutoDetect) {
        if (clipboardAutoDetect) {
            val clip = clipboardManager.getText()?.text?.trim()
            if (!clip.isNullOrBlank() &&
                (clip.startsWith("http://") || clip.startsWith("https://")) &&
                clip != dismissedClipboardUrl
            ) {
                clipboardDetectedUrl = clip
            }
        }
    }

    // Media Player state
    var activePlaybackPath by remember { mutableStateOf<String?>(null) }
    var activePlaybackTitle by remember { mutableStateOf("") }

    // Download modal state
    var mediaToDownload by remember { mutableStateOf<DetectedMedia?>(null) }

    // Observers
    val allDownloads by database.downloadDao().getAllDownloads().collectAsState(initial = emptyList())
    val publicDownloads by database.downloadDao().getPublicDownloads().collectAsState(initial = emptyList())
    val privateDownloads by database.downloadDao().getPrivateDownloads().collectAsState(initial = emptyList())
    val folders by database.folderDao().getAllFolders().collectAsState(initial = emptyList())
    val bookmarks by database.bookmarkDao().getAllBookmarks().collectAsState(initial = emptyList())

    // Handle incoming share url
    LaunchedEffect(incomingUrl) {
        if (incomingUrl != null) {
            mediaToDownload = DetectedMedia(
                title = "Shared Media Link",
                sourceUrl = incomingUrl,
                directUrl = incomingUrl,
                mimeType = "video/mp4"
            )
            onClearIncomingUrl()
        }
    }

    BackHandler(enabled = activePlaybackPath != null || pagerState.currentPage != 0) {
        if (activePlaybackPath != null) {
            activePlaybackPath = null
        } else if (pagerState.currentPage != 0) {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (activePlaybackPath == null) {
                    val currentScreenIndex = pagerState.currentPage.coerceIn(0, (visibleScreens.size - 1).coerceAtLeast(0))
                    LiquidGlassBottomBar(
                        currentScreen = visibleScreens.getOrElse(currentScreenIndex) { Screen.DOWNLOADS },
                        onScreenSelected = { targetScreen ->
                            val targetIndex = visibleScreens.indexOf(targetScreen)
                            if (targetIndex >= 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(targetIndex)
                                }
                            }
                        },
                        activeDownloadsCount = allDownloads.count { it.status == DownloadStatus.DOWNLOADING },
                        strings = strings,
                        transparency = bottomBarTransparency,
                        blurLevel = bottomBarBlurLevel,
                        labelStyle = bottomBarLabelStyle,
                        visibleTabs = visibleScreens,
                        showFloatingSearch = showBottomFloatingSearch,
                        onSearchClick = {
                            val browserIndex = visibleScreens.indexOf(Screen.BROWSER)
                            if (browserIndex >= 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(browserIndex)
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = isSwipeNavigationEnabled,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (visibleScreens.getOrElse(page) { Screen.DOWNLOADS }) {
                        Screen.DOWNLOADS -> {
                            DownloadsDashboardScreen(
                                downloads = publicDownloads,
                                downloadManager = downloadManager,
                                strings = strings,
                                layoutMode = layoutMode,
                                showSpeedMeter = showSpeedMeter,
                                showStorageBar = showStorageBar,
                                onPlayMedia = { path, title ->
                                    activePlaybackPath = path
                                    activePlaybackTitle = title
                                },
                                onAddNewDownload = { url, quality, isPrivate, scheduleTime ->
                                    downloadManager.enqueueDownload(
                                        title = "Media_${System.currentTimeMillis() % 10000}",
                                        url = url,
                                        format = quality.format,
                                        quality = quality.label,
                                        scheduledTime = scheduleTime,
                                        isPrivate = isPrivate
                                    )
                                }
                            )
                        }

                        Screen.BROWSER -> {
                            BrowserScreen(
                                strings = strings,
                                isAdBlockEnabled = adBlockEnabled,
                                bookmarks = bookmarks,
                                wallpaperStyle = wallpaperStyle,
                                onWallpaperChanged = { preferencesManager.setWallpaperStyle(it) },
                                userDownloads = allDownloads,
                                prefs = preferencesManager,
                                showBanner = showBrowserBanner,
                                showBookmarks = showBrowserBookmarks,
                                showNews = showBrowserNews,
                                showSamples = showBrowserSamples,
                                onMediaDetected = { detected ->
                                    mediaToDownload = detected
                                }
                            )
                        }

                        Screen.FILE_MANAGER -> {
                            FileManagerScreen(
                                downloads = publicDownloads,
                                vaultFiles = privateDownloads,
                                folders = folders,
                                prefs = preferencesManager,
                                strings = strings,
                                onCreateFolder = { name, isProtected, passwordHash ->
                                    scope.launch(Dispatchers.IO) {
                                        database.folderDao().insertFolder(
                                            FolderEntity(
                                                name = name,
                                                isProtected = isProtected,
                                                passwordHash = passwordHash
                                            )
                                        )
                                    }
                                },
                                onMoveToFolder = { downloadId, folderId ->
                                    scope.launch(Dispatchers.IO) {
                                        database.downloadDao().moveToFolder(downloadId, folderId)
                                    }
                                },
                                onMoveToVault = { downloadId ->
                                    scope.launch(Dispatchers.IO) {
                                        database.downloadDao().setPrivate(downloadId, true)
                                    }
                                },
                                onRestoreFromVault = { downloadId ->
                                    scope.launch(Dispatchers.IO) {
                                        database.downloadDao().setPrivate(downloadId, false)
                                    }
                                },
                                onDeleteFile = { downloadId ->
                                    downloadManager.cancelOrDeleteDownload(downloadId)
                                },
                                onRenameFile = { downloadId, newTitle ->
                                    scope.launch(Dispatchers.IO) {
                                        val item = database.downloadDao().getDownloadById(downloadId)
                                        if (item != null) {
                                            database.downloadDao().updateDownload(item.copy(title = newTitle))
                                        }
                                    }
                                },
                                onPlayMedia = { path, title ->
                                    activePlaybackPath = path
                                    activePlaybackTitle = title
                                }
                            )
                        }

                        Screen.SETTINGS -> {
                            SettingsScreen(
                                prefs = preferencesManager,
                                downloads = allDownloads,
                                strings = strings,
                                onBackupData = {
                                    scope.launch(Dispatchers.IO) {
                                        preferencesManager.setCloudSyncTime(System.currentTimeMillis())
                                    }
                                },
                                onRestoreData = {
                                    scope.launch(Dispatchers.IO) {
                                        preferencesManager.setCloudSyncTime(System.currentTimeMillis())
                                    }
                                },
                                onDeleteFile = { downloadId ->
                                    downloadManager.cancelOrDeleteDownload(downloadId)
                                },
                                onPlayMedia = { path, title ->
                                    activePlaybackPath = path
                                    activePlaybackTitle = title
                                }
                            )
                        }
                    }
                }

                // Floating Clipboard Detected URL Banner
                AnimatedVisibility(
                    visible = clipboardDetectedUrl != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    clipboardDetectedUrl?.let { url ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Download Link in Clipboard?",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        mediaToDownload = DetectedMedia(
                                            title = "Media from Clipboard",
                                            sourceUrl = url,
                                            directUrl = url,
                                            mimeType = "video/mp4"
                                        )
                                        dismissedClipboardUrl = url
                                        clipboardDetectedUrl = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 12.sp)
                                }

                                IconButton(
                                    onClick = {
                                        dismissedClipboardUrl = url
                                        clipboardDetectedUrl = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // MX Player Full-Screen Overlay
        activePlaybackPath?.let { path ->
            MXPlayerView(
                mediaPath = path,
                title = activePlaybackTitle,
                onClose = { activePlaybackPath = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Snaptube-style Download Quality & Format Dialog
        mediaToDownload?.let { media ->
            QualitySelectionDialog(
                detectedMedia = media,
                strings = strings,
                onDismiss = { mediaToDownload = null },
                onDownload = { quality, isPrivate, scheduleTime ->
                    downloadManager.enqueueDownload(
                        title = media.title,
                        url = quality.url.ifBlank { media.directUrl },
                        format = quality.format,
                        quality = quality.label,
                        thumbnailUrl = media.thumbnailUrl,
                        scheduledTime = scheduleTime,
                        isPrivate = isPrivate
                    )
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            )
        }
    }
}
