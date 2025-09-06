package com.example.bdrowclient.ui.components

import android.graphics.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

@Composable
fun DrawableImageView(
    bitmap: Bitmap,
    isDrawingEnabled: Boolean,
    isEraserMode: Boolean,
    brushSize: Float,
    brushColor: androidx.compose.ui.graphics.Color,
    onBitmapChanged: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var drawingCanvas by remember { mutableStateOf<android.graphics.Canvas?>(null) }
    var drawingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scaleX by remember { mutableStateOf(1f) }
    var scaleY by remember { mutableStateOf(1f) }
    var initializedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Initialize drawing bitmap only when bitmap changes from external source
    LaunchedEffect(bitmap) {
        // Only initialize if this is a different bitmap from what we've been editing
        if (bitmap != initializedBitmap) {
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            currentBitmap = mutableBitmap
            drawingBitmap = mutableBitmap
            drawingCanvas = android.graphics.Canvas(mutableBitmap)
            initializedBitmap = bitmap
        }
    }
    
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }
    
    // Update paint properties
    LaunchedEffect(isEraserMode, brushSize, brushColor) {
        paint.apply {
            strokeWidth = brushSize * density.density
            if (isEraserMode) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                color = Color.TRANSPARENT
            } else {
                xfermode = null
                color = brushColor.toArgb()
            }
        }
    }
    
    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    val path = remember { Path() }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isDrawingEnabled) {
                if (isDrawingEnabled) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Calculate scale to maintain aspect ratio
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val bitmapWidth = currentBitmap?.width?.toFloat() ?: return@detectDragGestures
                            val bitmapHeight = currentBitmap?.height?.toFloat() ?: return@detectDragGestures
                            
                            val scale = minOf(
                                canvasWidth / bitmapWidth,
                                canvasHeight / bitmapHeight
                            )
                            
                            val scaledWidth = bitmapWidth * scale
                            val scaledHeight = bitmapHeight * scale
                            
                            // Calculate offset for centered image
                            val offsetX = (canvasWidth - scaledWidth) / 2
                            val offsetY = (canvasHeight - scaledHeight) / 2
                            
                            // Adjust touch point relative to image position and scale
                            val adjustedX = offset.x - offsetX
                            val adjustedY = offset.y - offsetY
                            
                            // Only process if touch is within image bounds
                            if (adjustedX >= 0 && adjustedX <= scaledWidth &&
                                adjustedY >= 0 && adjustedY <= scaledHeight) {
                                
                                // Scale to bitmap coordinates
                                scaleX = bitmapWidth / scaledWidth
                                scaleY = bitmapHeight / scaledHeight
                                
                                val scaledOffset = Offset(
                                    adjustedX * scaleX,
                                    adjustedY * scaleY
                                )
                                lastPoint = scaledOffset
                                path.reset()
                                path.moveTo(scaledOffset.x, scaledOffset.y)
                            } else {
                                lastPoint = null
                            }
                        },
                        onDragEnd = {
                            lastPoint = null
                            currentBitmap?.let { onBitmapChanged(it) }
                        },
                        onDrag = { _, dragAmount ->
                            lastPoint?.let { last ->
                                // Scale the drag amount to bitmap coordinates
                                val newPoint = Offset(
                                    last.x + dragAmount.x * scaleX,
                                    last.y + dragAmount.y * scaleY
                                )
                                
                                // Clamp to bitmap bounds
                                val clampedPoint = Offset(
                                    newPoint.x.coerceIn(0f, currentBitmap?.width?.toFloat() ?: 0f),
                                    newPoint.y.coerceIn(0f, currentBitmap?.height?.toFloat() ?: 0f)
                                )
                                
                                path.lineTo(clampedPoint.x, clampedPoint.y)
                                
                                drawingCanvas?.let { canvas ->
                                    // Apply stroke width without excessive scaling
                                    paint.strokeWidth = brushSize * density.density
                                    canvas.drawPath(path, paint)
                                    currentBitmap = drawingBitmap!!
                                }
                                
                                lastPoint = clampedPoint
                            }
                        }
                    )
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            currentBitmap?.let { bmp ->
                // Calculate scale to maintain aspect ratio
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bitmapWidth = bmp.width.toFloat()
                val bitmapHeight = bmp.height.toFloat()
                
                val scale = minOf(
                    canvasWidth / bitmapWidth,
                    canvasHeight / bitmapHeight
                )
                
                val scaledWidth = (bitmapWidth * scale).toInt()
                val scaledHeight = (bitmapHeight * scale).toInt()
                
                // Center the image
                val left = ((canvasWidth - scaledWidth) / 2).toInt()
                val top = ((canvasHeight - scaledHeight) / 2).toInt()
                
                val dstRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                
                val bitmapPaint = Paint().apply {
                    isFilterBitmap = true
                    isAntiAlias = true
                }
                
                canvas.nativeCanvas.drawBitmap(
                    bmp,
                    srcRect,
                    dstRect,
                    bitmapPaint
                )
            }
        }
    }
}
