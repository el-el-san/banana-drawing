package com.example.bdrowclient.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TimelineSlider(
    currentPosition: Int,
    timelineImages: Map<Int, Bitmap>,
    onPositionChange: (Int) -> Unit,
    onDeleteImage: (Int) -> Unit,
    onMoveImage: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    maxSeconds: Int = 15
) {
    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }
    var draggingMarker by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Timeline header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${currentPosition}/${maxSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Slider and markers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            // Main slider
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onPositionChange(it.roundToInt()) },
                valueRange = 0f..maxSeconds.toFloat(),
                steps = maxSeconds - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            )
            
            // Image markers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val density = LocalDensity.current
                timelineImages.forEach { (timestamp, bitmap) ->
                    val position = timestamp.toFloat() / maxSeconds.toFloat()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(position)
                            .wrapContentWidth(Alignment.End)
                    ) {
                        TimelineMarker(
                            bitmap = bitmap,
                            timestamp = timestamp,
                            isActive = currentPosition == timestamp,
                            isDragging = draggingMarker == timestamp,
                            onLongPress = { showDeleteDialog = timestamp },
                            onDragStart = { draggingMarker = timestamp },
                            onDragEnd = { newPosition ->
                                draggingMarker?.let { from ->
                                    if (from != newPosition && newPosition in 0..maxSeconds) {
                                        onMoveImage(from, newPosition)
                                    }
                                }
                                draggingMarker = null
                            },
                            maxSeconds = maxSeconds
                        )
                    }
                }
            }
        }
        
        // Second indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..maxSeconds step 5) {
                Text(
                    text = "${i}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { timestamp ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Image") },
            text = { Text("Delete image at ${timestamp}s?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteImage(timestamp)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineMarker(
    bitmap: Bitmap,
    timestamp: Int,
    isActive: Boolean,
    isDragging: Boolean,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: (Int) -> Unit,
    maxSeconds: Int
) {
    var offsetX by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .size(if (isActive) 36.dp else 30.dp)
            .offset(x = with(LocalDensity.current) { offsetX.toDp() })
            .pointerInput(timestamp) {
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                        offsetX = 0f
                    },
                    onDragEnd = {
                        val newPosition = ((timestamp + (offsetX / size.width * maxSeconds)).roundToInt())
                            .coerceIn(0, maxSeconds)
                        onDragEnd(newPosition)
                        offsetX = 0f
                    },
                    onDrag = { _, dragAmount ->
                        offsetX += dragAmount.x
                    }
                )
            }
            .combinedClickable(
                onClick = { },
                onLongClick = onLongPress
            )
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer
            )
            .border(
                width = if (isDragging) 2.dp else 1.dp,
                color = if (isDragging) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image at $timestamp seconds",
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        // Timestamp badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(12.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                )
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.surface,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timestamp.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun TimelineControls(
    isPlaying: Boolean,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onLoop: () -> Unit,
    isLooping: Boolean,
    onClearAll: () -> Unit,
    hasImages: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset button
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Play/pause button
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp),
                    enabled = hasImages
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Loop button
                IconToggleButton(
                    checked = isLooping,
                    onCheckedChange = { onLoop() }
                ) {
                    Icon(
                        Icons.Default.Loop,
                        contentDescription = "Loop",
                        tint = if (isLooping) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Playback speed control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed:",
                    style = MaterialTheme.typography.bodySmall
                )
                listOf(0.5f to "0.5x", 1f to "1x", 2f to "2x").forEach { (speed, label) ->
                    FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = { onSpeedChange(speed) },
                        label = { 
                            Text(
                                label, 
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
            
            // Clear button
            if (hasImages) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Timeline", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}