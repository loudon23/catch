package com.loudon23.acatch.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log // Log import 추가

object ThumbnailExtractor {

    suspend fun extractThumbnail(context: Context, videoUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val TAG = "ThumbnailExtractor" // TAG 추가
        val retriever = MediaMetadataRetriever()
        try {
            Log.d(TAG, "Attempting to extract thumbnail for URI: $videoUri")
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                Log.d(TAG, "Thumbnail extracted successfully for URI: $videoUri")
            } else {
                Log.w(TAG, "Failed to get frame at time 0 for URI: $videoUri (Bitmap is null)")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting thumbnail for URI: $videoUri", e)
            null
        } finally {
            try {
                retriever.release()
                Log.d(TAG, "MediaMetadataRetriever released for URI: $videoUri")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever for URI: $videoUri", e)
            }
        }
    }
}