package com.ridesync.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * High-performance image loading and bitmap caching utility for Rider & Bike profile photos.
 * Ensures zero-lag rendering inside 3D Google Maps MarkerComposables and UI avatars.
 */
object ImageLoaderHelper {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8th of available heap memory for bitmap cache

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun loadBitmap(context: Context, source: String, targetSize: Int = 256): Bitmap? {
        if (source.isBlank()) return null

        // Check in-memory cache first
        val cached = bitmapCache.get(source)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = when {
                    // Base64 Data URL
                    source.startsWith("data:image") -> {
                        val base64Data = source.substringAfter("base64,")
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }

                    // Content URI (from Image Picker / Gallery)
                    source.startsWith("content://") || source.startsWith("file://") -> {
                        val uri = Uri.parse(source)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val imgSource = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(imgSource) { decoder, _, _ ->
                                decoder.setTargetSampleSize(1)
                                decoder.isMutableRequired = false
                            }
                        } else {
                            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                            val bmp = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            bmp
                        }
                    }

                    // Plain File Path
                    else -> {
                        BitmapFactory.decodeFile(source)
                    }
                }

                if (bitmap != null) {
                    val scaledBitmap = scaleAndCropCenter(bitmap, targetSize, targetSize)
                    bitmapCache.put(source, scaledBitmap)
                    scaledBitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun scaleAndCropCenter(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = source.width
        val height = source.height
        val targetAspectRatio = targetWidth.toFloat() / targetHeight
        val sourceAspectRatio = width.toFloat() / height

        val scale = if (sourceAspectRatio > targetAspectRatio) {
            targetHeight.toFloat() / height
        } else {
            targetWidth.toFloat() / width
        }

        val scaledWidth = (scale * width).toInt()
        val scaledHeight = (scale * height).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        val x = (scaledWidth - targetWidth) / 2
        val y = (scaledHeight - targetHeight) / 2

        val finalCrop = Bitmap.createBitmap(scaledBitmap, x.coerceAtLeast(0), y.coerceAtLeast(0), targetWidth.coerceAtMost(scaledWidth), targetHeight.coerceAtMost(scaledHeight))
        if (scaledBitmap != source && scaledBitmap != finalCrop) {
            scaledBitmap.recycle()
        }
        return finalCrop
    }
}

/**
 * Composable remember helper that loads and caches a bitmap from any URI or path asynchronously.
 */
@Composable
fun rememberRiderAvatarBitmap(photoUrl: String): State<Bitmap?> {
    val context = LocalContext.current
    val bitmapState = remember(photoUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUrl) {
        if (photoUrl.isNotBlank()) {
            bitmapState.value = ImageLoaderHelper.loadBitmap(context, photoUrl)
        } else {
            bitmapState.value = null
        }
    }

    return bitmapState
}
