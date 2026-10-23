package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.VaultCrypto
import com.example.data.local.*
import com.example.ui.components.LiquidGlassSwitch
import com.example.ui.i18n.StringsProvider
import com.example.ui.screens.browser.MasterDevConsoleDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    downloads: List<DownloadEntity>,
    strings: StringsProvider.AppStrings,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    onDeleteFile: (Long) -> Unit,
    onPlayMedia: (String, String) -> Unit,
    resetTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val themeMode by prefs.themeMode.collectAsState()
    val accentTheme by prefs.accentTheme.collectAsState()
    val layoutMode by prefs.layoutMode.collectAsState()
    val wallpaperStyle by prefs.wallpaperStyle.collectAsState()
    val appName by prefs.appName.collectAsState()
    val appIconStyle by prefs.appIconStyle.collectAsState()

    val networkPolicy by prefs.networkPolicy.collectAsState()
    val videoPath by prefs.videoPath.collectAsState()
    val audioPath by prefs.audioPath.collectAsState()
    val imagePath by prefs.imagePath.collectAsState()
    val maxParallel by prefs.maxParallelDownloads.collectAsState()
    val lowPowerMode by prefs.lowPowerMode.collectAsState()
    val turboSpeedBoost by prefs.turboSpeedBoost.collectAsState()
    val clipboardAutoDetect by prefs.clipboardAutoDetect.collectAsState()

    // Modular UI
    val showBrowserBanner by prefs.showBrowserBanner.collectAsState()
    val showBrowserBookmarks by prefs.showBrowserBookmarks.collectAsState()
    val showBrowserNews by prefs.showBrowserNews.collectAsState()
    val showBrowserSamples by prefs.showBrowserSamples.collectAsState()
    val showDashboardSpeedMeter by prefs.showDashboardSpeedMeter.collectAsState()
    val showDashboardStorageBar by prefs.showDashboardStorageBar.collectAsState()

    // Notifications
    val notifyToolbar by prefs.notifyToolbar.collectAsState()
    val notifyDownloading by prefs.notifyDownloading.collectAsState()
    val notifyCompleted by prefs.notifyCompleted.collectAsState()
    val notifyNews by prefs.notifyNews.collectAsState()
    val notifySoundVibrate by prefs.notifySoundVibrate.collectAsState()

    // Bottom Bar Customization
    val bottomBarTransparency by prefs.bottomBarTransparency.collectAsState()
    val bottomBarBlurLevel by prefs.bottomBarBlurLevel.collectAsState()
    val bottomBarLabelStyle by prefs.bottomBarLabelStyle.collectAsState()
    val isSwipeNavigationEnabled by prefs.isSwipeNavigationEnabled.collectAsState()
    val isInfiniteSwipeEnabled by prefs.isInfiniteSwipeEnabled.collectAsState()
    val showBottomFloatingSearch by prefs.showBottomFloatingSearch.collectAsState()
    val enabledBottomTabs by prefs.enabledBottomTabs.collectAsState()

    // Developer & Engine
    val devBypassSslErrors by prefs.devBypassSslErrors.collectAsState()
    val devHardwareAccel by prefs.devHardwareAccel.collectAsState()
    var showDevDialog by remember { mutableStateOf(false) }

    val adBlockEnabled by prefs.adBlockEnabled.collectAsState()
    val languageCode by prefs.languageCode.collectAsState()
    val cloudSyncTime by prefs.cloudSyncTime.collectAsState()

    // Accordion foldered expansion state (only open 1 or 2 at a time for aesthetic minimalism)
    var expandedFolder by remember { mutableStateOf<String?>("appearance") }
    val listState = rememberLazyListState()

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0L) {
            listState.animateScrollToItem(0)
            expandedFolder = "appearance"
        }
    }

    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showNameChangeDialog by remember { mutableStateOf(false) }
    var showPathEditDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showStorageAnalysis by remember { mutableStateOf(false) }

    val freeDeviceSpace = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            18400000000L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Settings & Customization", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick Storage Glance Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStorageAnalysis = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Storage: ${formatBytes(freeDeviceSpace)} Free", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tap for detailed space analysis & deep clean", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 1. FOLDER: Visual Themes & Wallpapers
            item {
                SettingsFolderCard(
                    folderId = "appearance",
                    title = "Themes, Wallpapers & Layout",
                    summary = "${themeMode.name} • ${accentTheme.title}",
                    icon = Icons.Default.Palette,
                    isExpanded = expandedFolder == "appearance",
                    onToggle = { expandedFolder = if (expandedFolder == "appearance") null else "appearance" }
                ) {
                    Text(strings.themeMode, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == ThemeMode.DARK_AMOLED,
                            onClick = { prefs.setThemeMode(ThemeMode.DARK_AMOLED) },
                            label = { Text("AMOLED") }
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.DARK_SLATE,
                            onClick = { prefs.setThemeMode(ThemeMode.DARK_SLATE) },
                            label = { Text("Dark Slate") }
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { prefs.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text("Light") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(strings.accentColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentTheme.entries.forEach { accent ->
                            val isSelected = accent == accentTheme
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.primaryHex))
                                    .clickable { prefs.setAccentTheme(accent) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Browser Wallpaper Style", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    WallpaperStyle.entries.forEach { style ->
                        val isSelected = style == wallpaperStyle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { prefs.setWallpaperStyle(style) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { prefs.setWallpaperStyle(style) })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(style.title, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.layoutStyle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = layoutMode == LayoutMode.DETAILED_LIST,
                            onClick = { prefs.setLayoutMode(LayoutMode.DETAILED_LIST) },
                            label = { Text("Detailed List") }
                        )
                        FilterChip(
                            selected = layoutMode == LayoutMode.COMPACT_GRID,
                            onClick = { prefs.setLayoutMode(LayoutMode.COMPACT_GRID) },
                            label = { Text("Compact Grid") }
                        )
                    }
                }
            }

            // 2. FOLDER: App Branding & Custom Identity
            item {
                SettingsFolderCard(
                    folderId = "branding",
                    title = "App Branding & Launcher Icon",
                    summary = "$appName • ${appIconStyle.title}",
                    icon = Icons.Default.Badge,
                    isExpanded = expandedFolder == "branding",
                    onToggle = { expandedFolder = if (expandedFolder == "branding") null else "branding" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNameChangeDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Custom App Name", fontWeight = FontWeight.SemiBold)
                            Text(appName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { showNameChangeDialog = true }) {
                            Text("Rename")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Launcher Icon Appearance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppIconStyle.entries.forEach { style ->
                            val isSelected = style == appIconStyle
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        prefs.setAppIconStyle(style)
                                        Toast.makeText(context, "Icon theme set to ${style.title}", Toast.LENGTH_SHORT).show()
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(style.primaryColorHex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(style.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. FOLDER: Liquid Glass Bottom Bar Customizer
            item {
                SettingsFolderCard(
                    folderId = "bottom_bar",
                    title = "Bottom Bar & Swipe Customizer",
                    summary = "Transparency, blur, swipable gestures & tab selector",
                    icon = Icons.Default.ViewStream,
                    isExpanded = expandedFolder == "bottom_bar",
                    onToggle = { expandedFolder = if (expandedFolder == "bottom_bar") null else "bottom_bar" }
                ) {
                    Text("Liquid Transparency (${(bottomBarTransparency * 100).toInt()}%)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Slider(
                        value = bottomBarTransparency,
                        onValueChange = { prefs.setBottomBarTransparency(it) },
                        valueRange = 0.2f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Glass Glow & Blur Density", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "Subtle Glass", 2 to "Medium Liquid", 3 to "Intense Frosted").forEach { (lvl, title) ->
                            FilterChip(
                                selected = bottomBarBlurLevel == lvl,
                                onClick = { prefs.setBottomBarBlurLevel(lvl) },
                                label = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Tab Label Display Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BottomBarLabelStyle.entries.forEach { style ->
                            FilterChip(
                                selected = bottomBarLabelStyle == style,
                                onClick = { prefs.setBottomBarLabelStyle(style) },
                                label = {
                                    Text(
                                        when (style) {
                                            BottomBarLabelStyle.ACTIVE_ONLY -> "Active Tab"
                                            BottomBarLabelStyle.ALWAYS_SHOW -> "All Labels"
                                            BottomBarLabelStyle.ICON_ONLY -> "Icon Only"
                                        },
                                        fontSize = 10.sp
                                    )
                                }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Swipe & Navigation Physics", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactSwitchRow("Swipe Gestures Between Screens", isSwipeNavigationEnabled) { prefs.setSwipeNavigationEnabled(it) }
                    CompactSwitchRow("Infinite Swipe Navigation", isInfiniteSwipeEnabled) { prefs.setInfiniteSwipeEnabled(it) }
                    CompactSwitchRow("Floating Round Quick Search Bar", showBottomFloatingSearch) { prefs.setShowBottomFloatingSearch(it) }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Customizable Bottom Bar Tabs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Select which tabs appear on the dock (minimum 2 tabs)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    val allTabsList = listOf(
                        "DOWNLOADS" to "📥 Downloads Dashboard",
                        "BROWSER" to "🌐 Web & Video Browser",
                        "FILE_MANAGER" to "📁 File Manager & Vault",
                        "SETTINGS" to "⚙️ App Settings"
                    )

                    allTabsList.forEach { (tabId, label) ->
                        val isChecked = enabledBottomTabs.contains(tabId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = enabledBottomTabs.toMutableList()
                                    if (isChecked) {
                                        if (current.size > 2) {
                                            current.remove(tabId)
                                            prefs.setEnabledBottomTabs(current)
                                        } else {
                                            Toast.makeText(context, "At least 2 tabs must remain visible", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        current.add(tabId)
                                        prefs.setEnabledBottomTabs(current)
                                    }
                                }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 12.sp)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val current = enabledBottomTabs.toMutableList()
                                    if (checked) {
                                        if (!current.contains(tabId)) current.add(tabId)
                                    } else {
                                        if (current.size > 2) {
                                            current.remove(tabId)
                                        } else {
                                            Toast.makeText(context, "At least 2 tabs must remain visible", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    prefs.setEnabledBottomTabs(current)
                                }
                            )
                        }
                    }
                }
            }

            // 4. FOLDER: Modular UI Customizer
            item {
                SettingsFolderCard(
                    folderId = "modular_ui",
                    title = "Modular UI Customizer",
                    summary = "Choose what sections appear on your app",
                    icon = Icons.Default.DashboardCustomize,
                    isExpanded = expandedFolder == "modular_ui",
                    onToggle = { expandedFolder = if (expandedFolder == "modular_ui") null else "modular_ui" }
                ) {
                    Text("Browser Screen Sections", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactSwitchRow("Show Browser Welcome Hero Banner", showBrowserBanner) { prefs.setShowBrowserBanner(it) }
                    CompactSwitchRow("Show Suggested Video Sites Grid", showBrowserBookmarks) { prefs.setShowBrowserBookmarks(it) }
                    CompactSwitchRow("Show Activity & Trending News Section", showBrowserNews) { prefs.setShowBrowserNews(it) }
                    CompactSwitchRow("Show 1-Click Test Videos Ribbon", showBrowserSamples) { prefs.setShowBrowserSamples(it) }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Dashboard Sections", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactSwitchRow("Show Real-time Speed Gauge", showDashboardSpeedMeter) { prefs.setShowDashboardSpeedMeter(it) }
                    CompactSwitchRow("Show Multi-segment Storage Bar", showDashboardStorageBar) { prefs.setShowDashboardStorageBar(it) }
                }
            }

            // 4. FOLDER: Download Engine & Network Policy
            item {
                SettingsFolderCard(
                    folderId = "download_engine",
                    title = "Download Engine & Storage Paths",
                    summary = "${networkPolicy.name} • $maxParallel threads",
                    icon = Icons.Default.Speed,
                    isExpanded = expandedFolder == "download_engine",
                    onToggle = { expandedFolder = if (expandedFolder == "download_engine") null else "download_engine" }
                ) {
                    CompactSwitchRow("Turbo Multi-Threaded Speed Boost", turboSpeedBoost) { prefs.setTurboSpeedBoost(it) }
                    CompactSwitchRow("Clipboard Video Link Auto-Detect", clipboardAutoDetect) { prefs.setClipboardAutoDetect(it) }
                    CompactSwitchRow("Battery Saver Low Power Mode", lowPowerMode) { prefs.setLowPowerMode(it) }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Network Conditions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    NetworkPolicy.entries.forEach { policy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { prefs.setNetworkPolicy(policy) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = networkPolicy == policy, onClick = { prefs.setNetworkPolicy(policy) })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(policy.label, fontSize = 12.sp)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Custom Storage Directories", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    PathConfigItem("🎬 Video Path", videoPath) { showPathEditDialog = "Video" to videoPath }
                    PathConfigItem("🎵 Audio Path", audioPath) { showPathEditDialog = "Audio" to audioPath }
                    PathConfigItem("🖼️ Image Path", imagePath) { showPathEditDialog = "Image" to imagePath }
                }
            }

            // 5. FOLDER: Notification Control Center
            item {
                SettingsFolderCard(
                    folderId = "notifications",
                    title = "Notification Controls",
                    summary = "Manage all alerts and toolbar controls",
                    icon = Icons.Default.Notifications,
                    isExpanded = expandedFolder == "notifications",
                    onToggle = { expandedFolder = if (expandedFolder == "notifications") null else "notifications" }
                ) {
                    CompactSwitchRow("Status Bar / Toolbar Notification", notifyToolbar) { prefs.setNotifyToolbar(it) }
                    CompactSwitchRow("Live Downloading Speed & Progress", notifyDownloading) { prefs.setNotifyDownloading(it) }
                    CompactSwitchRow("Download Complete Alerts", notifyCompleted) { prefs.setNotifyCompleted(it) }
                    CompactSwitchRow("News & Activity Feed Alerts", notifyNews) { prefs.setNotifyNews(it) }
                    CompactSwitchRow("Sound & Vibration on Finish", notifySoundVibrate) { prefs.setNotifySoundVibrate(it) }
                }
            }

            // 6. FOLDER: Security & Private Vault
            item {
                SettingsFolderCard(
                    folderId = "security",
                    title = "Security & Private Vault",
                    summary = "AES-256 encrypted sandbox & PIN",
                    icon = Icons.Default.Security,
                    isExpanded = expandedFolder == "security",
                    onToggle = { expandedFolder = if (expandedFolder == "security") null else "security" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPinChangeDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Update 4-Digit Security PIN", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Used to access private files inside Vault tab", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 7. FOLDER: Language & Cloud Sync
            item {
                SettingsFolderCard(
                    folderId = "sync_lang",
                    title = "Language & Cloud Sync",
                    summary = "8 Languages • Backup & Restore",
                    icon = Icons.Default.Public,
                    isExpanded = expandedFolder == "sync_lang",
                    onToggle = { expandedFolder = if (expandedFolder == "sync_lang") null else "sync_lang" }
                ) {
                    Text("Select App Language", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val languages = listOf(
                        "en" to "English",
                        "es" to "Español",
                        "hi" to "हिन्दी",
                        "fr" to "Français",
                        "pt" to "Português",
                        "bn" to "বাংলা",
                        "id" to "Bahasa Indonesia",
                        "ar" to "العربية"
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            languages.take(4).forEach { (code, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { prefs.setLanguageCode(code) }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = languageCode == code, onClick = { prefs.setLanguageCode(code) })
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(name, fontSize = 12.sp)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            languages.drop(4).forEach { (code, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { prefs.setLanguageCode(code) }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = languageCode == code, onClick = { prefs.setLanguageCode(code) })
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(name, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    val syncDate = if (cloudSyncTime > 0) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(cloudSyncTime))
                    } else "Never"
                    Text("Last Cloud Sync: $syncDate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onBackupData()
                                prefs.setCloudSyncTime(System.currentTimeMillis())
                                Toast.makeText(context, "Cloud sync data backed up!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Backup Profile")
                        }

                        OutlinedButton(
                            onClick = {
                                onRestoreData()
                                Toast.makeText(context, "Cloud sync data restored!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Profile")
                        }
                    }
                }
            }

            // 8. FOLDER: Master Developer & Engine Console
            item {
                SettingsFolderCard(
                    folderId = "dev_console",
                    title = "Master Developer & Engine Console",
                    summary = "User-agent, JS console, Turbo chunk simulator",
                    icon = Icons.Default.Terminal,
                    isExpanded = expandedFolder == "dev_console",
                    onToggle = { expandedFolder = if (expandedFolder == "dev_console") null else "dev_console" }
                ) {
                    Text("Full App Developer Engine Console", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Deep customization for browser, network, and execution engine. You can also open this by long-pressing the header on the Browser screen.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDevDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Master Developer Console")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CompactSwitchRow("Bypass Untrusted SSL Certificates", devBypassSslErrors) { prefs.setDevBypassSslErrors(it) }
                    CompactSwitchRow("GPU Hardware Acceleration", devHardwareAccel) { prefs.setDevHardwareAccel(it) }
                }
            }

            // 9. FOLDER: About App & Creator Profile
            item {
                SettingsFolderCard(
                    folderId = "about",
                    title = "About SnapLoad & Creator",
                    summary = "SHAFAYET PARVEZ DIP • Owner & Contact Info",
                    icon = Icons.Default.Person,
                    isExpanded = expandedFolder == "about",
                    onToggle = { expandedFolder = if (expandedFolder == "about") null else "about" }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Creator Photo with Glowing Liquid Gradient Ring
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFFFFD700),
                                            Color(0xFF00E676),
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                )
                                .padding(3.5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.owner_photo),
                                contentDescription = "SHAFAYET PARVEZ DIP - Owner of this app",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Owner Name
                        Text(
                            text = "SHAFAYET PARVEZ DIP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Owner Subtitle / Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Owner of this app : SHAFAYET PARVEZ DIP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // App Description Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = "This is my First App build by Google App Studio and My brain . It's a little try to make an alternative of Snaptube , Vidmate and Other Video downloader apps . If you want to contract me .",
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Direct Contact & Socials",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Contract Website: https://dipraj.carrd.co
                        AboutContactRow(
                            icon = Icons.Default.Language,
                            title = "Contract website",
                            value = "https://dipraj.carrd.co",
                            actionLabel = "Open Site",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dipraj.carrd.co"))
                                context.startActivity(intent)
                            }
                        )

                        // Contract Number: +8801781586834
                        AboutContactRow(
                            icon = Icons.Default.Phone,
                            title = "Contract Number",
                            value = "+8801781586834",
                            actionLabel = "Call",
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801781586834"))
                                context.startActivity(intent)
                            }
                        )

                        // Contract Email: aryan.shafayet@gmail.com
                        AboutContactRow(
                            icon = Icons.Default.Email,
                            title = "Contract Email",
                            value = "aryan.shafayet@gmail.com",
                            actionLabel = "Email",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:aryan.shafayet@gmail.com"))
                                context.startActivity(intent)
                            }
                        )

                        // Whatsapp: Whatsapp (link:wa.me/+8801781586834)
                        AboutContactRow(
                            icon = Icons.Default.Chat,
                            title = "Whatsapp",
                            value = "Whatsapp (link:wa.me/+8801781586834)",
                            actionLabel = "Chat",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/+8801781586834"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp)) // padding for bottom navigation
            }
        }
    }

    // App Name Change Dialog
    if (showNameChangeDialog) {
        var tempName by remember { mutableStateOf(appName) }
        AlertDialog(
            onDismissRequest = { showNameChangeDialog = false },
            title = { Text("Change App Display Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    prefs.setCustomAppName(tempName)
                    showNameChangeDialog = false
                    Toast.makeText(context, "App name set to $tempName", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameChangeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Path Edit Dialog
    showPathEditDialog?.let { (type, path) ->
        var tempPath by remember { mutableStateOf(path) }
        AlertDialog(
            onDismissRequest = { showPathEditDialog = null },
            title = { Text("Edit $type Download Path") },
            text = {
                Column {
                    Text("Customize destination folder for $type files:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempPath,
                        onValueChange = { tempPath = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    when (type) {
                        "Video" -> prefs.setVideoPath(tempPath)
                        "Audio" -> prefs.setAudioPath(tempPath)
                        "Image" -> prefs.setImagePath(tempPath)
                    }
                    showPathEditDialog = null
                    Toast.makeText(context, "$type path saved!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPathEditDialog = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // PIN Change Dialog
    if (showPinChangeDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        val stored = prefs.getVaultPinHash()

        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = { Text("Update Security PIN") },
            text = {
                Column {
                    if (stored != null) {
                        OutlinedTextField(
                            value = oldPin,
                            onValueChange = { oldPin = it.take(4) },
                            label = { Text("Current 4-digit PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.take(4) },
                        label = { Text("New 4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    err?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = newPin.length == 4,
                    onClick = {
                        if (stored != null && !VaultCrypto.verifyPin(oldPin, stored)) {
                            err = "Incorrect current PIN"
                        } else {
                            prefs.setVaultPinHash(VaultCrypto.hashPin(newPin))
                            showPinChangeDialog = false
                            Toast.makeText(context, strings.pinSetSuccess, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Storage Analysis Sheet
    if (showStorageAnalysis) {
        StorageAnalysisDialog(
            downloads = downloads,
            onDismiss = { showStorageAnalysis = false },
            onDeleteFile = onDeleteFile,
            onPlayMedia = onPlayMedia
        )
    }

    // Master Dev Console Dialog
    if (showDevDialog) {
        MasterDevConsoleDialog(
            prefs = prefs,
            webView = null,
            activeUrl = null,
            onDismiss = { showDevDialog = false }
        )
    }
}

@Composable
fun AboutContactRow(
    icon: ImageVector,
    title: String,
    value: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsFolderCard(
    folderId: String,
    title: String,
    summary: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    if (isExpanded) primaryColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.15f),
                    if (isExpanded) primaryColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(modifier = Modifier.padding(bottom = 12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun CompactSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f))
        LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PathConfigItem(label: String, path: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onEdit() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                path,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit Path",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}
