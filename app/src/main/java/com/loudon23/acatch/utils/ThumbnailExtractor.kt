package com.loudon23.acatch.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ThumbnailExtractor {

    private const val TAG = "ThumbnailExtractor"

    suspend fun extractThumbnail(context: Context, videoUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val fileName = "thumb_${videoUri.toString().hashCode()}.jpg"
        val cacheFile = File(context.cacheDir, fileName)

        if (cacheFile.exists()) {
            Log.d(TAG, "Loading thumbnail from cache for URI: $videoUri")
            return@withContext try {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading thumbnail from cache for URI: $videoUri", e)
                // If loading fails, proceed to extract it again.
                extractAndSaveThumbnail(context, videoUri, cacheFile)
            }
        }

        Log.d(TAG, "No cache found. Extracting new thumbnail for URI: $videoUri")
        extractAndSaveThumbnail(context, videoUri, cacheFile)
    }

    private fun extractAndSaveThumbnail(context: Context, videoUri: Uri, cacheFile: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            Log.d(TAG, "Attempting to extract thumbnail for URI: $videoUri")
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (bitmap != null) {
                Log.d(TAG, "Thumbnail extracted successfully for URI: $videoUri")
                saveBitmapToCache(bitmap, cacheFile)
            } else {
                Log.w(TAG, "Failed to get frame at time 0 for URI: $videoUri (Bitmap is null)")
            }
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting thumbnail for URI: $videoUri", e)
            return null
        } finally {
            try {
                retriever.release()
                Log.d(TAG, "MediaMetadataRetriever released for URI: $videoUri")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever for URI: $videoUri", e)
            }
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap, cacheFile: File) {
        try {
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                Log.d(TAG, "Thumbnail saved to cache: ${cacheFile.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving thumbnail to cache for file: ${cacheFile.absolutePath}", e)
        }
    }
}