package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Liquid Glass Switch with translucent frosted glass capsule,
 * smooth sliding thumb, glowing border accents, and tactile ON/OFF state indicators.
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackWidth = 54.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp

    val targetThumbOffset = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding
    val thumbOffset by animateDpAsState(
        targetValue = targetThumbOffset,
        animationSpec = tween(durationMillis = 220),
        label = "thumb_offset"
    )

    val trackBgColor by animateColorAsState(
        targetValue = if (checked) primaryColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 220),
        label = "track_bg"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) primaryColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 220),
        label = "track_border"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) primaryColor else Color(0xFFE0E0E0),
        animationSpec = tween(durationMillis = 220),
        label = "thumb_color"
    )

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 48.dp) // Touch target minimum 48dp
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Capsule
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            trackBgColor,
                            trackBgColor.copy(alpha = if (checked) 0.5f else 0.12f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            listOf(
                                trackBorderColor,
                                trackBorderColor.copy(alpha = 0.3f)
                            )
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            // Subtle ON / OFF indicator text
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (checked) "ON" else "",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor.copy(alpha = 0.9f)
                )
                Text(
                    text = if (!checked) "OFF" else "",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }

            // Glowing Floating Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .align(Alignment.CenterStart)
                    .size(thumbSize)
                    .shadow(
                        elevation = if (checked) 6.dp else 2.dp,
                        shape = CircleShape,
                        spotColor = if (checked) primaryColor else Color.Black
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                thumbColor
                            )
                        )
                    )
                    .border(0.8.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}
