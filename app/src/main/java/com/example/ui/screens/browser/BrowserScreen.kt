package com.example.ui.screens.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.WallpaperStyle
import com.example.data.model.DetectedMedia
import com.example.ui.i18n.StringsProvider
import java.io.ByteArrayInputStream
import java.util.UUID

// Ad blocking domain list
private val AD_DOMAINS = setOf(
    "doubleclick.net",
    "googleadservices.com",
    "googlesyndication.com",
    "adnxs.com",
    "popads.net",
    "scorecardresearch.com",
    "serving-sys.com",
    "adservice.google.com",
    "advertising.com",
    "taboola.com",
    "outbrain.com"
)

// Curated suggested sites including user requested: dipraj.carrd.co, facebook, instagram, youtube, etc.
data class SuggestedSite(
    val title: String,
    val url: String,
    val isFeatured: Boolean = false,
    val iconTint: Long = 0xFFFF9800
)

val SUGGESTED_SITES = listOf(
    SuggestedSite("Dipraj", "https://dipraj.carrd.co", isFeatured = true, iconTint = 0xFFFFD700),
    SuggestedSite("YouTube", "https://m.youtube.com", iconTint = 0xFFFF0000),
    SuggestedSite("Facebook", "https://m.facebook.com", iconTint = 0xFF1877F2),
    SuggestedSite("Instagram", "https://www.instagram.com", iconTint = 0xFFE1306C),
    SuggestedSite("TikTok", "https://www.tiktok.com", iconTint = 0xFF00F2FE),
    SuggestedSite("Reddit", "https://www.reddit.com", iconTint = 0xFFFF4500),
    SuggestedSite("Vimeo", "https://vimeo.com/watch", iconTint = 0xFF1AB7EA),
    SuggestedSite("X / Twitter", "https://x.com", iconTint = 0xFFFFFFFF)
)

data class SampleMedia(val title: String, val url: String, val thumb: String, val resolution: String)
val SAMPLE_TEST_VIDEOS = listOf(
    SampleMedia(
        "Big Buck Bunny (HD 720p)",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
        "720p MP4"
    ),
    SampleMedia(
        "Elephants Dream (1080p)",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
        "1080p MP4"
    ),
    SampleMedia(
        "Tears of Steel (Sci-Fi)",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
        "1080p MP4"
    ),
    SampleMedia(
        "Sintel Animated Demo",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
        "720p MP4"
    )
)

// Activity-based News Item
data class ActivityNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val timeAgo: String,
    val source: String,
    val icon: ImageVector,
    val linkUrl: String
)

// Multi-Tab Representation
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var url: String? = null,
    var title: String = "Home Portal",
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false
)

// Long-press Context Action Item
data class ContextActionTarget(
    val title: String,
    val url: String,
    val isMedia: Boolean = false
)

class SnapSniffer(val onMediaSniffed: (url: String, title: String) -> Unit) {
    @JavascriptInterface
    fun onMediaFound(url: String, title: String) {
        onMediaSniffed(url, title)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    strings: StringsProvider.AppStrings,
    isAdBlockEnabled: Boolean,
    bookmarks: List<BookmarkEntity>,
    wallpaperStyle: WallpaperStyle,
    onWallpaperChanged: (WallpaperStyle) -> Unit,
    userDownloads: List<DownloadEntity>,
    prefs: PreferencesManager? = null,
    showBanner: Boolean = true,
    showBookmarks: Boolean = true,
    showNews: Boolean = true,
    showSamples: Boolean = true,
    onMediaDetected: (DetectedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Tabs State
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(url = null, title = "Home Portal")
        )
    }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var showTabSwitcher by remember { mutableStateOf(false) }

    // Tap and Hold Context Menus
    var activeContextTarget by remember { mutableStateOf<ContextActionTarget?>(null) }
    var showDevConsole by remember { mutableStateOf(false) }

    val activeTab = tabs.getOrNull(activeTabIndex) ?: tabs[0]
    var currentUrl by remember(activeTabIndex) { mutableStateOf(activeTab.url) }
    var inputUrl by remember(activeTabIndex) { mutableStateOf(activeTab.url ?: "") }
    var pageTitle by remember(activeTabIndex) { mutableStateOf(activeTab.title) }

    var webView: WebView? by remember { mutableStateOf(null) }
    var loadProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var renderCrashed by remember { mutableStateOf(false) }

    var showWallpaperPicker by remember { mutableStateOf(false) }
    val detectedList = remember { mutableStateListOf<DetectedMedia>() }

    // Sync active tab state
    fun navigateTo(url: String?) {
        currentUrl = url
        inputUrl = url ?: ""
        activeTab.url = url
        activeTab.title = if (url == null) "Home Portal" else "Loading..."
        if (url != null) {
            webView?.loadUrl(url)
        }
    }

    fun createNewTab(targetUrl: String? = null) {
        val newTab = BrowserTab(
            url = targetUrl,
            title = if (targetUrl == null) "Home Portal" else "Loading..."
        )
        tabs.add(newTab)
        activeTabIndex = tabs.size - 1
        currentUrl = targetUrl
        inputUrl = targetUrl ?: ""
        if (targetUrl != null) {
            webView?.loadUrl(targetUrl)
        }
    }

    fun closeTab(index: Int) {
        if (tabs.size > 1) {
            tabs.removeAt(index)
            if (activeTabIndex >= tabs.size) {
                activeTabIndex = tabs.size - 1
            }
            val current = tabs[activeTabIndex]
            currentUrl = current.url
            inputUrl = current.url ?: ""
            if (current.url != null) {
                webView?.loadUrl(current.url!!)
            }
        } else {
            // Reset lone tab to home
            tabs[0].url = null
            tabs[0].title = "Home Portal"
            currentUrl = null
            inputUrl = ""
            detectedList.clear()
        }
    }

    BackHandler(enabled = currentUrl != null) {
        if (canGoBack && webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            navigateTo(null)
            detectedList.clear()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Address Bar & Controls with Tab Counter and Dev Console
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUrl != null) {
                        IconButton(
                            onClick = {
                                if (canGoBack && webView?.canGoBack() == true) {
                                    webView?.goBack()
                                } else {
                                    navigateTo(null)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }

                        IconButton(
                            onClick = {
                                navigateTo(null)
                                detectedList.clear()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        }
                    }

                    // Input Address Field
                    TextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder = {
                            Text(
                                strings.searchOrUrlPlaceholder,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            var target = inputUrl.trim()
                            if (target.isNotBlank()) {
                                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                    target = if (target.contains(".") && !target.contains(" ")) {
                                        "https://$target"
                                    } else {
                                        "https://www.google.com/search?q=${target.replace(" ", "+")}"
                                    }
                                }
                                navigateTo(target)
                            }
                        }),
                        leadingIcon = {
                            if (isAdBlockEnabled) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "AdBlock Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentUrl != null && isLoading) {
                                    IconButton(onClick = { webView?.stopLoading() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                                    }
                                } else if (currentUrl != null) {
                                    IconButton(onClick = { webView?.reload() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = { showWallpaperPicker = true }) {
                                        Icon(Icons.Default.Wallpaper, contentDescription = "Wallpaper", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Tab Counter Button (e.g. "[ 2 ]")
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTabSwitcher = true },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Tab,
                                contentDescription = "Tabs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${tabs.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Developer Options Icon (Click or Long-press triggers Master Console)
                    IconButton(
                        onClick = { showDevConsole = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Master Developer Console",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isLoading && currentUrl != null) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = detectedList.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        detectedList.firstOrNull()?.let { media ->
                            onMediaDetected(media)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Download Detected Video",
                            modifier = Modifier.size(32.dp)
                        )
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = (-10).dp),
                            containerColor = Color.Red
                        ) {
                            Text("${detectedList.size}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (currentUrl == null) {
            // Home Portal with Tap-and-Hold Context Actions & Long-press for Developer Console
            BrowserHomePortal(
                wallpaperStyle = wallpaperStyle,
                onSelectWallpaper = { showWallpaperPicker = true },
                userDownloads = userDownloads,
                showBanner = showBanner,
                showBookmarks = showBookmarks,
                showNews = showNews,
                showSamples = showSamples,
                onNavigateToUrl = { url -> navigateTo(url) },
                onShowContextMenu = { title, url, isMedia ->
                    activeContextTarget = ContextActionTarget(title, url, isMedia)
                },
                onOpenDevConsole = { showDevConsole = true },
                onDownloadSample = { sample ->
                    val detected = DetectedMedia(
                        title = sample.title,
                        sourceUrl = sample.url,
                        directUrl = sample.url,
                        thumbnailUrl = sample.thumb,
                        mimeType = "video/mp4"
                    )
                    onMediaDetected(detected)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Live WebView with Long-press Detection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (renderCrashed) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Web page refreshed to free memory.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            renderCrashed = false
                            currentUrl?.let { webView?.loadUrl(it) }
                        }) {
                            Text("Reload Page")
                        }
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                val userAgentSetting = prefs?.devUserAgentMode?.value ?: "DEFAULT"
                                val bypassSsl = prefs?.devBypassSslErrors?.value ?: false
                                val hardwareAccel = prefs?.devHardwareAccel?.value ?: true

                                if (hardwareAccel) {
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                } else {
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                }

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT

                                    userAgentString = when (userAgentSetting) {
                                        "DESKTOP" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                        "IPHONE" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
                                        "IPAD" -> "Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
                                        else -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                    }
                                }

                                // WebView Tap and Hold / Long Press Detection on Links & Media
                                setOnLongClickListener {
                                    val hitTest = hitTestResult
                                    val extra = hitTest.extra
                                    if (!extra.isNullOrBlank()) {
                                        val isMedia = hitTest.type == WebView.HitTestResult.IMAGE_TYPE ||
                                                hitTest.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
                                                extra.endsWith(".mp4") || extra.endsWith(".mp3")
                                        activeContextTarget = ContextActionTarget(
                                            title = if (isMedia) "Media / Link" else "Web Link",
                                            url = extra,
                                            isMedia = isMedia
                                        )
                                        true
                                    } else {
                                        false
                                    }
                                }

                                addJavascriptInterface(
                                    SnapSniffer { url, title ->
                                        post {
                                            if (detectedList.none { it.directUrl == url }) {
                                                val detected = DetectedMedia(
                                                    title = title.ifBlank { pageTitle },
                                                    sourceUrl = currentUrl ?: url,
                                                    directUrl = url,
                                                    mimeType = if (url.endsWith(".mp3") || url.endsWith(".m4a")) "audio/mp4" else "video/mp4"
                                                )
                                                detectedList.add(0, detected)
                                            }
                                        }
                                    },
                                    "SnapSniffer"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        post { renderCrashed = true }
                                        return true
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        if (bypassSsl) {
                                            handler?.proceed()
                                        } else {
                                            super.onReceivedSslError(view, handler, error)
                                        }
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val reqUrl = request?.url?.toString() ?: return null
                                        if (isAdBlockEnabled) {
                                            for (adDomain in AD_DOMAINS) {
                                                if (reqUrl.contains(adDomain)) {
                                                    return WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                                                }
                                            }
                                        }

                                        // Passive stream detection
                                        val lower = reqUrl.lowercase()
                                        if (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mp3") || lower.contains("googlevideo.com/videoplayback")) {
                                            post {
                                                if (detectedList.none { it.directUrl == reqUrl }) {
                                                    val detected = DetectedMedia(
                                                        title = pageTitle.ifBlank { "Stream_${System.currentTimeMillis() % 1000}" },
                                                        sourceUrl = currentUrl ?: reqUrl,
                                                        directUrl = reqUrl,
                                                        mimeType = if (lower.contains(".mp3")) "audio/mp4" else "video/mp4"
                                                    )
                                                    detectedList.add(0, detected)
                                                }
                                            }
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        canGoBack = view?.canGoBack() ?: false
                                        canGoForward = view?.canGoForward() ?: false
                                        pageTitle = view?.title ?: "Page"
                                        activeTab.title = pageTitle

                                        val snifferScript = """
                                            (function() {
                                                try {
                                                    var videos = Array.from(document.querySelectorAll('video, audio, source'));
                                                    for (var i = 0; i < videos.length; i++) {
                                                        var src = videos[i].src || videos[i].currentSrc;
                                                        if (src && src.startsWith('http')) {
                                                            window.SnapSniffer.onMediaFound(src, document.title);
                                                        }
                                                    }
                                                } catch(e) {}
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(snifferScript, null)
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = newProgress
                                        if (newProgress == 100) isLoading = false
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        title?.let {
                                            pageTitle = it
                                            activeTab.title = it
                                        }
                                    }
                                }

                                currentUrl?.let { loadUrl(it) }
                                webView = this
                            }
                        }
                    )
                }
            }
        }
    }

    // Tab Switcher Modal
    if (showTabSwitcher) {
        TabSwitcherSheet(
            tabs = tabs,
            activeTabIndex = activeTabIndex,
            onSelectTab = { index ->
                activeTabIndex = index
                val tab = tabs[index]
                currentUrl = tab.url
                inputUrl = tab.url ?: ""
                showTabSwitcher = false
                if (tab.url != null) {
                    webView?.loadUrl(tab.url!!)
                }
            },
            onNewTab = {
                createNewTab(null)
                showTabSwitcher = false
            },
            onCloseTab = { index ->
                closeTab(index)
            },
            onCloseAll = {
                while (tabs.size > 1) {
                    tabs.removeAt(1)
                }
                tabs[0].url = null
                tabs[0].title = "Home Portal"
                activeTabIndex = 0
                currentUrl = null
                inputUrl = ""
                showTabSwitcher = false
            },
            onDismiss = { showTabSwitcher = false }
        )
    }

    // Tap and Hold Context Menu Modal
    activeContextTarget?.let { target ->
        BrowserContextMenuSheet(
            target = target,
            onOpenInCurrentTab = {
                navigateTo(target.url)
                activeContextTarget = null
            },
            onOpenInNewTab = {
                createNewTab(target.url)
                Toast.makeText(context, "Opened in new tab", Toast.LENGTH_SHORT).show()
                activeContextTarget = null
            },
            onDownloadMedia = {
                val detected = DetectedMedia(
                    title = target.title,
                    sourceUrl = target.url,
                    directUrl = target.url,
                    mimeType = if (target.url.endsWith(".mp3")) "audio/mp4" else "video/mp4"
                )
                onMediaDetected(detected)
                activeContextTarget = null
            },
            onCopyLink = {
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("Link", target.url))
                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                activeContextTarget = null
            },
            onShareLink = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, target.url)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share link"))
                activeContextTarget = null
            },
            onDismiss = { activeContextTarget = null }
        )
    }

    // Master Developer & Engine Console Modal
    if (showDevConsole) {
        MasterDevConsoleDialog(
            prefs = prefs,
            webView = webView,
            activeUrl = currentUrl,
            onDismiss = { showDevConsole = false }
        )
    }

    // Wallpaper Picker Dialog
    if (showWallpaperPicker) {
        AlertDialog(
            onDismissRequest = { showWallpaperPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Background Wallpaper")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WallpaperStyle.entries.forEach { style ->
                        val isSelected = style == wallpaperStyle
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onWallpaperChanged(style)
                                    showWallpaperPicker = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                style.gradientColors.map { Color(it) }
                                            )
                                        )
                                    )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = style.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWallpaperPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Browser Home Portal with Tap-and-Hold on everything & long-press header for Developer Options
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserHomePortal(
    wallpaperStyle: WallpaperStyle,
    onSelectWallpaper: () -> Unit,
    userDownloads: List<DownloadEntity>,
    showBanner: Boolean = true,
    showBookmarks: Boolean = true,
    showNews: Boolean = true,
    showSamples: Boolean = true,
    onNavigateToUrl: (String) -> Unit,
    onShowContextMenu: (title: String, url: String, isMedia: Boolean) -> Unit,
    onOpenDevConsole: () -> Unit,
    onDownloadSample: (SampleMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasVideoActivity = userDownloads.any { !it.format.isAudio }
    val hasAudioActivity = userDownloads.any { it.format.isAudio }

    val activityNews = remember(hasVideoActivity, hasAudioActivity, userDownloads.size) {
        val list = mutableListOf<ActivityNewsItem>()

        // Dipraj feature news
        list.add(
            ActivityNewsItem(
                id = "dipraj_hub",
                title = "Featured Creator: Discover Dipraj's Portfolio & Projects",
                summary = "Explore personal works, creative media showcases, and latest web design highlights at dipraj.carrd.co.",
                category = "Creator Spotlight",
                timeAgo = "Trending Now",
                source = "dipraj.carrd.co",
                icon = Icons.Default.Star,
                linkUrl = "https://dipraj.carrd.co"
            )
        )

        if (hasVideoActivity) {
            list.add(
                ActivityNewsItem(
                    id = "cinema_4k",
                    title = "Cinema 4K & HDR10+: New Encoding Benchmarks Released",
                    summary = "AV1 and H.265 high-efficiency video streams show 48% data reduction for mobile 1080p and 4K playback.",
                    category = "Movies & Cinema",
                    timeAgo = "2h ago",
                    source = "VideoTech Daily",
                    icon = Icons.Default.Movie,
                    linkUrl = "https://en.wikipedia.org/wiki/AV1"
                )
            )
            list.add(
                ActivityNewsItem(
                    id = "hollywood_sci_fi",
                    title = "Top Sci-Fi & Animated Releases This Weekend",
                    summary = "Check out the newest visual effects masterpieces and open-source cinema demos streaming online.",
                    category = "Entertainment",
                    timeAgo = "4h ago",
                    source = "CinemaWire",
                    icon = Icons.Default.Theaters,
                    linkUrl = "https://archive.org/details/movies"
                )
            )
        }

        if (hasAudioActivity) {
            list.add(
                ActivityNewsItem(
                    id = "audio_lossless",
                    title = "Hi-Res 320kbps Audio & Lossless Streaming Trends",
                    summary = "Why high-bitrate audio extraction preserves richer dynamics and spatial acoustics on mobile headphones.",
                    category = "Music & Audio",
                    timeAgo = "1h ago",
                    source = "SoundWave Mag",
                    icon = Icons.Default.Audiotrack,
                    linkUrl = "https://en.wikipedia.org/wiki/MP3"
                )
            )
        }

        list.add(
            ActivityNewsItem(
                id = "social_trends",
                title = "Vertical Video Algorithms: What Goes Viral in 2026",
                summary = "How short-form video formats on Reels, TikTok, and Shorts are driving 80% of mobile internet video bandwidth.",
                category = "Social Media",
                timeAgo = "5h ago",
                source = "TrendPulse",
                icon = Icons.Default.TrendingUp,
                linkUrl = "https://www.reddit.com"
            )
        )

        list
    }

    val gradientColors = wallpaperStyle.gradientColors.map { Color(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Wallpaper & Greeting Header: Tap and hold here opens Master Developer Console!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onOpenDevConsole() }
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Discover & Download",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    "DEV HOLD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Hold header for Developer Options • Wallpaper: ${wallpaperStyle.title}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    AssistChip(
                        onClick = onSelectWallpaper,
                        label = { Text("Change BG", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            labelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            // Suggested Sites Grid: Supports Tap and Hold for Context Options
            if (showBookmarks) {
                item {
                    Text(
                        "Suggested Sites (Hold for options)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(SUGGESTED_SITES) { site ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { onNavigateToUrl(site.url) },
                                        onLongClick = { onShowContextMenu(site.title, site.url, false) }
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(site.iconTint).copy(alpha = 0.2f))
                                        .then(
                                            if (site.isFeatured) BorderStroke(1.5.dp, Color(site.iconTint)).let {
                                                Modifier.background(Color.Transparent)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = site.title.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color(site.iconTint)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = site.title,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Activity News Feed: Tap and Hold for Context Menu
            if (showNews) {
                item {
                    Text(
                        "Activity Feed & Trending News",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                items(activityNews) { news ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onNavigateToUrl(news.linkUrl) },
                                onLongClick = { onShowContextMenu(news.title, news.linkUrl, false) }
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0x331C1D2E)),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = news.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        news.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "${news.source} • ${news.timeAgo}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = news.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = news.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // Quick Test Videos: Tap and Hold to inspect or download
            if (showSamples) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Quick Test Videos (Hold for options)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(SAMPLE_TEST_VIDEOS) { sample ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onDownloadSample(sample) },
                                onLongClick = { onShowContextMenu(sample.title, sample.url, true) }
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0x28FFFFFF)),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = sample.thumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp, 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sample.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    sample.resolution,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = { onDownloadSample(sample) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Download", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp)) // padding for liquid glass bottom bar
            }
        }
    }
}

// Tab Switcher Modal
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<BrowserTab>,
    activeTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onCloseAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tab, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Browser Tabs (${tabs.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row {
                    TextButton(onClick = onCloseAll) {
                        Text("Close All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onNewTab,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Tab", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(tabs.size) { index ->
                    val tab = tabs[index]
                    val isSelected = index == activeTabIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTab(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tab ${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { onCloseTab(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Tab",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tab.url ?: "Home Portal",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Tap and Hold Context Menu Modal
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserContextMenuSheet(
    target: ContextActionTarget,
    onOpenInCurrentTab: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onDownloadMedia: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = target.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = target.url,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Actions
            ContextMenuItem(
                icon = Icons.Default.FileDownload,
                title = "Download / Sniff Media",
                subtitle = "Extract video or audio stream immediately"
            ) { onDownloadMedia() }

            ContextMenuItem(
                icon = Icons.Default.OpenInNew,
                title = "Open in New Tab",
                subtitle = "Keep current page and view in background tab"
            ) { onOpenInNewTab() }

            ContextMenuItem(
                icon = Icons.Default.Launch,
                title = "Open in Current Tab",
                subtitle = "Navigate to link on this screen"
            ) { onOpenInCurrentTab() }

            ContextMenuItem(
                icon = Icons.Default.ContentCopy,
                title = "Copy Link URL",
                subtitle = "Copy to clipboard for external sharing"
            ) { onCopyLink() }

            ContextMenuItem(
                icon = Icons.Default.Share,
                title = "Share Link",
                subtitle = "Send link to social apps"
            ) { onShareLink() }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ContextMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Master Developer & Engine Console Modal (Secret hold on browser or code icon)
@Composable
fun MasterDevConsoleDialog(
    prefs: PreferencesManager?,
    webView: WebView?,
    activeUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedUserAgent by remember { mutableStateOf(prefs?.devUserAgentMode?.value ?: "DEFAULT") }
    var bypassSsl by remember { mutableStateOf(prefs?.devBypassSslErrors?.value ?: false) }
    var turboMultiplier by remember { mutableIntStateOf(prefs?.devTurboMultiplier?.value ?: 4) }
    var hardwareAccel by remember { mutableStateOf(prefs?.devHardwareAccel?.value ?: true) }
    var jsConsoleInput by remember { mutableStateOf("document.title") }
    var jsConsoleResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("SnapLoad Master Dev Console", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Advanced Browser & Engine Tweaks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("1. User-Agent Switcher", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("DEFAULT", "DESKTOP", "IPHONE", "IPAD").forEach { mode ->
                                FilterChip(
                                    selected = selectedUserAgent == mode,
                                    onClick = {
                                        selectedUserAgent = mode
                                        prefs?.setDevUserAgentMode(mode)
                                        Toast.makeText(context, "User-Agent: $mode (reloads page)", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(mode, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Divider()
                        Text("2. Engine & Network Acceleration", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Turbo Parallel Chunks (${turboMultiplier}x)", fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1, 2, 4, 8).forEach { multiplier ->
                                    FilterChip(
                                        selected = turboMultiplier == multiplier,
                                        onClick = {
                                            turboMultiplier = multiplier
                                            prefs?.setDevTurboMultiplier(multiplier)
                                        },
                                        label = { Text("${multiplier}x", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bypass Untrusted SSL Certificates", fontSize = 12.sp)
                            Switch(
                                checked = bypassSsl,
                                onCheckedChange = {
                                    bypassSsl = it
                                    prefs?.setDevBypassSslErrors(it)
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GPU Hardware Acceleration", fontSize = 12.sp)
                            Switch(
                                checked = hardwareAccel,
                                onCheckedChange = {
                                    hardwareAccel = it
                                    prefs?.setDevHardwareAccel(it)
                                }
                            )
                        }
                    }

                    item {
                        Divider()
                        Text("3. Live JavaScript Evaluation Console", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(
                            value = jsConsoleInput,
                            onValueChange = { jsConsoleInput = it },
                            placeholder = { Text("e.g. document.querySelector('video')?.src") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    webView?.evaluateJavascript(jsConsoleInput) { result ->
                                        jsConsoleResult = result
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Execute Code", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    webView?.clearCache(true)
                                    CookieManager.getInstance().removeAllCookies(null)
                                    Toast.makeText(context, "Cache and cookies flushed", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Flush Cache", fontSize = 11.sp)
                            }
                        }

                        jsConsoleResult?.let { res ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Result: $res",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Console")
            }
        }
    )
}
