package com.streamvault.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.streamvault.ui.theme.SVColors

// ── Search bar ───────────────────────────────────────────────────────────────
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(SVColors.SurfaceVar, RoundedCornerShape(12.dp))
            .border(1.dp, SVColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search, "Search",
            tint = SVColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = SVColors.TextPrimary),
            decorationBox = { inner ->
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search videos…", style = MaterialTheme.typography.bodyMedium, color = SVColors.TextMuted)
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f)
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Filled.Close, "Clear", tint = SVColors.TextMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Shimmer loading card ──────────────────────────────────────────────────────
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val progress by shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer_pos"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            SVColors.SurfaceVar,
            SVColors.SurfaceHigh,
            SVColors.SurfaceVar,
        ),
        start = Offset(progress * 400f, 0f),
        end = Offset((progress + 1) * 400f, 300f)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SVColors.Surface),
        border = BorderStroke(1.dp, SVColors.Border)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(brush)
            )
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth(0.85f).height(12.dp).background(brush, RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(0.5f).height(10.dp).background(brush, RoundedCornerShape(4.dp)))
            }
        }
    }
}

// ── Delete confirmation dialog ────────────────────────────────────────────────
@Composable
fun DeleteConfirmDialog(
    videoTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SVColors.Surface,
        shape = RoundedCornerShape(18.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SVColors.Danger.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DeleteOutline, null, tint = SVColors.Danger, modifier = Modifier.size(22.dp))
            }
        },
        title = {
            Text("Delete Video?", color = SVColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "\"$videoTitle\" will be permanently deleted along with all HLS segments.",
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SVColors.Danger),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SVColors.TextMuted)
            }
        }
    )
}

// ── Styled text field ─────────────────────────────────────────────────────────
@Composable
fun SVTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SVColors.TextMuted,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = SVColors.TextMuted, style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SVColors.TextPrimary,
                unfocusedTextColor = SVColors.TextPrimary,
                focusedBorderColor = SVColors.Cyan,
                unfocusedBorderColor = SVColors.Border,
                cursorColor = SVColors.Cyan,
                focusedContainerColor = SVColors.SurfaceVar,
                unfocusedContainerColor = SVColors.SurfaceVar,
            )
        )
    }
}

// ── BasicTextField shim for Gallery search ───────────────────────────────────
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        decorationBox = decorationBox
    )
}

