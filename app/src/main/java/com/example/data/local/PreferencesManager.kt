package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ThemeMode {
    SYSTEM,
    DARK_AMOLED,
    DARK_SLATE,
    LIGHT
}

enum class AccentTheme(val title: String, val primaryHex: Long) {
    SNAP_AMBER("Snap Amber", 0xFFFF9800),
    ELECTRIC_BLUE("Electric Blue", 0xFF2979FF),
    NEON_EMERALD("Neon Emerald", 0xFF00E676),
    CYBER_PURPLE("Cyber Purple", 0xFFD500F9),
    SUNSET_CRIMSON("Sunset Crimson", 0xFFFF3D00)
}

enum class LayoutMode {
    DETAILED_LIST,
    COMPACT_GRID
}

enum class NetworkPolicy(val label: String) {
    ANY("Any Network (Wi-Fi or Mobile Data)"),
    WIFI_ONLY("Download Over Wi-Fi Only"),
    MOBILE_DATA_ONLY("Download Over Mobile Data Only")
}

enum class AppIconStyle(val title: String, val primaryColorHex: Long) {
    SNAP_GOLD("Snap Classic Gold", 0xFFFF9800),
    CYBER_BLUE("Cyber Electric Blue", 0xFF2979FF),
    NEON_EMERALD("Neon Matrix Emerald", 0xFF00E676),
    STEALTH_DARK("Stealth AMOLED Carbon", 0xFF424242),
    RUBY_RED("Ruby Crimson Flame", 0xFFFF1744)
}

enum class WallpaperStyle(val title: String, val gradientColors: List<Long>) {
    LIQUID_GOLD("Liquid Gold & Obsidian", listOf(0xFF1E170A, 0xFF0C0B12, 0xFF171309)),
    NEBULA_SPACE("Cosmic Nebula & Violet", listOf(0xFF130924, 0xFF0B0A14, 0xFF190F2E)),
    CYBERPUNK("Cyberpunk Neon Night", listOf(0xFF0F172A, 0xFF0A101D, 0xFF1E102A)),
    DEEP_SLATE("Frosted Deep Slate", listOf(0xFF131520, 0xFF1A1C2B, 0xFF11121C)),
    AMOLED_PURE("Pure Minimal AMOLED", listOf(0xFF000000, 0xFF050508, 0xFF000000))
}

enum class BottomBarLabelStyle(val label: String) {
    ACTIVE_ONLY("Active Tab Only (Expanding Pill)"),
    ALWAYS_SHOW("Always Show Labels"),
    ICON_ONLY("Icon Only (Minimal)")
}

enum class VaultLockType(val label: String, val pinLength: Int) {
    PIN_4("4-Digit PIN", 4),
    PIN_6("6-Digit PIN", 6),
    PATTERN("Pattern Lock", 0)
}

data class HomepageBookmark(
    val id: String,
    val title: String,
    val url: String,
    val isFeatured: Boolean = false,
    val iconTint: Long = 0xFFFF9800
)

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("snapload_prefs", Context.MODE_PRIVATE)

    // Appearance
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentTheme = MutableStateFlow(loadAccentTheme())
    val accentTheme: StateFlow<AccentTheme> = _accentTheme.asStateFlow()

    private val _layoutMode = MutableStateFlow(loadLayoutMode())
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _wallpaperStyle = MutableStateFlow(loadWallpaperStyle())
    val wallpaperStyle: StateFlow<WallpaperStyle> = _wallpaperStyle.asStateFlow()

    // App Identity Customization
    private val _appName = MutableStateFlow(prefs.getString("custom_app_name", "SnapLoad") ?: "SnapLoad")
    val appName: StateFlow<String> = _appName.asStateFlow()

    private val _appIconStyle = MutableStateFlow(loadAppIconStyle())
    val appIconStyle: StateFlow<AppIconStyle> = _appIconStyle.asStateFlow()

    // Download settings
    private val _networkPolicy = MutableStateFlow(loadNetworkPolicy())
    val networkPolicy: StateFlow<NetworkPolicy> = _networkPolicy.asStateFlow()

    private val _videoPath = MutableStateFlow(
        prefs.getString("video_path", defaultDir("Videos")) ?: defaultDir("Videos")
    )
    val videoPath: StateFlow<String> = _videoPath.asStateFlow()

    private val _audioPath = MutableStateFlow(
        prefs.getString("audio_path", defaultDir("Audio")) ?: defaultDir("Audio")
    )
    val audioPath: StateFlow<String> = _audioPath.asStateFlow()

    private val _imagePath = MutableStateFlow(
        prefs.getString("image_path", defaultDir("Images")) ?: defaultDir("Images")
    )
    val imagePath: StateFlow<String> = _imagePath.asStateFlow()

    private val _maxParallelDownloads = MutableStateFlow(prefs.getInt("max_parallel", 3))
    val maxParallelDownloads: StateFlow<Int> = _maxParallelDownloads.asStateFlow()

    private val _lowPowerMode = MutableStateFlow(prefs.getBoolean("low_power_mode", false))
    val lowPowerMode: StateFlow<Boolean> = _lowPowerMode.asStateFlow()

    private val _turboSpeedBoost = MutableStateFlow(prefs.getBoolean("turbo_speed_boost", true))
    val turboSpeedBoost: StateFlow<Boolean> = _turboSpeedBoost.asStateFlow()

    private val _clipboardAutoDetect = MutableStateFlow(prefs.getBoolean("clipboard_auto_detect", true))
    val clipboardAutoDetect: StateFlow<Boolean> = _clipboardAutoDetect.asStateFlow()

    private val _adBlockEnabled = MutableStateFlow(prefs.getBoolean("ad_block_enabled", true))
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _languageCode = MutableStateFlow(prefs.getString("language_code", "en") ?: "en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _defaultQuality = MutableStateFlow(prefs.getString("default_quality", "720p") ?: "720p")
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    private val _fileSortOrder = MutableStateFlow(prefs.getString("file_sort_order", "DATE_DESC") ?: "DATE_DESC")
    val fileSortOrder: StateFlow<String> = _fileSortOrder.asStateFlow()

    private val _cloudSyncTime = MutableStateFlow(prefs.getLong("cloud_sync_time", 0L))
    val cloudSyncTime: StateFlow<Long> = _cloudSyncTime.asStateFlow()

    // Modular UI Customization: What to show or hide
    private val _showBrowserBanner = MutableStateFlow(prefs.getBoolean("show_browser_banner", true))
    val showBrowserBanner: StateFlow<Boolean> = _showBrowserBanner.asStateFlow()

    private val _showBrowserBookmarks = MutableStateFlow(prefs.getBoolean("show_browser_bookmarks", true))
    val showBrowserBookmarks: StateFlow<Boolean> = _showBrowserBookmarks.asStateFlow()

    private val _showBrowserNews = MutableStateFlow(prefs.getBoolean("show_browser_news", true))
    val showBrowserNews: StateFlow<Boolean> = _showBrowserNews.asStateFlow()

    private val _showBrowserSamples = MutableStateFlow(prefs.getBoolean("show_browser_samples", true))
    val showBrowserSamples: StateFlow<Boolean> = _showBrowserSamples.asStateFlow()

    private val _showDashboardSpeedMeter = MutableStateFlow(prefs.getBoolean("show_dashboard_speed_meter", true))
    val showDashboardSpeedMeter: StateFlow<Boolean> = _showDashboardSpeedMeter.asStateFlow()

    private val _showDashboardStorageBar = MutableStateFlow(prefs.getBoolean("show_dashboard_storage_bar", true))
    val showDashboardStorageBar: StateFlow<Boolean> = _showDashboardStorageBar.asStateFlow()

    // Notification Settings
    private val _notifyToolbar = MutableStateFlow(prefs.getBoolean("notify_toolbar", true))
    val notifyToolbar: StateFlow<Boolean> = _notifyToolbar.asStateFlow()

    private val _notifyDownloading = MutableStateFlow(prefs.getBoolean("notify_downloading", true))
    val notifyDownloading: StateFlow<Boolean> = _notifyDownloading.asStateFlow()

    private val _notifyCompleted = MutableStateFlow(prefs.getBoolean("notify_completed", true))
    val notifyCompleted: StateFlow<Boolean> = _notifyCompleted.asStateFlow()

    private val _notifyNews = MutableStateFlow(prefs.getBoolean("notify_news", true))
    val notifyNews: StateFlow<Boolean> = _notifyNews.asStateFlow()

    private val _notifySoundVibrate = MutableStateFlow(prefs.getBoolean("notify_sound_vibrate", true))
    val notifySoundVibrate: StateFlow<Boolean> = _notifySoundVibrate.asStateFlow()

    // Bottom Bar Customization
    private val _bottomBarTransparency = MutableStateFlow(prefs.getFloat("bottom_bar_transparency", 0.88f))
    val bottomBarTransparency: StateFlow<Float> = _bottomBarTransparency.asStateFlow()

    private val _bottomBarBlurLevel = MutableStateFlow(prefs.getInt("bottom_bar_blur_level", 2))
    val bottomBarBlurLevel: StateFlow<Int> = _bottomBarBlurLevel.asStateFlow()

    private val _bottomBarLabelStyle = MutableStateFlow(loadBottomBarLabelStyle())
    val bottomBarLabelStyle: StateFlow<BottomBarLabelStyle> = _bottomBarLabelStyle.asStateFlow()

    private val _isSwipeNavigationEnabled = MutableStateFlow(prefs.getBoolean("is_swipe_nav_enabled", true))
    val isSwipeNavigationEnabled: StateFlow<Boolean> = _isSwipeNavigationEnabled.asStateFlow()

    private val _isInfiniteSwipeEnabled = MutableStateFlow(prefs.getBoolean("is_infinite_swipe_enabled", false))
    val isInfiniteSwipeEnabled: StateFlow<Boolean> = _isInfiniteSwipeEnabled.asStateFlow()

    private val _showBottomFloatingSearch = MutableStateFlow(prefs.getBoolean("show_bottom_floating_search", true))
    val showBottomFloatingSearch: StateFlow<Boolean> = _showBottomFloatingSearch.asStateFlow()

    private val _enabledBottomTabs = MutableStateFlow(loadEnabledBottomTabs())
    val enabledBottomTabs: StateFlow<List<String>> = _enabledBottomTabs.asStateFlow()

    // Developer & Full App Engine Customization
    private val _devUserAgentMode = MutableStateFlow(prefs.getString("dev_user_agent", "DEFAULT") ?: "DEFAULT")
    val devUserAgentMode: StateFlow<String> = _devUserAgentMode.asStateFlow()

    private val _devBypassSslErrors = MutableStateFlow(prefs.getBoolean("dev_bypass_ssl", false))
    val devBypassSslErrors: StateFlow<Boolean> = _devBypassSslErrors.asStateFlow()

    private val _devTurboMultiplier = MutableStateFlow(prefs.getInt("dev_turbo_multiplier", 4))
    val devTurboMultiplier: StateFlow<Int> = _devTurboMultiplier.asStateFlow()

    private val _devHardwareAccel = MutableStateFlow(prefs.getBoolean("dev_hardware_accel", true))
    val devHardwareAccel: StateFlow<Boolean> = _devHardwareAccel.asStateFlow()

    private val _devJsConsoleEnabled = MutableStateFlow(prefs.getBoolean("dev_js_console", false))
    val devJsConsoleEnabled: StateFlow<Boolean> = _devJsConsoleEnabled.asStateFlow()

    // Vault Security Type & Pattern
    private val _vaultLockType = MutableStateFlow(loadVaultLockType())
    val vaultLockType: StateFlow<VaultLockType> = _vaultLockType.asStateFlow()

    // Dynamic Homepage Bookmarks
    private val _homepageBookmarks = MutableStateFlow(loadHomepageBookmarks())
    val homepageBookmarks: StateFlow<List<HomepageBookmark>> = _homepageBookmarks.asStateFlow()

    private fun defaultDir(subfolder: String): String {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val target = File(base, subfolder)
        if (!target.exists()) target.mkdirs()
        return target.absolutePath
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.DARK_AMOLED.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.DARK_AMOLED.name)
        } catch (_: Exception) {
            ThemeMode.DARK_AMOLED
        }
    }

    fun setAccentTheme(accent: AccentTheme) {
        prefs.edit().putString("accent_theme", accent.name).apply()
        _accentTheme.value = accent
    }

    private fun loadAccentTheme(): AccentTheme {
        val name = prefs.getString("accent_theme", AccentTheme.SNAP_AMBER.name)
        return try {
            AccentTheme.valueOf(name ?: AccentTheme.SNAP_AMBER.name)
        } catch (_: Exception) {
            AccentTheme.SNAP_AMBER
        }
    }

    fun setLayoutMode(mode: LayoutMode) {
        prefs.edit().putString("layout_mode", mode.name).apply()
        _layoutMode.value = mode
    }

    private fun loadLayoutMode(): LayoutMode {
        val name = prefs.getString("layout_mode", LayoutMode.DETAILED_LIST.name)
        return try {
            LayoutMode.valueOf(name ?: LayoutMode.DETAILED_LIST.name)
        } catch (_: Exception) {
            LayoutMode.DETAILED_LIST
        }
    }

    fun setWallpaperStyle(style: WallpaperStyle) {
        prefs.edit().putString("wallpaper_style", style.name).apply()
        _wallpaperStyle.value = style
    }

    private fun loadWallpaperStyle(): WallpaperStyle {
        val name = prefs.getString("wallpaper_style", WallpaperStyle.LIQUID_GOLD.name)
        return try {
            WallpaperStyle.valueOf(name ?: WallpaperStyle.LIQUID_GOLD.name)
        } catch (_: Exception) {
            WallpaperStyle.LIQUID_GOLD
        }
    }

    fun setCustomAppName(name: String) {
        val clean = name.trim().ifEmpty { "SnapLoad" }
        prefs.edit().putString("custom_app_name", clean).apply()
        _appName.value = clean
    }

    fun setAppIconStyle(icon: AppIconStyle) {
        prefs.edit().putString("app_icon_style", icon.name).apply()
        _appIconStyle.value = icon
    }

    private fun loadAppIconStyle(): AppIconStyle {
        val name = prefs.getString("app_icon_style", AppIconStyle.SNAP_GOLD.name)
        return try {
            AppIconStyle.valueOf(name ?: AppIconStyle.SNAP_GOLD.name)
        } catch (_: Exception) {
            AppIconStyle.SNAP_GOLD
        }
    }

    fun setNetworkPolicy(policy: NetworkPolicy) {
        prefs.edit().putString("network_policy", policy.name).apply()
        _networkPolicy.value = policy
    }

    private fun loadNetworkPolicy(): NetworkPolicy {
        val name = prefs.getString("network_policy", NetworkPolicy.ANY.name)
        return try {
            NetworkPolicy.valueOf(name ?: NetworkPolicy.ANY.name)
        } catch (_: Exception) {
            NetworkPolicy.ANY
        }
    }

    fun setVideoPath(path: String) {
        prefs.edit().putString("video_path", path).apply()
        _videoPath.value = path
    }

    fun setAudioPath(path: String) {
        prefs.edit().putString("audio_path", path).apply()
        _audioPath.value = path
    }

    fun setImagePath(path: String) {
        prefs.edit().putString("image_path", path).apply()
        _imagePath.value = path
    }

    fun setMaxParallelDownloads(count: Int) {
        val clamped = count.coerceIn(1, 5)
        prefs.edit().putInt("max_parallel", clamped).apply()
        _maxParallelDownloads.value = clamped
    }

    fun setLowPowerMode(enabled: Boolean) {
        prefs.edit().putBoolean("low_power_mode", enabled).apply()
        _lowPowerMode.value = enabled
    }

    fun setTurboSpeedBoost(enabled: Boolean) {
        prefs.edit().putBoolean("turbo_speed_boost", enabled).apply()
        _turboSpeedBoost.value = enabled
    }

    fun setClipboardAutoDetect(enabled: Boolean) {
        prefs.edit().putBoolean("clipboard_auto_detect", enabled).apply()
        _clipboardAutoDetect.value = enabled
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ad_block_enabled", enabled).apply()
        _adBlockEnabled.value = enabled
    }

    fun setLanguageCode(code: String) {
        prefs.edit().putString("language_code", code).apply()
        _languageCode.value = code
    }

    fun setDefaultQuality(quality: String) {
        prefs.edit().putString("default_quality", quality).apply()
        _defaultQuality.value = quality
    }

    fun setFileSortOrder(order: String) {
        prefs.edit().putString("file_sort_order", order).apply()
        _fileSortOrder.value = order
    }

    fun setCloudSyncTime(time: Long) {
        prefs.edit().putLong("cloud_sync_time", time).apply()
        _cloudSyncTime.value = time
    }

    // Modular UI Toggles
    fun setShowBrowserBanner(show: Boolean) {
        prefs.edit().putBoolean("show_browser_banner", show).apply()
        _showBrowserBanner.value = show
    }

    fun setShowBrowserBookmarks(show: Boolean) {
        prefs.edit().putBoolean("show_browser_bookmarks", show).apply()
        _showBrowserBookmarks.value = show
    }

    fun setShowBrowserNews(show: Boolean) {
        prefs.edit().putBoolean("show_browser_news", show).apply()
        _showBrowserNews.value = show
    }

    fun setShowBrowserSamples(show: Boolean) {
        prefs.edit().putBoolean("show_browser_samples", show).apply()
        _showBrowserSamples.value = show
    }

    fun setShowDashboardSpeedMeter(show: Boolean) {
        prefs.edit().putBoolean("show_dashboard_speed_meter", show).apply()
        _showDashboardSpeedMeter.value = show
    }

    fun setShowDashboardStorageBar(show: Boolean) {
        prefs.edit().putBoolean("show_dashboard_storage_bar", show).apply()
        _showDashboardStorageBar.value = show
    }

    // Notification Toggles
    fun setNotifyToolbar(enabled: Boolean) {
        prefs.edit().putBoolean("notify_toolbar", enabled).apply()
        _notifyToolbar.value = enabled
    }

    fun setNotifyDownloading(enabled: Boolean) {
        prefs.edit().putBoolean("notify_downloading", enabled).apply()
        _notifyDownloading.value = enabled
    }

    fun setNotifyCompleted(enabled: Boolean) {
        prefs.edit().putBoolean("notify_completed", enabled).apply()
        _notifyCompleted.value = enabled
    }

    fun setNotifyNews(enabled: Boolean) {
        prefs.edit().putBoolean("notify_news", enabled).apply()
        _notifyNews.value = enabled
    }

    fun setNotifySoundVibrate(enabled: Boolean) {
        prefs.edit().putBoolean("notify_sound_vibrate", enabled).apply()
        _notifySoundVibrate.value = enabled
    }

    // Vault Security Management (PIN 4, PIN 6, Pattern & Recovery Code 7450)
    fun setVaultLockType(type: VaultLockType) {
        prefs.edit().putString("vault_lock_type", type.name).apply()
        _vaultLockType.value = type
    }

    private fun loadVaultLockType(): VaultLockType {
        val name = prefs.getString("vault_lock_type", VaultLockType.PIN_4.name)
        return try {
            VaultLockType.valueOf(name ?: VaultLockType.PIN_4.name)
        } catch (_: Exception) {
            VaultLockType.PIN_4
        }
    }

    fun getVaultPinHash(): String? {
        return prefs.getString("vault_pin_hash", null)
    }

    fun setVaultPinHash(hash: String?) {
        prefs.edit().putString("vault_pin_hash", hash).apply()
    }

    fun getVaultPatternHash(): String? {
        return prefs.getString("vault_pattern_hash", null)
    }

    fun setVaultPatternHash(hash: String?) {
        prefs.edit().putString("vault_pattern_hash", hash).apply()
    }

    fun resetVaultWithRecoveryCode(code: String): Boolean {
        if (code.trim() == "7450") {
            prefs.edit()
                .remove("vault_pin_hash")
                .remove("vault_pattern_hash")
                .putString("vault_lock_type", VaultLockType.PIN_4.name)
                .apply()
            _vaultLockType.value = VaultLockType.PIN_4
            return true
        }
        return false
    }

    fun isVaultSetup(): Boolean {
        return getVaultPinHash() != null || getVaultPatternHash() != null
    }

    // Dynamic Homepage Bookmarks Management
    private fun defaultBookmarks(): List<HomepageBookmark> {
        return listOf(
            HomepageBookmark("site_dipraj", "Dipraj", "https://dipraj.carrd.co", isFeatured = true, iconTint = 0xFFFFD700),
            HomepageBookmark("site_yt", "YouTube", "https://m.youtube.com", iconTint = 0xFFFF0000),
            HomepageBookmark("site_fb", "Facebook", "https://m.facebook.com", iconTint = 0xFF1877F2),
            HomepageBookmark("site_ig", "Instagram", "https://www.instagram.com", iconTint = 0xFFE1306C),
            HomepageBookmark("site_tt", "TikTok", "https://www.tiktok.com", iconTint = 0xFF00F2FE),
            HomepageBookmark("site_rd", "Reddit", "https://www.reddit.com", iconTint = 0xFFFF4500),
            HomepageBookmark("site_vm", "Vimeo", "https://vimeo.com/watch", iconTint = 0xFF1AB7EA),
            HomepageBookmark("site_x", "X / Twitter", "https://x.com", iconTint = 0xFFFFFFFF)
        )
    }

    private fun loadHomepageBookmarks(): List<HomepageBookmark> {
        val raw = prefs.getString("homepage_bookmarks_json", null)
        if (raw.isNullOrBlank()) return defaultBookmarks()
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<HomepageBookmark>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HomepageBookmark(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        iconTint = obj.optLong("iconTint", 0xFFFF9800)
                    )
                )
            }
            if (list.isEmpty()) defaultBookmarks() else list
        } catch (_: Exception) {
            defaultBookmarks()
        }
    }

    private fun saveHomepageBookmarks(list: List<HomepageBookmark>) {
        val array = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("isFeatured", item.isFeatured)
            obj.put("iconTint", item.iconTint)
            array.put(obj)
        }
        prefs.edit().putString("homepage_bookmarks_json", array.toString()).apply()
        _homepageBookmarks.value = list
    }

    fun addHomepageBookmark(title: String, url: String) {
        val current = _homepageBookmarks.value.toMutableList()
        val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val newBookmark = HomepageBookmark(
            id = "bm_${System.currentTimeMillis()}",
            title = title.ifBlank { "Site ${current.size + 1}" },
            url = formattedUrl,
            iconTint = 0xFFFF9800
        )
        current.add(newBookmark)
        saveHomepageBookmarks(current)
    }

    fun removeHomepageBookmark(id: String) {
        val filtered = _homepageBookmarks.value.filter { it.id != id }
        saveHomepageBookmarks(filtered)
    }

    fun restoreDefaultHomepageBookmarks() {
        val defaults = defaultBookmarks()
        saveHomepageBookmarks(defaults)
    }

    // Bottom Bar Customization Setters & Loaders
    fun setBottomBarTransparency(transparency: Float) {
        val clamped = transparency.coerceIn(0.2f, 1.0f)
        prefs.edit().putFloat("bottom_bar_transparency", clamped).apply()
        _bottomBarTransparency.value = clamped
    }

    fun setBottomBarBlurLevel(level: Int) {
        val clamped = level.coerceIn(1, 3)
        prefs.edit().putInt("bottom_bar_blur_level", clamped).apply()
        _bottomBarBlurLevel.value = clamped
    }

    fun setBottomBarLabelStyle(style: BottomBarLabelStyle) {
        prefs.edit().putString("bottom_bar_label_style", style.name).apply()
        _bottomBarLabelStyle.value = style
    }

    private fun loadBottomBarLabelStyle(): BottomBarLabelStyle {
        val name = prefs.getString("bottom_bar_label_style", BottomBarLabelStyle.ACTIVE_ONLY.name)
        return try {
            BottomBarLabelStyle.valueOf(name ?: BottomBarLabelStyle.ACTIVE_ONLY.name)
        } catch (_: Exception) {
            BottomBarLabelStyle.ACTIVE_ONLY
        }
    }

    fun setSwipeNavigationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_swipe_nav_enabled", enabled).apply()
        _isSwipeNavigationEnabled.value = enabled
    }

    fun setInfiniteSwipeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_infinite_swipe_enabled", enabled).apply()
        _isInfiniteSwipeEnabled.value = enabled
    }

    fun setShowBottomFloatingSearch(enabled: Boolean) {
        prefs.edit().putBoolean("show_bottom_floating_search", enabled).apply()
        _showBottomFloatingSearch.value = enabled
    }

    fun setEnabledBottomTabs(tabs: List<String>) {
        val safeTabs = if (tabs.size < 2) listOf("DOWNLOADS", "BROWSER") else tabs
        prefs.edit().putString("enabled_bottom_tabs", safeTabs.joinToString(",")).apply()
        _enabledBottomTabs.value = safeTabs
    }

    private fun loadEnabledBottomTabs(): List<String> {
        val raw = prefs.getString("enabled_bottom_tabs", null)
        return if (raw.isNullOrBlank()) {
            listOf("DOWNLOADS", "BROWSER", "FILE_MANAGER", "SETTINGS")
        } else {
            raw.split(",").filter { it.isNotBlank() }
        }
    }

    // Developer & Full App Engine Setters
    fun setDevUserAgentMode(mode: String) {
        prefs.edit().putString("dev_user_agent", mode).apply()
        _devUserAgentMode.value = mode
    }

    fun setDevBypassSslErrors(bypass: Boolean) {
        prefs.edit().putBoolean("dev_bypass_ssl", bypass).apply()
        _devBypassSslErrors.value = bypass
    }

    fun setDevTurboMultiplier(multiplier: Int) {
        val clamped = multiplier.coerceIn(1, 8)
        prefs.edit().putInt("dev_turbo_multiplier", clamped).apply()
        _devTurboMultiplier.value = clamped
    }

    fun setDevHardwareAccel(enabled: Boolean) {
        prefs.edit().putBoolean("dev_hardware_accel", enabled).apply()
        _devHardwareAccel.value = enabled
    }

    fun setDevJsConsoleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dev_js_console", enabled).apply()
        _devJsConsoleEnabled.value = enabled
    }
}
