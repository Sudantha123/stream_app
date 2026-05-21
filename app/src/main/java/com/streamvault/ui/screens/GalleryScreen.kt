package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamvault.data.models.Video
import com.streamvault.data.repository.PreferencesRepository
import com.streamvault.ui.components.*
import com.streamvault.ui.theme.SVColors
import com.streamvault.viewmodel.GalleryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@Composable
fun GalleryScreen(
    onVideoClick: (String) -> Unit,
    onAdminClick: () -> Unit,
    vm: GalleryViewModel = hiltViewModel()
) {
    val videos by vm.filteredVideos.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Video?>(null) }

    Scaffold(
        containerColor = SVColors.Background,
        topBar = {
            GalleryTopBar(
                searchQuery = searchQuery,
                onSearchChange = vm::setSearchQuery,
                onAdminClick = onAdminClick
            )
        }
    ) { padding ->

        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && videos.isEmpty() -> {
                    LoadingGrid()
                }
                videos.isEmpty() -> {
                    EmptyGallery()
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300, delayMillis = (index * 40).coerceAtMost(400))) +
                                        slideInVertically(tween(300, delayMillis = (index * 40).coerceAtMost(400))) { it / 3 }
                            ) {
                                VideoCard(
                                    video = video,
                                    onClick = { onVideoClick(video.id) },
                                    onDelete = { deleteTarget = video }
                                )
                            }
                        }
                    }
                }
            }

            // Pull to refresh indicator
            if (isLoading && videos.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = SVColors.Cyan,
                    trackColor = SVColors.Border
                )
            }
        }
    }

    // Delete confirm dialog
    deleteTarget?.let { video ->
        DeleteConfirmDialog(
            videoTitle = video.title,
            onConfirm = {
                vm.deleteVideo(video.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAdminClick: () -> Unit
) {
    var searchActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(SVColors.Background, SVColors.Background.copy(alpha = 0f))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            Brush.linearGradient(listOf(SVColors.Cyan, SVColors.Violet)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "StreamVault",
                    style = MaterialTheme.typography.titleLarge,
                    color = SVColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            IconButton(
                onClick = onAdminClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(SVColors.SurfaceVar, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Outlined.Dashboard, "Admin", tint = SVColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onClick,
                onClickLabel = "Play ${video.title}"
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SVColors.Surface),
        border = BorderStroke(1.dp, SVColors.Border)
    ) {
        Box {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(SVColors.SurfaceHigh)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("${video.thumbnail}")
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x88000000))
                            )
                        )
                )

                // Play icon center
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .background(SVColors.Cyan.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow, null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp).offset(x = 1.dp)
                    )
                }

                // Duration badge
                if (video.duration > 0) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(SVColors.Danger.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Filled.Close, "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // Info
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SVColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDate(video.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = SVColors.TextMuted
                )
                if (video.sizeBytes > 0) {
                    Text(
                        formatBytes(video.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = SVColors.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val inf = rememberInfiniteTransition(label = "pulse")
        val alpha by inf.animateFloat(
            0.3f, 0.7f,
            infiniteRepeatable(tween(1800), RepeatMode.Reverse),
            label = "icon_pulse"
        )
        Icon(
            Icons.Outlined.VideoLibrary, null,
            tint = SVColors.Cyan.copy(alpha = alpha),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("No videos yet", style = MaterialTheme.typography.titleMedium, color = SVColors.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text("Go to Dashboard to add videos", style = MaterialTheme.typography.bodySmall, color = SVColors.TextMuted)
    }
}

@Composable
private fun LoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) { index ->
            ShimmerCard(
                modifier = Modifier.animateItem(tween(300, delayMillis = index * 60))
            )
        }
    }
}

fun formatDuration(sec: Double): String {
    val total = sec.toLong()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    } catch (e: Exception) { "" }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024f)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
        else -> "%.2f GB".format(bytes / (1024f * 1024f * 1024f))
    }
}

