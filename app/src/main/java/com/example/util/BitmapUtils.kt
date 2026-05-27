package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val ratio = width.toFloat() / height.toFloat()
    
    val (newWidth, newHeight) = if (width > height) {
        if (width > maxSize) {
            Pair(maxSize, (maxSize / ratio).toInt())
        } else {
            Pair(width, height)
        }
    } else {
        if (height > maxSize) {
            Pair((maxSize * ratio).toInt(), maxSize)
        } else {
            Pair(width, height)
        }
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

fun addWatermarkToBitmap(bitmap: Bitmap, location: String, dateStr: String): Bitmap {
    try {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = (bitmap.height / 22).toFloat().coerceAtLeast(16f)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }
        
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(120, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        
        val textHeight = paint.textSize
        val padding = textHeight / 3
        val bannerHeight = textHeight * 2.2f
        
        canvas.drawRect(
            0f,
            bitmap.height - bannerHeight,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            bgPaint
        )
        
        val xPos = padding
        val yPosLine1 = bitmap.height - bannerHeight + textHeight + padding
        val yPosLine2 = yPosLine1 + textHeight + padding / 2
        
        canvas.drawText("📍 $location", xPos, yPosLine1, paint)
        canvas.drawText("📅 $dateStr | IT Hub Secured Verified", xPos, yPosLine2, paint)
        
        return result
    } catch (e: Exception) {
        return bitmap
    }
}

fun compressAndWatermarkBitmap(
    bitmap: Bitmap,
    location: String,
    dateStr: String,
    maxSize: Int = 600,
    quality: Int = 30
): String {
    val resized = resizeBitmap(bitmap, maxSize)
    val watermarked = addWatermarkToBitmap(resized, location, dateStr)
    val outputStream = ByteArrayOutputStream()
    watermarked.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val bytes = outputStream.toByteArray()
    val base64Str = Base64.encodeToString(bytes, Base64.DEFAULT)
    return "data:image/jpeg;base64,$base64Str"
}
