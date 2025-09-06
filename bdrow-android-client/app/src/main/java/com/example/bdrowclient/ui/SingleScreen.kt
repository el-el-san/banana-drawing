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
                    // API Key settings button
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
            // Output image section
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
                            text = "Output Image",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.resultImageBitmap != null || state.timelineImages.isNotEmpty()) {
                            Row {
                                // Add to timeline button
                                if (state.resultImageBitmap != null) {
                                    IconButton(
                                        onClick = {
                                            viewModel.addImageToTimeline()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Image added at ${state.currentTimePosition}s"
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add to Timeline")
                                    }
                                }
                                // Save button
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val bitmap = viewModel.getCurrentTimelineImage() ?: state.resultImageBitmap
                                            bitmap?.let {
                                                val success = saveImage(context, it)
                                                snackbarHostState.showSnackbar(
                                                    if (success) "Image saved"
                                                    else "Failed to save"
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                }
                            }
                        }
                    }
                    
                    // Timeline control (always visible)
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
                    
                    // Display image selection logic - prioritize generation results, timeline images only during timeline operation
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
                            contentDescription = "Result Image",
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
                            Text("Processing results will be displayed here")
                        }
                    }
                    
                    // Result text
                    if (state.resultText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.resultText!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Input image section (multiple images supported)
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
                            text = "Input Images (${state.inputImages.size}/4)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            if (state.inputImages.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearAllImages() }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete All")
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
            
            // Text input and submit
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
                        label = { Text("Prompt") },
                        placeholder = { Text("Example: smiling with closed eyes") },
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
                            text = "✨ Send to Gemini",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.inputImages.isNotEmpty() || inputText.isNotEmpty()
                        )
                    }
                }
            }
            
            // Debug information
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
                                    text = "🐛 Debug Information",
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
            
            // Error display
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
    
    // API key setup dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSave = { apiKey ->
                viewModel.saveApiKey(apiKey)
                showApiKeyDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("API key saved")
                }
            },
            currentApiKey = viewModel.getApiKey()
        )
    }
    
    // Image edit dialog
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
    
    // Playback processing
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
        title = { Text("Gemini API Key Setup") },
        text = {
            Column {
                Text(
                    text = "Please enter your Gemini API key",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
