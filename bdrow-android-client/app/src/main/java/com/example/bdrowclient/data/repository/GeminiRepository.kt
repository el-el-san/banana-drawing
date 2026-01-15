package com.example.bdrowclient.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.bdrowclient.data.api.GeminiApiClient
import com.example.bdrowclient.data.api.GeminiService
import com.example.bdrowclient.data.models.*
import com.example.bdrowclient.data.preferences.ApiKeyPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ProcessResult(
    val text: String?,
    val processedImage: Bitmap?,
    val debugInfo: String? = null
)

class GeminiRepository(private val context: Context) {
    
    private val apiKeyPreferences = ApiKeyPreferences(context)
    private val geminiService: GeminiService = GeminiApiClient.create()
    private val gson = Gson()
    
    companion object {
        private const val MODEL_TEXT = "gemini-2.5-flash-lite"
        private const val MODEL_IMAGE = "gemini-2.5-flash-lite"
        private const val MODEL_IMAGE_GENERATION = "gemini-3-pro-image-preview"
    }
    
    fun isAuthenticated(): Boolean {
        return apiKeyPreferences.hasApiKey() && apiKeyPreferences.isUsingApiKey()
    }
    
    fun logout() {
        // Clear API key
        apiKeyPreferences.clearApiKey()
    }
    
    suspend fun processMultipleImagesAndText(
        images: List<Bitmap>,
        text: String
    ): ProcessResult = withContext(Dispatchers.IO) {
        try {
            val apiKey = apiKeyPreferences.getApiKey()
            android.util.Log.d("GeminiRepo", "API Key: ${if (apiKey != null) "Present (${apiKey.take(10)}...)" else "Missing"}")
            
            if (apiKey == null) {
                return@withContext ProcessResult(
                    text = "API key is not configured",
                    processedImage = images.firstOrNull()
                )
            }
            
            if (images.isNotEmpty()) {
                // Multiple images + Text processing with two-stage enhancement
                processMultipleImagesWithTextEnhanced(images, text, apiKey)
            } else if (text.contains("generate image") || text.contains("create image") || text.contains("make image")) {
                // Image generation request
                generateImage(text, apiKey)
            } else {
                // Text-only processing
                processTextOnly(text, apiKey)
            }
        } catch (e: Exception) {
            ProcessResult(
                text = "Processing error: ${e.message}",
                processedImage = images.firstOrNull()
            )
        }
    }
    
    suspend fun processImageAndText(
        image: Bitmap?,
        text: String
    ): ProcessResult = withContext(Dispatchers.IO) {
        try {
            val apiKey = apiKeyPreferences.getApiKey()
            android.util.Log.d("GeminiRepo", "API Key: ${if (apiKey != null) "Present (${apiKey.take(10)}...)" else "Missing"}")
            
            if (apiKey == null) {
                return@withContext ProcessResult(
                    text = "API key is not configured",
                    processedImage = image
                )
            }
            
            if (image != null) {
                // Image + Text processing with two-stage enhancement
                processImageWithTextEnhanced(image, text, apiKey)
            } else if (text.contains("generate image") || text.contains("create image") || text.contains("make image")) {
                // Image generation request
                generateImage(text, apiKey)
            } else {
                // Text-only processing
                processTextOnly(text, apiKey)
            }
        } catch (e: Exception) {
            ProcessResult(
                text = "Processing error: ${e.message}",
                processedImage = image
            )
        }
    }

    private suspend fun processMultipleImagesWithTextEnhanced(
        images: List<Bitmap>,
        text: String,
        apiKey: String
    ): ProcessResult {
        val debugBuilder = StringBuilder()
        debugBuilder.appendLine("[Image Generation - Gemini 3 Pro]")
        debugBuilder.appendLine("Prompt: $text")

        return try {
            // Convert all bitmaps to base64
            val imageParts = images.map { bitmap ->
                Part(inlineData = InlineData(
                    mimeType = "image/jpeg",
                    data = bitmapToBase64(bitmap)
                ))
            }

            // Combine text and all images in parts
            val allParts = mutableListOf<Part>()
            allParts.add(Part(text = text))
            allParts.addAll(imageParts)
            
            val request = GeminiRequest(
                model = MODEL_IMAGE_GENERATION,
                contents = listOf(
                    Content(
                        role = "user",
                        parts = allParts
                    )
                ),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("IMAGE", "TEXT"),
                    temperature = 1.0f,
                    topK = 40,
                    topP = 0.95f,
                    maxOutputTokens = 8192,
                    imageConfig = ImageConfig(imageSize = "1K")
                )
            )
            
            debugBuilder.appendLine("\n[Request]")
            debugBuilder.appendLine("Model: $MODEL_IMAGE_GENERATION")
            debugBuilder.appendLine("Images: ${images.size}")
            images.forEachIndexed { index, img ->
                debugBuilder.appendLine("  Image ${index + 1}: ${img.width}x${img.height}")
            }
            
            android.util.Log.d("GeminiRepo", "Sending enhanced request with ${images.size} images")
            val response = geminiService.generateContent(MODEL_IMAGE_GENERATION, apiKey, request)
            android.util.Log.d("GeminiRepo", "Response received from API")
            
            if (response.candidates?.isNotEmpty() == true) {
                val candidate = response.candidates[0]
                var responseText: String? = null
                var generatedImage: Bitmap? = null
                
                candidate.content?.parts?.forEach { part ->
                    when {
                        part.text != null -> {
                            responseText = part.text
                            debugBuilder.appendLine("  Text: ${part.text.take(100)}...")
                        }
                        part.inlineData != null -> {
                            try {
                                val imageBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                                generatedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                debugBuilder.appendLine("  Generated Image: ${generatedImage?.width}x${generatedImage?.height}")
                            } catch (e: Exception) {
                                debugBuilder.appendLine("  Image decode error: ${e.message}")
                            }
                        }
                    }
                }
                
                val resultText = when {
                    generatedImage != null -> "Image generated successfully\nPrompt: $text"
                    responseText != null -> responseText
                    else -> "Processing completed"
                }
                
                ProcessResult(
                    text = resultText,
                    processedImage = generatedImage ?: images.firstOrNull(),
                    debugInfo = debugBuilder.toString()
                )
            } else {
                ProcessResult(
                    text = response.error?.message ?: "API response was empty",
                    processedImage = images.firstOrNull(),
                    debugInfo = debugBuilder.toString()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepo", "API call failed", e)
            ProcessResult(
                text = "API error: ${e.message}",
                processedImage = images.firstOrNull(),
                debugInfo = debugBuilder.toString() + "\n[Exception]\n${e.message}"
            )
        }
    }
    
    private suspend fun processImageWithTextEnhanced(
        image: Bitmap,
        text: String,
        apiKey: String
    ): ProcessResult {
        val images = listOf(image)
        return processMultipleImagesWithTextEnhanced(images, text, apiKey)
    }
    
    private suspend fun processMultipleImagesWithText(
        images: List<Bitmap>,
        text: String,
        apiKey: String
    ): ProcessResult {
        return try {
            // Convert all bitmaps to base64
            val imageParts = images.map { bitmap ->
                Part(inlineData = InlineData(
                    mimeType = "image/jpeg",
                    data = bitmapToBase64(bitmap)
                ))
            }
            
            // Combine text and all images in parts
            val allParts = mutableListOf<Part>()
            allParts.add(Part(text = text))
            allParts.addAll(imageParts)
            
            val request = GeminiRequest(
                model = MODEL_IMAGE,
                contents = listOf(
                    Content(
                        role = "user",
                        parts = allParts
                    )
                )
            )
            
            val debugBuilder = StringBuilder()
            debugBuilder.appendLine("[Request]")
            debugBuilder.appendLine("Model: $MODEL_IMAGE")
            debugBuilder.appendLine("Text: $text")
            debugBuilder.appendLine("Images: ${images.size}")
            images.forEachIndexed { index, img ->
                debugBuilder.appendLine("  Image ${index + 1}: ${img.width}x${img.height}")
            }
            
            android.util.Log.d("GeminiRepo", "Sending request with ${images.size} images")
            val response = geminiService.generateContent(MODEL_IMAGE, apiKey, request)
            android.util.Log.d("GeminiRepo", "Response received from API")
            
            val responseJson = gson.toJson(response)
            debugBuilder.appendLine("\n[Response]")
            debugBuilder.appendLine("Raw JSON: ${responseJson.take(500)}...")
            
            if (response.candidates?.isNotEmpty() == true) {
                val candidate = response.candidates[0]
                var responseText: String? = null
                var generatedImage: Bitmap? = null
                
                candidate.content?.parts?.forEach { part ->
                    when {
                        part.text != null -> {
                            responseText = part.text
                            debugBuilder.appendLine("  Text: ${part.text.take(100)}...")
                        }
                        part.inlineData != null -> {
                            try {
                                val imageBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                                generatedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                debugBuilder.appendLine("  Generated Image: ${generatedImage?.width}x${generatedImage?.height}")
                            } catch (e: Exception) {
                                debugBuilder.appendLine("  Image decode error: ${e.message}")
                            }
                        }
                    }
                }
                
                ProcessResult(
                    text = responseText ?: "Processing completed",
                    processedImage = generatedImage ?: images.firstOrNull(),
                    debugInfo = debugBuilder.toString()
                )
            } else {
                ProcessResult(
                    text = response.error?.message ?: "API response was empty",
                    processedImage = images.firstOrNull(),
                    debugInfo = debugBuilder.toString()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepo", "API call failed", e)
            ProcessResult(
                text = "API error: ${e.message}",
                processedImage = images.firstOrNull(),
                debugInfo = "[Exception]\n${e.message}"
            )
        }
    }
    
    private suspend fun processImageWithText(
        image: Bitmap,
        text: String,
        apiKey: String
    ): ProcessResult {
        return try {
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(image)
            
            val request = GeminiRequest(
                model = MODEL_IMAGE,
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(
                            Part(text = text),
                            Part(inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            ))
                        )
                    )
                )
            )
            
            val debugBuilder = StringBuilder()
            debugBuilder.appendLine("[Request]")
            debugBuilder.appendLine("Model: $MODEL_IMAGE")
            debugBuilder.appendLine("Text: $text")
            debugBuilder.appendLine("Has Image: true (${image.width}x${image.height})")
            
            android.util.Log.d("GeminiRepo", "Sending request to API with text: $text")
            val response = geminiService.generateContent(MODEL_IMAGE, apiKey, request)
            android.util.Log.d("GeminiRepo", "Response received from API")
            android.util.Log.d("GeminiRepo", "Response JSON: ${gson.toJson(response)}")
            
            val responseJson = gson.toJson(response)
            debugBuilder.appendLine("\n[Response]")
            debugBuilder.appendLine("Raw JSON: ${responseJson.take(500)}...")
            
            if (response.candidates?.isNotEmpty() == true) {
                val candidate = response.candidates[0]
                debugBuilder.appendLine("\nCandidates: ${response.candidates.size}")
                debugBuilder.appendLine("Finish Reason: ${candidate.finishReason}")
                
                android.util.Log.d("GeminiRepo", "Candidate content: ${gson.toJson(candidate.content)}")
                
                var responseText: String? = null
                var generatedImage: Bitmap? = null
                var partCount = 0
                
                candidate.content?.parts?.forEach { part ->
                    partCount++
                    debugBuilder.appendLine("\nPart $partCount:")
                    android.util.Log.d("GeminiRepo", "Part: text=${part.text}, hasInlineData=${part.inlineData != null}")
                    when {
                        part.text != null -> {
                            responseText = part.text
                            debugBuilder.appendLine("  Text: ${part.text.take(100)}...")
                        }
                        part.inlineData != null -> {
                            debugBuilder.appendLine("  InlineData: ${part.inlineData.mimeType}")
                            try {
                                val imageBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                                generatedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                debugBuilder.appendLine("  Image decoded: ${generatedImage?.width}x${generatedImage?.height}")
                                android.util.Log.d("GeminiRepo", "Generated image: ${generatedImage?.width}x${generatedImage?.height}")
                            } catch (e: Exception) {
                                debugBuilder.appendLine("  Image decode error: ${e.message}")
                                android.util.Log.e("GeminiRepo", "Failed to decode image", e)
                            }
                        }
                    }
                }
                
                debugBuilder.appendLine("\n[Result]")
                debugBuilder.appendLine("Has Text: ${responseText != null}")
                debugBuilder.appendLine("Has Generated Image: ${generatedImage != null}")
                
                // Set appropriate message when image is generated
                val resultText = when {
                    responseText != null -> responseText
                    generatedImage != null && generatedImage != image -> {
                        "Image generated successfully\n" +
                        "Prompt: $text\n" +
                        "Size: ${generatedImage!!.width}x${generatedImage!!.height}"
                    }
                    else -> "No response received"
                }
                
                ProcessResult(
                    text = resultText,
                    processedImage = generatedImage ?: image,
                    debugInfo = debugBuilder.toString()
                )
            } else {
                debugBuilder.appendLine("\n[Error]")
                debugBuilder.appendLine("No candidates in response")
                debugBuilder.appendLine("Error: ${response.error?.message}")
                
                ProcessResult(
                    text = response.error?.message ?: "API response was empty",
                    processedImage = image,
                    debugInfo = debugBuilder.toString()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepo", "API call failed", e)
            val debugInfo = "[Exception]\n${e.javaClass.simpleName}: ${e.message}\n\nStackTrace:\n${e.stackTrace.take(5).joinToString("\n")}"
            ProcessResult(
                text = "API error: ${e.message}",
                processedImage = image,
                debugInfo = debugInfo
            )
        }
    }
    
    private suspend fun generateImage(
        prompt: String,
        apiKey: String
    ): ProcessResult {
        val request = GeminiRequest(
            model = MODEL_IMAGE_GENERATION,
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseModalities = listOf("IMAGE", "TEXT"),
                temperature = 1.0f,
                topK = 40,
                topP = 0.95f,
                maxOutputTokens = 8192,
                imageConfig = ImageConfig(imageSize = "1K")
            )
        )

        try {
            android.util.Log.d("GeminiRepo", "Requesting image generation: $prompt")
            val response = geminiService.generateContent(MODEL_IMAGE_GENERATION, apiKey, request)
            android.util.Log.d("GeminiRepo", "Image generation response: ${gson.toJson(response)}")
            
            if (response.candidates?.isNotEmpty() == true) {
                val content = response.candidates[0].content
                var generatedImage: Bitmap? = null
                var responseText: String? = null
                
                content?.parts?.forEach { part ->
                    android.util.Log.d("GeminiRepo", "Image gen part: text=${part.text}, hasInlineData=${part.inlineData != null}")
                    when {
                        part.inlineData != null -> {
                            try {
                                // Decode base64 image
                                val imageBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                                generatedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                android.util.Log.d("GeminiRepo", "Generated image decoded: ${generatedImage?.width}x${generatedImage?.height}")
                            } catch (e: Exception) {
                                android.util.Log.e("GeminiRepo", "Failed to decode generated image", e)
                            }
                        }
                        part.text != null -> {
                            responseText = part.text
                        }
                    }
                }
                
                return ProcessResult(
                    text = responseText ?: "Image generated successfully",
                    processedImage = generatedImage
                )
            } else {
                return ProcessResult(
                    text = response.error?.message ?: "Image generation failed",
                    processedImage = null
                )
            }
        } catch (e: Exception) {
            // Fallback to text generation if image generation fails
            return processTextOnly("Image generation error: $prompt\n${e.message}", apiKey)
        }
    }
    
    private suspend fun processTextOnly(
        text: String,
        apiKey: String
    ): ProcessResult {
        return try {
            val request = GeminiRequest(
                model = MODEL_TEXT,
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = text))
                    )
                )
            )
            
            android.util.Log.d("GeminiRepo", "Sending text-only request: $text")
            val response = geminiService.generateContent(MODEL_TEXT, apiKey, request)
            android.util.Log.d("GeminiRepo", "Response received from API")
            android.util.Log.d("GeminiRepo", "Response JSON: ${gson.toJson(response)}")
            
            if (response.candidates?.isNotEmpty() == true) {
                val candidate = response.candidates[0]
                android.util.Log.d("GeminiRepo", "Candidate content: ${gson.toJson(candidate.content)}")
                
                var responseText: String? = null
                var generatedImage: Bitmap? = null
                
                candidate.content?.parts?.forEach { part ->
                    android.util.Log.d("GeminiRepo", "Part: text=${part.text}, hasInlineData=${part.inlineData != null}")
                    when {
                        part.text != null -> responseText = part.text
                        part.inlineData != null -> {
                            try {
                                val imageBytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                                generatedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                android.util.Log.d("GeminiRepo", "Generated image: ${generatedImage?.width}x${generatedImage?.height}")
                            } catch (e: Exception) {
                                android.util.Log.e("GeminiRepo", "Failed to decode image", e)
                            }
                        }
                    }
                }
                
                // Return text or image generation result
                val resultText = when {
                    responseText != null -> responseText
                    generatedImage != null -> {
                        "Image generated successfully\n" +
                        "Prompt: $text\n" +
                        "Size: ${generatedImage!!.width}x${generatedImage!!.height}"
                    }
                    else -> "No response received"
                }
                
                ProcessResult(
                    text = resultText,
                    processedImage = generatedImage
                )
            } else {
                ProcessResult(
                    text = response.error?.message ?: "API response was empty",
                    processedImage = null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepo", "API call failed", e)
            ProcessResult(
                text = "API error: ${e.message}",
                processedImage = null
            )
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        android.util.Log.d("GeminiRepo", "Converted bitmap to base64: size=${imageBytes.size} bytes, base64 length=${base64.length}")
        return base64
    }
    
    suspend fun getVersion(): String {
        return "Gemini API v1.0"
    }
}
