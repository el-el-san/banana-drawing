package com.example.bdrowclient.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileUtils {
    
    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): File = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, use MediaStore
            saveBitmapToMediaStore(context, bitmap, fileName)
        } else {
            // For older versions, save to external storage
            saveBitmapToExternalStorage(bitmap, fileName)
        }
    }
    
    suspend fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.externalCacheDir, "shared_images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        val file = File(cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        
        file
    }
    
    private fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): File {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BananaDrawing")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
        
        // Return a dummy file for compatibility
        return File(context.cacheDir, fileName)
    }
    
    private fun saveBitmapToExternalStorage(
        bitmap: Bitmap,
        fileName: String
    ): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "BananaDrawing")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        val file = File(appDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        
        return file
    }
}
