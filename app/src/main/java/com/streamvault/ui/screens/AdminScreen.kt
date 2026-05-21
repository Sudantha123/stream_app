package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.data.models.Job
import com.streamvault.data.models.JobStatus
import com.streamvault.ui.components.SVTextField
import com.streamvault.ui.theme.SVColors
import com.streamvault.viewmodel.AddJobState
import com.streamvault.viewmodel.AdminViewModel

@Composable
fun AdminScreen(
    onBack: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val addJobState by vm.addJobState.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var segLength by remember { mutableStateOf(6) }
    var showAddSheet by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<Job?>(null) }

    // Handle success
    LaunchedEffect(addJobState) {
        if (addJobState is AddJobState.Success) {
            urlInput = ""
            titleInput = ""
            showAddSheet = false
            vm.resetAddState()
        }
    }

    Scaffold(
        containerColor = SVColors.Background,
        topBar = {
            AdminTopBar(onBack = onBack, jobCount = jobs.size)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = SVColors.Cyan,
                contentColor = Color.Black,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New Job", fontWeight = FontWeight.Bold) },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { padding ->

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (jobs.isEmpty()) {
                item {
                    EmptyJobs(modifier = Modifier.fillParentMaxSize().padding(top = 60.dp))
                }
            } else {
                itemsIndexed(jobs, key = { _, j -> j.id }) { index, job ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(250, index * 50)) + slideInVertically(tween(250, index * 50)) { it / 4 }
                    ) {
                        JobCard(job = job, onCancel = { cancelTarget = job })
                    }
                }
            }
        }
    }

    // Add Job Bottom Sheet
    if (showAddSheet) {
        AddJobSheet(
            urlInput = urlInput,
            titleInput = titleInput,
            segLength = segLength,
            isLoading = addJobState is AddJobState.Loading,
            error = (addJobState as? AddJobState.Error)?.msg,
            onUrlChange = { urlInput = it },
            onTitleChange = { titleInput = it },
            onSegLengthChange = { segLength = it },
            onSubmit = { vm.addJob(urlInput, titleInput, segLength) },
            onDismiss = { showAddSheet = false; vm.resetAddState() }
        )
    }

    // Cancel confirm
    cancelTarget?.let { job ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            containerColor = SVColors.Surface,
            title = { Text("Cancel Job?", color = SVColors.TextPrimary) },
            text = { Text("This will stop the current job.", color = SVColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.cancelJob(job.id); cancelTarget = null }) {
                    Text("Cancel Job", color = SVColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Keep", color = SVColors.TextMuted)
                }
            }
        )
    }
}

@Composable
private fun AdminTopBar(onBack: () -> Unit, jobCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SVColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = SVColors.TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "Dashboard",
            style = MaterialTheme.typography.titleLarge,
            color = SVColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        if (jobCount > 0) {
            Box(
                modifier = Modifier
                    .background(SVColors.Cyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "$jobCount active",
                    style = MaterialTheme.typography.labelSmall,
                    color = SVColors.Cyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun JobCard(job: Job, onCancel: () -> Unit) {
    val status = JobStatus.from(job.status)
    val isActive = status in listOf(JobStatus.PENDING, JobStatus.DOWNLOADING, JobStatus.TRANSCODING, JobStatus.THUMBNAILING)

    val borderColor = when (status) {
        JobStatus.DOWNLOADING  -> SVColors.Cyan.copy(alpha = 0.3f)
        JobStatus.TRANSCODING  -> SVColors.Violet.copy(alpha = 0.3f)
        JobStatus.DONE         -> SVColors.Success.copy(alpha = 0.3f)
        JobStatus.FAILED       -> SVColors.Danger.copy(alpha = 0.3f)
        JobStatus.CANCELLED    -> SVColors.Border
        else -> SVColors.Border
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SVColors.Surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        job.title.ifBlank { job.id.take(12) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SVColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        job.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = SVColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(status)
            }

            Spacer(Modifier.height(10.dp))

            // Download progress
            if (status == JobStatus.DOWNLOADING || job.downloadPct > 0) {
                ProgressSection(
                    label = "Download",
                    pct = job.downloadPct.toFloat() / 100f,
                    color = SVColors.Cyan,
                    extra = buildString {
                        if (job.downloadSpeed > 0) append(formatSpeed(job.downloadSpeed))
                        if (job.totalBytes > 0) append(" · ${formatBytes(job.downloadedBytes)} / ${formatBytes(job.totalBytes)}")
                        if (job.downloadEta > 0) append(" · ETA ${formatEta(job.downloadEta)}")
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Transcode progress
            if (status == JobStatus.TRANSCODING || job.transcodePct > 0) {
                ProgressSection(
                    label = "HLS Segment",
                    pct = job.transcodePct.toFloat() / 100f,
                    color = SVColors.Violet,
                    extra = "${job.transcodeDone} / ${job.transcodeSegments} segs"
                )
                Spacer(Modifier.height(8.dp))
            }

            // Error
            if (!job.error.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SVColors.Danger.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = SVColors.Danger, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(job.error, style = MaterialTheme.typography.labelSmall, color = SVColors.Danger)
                }
                Spacer(Modifier.height(8.dp))
            }

            // Cancel button
            if (isActive) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.textButtonColors(contentColor = SVColors.Danger)
                ) {
                    Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(label: String, pct: Float, color: Color, extra: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SVColors.TextMuted)
            Text("%.1f%%".format(pct * 100), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = SVColors.SurfaceHigh,
            strokeCap = StrokeCap.Round
        )
        if (extra.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(extra, style = MaterialTheme.typography.labelSmall, color = SVColors.TextMuted)
        }
    }
}

@Composable
private fun StatusChip(status: JobStatus) {
    val (color, text) = when (status) {
        JobStatus.PENDING      -> Pair(SVColors.TextMuted,    "Pending")
        JobStatus.DOWNLOADING  -> Pair(SVColors.Cyan,         "Downloading")
        JobStatus.TRANSCODING  -> Pair(SVColors.VioletLight,  "Segmenting")
        JobStatus.THUMBNAILING -> Pair(SVColors.Warning,      "Thumb")
        JobStatus.DONE         -> Pair(SVColors.Success,      "Done")
        JobStatus.FAILED       -> Pair(SVColors.Danger,       "Failed")
        JobStatus.CANCELLED    -> Pair(SVColors.TextMuted,    "Cancelled")
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddJobSheet(
    urlInput: String,
    titleInput: String,
    segLength: Int,
    isLoading: Boolean,
    error: String?,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onSegLengthChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val segOptions = listOf(4, 6, 8, 10, 12, 20, 30)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SVColors.Surface,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp).height(4.dp)
                    .background(SVColors.Border, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("New Download Job", style = MaterialTheme.typography.titleMedium, color = SVColors.TextPrimary, fontWeight = FontWeight.Bold)

            SVTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                label = "Direct URL",
                placeholder = "https://example.com/video.mp4",
                keyboardType = KeyboardType.Uri
            )

            SVTextField(
                value = titleInput,
                onValueChange = onTitleChange,
                label = "Title (optional)",
                placeholder = "My Video"
            )

            // Segment length selector
            Column {
                Text("Segment Length", style = MaterialTheme.typography.labelSmall, color = SVColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    segOptions.forEach { sec ->
                        val selected = sec == segLength
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) SVColors.Cyan.copy(alpha = 0.12f) else SVColors.SurfaceVar,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (selected) SVColors.Cyan else SVColors.Border, RoundedCornerShape(8.dp))
                                .clickable { onSegLengthChange(sec) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${sec}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) SVColors.Cyan else SVColors.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Error
            AnimatedVisibility(visible = error != null) {
                error?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(SVColors.Danger.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, null, tint = SVColors.Danger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = SVColors.Danger)
                    }
                }
            }

            Button(
                onClick = onSubmit,
                enabled = urlInput.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SVColors.Cyan, contentColor = Color.Black)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Download", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyJobs(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Inbox, null, tint = SVColors.TextMuted, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("No active jobs", style = MaterialTheme.typography.titleSmall, color = SVColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text("Tap + to add a download", style = MaterialTheme.typography.bodySmall, color = SVColors.TextMuted)
    }
}

private fun formatSpeed(bps: Double): String {
    return when {
        bps < 1024 * 1024 -> "%.0f KB/s".format(bps / 1024)
        else -> "%.1f MB/s".format(bps / (1024 * 1024))
    }
}

private fun formatEta(sec: Double): String {
    val s = sec.toLong()
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}

