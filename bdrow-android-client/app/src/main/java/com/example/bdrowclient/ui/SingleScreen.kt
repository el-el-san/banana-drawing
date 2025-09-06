package com.example.bdrowclient.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.bdrowclient.ui.components.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.bdrowclient.MainViewModel
import com.example.bdrowclient.InputImage
import com.example.bdrowclient.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isDrawingMode by remember { mutableStateOf(false) }
    var isEraserMode by remember { mutableStateOf(false) }
    var brushSize by remember { mutableStateOf(10f) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    withContext(Dispatchers.Main) {
                        viewModel.addInputImage(bitmap)
                    }
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✨ Banana Drawing",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // API Key設定ボタン
                    ModernFloatingButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.size(48.dp),
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 出力画像セクション
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "出力画像",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.resultImageBitmap != null || state.timelineImages.isNotEmpty()) {
                            Row {
                                // タイムラインに追加ボタン
                                if (state.resultImageBitmap != null) {
                                    IconButton(
                                        onClick = {
                                            viewModel.addImageToTimeline()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "${state.currentTimePosition}秒に画像を追加しました"
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "タイムラインに追加")
                                    }
                                }
                                // 保存ボタン
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val bitmap = viewModel.getCurrentTimelineImage() ?: state.resultImageBitmap
                                            bitmap?.let {
                                                val success = saveImage(context, it)
                                                snackbarHostState.showSnackbar(
                                                    if (success) "画像を保存しました"
                                                    else "保存に失敗しました"
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "保存")
                                }
                            }
                        }
                    }
                    
                    // タイムラインコントロール（常に表示）
                    TimelineSlider(
                        currentPosition = state.currentTimePosition,
                        timelineImages = state.timelineImages,
                        onPositionChange = { viewModel.setTimePosition(it) },
                        onDeleteImage = { viewModel.removeImageFromTimeline(it) },
                        onMoveImage = { from, to -> viewModel.moveImageInTimeline(from, to) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TimelineControls(
                        isPlaying = state.isPlaying,
                        playbackSpeed = state.playbackSpeed,
                        onPlayPause = { viewModel.setPlayingState(!state.isPlaying) },
                        onReset = { viewModel.resetTimeline() },
                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                        onLoop = { viewModel.toggleLoop() },
                        isLooping = state.isLooping,
                        onClearAll = { viewModel.clearTimeline() },
                        hasImages = state.timelineImages.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 表示する画像の選択ロジック - 生成結果を優先、タイムライン操作時のみタイムライン画像
                    val displayImage = if (state.showTimelineImage) {
                        viewModel.getCurrentTimelineImage() ?: state.resultImageBitmap
                    } else {
                        state.resultImageBitmap ?: viewModel.getCurrentTimelineImage()
                    }
                    
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PulsingLoader()
                        }
                    } else if (displayImage != null) {
                        Image(
                            bitmap = displayImage.asImageBitmap(),
                            contentDescription = "結果画像",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("処理結果がここに表示されます")
                        }
                    }
                    
                    // 結果テキスト
                    if (state.resultText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.resultText!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // 入力画像セクション（複数画像対応）
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "入力画像 (${state.inputImages.size}/4)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            if (state.inputImages.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearAllImages() }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "全削除")
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    BentoImageGrid(
                        images = state.inputImages,
                        selectedIndex = state.selectedImageIndex,
                        onImageClick = { index -> viewModel.selectImage(index) },
                        onAddImage = { 
                            if (state.inputImages.size < 4) {
                                galleryLauncher.launch("image/*")
                            }
                        },
                        onRemoveImage = { index -> viewModel.removeInputImage(index) },
                        onEditImage = { index -> editingImageIndex = index },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // テキスト入力と送信
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("プロンプト") },
                        placeholder = { Text("例: 目を閉じた笑顔") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PulsingLoader()
                        }
                    } else {
                        GradientButton(
                            onClick = {
                                viewModel.setInputText(inputText)
                                viewModel.sendToGemini()
                            },
                            text = "✨ Geminiに送信",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.inputImages.isNotEmpty() || inputText.isNotEmpty()
                        )
                    }
                }
            }
            
            // デバッグ情報
            AnimatedVisibility(
                visible = state.debugInfo != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                state.debugInfo?.let {
                    SoftShadowCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🐛 デバッグ情報",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // エラー表示
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                state.errorMessage?.let {
                    SoftShadowCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // APIキー設定ダイアログ
    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSave = { apiKey ->
                viewModel.saveApiKey(apiKey)
                showApiKeyDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("APIキーを保存しました")
                }
            },
            currentApiKey = viewModel.getApiKey()
        )
    }
    
    // 画像編集ダイアログ
    editingImageIndex?.let { index ->
        state.inputImages.getOrNull(index)?.let { image ->
            ModernImageEditDialog(
                bitmap = image.bitmap,
                onDismiss = { editingImageIndex = null },
                onSave = { editedBitmap ->
                    viewModel.updateInputImage(index, editedBitmap)
                    editingImageIndex = null
                }
            )
        }
    }
    
    // 再生処理
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying && state.timelineImages.isNotEmpty()) {
            while (state.isPlaying) {
                delay((1000 / state.playbackSpeed).toLong())
                val nextPosition = if (state.currentTimePosition >= 15) {
                    if (state.isLooping) 0 else {
                        viewModel.setPlayingState(false)
                        15
                    }
                } else {
                    state.currentTimePosition + 1
                }
                if (state.isPlaying) {
                    viewModel.setTimePosition(nextPosition)
                }
            }
        }
    }
}

@Composable
fun ApiKeyDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    currentApiKey: String?
) {
    var apiKey by remember { mutableStateOf(currentApiKey ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini APIキー設定") },
        text = {
            Column {
                Text(
                    text = "Gemini APIキーを入力してください",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("APIキー") },
                    placeholder = { Text("AIza...") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide" else "Show"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private suspend fun saveImage(context: android.content.Context, bitmap: Bitmap): Boolean {
    return try {
        FileUtils.saveBitmapToFile(context, bitmap, "banana_${System.currentTimeMillis()}.jpg")
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
