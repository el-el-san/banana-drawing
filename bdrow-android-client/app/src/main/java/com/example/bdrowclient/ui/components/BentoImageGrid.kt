package com.example.bdrowclient.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.bdrowclient.InputImage
import com.example.bdrowclient.ui.theme.*

// Bentoグリッドスタイルの画像グリッド
@Composable
fun BentoImageGrid(
    images: List<InputImage>,
    selectedIndex: Int?,
    onImageClick: (Int) -> Unit,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onEditImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Column(modifier = modifier) {
        // アシンメトリックなBentoグリッドレイアウト
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左側: 大きなメイン画像スロット
            BentoImageSlot(
                index = 0,
                image = images.getOrNull(0),
                isSelected = selectedIndex == 0,
                onImageClick = onImageClick,
                onAddImage = onAddImage,
                onRemoveImage = onRemoveImage,
                onEditImage = onEditImage,
                shimmerTranslateX = shimmerTranslateX,
                modifier = Modifier
                    .weight(1.5f)
                    .aspectRatio(0.75f),
                isLarge = true
            )
            
            // 右側: 縦に2つの小さな画像スロット
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoImageSlot(
                    index = 1,
                    image = images.getOrNull(1),
                    isSelected = selectedIndex == 1,
                    onImageClick = onImageClick,
                    onAddImage = onAddImage,
                    onRemoveImage = onRemoveImage,
                    onEditImage = onEditImage,
                    shimmerTranslateX = shimmerTranslateX,
                    modifier = Modifier.aspectRatio(1.2f)
                )
                BentoImageSlot(
                    index = 2,
                    image = images.getOrNull(2),
                    isSelected = selectedIndex == 2,
                    onImageClick = onImageClick,
                    onAddImage = onAddImage,
                    onRemoveImage = onRemoveImage,
                    onEditImage = onEditImage,
                    shimmerTranslateX = shimmerTranslateX,
                    modifier = Modifier.aspectRatio(1.2f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 下部: 横長の画像スロット
        BentoImageSlot(
            index = 3,
            image = images.getOrNull(3),
            isSelected = selectedIndex == 3,
            onImageClick = onImageClick,
            onAddImage = onAddImage,
            onRemoveImage = onRemoveImage,
            onEditImage = onEditImage,
            shimmerTranslateX = shimmerTranslateX,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.5f),
            isWide = true
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BentoImageSlot(
    index: Int,
    image: InputImage?,
    isSelected: Boolean,
    onImageClick: (Int) -> Unit,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onEditImage: (Int) -> Unit,
    shimmerTranslateX: Float,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    isWide: Boolean = false
) {
    var isHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 0.95f
            isHovered -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(300)
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(CardShape)
            .background(
                if (image != null) {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            FrostedGlass.copy(alpha = 0.3f),
                            FrostedGlass.copy(alpha = 0.1f)
                        ),
                        start = Offset(shimmerTranslateX, 0f),
                        end = Offset(shimmerTranslateX + 100f, 100f)
                    )
                }
            )
            .shadow(
                elevation = if (isSelected) 12.dp else 4.dp,
                shape = CardShape,
                ambientColor = BubblegumPink.copy(alpha = 0.2f),
                spotColor = DeepLavender.copy(alpha = 0.3f)
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        BubblegumPink.copy(alpha = borderAlpha),
                        DeepLavender.copy(alpha = borderAlpha)
                    )
                ),
                shape = CardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    color = if (image != null) MaterialTheme.colorScheme.primary else BubblegumPink
                )
            ) {
                if (image != null) {
                    onImageClick(index)
                } else {
                    onAddImage()
                }
            }
    ) {
        AnimatedContent(
            targetState = image,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with
                    fadeOut(animationSpec = tween(300))
            }
        ) { targetImage ->
            if (targetImage != null) {
                // 画像表示
                Box {
                    Image(
                        bitmap = targetImage.bitmap.asImageBitmap(),
                        contentDescription = "Image ${index + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // グラデーションオーバーレイ
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.2f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    
                    // 編集済みバッジ
                    AnimatedVisibility(
                        visible = targetImage.isEdited,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(HotPink, BubblegumPink)
                                    ),
                                    shape = CircleShape
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Draw,
                                contentDescription = "Edited",
                                modifier = Modifier.size(16.dp),
                                tint = CloudWhite
                            )
                        }
                    }
                    
                    // アクションボタン
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally { it } + fadeIn(),
                            exit = slideOutHorizontally { it } + fadeOut()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FloatingActionButton(
                                    onClick = { onEditImage(index) },
                                    modifier = Modifier.size(36.dp),
                                    containerColor = FrostedGlass.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                FloatingActionButton(
                                    onClick = { onRemoveImage(index) },
                                    modifier = Modifier.size(36.dp),
                                    containerColor = FrostedGlass.copy(alpha = 0.8f),
                                    contentColor = ErrorRed,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 空のスロット
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val iconSize = when {
                        isLarge -> 48.dp
                        isWide -> 36.dp
                        else -> 32.dp
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(iconSize * 1.5f)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        PastelYellow.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add image",
                            modifier = Modifier.size(iconSize),
                            tint = BananaYellow
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when (index) {
                            0 -> "メイン画像"
                            1, 2 -> "サブ画像 ${index}"
                            3 -> "横長画像"
                            else -> "画像 ${index + 1}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (isLarge || isWide) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "タップして追加",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
