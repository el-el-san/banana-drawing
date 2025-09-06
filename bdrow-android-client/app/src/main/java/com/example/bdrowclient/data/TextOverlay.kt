package com.example.bdrowclient.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import java.util.UUID

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val position: Offset,  // Position in pixels
    val size: IntSize = IntSize(200, 50),  // Size in pixels
    val fontSize: Float = 24f,  // Base font size (dynamically scaled)
    val color: Color = Color.Black,
    val backgroundColor: Color = Color.White.copy(alpha = 0.8f),
    val rotation: Float = 0f
)
