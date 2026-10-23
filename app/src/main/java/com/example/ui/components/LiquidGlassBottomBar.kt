package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BottomBarLabelStyle
import com.example.ui.i18n.StringsProvider
import com.example.ui.navigation.Screen

@Composable
fun LiquidGlassBottomBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    activeDownloadsCount: Int,
    strings: StringsProvider.AppStrings,
    transparency: Float = 0.88f,
    blurLevel: Int = 2,
    labelStyle: BottomBarLabelStyle = BottomBarLabelStyle.ACTIVE_ONLY,
    visibleTabs: List<Screen> = Screen.entries,
    showFloatingSearch: Boolean = false,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayedTabs = remember(visibleTabs) {
        if (visibleTabs.size < 2) Screen.entries else visibleTabs
    }

    val borderAlpha = when (blurLevel) {
        1 -> 0.2f
        3 -> 0.5f
        else -> 0.35f
    }

    val borderWidth = when (blurLevel) {
        1 -> 1.0.dp
        3 -> 1.8.dp
        else -> 1.3.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Optional Extra Round Floating Search Bar / Pill
        if (showFloatingSearch) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(38.dp)
                    .padding(bottom = 6.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Color(0xDD141624).copy(alpha = transparency.coerceIn(0.5f, 1f))
                        )
                        .clip(CircleShape)
                        .clickable { onSearchClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Quick Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Search or URL...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Primary Liquid Glass Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
            shape = RoundedCornerShape(34.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = borderAlpha * 0.3f),
                        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha * 0.8f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF141624).copy(alpha = transparency),
                                Color(0xFF0B0C16).copy(alpha = transparency)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    displayedTabs.forEach { screen ->
                        val isSelected = currentScreen == screen
                        val label = when (screen) {
                            Screen.DOWNLOADS -> strings.navDownloads
                            Screen.BROWSER -> strings.navBrowser
                            Screen.FILE_MANAGER -> strings.navFiles
                            Screen.SETTINGS -> strings.navSettings
                        }

                        val showLabel = when (labelStyle) {
                            BottomBarLabelStyle.ICON_ONLY -> false
                            BottomBarLabelStyle.ALWAYS_SHOW -> true
                            BottomBarLabelStyle.ACTIVE_ONLY -> isSelected
                        }

                        val pillAlpha by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent,
                            label = "pillAlpha"
                        )
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f),
                            label = "iconColor"
                        )
                        val pillWidth by animateDpAsState(
                            targetValue = if (labelStyle == BottomBarLabelStyle.ICON_ONLY) {
                                52.dp
                            } else if (labelStyle == BottomBarLabelStyle.ALWAYS_SHOW) {
                                68.dp
                            } else {
                                if (isSelected) 100.dp else 52.dp
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "pillWidth"
                        )

                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .width(pillWidth)
                                .clip(RoundedCornerShape(25.dp))
                                .background(pillAlpha)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = MaterialTheme.colorScheme.primary)
                                ) { onScreenSelected(screen) }
                                .testTag("nav_${screen.route}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (labelStyle == BottomBarLabelStyle.ALWAYS_SHOW) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = label,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (screen == Screen.DOWNLOADS && activeDownloadsCount > 0) {
                                            Badge(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 6.dp, y = (-4).dp),
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    "$activeDownloadsCount",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = label,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = label,
                                            tint = iconColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        if (screen == Screen.DOWNLOADS && activeDownloadsCount > 0) {
                                            Badge(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 8.dp, y = (-6).dp),
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    "$activeDownloadsCount",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    if (showLabel) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
