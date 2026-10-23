package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val route: String, val icon: ImageVector) {
    DOWNLOADS("downloads", Icons.Default.Download),
    BROWSER("browser", Icons.Default.Public),
    FILE_MANAGER("files", Icons.Default.Folder),
    SETTINGS("settings", Icons.Default.Settings)
}
