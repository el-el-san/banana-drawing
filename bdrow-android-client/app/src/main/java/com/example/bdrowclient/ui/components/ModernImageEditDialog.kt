package com.example.bdrowclient.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bdrowclient.ui.theme.*
import com.example.bdrowclient.data.TextOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernImageEditDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    val fontScale = LocalDensity.current.fontScale
    var editedBitmap by remember { 
        mutableStateOf(bitmap.copy(Bitmap.Config.ARGB_8888, true))
    }
    var isDrawingMode by remember { mutableStateOf(false) }
    var isEraserMode by remember { mutableStateOf(false) }
    var isTextMode by remember { mutableStateOf(false) }
    var brushSize by remember { mutableStateOf(10f) }
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(HotPink) }
    
    // Text overlay state
    var textOverlays by remember { mutableStateOf(listOf<TextOverlay>()) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf(TextFieldValue("")) }
    var textFontSize by remember { mutableStateOf(24f) }
    var isEditingText by remember { mutableStateOf(false) }
    
    val colors = listOf(
        HotPink,
        BubblegumPink,
        BananaYellow,
        DeepLavender,
        SuccessGreen,
        InfoBlue,
        AlmostBlack
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = DialogShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 16.dp
        ) {
            Box {
                // Background gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PastelYellow.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                radius = 800f
                            )
                        )
                )
                
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Modern top bar
                    TopAppBar(
                        title = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Edit Image",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AnimatedChip(
                                    selected = true,
                                    onClick = {},
                                    label = "Editing",
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        },
                        navigationIcon = {
                            ModernFloatingButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp),
                                icon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        },
                        actions = {
                            GradientButton(
                                onClick = {
                                    // Integrate text into image and save
                                    val finalBitmap = mergeTextOverlays(editedBitmap, textOverlays, density.density, fontScale)
                                    onSave(finalBitmap)
                                    onDismiss()
                                },
                                text = "Save",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    // Toolbar
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column {
                            // Tool buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ToolButton(
                                    icon = Icons.Default.Edit,
                                    label = "Pen",
                                    isSelected = isDrawingMode && !isEraserMode && !isTextMode,
                                    onClick = {
                                        isDrawingMode = true
                                        isEraserMode = false
                                        isTextMode = false
                                    }
                                )
                                ToolButton(
                                    icon = Icons.Default.Clear,
                                    label = "Eraser",
                                    isSelected = isEraserMode,
                                    onClick = {
                                        isDrawingMode = true
                                        isEraserMode = true
                                        isTextMode = false
                                    }
                                )
                                ToolButton(
                                    icon = Icons.Default.TextFields,
                                    label = "Text",
                                    isSelected = isTextMode,
                                    onClick = {
                                        isTextMode = true
                                        isDrawingMode = false
                                        isEraserMode = false
                                        showTextInputDialog = true
                                    }
                                )
                                ToolButton(
                                    icon = Icons.Default.Palette,
                                    label = "Color",
                                    isSelected = showColorPicker,
                                    onClick = {
                                        showColorPicker = !showColorPicker
                                    }
                                )
                            }

                            // Color picker
                            AnimatedVisibility(
                                visible = showColorPicker,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    colors.forEach { color ->
                                        ColorButton(
                                            color = color,
                                            isSelected = selectedColor == color,
                                            onClick = { selectedColor = color }
                                        )
                                    }
                                }
                            }

                            // Brush size slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Brush,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${brushSize.toInt()}px",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(40.dp)
                                )
                                Slider(
                                    value = brushSize,
                                    onValueChange = { brushSize = it },
                                    valueRange = 1f..50f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    // Image editor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        SoftShadowCard(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val boxWidth = constraints.maxWidth.toFloat()
                                val boxHeight = constraints.maxHeight.toFloat()
                                
                                // Calculate image scale and offset
                                val bitmapWidth = editedBitmap.width.toFloat()
                                val bitmapHeight = editedBitmap.height.toFloat()
                                val imageScale = minOf(
                                    boxWidth / bitmapWidth,
                                    boxHeight / bitmapHeight
                                )
                                val scaledImageWidth = bitmapWidth * imageScale
                                val scaledImageHeight = bitmapHeight * imageScale
                                val imageOffsetX = (boxWidth - scaledImageWidth) / 2
                                val imageOffsetY = (boxHeight - scaledImageHeight) / 2
                                
                                // Image drawing layer
                                DrawableImageView(
                                    bitmap = editedBitmap,
                                    isDrawingEnabled = isDrawingMode && !isTextMode,
                                    isEraserMode = isEraserMode,
                                    brushSize = brushSize,
                                    brushColor = selectedColor,
                                    onBitmapChanged = { editedBitmap = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Text overlay layer
                                textOverlays.forEach { overlay ->
                                    // Convert from bitmap coordinates to display coordinates
                                    val displayPosition = Offset(
                                        imageOffsetX + overlay.position.x * imageScale,
                                        imageOffsetY + overlay.position.y * imageScale
                                    )
                                    val displaySize = IntSize(
                                        (overlay.size.width * imageScale).toInt(),
                                        (overlay.size.height * imageScale).toInt()
                                    )
                                    
                                    TextOverlayItem(
                                        textOverlay = overlay.copy(
                                            position = displayPosition,
                                            size = displaySize
                                        ),
                                        isSelected = selectedTextId == overlay.id,
                                        isEditMode = isTextMode,
                                        imageScale = imageScale,
                                        imageOffset = Offset(imageOffsetX, imageOffsetY),
                                        onMove = { id, newPosition ->
                                            // Convert from display coordinates to bitmap coordinates
                                            val bitmapPosition = Offset(
                                                (newPosition.x - imageOffsetX) / imageScale,
                                                (newPosition.y - imageOffsetY) / imageScale
                                            )
                                            textOverlays = textOverlays.map {
                                                if (it.id == id) it.copy(position = bitmapPosition) else it
                                            }
                                        },
                                        onResize = { id, newSize ->
                                            // Convert from display size to bitmap size
                                            val bitmapSize = IntSize(
                                                (newSize.width / imageScale).toInt(),
                                                (newSize.height / imageScale).toInt()
                                            )
                                            textOverlays = textOverlays.map {
                                                if (it.id == id) it.copy(size = bitmapSize) else it
                                            }
                                        },
                                        onDelete = { id ->
                                            textOverlays = textOverlays.filter { it.id != id }
                                            if (selectedTextId == id) selectedTextId = null
                                        },
                                        onClick = { id ->
                                            selectedTextId = if (selectedTextId == id) null else id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Text input dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Add Text") },
            text = {
                Column {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Text") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size: ")
                        Slider(
                            value = textFontSize,
                            onValueChange = { textFontSize = it },
                            valueRange = 12f..72f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${textFontSize.toInt()}px")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (textInput.text.isNotEmpty()) {
                            val newOverlay = TextOverlay(
                                text = textInput.text,
                                position = Offset(100f, 100f), // Bitmap pixel coordinates
                                size = IntSize(200, 50), // Bitmap pixel size
                                fontSize = textFontSize,
                                color = selectedColor
                            )
                            textOverlays = textOverlays + newOverlay
                            selectedTextId = newOverlay.id  // Set newly added text as selected
                            textInput = TextFieldValue("")
                            showTextInputDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTextInputDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = CloudWhite,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp),
                tint = CloudWhite
            )
        }
    }
}

// Function to integrate text overlay into image
private fun mergeTextOverlays(bitmap: Bitmap, textOverlays: List<TextOverlay>, density: Float, fontScale: Float): Bitmap {
    val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)
    
    textOverlays.forEach { overlay ->
        // Convert UI position and size (managed in dp) to pixels
        val widthPx = overlay.size.width.toFloat() // IntSize width is already in pixels
        val heightPx = overlay.size.height.toFloat() // IntSize height is already in pixels  
        val positionXPx = overlay.position.x  // Offset is in pixels
        val positionYPx = overlay.position.y  // Offset is in pixels
        
        // Dynamically calculate text size based on box size (same logic as UI)
        // size.height is in pixels, so convert to dp before calculation
        val heightInDp = overlay.size.height.toFloat() / density
        val scaledFontSize = overlay.fontSize * (heightInDp / 50f)
        
        val paint = Paint().apply {
            color = overlay.color.toArgb()
            // UI uses sp and internally multiplies by density * fontScale,
            // but here we draw directly in pixels so we perform equivalent calculation
            textSize = scaledFontSize * density * fontScale
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        
        // Draw background
        val bgPaint = Paint().apply {
            color = overlay.backgroundColor.toArgb()
            style = Paint.Style.FILL
        }
        
        val rect = RectF(
            positionXPx,
            positionYPx,
            positionXPx + widthPx,
            positionYPx + heightPx
        )
        canvas.drawRoundRect(rect, 8f * density, 8f * density, bgPaint)
        
        // Draw text centered
        val textX = positionXPx + widthPx / 2
        val textY = positionYPx + heightPx / 2 + (paint.descent() - paint.ascent()) / 2 - paint.descent()
        
        canvas.drawText(
            overlay.text,
            textX,
            textY,
            paint
        )
    }
    
    return resultBitmap
}

// Text overlay item component
@Composable
private fun TextOverlayItem(
    textOverlay: TextOverlay,
    isSelected: Boolean,
    isEditMode: Boolean,
    imageScale: Float = 1f,
    imageOffset: Offset = Offset.Zero,
    onMove: (String, Offset) -> Unit,
    onResize: (String, IntSize) -> Unit,
    onDelete: (String) -> Unit,
    onClick: (String) -> Unit
) {
    val density = LocalDensity.current.density
    var offset by remember(textOverlay.id) { mutableStateOf(textOverlay.position) }
    var size by remember(textOverlay.id) { mutableStateOf(textOverlay.size) }
    
    Box(
        modifier = Modifier
            .offset { 
                IntOffset(
                    offset.x.toInt(),
                    offset.y.toInt()
                )
            }
            .size(width = (size.width / density).dp, height = (size.height / density).dp)
            .pointerInput(isEditMode, textOverlay.id) {
                if (isEditMode) {
                    detectDragGestures { _, dragAmount ->
                        val newOffset = Offset(
                            (offset.x + dragAmount.x).coerceAtLeast(0f),
                            (offset.y + dragAmount.y).coerceAtLeast(0f)
                        )
                        offset = newOffset
                        onMove(textOverlay.id, newOffset)
                    }
                }
            }
            .clickable(enabled = isEditMode) {
                onClick(textOverlay.id)
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected && isEditMode) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = textOverlay.backgroundColor
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dynamically calculate text size based on box size
                // size.height is in pixels, so convert to dp before calculation
                val heightInDp = size.height.toFloat() / density
                val scaledFontSize = (textOverlay.fontSize * (heightInDp / 50f)).sp
                Text(
                    text = textOverlay.text,
                    fontSize = scaledFontSize,
                    color = textOverlay.color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                // Delete button (only shown when selected)
                if (isSelected && isEditMode) {
                    IconButton(
                        onClick = { onDelete(textOverlay.id) },
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Resize handle (only shown when selected)
                if (isSelected && isEditMode) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    val newSize = IntSize(
                                        (size.width + dragAmount.x.toInt()).coerceAtLeast((100 * density).toInt()),
                                        (size.height + dragAmount.y.toInt()).coerceAtLeast((40 * density).toInt())
                                    )
                                    size = newSize
                                    onResize(textOverlay.id, newSize)
                                }
                            }
                    )
                }
            }
        }
    }
}
