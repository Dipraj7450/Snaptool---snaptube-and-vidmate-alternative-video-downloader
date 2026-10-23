package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.io.File

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    CROP("Zoom / Fill"),
    STRETCH("Stretch")
}

@Composable
fun MXPlayerView(
    mediaPath: String,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = remember { context as? Activity }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }

    // Floating HUD for MX gestures
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var hudProgress by remember { mutableStateOf<Float?>(null) }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var currentBrightness by remember {
        mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f)
    }

    var pendingSeekMs by remember { mutableStateOf<Int?>(null) }

    // Auto hide controls timer
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    // Playback time tracking
    LaunchedEffect(mediaPlayer, isPlaying) {
        while (true) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosition = mp.currentPosition
                    duration = mp.duration
                }
            }
            delay(500)
        }
    }

    // Hide HUD after gesture release
    LaunchedEffect(hudText) {
        if (hudText != null) {
            delay(1200)
            hudText = null
            hudIcon = null
            hudProgress = null
        }
    }

    DisposableEffect(mediaPath) {
        val mp = MediaPlayer().apply {
            try {
                if (mediaPath.startsWith("http://") || mediaPath.startsWith("https://")) {
                    setDataSource(context, Uri.parse(mediaPath))
                } else {
                    val file = File(mediaPath)
                    setDataSource(file.absolutePath)
                }
                prepareAsync()
                setOnPreparedListener {
                    duration = it.duration
                    it.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    isPlaying = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = mp

        onDispose {
            mp.stop()
            mp.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        if (isLocked) {
                            showControls = !showControls
                        } else {
                            showControls = !showControls
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val isRightHalf = offset.x > size.width / 2
                            mediaPlayer?.let { mp ->
                                val delta = if (isRightHalf) 10000 else -10000
                                val target = (mp.currentPosition + delta).coerceIn(0, mp.duration)
                                mp.seekTo(target)
                                currentPosition = target
                                hudText = if (isRightHalf) "+10s" else "-10s"
                                hudIcon = if (isRightHalf) Icons.Default.FastForward else Icons.Default.FastRewind
                            }
                        }
                    }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                var dragDirection = 0 // 1: Horizontal seek, 2: Left Vertical (brightness), 3: Right Vertical (volume)
                var initialSeekPos = 0

                detectDragGestures(
                    onDragStart = { offset ->
                        initialSeekPos = mediaPlayer?.currentPosition ?: 0
                        dragDirection = 0
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val posX = change.position.x
                        val dx = dragAmount.x
                        val dy = dragAmount.y

                        if (dragDirection == 0) {
                            if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 10) {
                                dragDirection = 1 // Horizontal Seek
                            } else if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10) {
                                dragDirection = if (posX < size.width / 2) 2 else 3
                            }
                        }

                        when (dragDirection) {
                            1 -> { // Seek
                                val dur = duration.coerceAtLeast(1)
                                val seekRatio = dx / size.width.toFloat()
                                val seekDelta = (seekRatio * 90000).toInt() // 90 sec range across screen
                                val newPos = (initialSeekPos + seekDelta).coerceIn(0, dur)
                                initialSeekPos = newPos
                                pendingSeekMs = newPos

                                val formattedCurrent = formatTime(newPos)
                                val formattedTotal = formatTime(dur)
                                val diff = (newPos - (mediaPlayer?.currentPosition ?: 0)) / 1000
                                val diffStr = if (diff >= 0) "+${diff}s" else "${diff}s"
                                hudText = "$diffStr [$formattedCurrent / $formattedTotal]"
                                hudIcon = Icons.Default.Timelapse
                                hudProgress = newPos.toFloat() / dur.toFloat()
                            }
                            2 -> { // Brightness (Left half swipe up/down)
                                val deltaBrightness = -dy / (size.height * 0.7f)
                                val newBrightness = (currentBrightness + deltaBrightness).coerceIn(0.01f, 1.0f)
                                currentBrightness = newBrightness
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = newBrightness
                                    act.window.attributes = lp
                                }
                                val percent = (newBrightness * 100).toInt()
                                hudText = "Brightness: $percent%"
                                hudIcon = Icons.Default.BrightnessMedium
                                hudProgress = newBrightness
                            }
                            3 -> { // Volume (Right half swipe up/down)
                                val deltaVol = -dy / (size.height * 0.7f)
                                val floatVol = (currentVolume.toFloat() + deltaVol * maxVolume).coerceIn(0f, maxVolume.toFloat())
                                val intVol = floatVol.toInt()
                                if (intVol != currentVolume) {
                                    currentVolume = intVol
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intVol, 0)
                                }
                                val percent = ((intVol.toFloat() / maxVolume) * 100).toInt()
                                hudText = "Volume: $percent%"
                                hudIcon = if (intVol == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp
                                hudProgress = intVol.toFloat() / maxVolume.toFloat()
                            }
                        }
                    },
                    onDragEnd = {
                        pendingSeekMs?.let { seekTarget ->
                            mediaPlayer?.seekTo(seekTarget)
                            currentPosition = seekTarget
                            pendingSeekMs = null
                        }
                    }
                )
            }
    ) {
        // Video Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            mediaPlayer?.setDisplay(holder)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            mediaPlayer?.setDisplay(null)
                        }
                    })
                }
            }
        )

        // Gesture HUD overlay (Centered)
        hudText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    hudIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    hudProgress?.let { prog ->
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { prog },
                            modifier = Modifier
                                .width(140.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = Color(0xFFFF9800),
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Lock button (always visible or with controls)
        IconButton(
            onClick = {
                isLocked = !isLocked
                showControls = true
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock",
                tint = if (isLocked) Color(0xFFFF9800) else Color.White
            )
        }

        // Overlay controls
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Aspect ratio button
                    IconButton(onClick = {
                        aspectRatioMode = when (aspectRatioMode) {
                            AspectRatioMode.FIT -> AspectRatioMode.CROP
                            AspectRatioMode.CROP -> AspectRatioMode.STRETCH
                            AspectRatioMode.STRETCH -> AspectRatioMode.FIT
                        }
                    }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }

                    // Playback speed button
                    TextButton(onClick = {
                        playbackSpeed = when (playbackSpeed) {
                            0.75f -> 1.0f
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.5f
                            else -> 0.75f
                        }
                        mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(playbackSpeed) ?: return@TextButton
                    }) {
                        Text("${playbackSpeed}x", color = Color(0xFFFF9800), fontSize = 14.sp)
                    }

                    // Loop button
                    IconButton(onClick = {
                        isLooping = !isLooping
                        mediaPlayer?.isLooping = isLooping
                    }) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (isLooping) Color(0xFFFF9800) else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Center Play/Pause & Skip buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                                mp.seekTo(target)
                                currentPosition = target
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                if (mp.isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    mp.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFFF9800), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                val target = (mp.currentPosition + 10000).coerceAtMost(mp.duration)
                                mp.seekTo(target)
                                currentPosition = target
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // Bottom Seekbar & duration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { frac ->
                            val target = (frac * duration).toInt()
                            currentPosition = target
                            mediaPlayer?.seekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF9800),
                            activeTrackColor = Color(0xFFFF9800),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
