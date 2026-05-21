package com.streamvault.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamvault.ui.theme.SVColors
import com.streamvault.viewmodel.PlayerViewModel
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoId: String,
    onBack: () -> Unit,
    vm: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val video by vm.video.collectAsStateWithLifecycle()
    val streamUrl by vm.streamUrl.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    // Player state
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var brightnessGesture by remember { mutableStateOf<Float?>(null) }
    var volumeGesture by remember { mutableStateOf<Float?>(null) }

    val scope = rememberCoroutineScope()
    var hideControlsJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = scope.launch {
            delay(3500)
            if (isPlaying) showControls = false
        }
    }

    // ExoPlayer setup
    val player = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    // Fullscreen management
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            player.stop()
            player.release()
            onBack()
        }
    }

    // Load stream
    LaunchedEffect(videoId) { vm.loadVideo(videoId) }

    LaunchedEffect(streamUrl) {
        streamUrl?.let { url ->
            val dataSourceFactory = OkHttpDataSource.Factory(OkHttpClient())
            val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
            player.setMediaSource(hlsSource)
            player.prepare()
            player.playWhenReady = true
        }
    }

    // Progress update
    LaunchedEffect(player) {
        while (true) {
            delay(200)
            if (!isSeeking) {
                currentPosition = player.currentPosition
                bufferedPosition = player.bufferedPosition
                duration = player.duration.coerceAtLeast(0)
            }
            isPlaying = player.isPlaying
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (isFullscreen) Modifier.systemBarsPadding() else Modifier)
    ) {
        // Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val w = size.width
                            when {
                                offset.x < w * 0.35f -> {
                                    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                                    seekFeedback = SeekFeedback.BACKWARD
                                    scope.launch { delay(800); seekFeedback = null }
                                }
                                offset.x > w * 0.65f -> {
                                    player.seekTo(player.currentPosition + 10_000)
                                    seekFeedback = SeekFeedback.FORWARD
                                    scope.launch { delay(800); seekFeedback = null }
                                }
                                else -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                    seekFeedback = if (player.isPlaying) SeekFeedback.PAUSE else SeekFeedback.PLAY
                                    scope.launch { delay(600); seekFeedback = null }
                                }
                            }
                        },
                        onTap = {
                            showControls = !showControls
                            if (showControls) scheduleHideControls()
                        }
                    )
                }
        )

        // Double tap ripple feedback
        AnimatedVisibility(
            visible = seekFeedback != null,
            enter = fadeIn(tween(80)) + scaleIn(tween(120), 0.7f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), 1.2f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            seekFeedback?.let { feedback ->
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(SVColors.Cyan.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (feedback) {
                            SeekFeedback.BACKWARD -> Icons.Filled.Replay10
                            SeekFeedback.FORWARD  -> Icons.Filled.Forward10
                            SeekFeedback.PLAY     -> Icons.Filled.PlayArrow
                            SeekFeedback.PAUSE    -> Icons.Filled.Pause
                        },
                        contentDescription = null,
                        tint = SVColors.Cyan,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Loading spinner
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SVColors.Cyan,
                strokeWidth = 3.dp
            )
        }

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControlsOverlay(
                title = video?.title ?: "",
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                bufferedPosition = bufferedPosition,
                playbackSpeed = playbackSpeed,
                isFullscreen = isFullscreen,
                showSpeedMenu = showSpeedMenu,
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    scheduleHideControls()
                },
                onSeek = { pos ->
                    isSeeking = true
                    player.seekTo(pos)
                    currentPosition = pos
                    scope.launch { delay(200); isSeeking = false }
                },
                onSeekStart = { isSeeking = true },
                onSeekEnd = { isSeeking = false },
                onBack = {
                    if (isFullscreen) isFullscreen = false else {
                        player.stop(); player.release(); onBack()
                    }
                },
                onFullscreen = { isFullscreen = !isFullscreen },
                onSpeedToggle = { showSpeedMenu = !showSpeedMenu },
                onSpeedSelect = { speed ->
                    playbackSpeed = speed
                    player.setPlaybackSpeed(speed)
                    showSpeedMenu = false
                    scheduleHideControls()
                },
                onRewind = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
                onFastForward = { player.seekTo(player.currentPosition + 10_000) },
                onInteract = { scheduleHideControls() }
            )
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    showSpeedMenu: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    onBack: () -> Unit,
    onFullscreen: () -> Unit,
    onSpeedToggle: () -> Unit,
    onSpeedSelect: (Float) -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onInteract: () -> Unit
) {
    val pct = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val bufPct = if (duration > 0) bufferedPosition.toFloat() / duration.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xBB000000),
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xCC000000)
                    )
                )
            )
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
        }

        // Center controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rewind 10s
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0x44000000), CircleShape)
                    .clickable { onRewind(); onInteract() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Play / Pause
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(SVColors.Cyan, CircleShape)
                    .clickable { onPlayPause(); onInteract() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp).let { if (!isPlaying) it.offset(x = 2.dp) else it }
                )
            }

            // Fast forward 10s
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0x44000000), CircleShape)
                    .clickable { onFastForward(); onInteract() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDuration(currentPosition / 1000.0),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatDuration(duration / 1000.0),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(4.dp))

            // Seek bar
            PlayerSeekBar(
                progress = pct,
                buffered = bufPct,
                onSeekStart = onSeekStart,
                onSeek = { frac -> onSeek((frac * duration).toLong()) },
                onSeekEnd = onSeekEnd,
                onInteract = onInteract
            )

            Spacer(Modifier.height(8.dp))

            // Row of bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed
                Box {
                    TextButton(
                        onClick = { onSpeedToggle(); onInteract() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (playbackSpeed != 1f) SVColors.Cyan else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { onSpeedToggle() },
                        modifier = Modifier.background(SVColors.Surface)
                    ) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${speed}x",
                                        color = if (speed == playbackSpeed) SVColors.Cyan else SVColors.TextPrimary
                                    )
                                },
                                onClick = { onSpeedSelect(speed) }
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Fullscreen
                IconButton(onClick = { onFullscreen(); onInteract() }) {
                    Icon(
                        if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSeekBar(
    progress: Float,
    buffered: Float,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    onInteract: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.4f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "thumb"
    )
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 3.dp,
        label = "track_h"
    )

    val displayProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        onSeekStart()
                        onInteract()
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragProgress)
                        onSeekEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onSeekEnd()
                    },
                    onHorizontalDrag = { change, _ ->
                        val w = size.width.toFloat()
                        dragProgress = (change.position.x / w).coerceIn(0f, 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(frac)
                    onInteract()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(SVColors.SeekBarBg)
        ) {
            // Buffer
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(buffered)
                    .background(SVColors.SeekBarBuffer)
            )
            // Progress
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(displayProgress)
                    .background(SVColors.SeekBar)
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (displayProgress * 1f).let { 0.dp }) // handled by fraction
                .fillMaxWidth(displayProgress)
                .wrapContentWidth(Alignment.End)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp * thumbScale)
                    .background(SVColors.SeekBar, CircleShape)
                    .shadow(4.dp, CircleShape)
            )
        }
    }
}

enum class SeekFeedback { BACKWARD, FORWARD, PLAY, PAUSE }
