package com.example.bdrowclient

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bdrowclient.data.repository.GeminiRepository
import com.example.bdrowclient.data.preferences.ApiKeyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InputImage(
    val id: Int,
    val bitmap: Bitmap,
    val isEdited: Boolean = false
)

data class AppState(
    val imagePath: String? = null,
    val editedBitmap: Bitmap? = null,
    val inputImages: List<InputImage> = emptyList(),
    val selectedImageIndex: Int? = null,
    val inputText: String = "",
    val isLoading: Boolean = false,
    val resultText: String? = null,
    val resultImageBitmap: Bitmap? = null,
    val errorMessage: String? = null,
    val debugInfo: String? = null,
    // Timeline features
    val timelineImages: Map<Int, Bitmap> = emptyMap(),
    val currentTimePosition: Int = 0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val showTimelineImage: Boolean = false
)

class MainViewModel(
    private val context: android.app.Application
) : AndroidViewModel(context) {
    val repository = GeminiRepository(context)
    private val apiKeyPreferences = ApiKeyPreferences(context)
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    fun setImagePath(path: String) {
        _state.value = _state.value.copy(imagePath = path)
    }
    
    fun setEditedBitmap(bitmap: Bitmap) {
        _state.value = _state.value.copy(editedBitmap = bitmap)
    }
    
    fun addInputImage(bitmap: Bitmap) {
        val currentImages = _state.value.inputImages
        if (currentImages.size < 4) {
            val newImage = InputImage(
                id = currentImages.size,
                bitmap = bitmap
            )
            _state.value = _state.value.copy(
                inputImages = currentImages + newImage
            )
        }
    }
    
    fun removeInputImage(index: Int) {
        val currentImages = _state.value.inputImages.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            // Re-index the images
            val reindexedImages = currentImages.mapIndexed { idx, img ->
                img.copy(id = idx)
            }
            _state.value = _state.value.copy(
                inputImages = reindexedImages,
                selectedImageIndex = if (_state.value.selectedImageIndex == index) null else _state.value.selectedImageIndex
            )
        }
    }
    
    fun updateInputImage(index: Int, bitmap: Bitmap) {
        val currentImages = _state.value.inputImages.toMutableList()
        if (index in currentImages.indices) {
            currentImages[index] = currentImages[index].copy(
                bitmap = bitmap,
                isEdited = true
            )
            _state.value = _state.value.copy(inputImages = currentImages)
        }
    }
    
    fun selectImage(index: Int?) {
        _state.value = _state.value.copy(selectedImageIndex = index)
    }
    
    fun setInputText(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }
    
    fun sendToGemini() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true, 
                errorMessage = null,
                showTimelineImage = false
            )
            
            try {
                // Use multiple images if available, otherwise fall back to single image
                val imagesToSend = if (_state.value.inputImages.isNotEmpty()) {
                    _state.value.inputImages.map { it.bitmap }
                } else if (_state.value.editedBitmap != null) {
                    listOf(_state.value.editedBitmap!!)
                } else {
                    emptyList()
                }
                
                android.util.Log.d("MainViewModel", "Sending to Gemini: text='${_state.value.inputText}', images count=${imagesToSend.size}")
                
                val result = if (imagesToSend.size > 1) {
                    repository.processMultipleImagesAndText(
                        images = imagesToSend,
                        text = _state.value.inputText
                    )
                } else {
                    repository.processImageAndText(
                        image = imagesToSend.firstOrNull(),
                        text = _state.value.inputText
                    )
                }
                
                android.util.Log.d("MainViewModel", "Result received: text='${result.text}', hasImage=${result.processedImage != null}")
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    resultText = result.text,
                    resultImageBitmap = result.processedImage,
                    debugInfo = result.debugInfo
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error processing request", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An error occurred"
                )
            }
        }
    }
    
    fun clearResults() {
        _state.value = AppState()
    }
    
    fun clearAllImages() {
        _state.value = _state.value.copy(
            inputImages = emptyList(),
            selectedImageIndex = null,
            editedBitmap = null
        )
    }
    
    fun saveApiKey(apiKey: String) {
        apiKeyPreferences.saveApiKey(apiKey)
    }
    
    fun getApiKey(): String? {
        return apiKeyPreferences.getApiKey()
    }
    
    // Timeline functions
    fun setTimePosition(position: Int) {
        _state.value = _state.value.copy(
            currentTimePosition = position,
            showTimelineImage = true
        )
    }
    
    fun addImageToTimeline(position: Int? = null) {
        val targetPosition = position ?: _state.value.currentTimePosition
        _state.value.resultImageBitmap?.let { bitmap ->
            val updatedTimeline = _state.value.timelineImages.toMutableMap()
            updatedTimeline[targetPosition] = bitmap
            _state.value = _state.value.copy(timelineImages = updatedTimeline)
        }
    }
    
    fun removeImageFromTimeline(position: Int) {
        val updatedTimeline = _state.value.timelineImages.toMutableMap()
        updatedTimeline.remove(position)
        _state.value = _state.value.copy(timelineImages = updatedTimeline)
    }
    
    fun moveImageInTimeline(from: Int, to: Int) {
        val updatedTimeline = _state.value.timelineImages.toMutableMap()
        updatedTimeline[from]?.let { image ->
            updatedTimeline.remove(from)
            updatedTimeline[to] = image
            _state.value = _state.value.copy(timelineImages = updatedTimeline)
        }
    }
    
    fun clearTimeline() {
        _state.value = _state.value.copy(
            timelineImages = emptyMap(),
            currentTimePosition = 0,
            isPlaying = false
        )
    }
    
    fun setPlayingState(playing: Boolean) {
        _state.value = _state.value.copy(
            isPlaying = playing,
            showTimelineImage = playing || _state.value.showTimelineImage
        )
    }
    
    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
    }
    
    fun toggleLoop() {
        _state.value = _state.value.copy(isLooping = !_state.value.isLooping)
    }
    
    fun resetTimeline() {
        _state.value = _state.value.copy(currentTimePosition = 0, isPlaying = false)
    }
    
    fun getCurrentTimelineImage(): Bitmap? {
        if (!_state.value.showTimelineImage) return null
        
        val position = _state.value.currentTimePosition
        val timeline = _state.value.timelineImages
        
        // Find the image at the current position or the most recent one before it
        for (i in position downTo 0) {
            timeline[i]?.let { return it }
        }
        
        return null
    }
}
