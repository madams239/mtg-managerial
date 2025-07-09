package com.mtginventory.app.data.mlkit

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

@Singleton
class TextRecognitionService @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): Result<List<RecognizedText>> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            
            val recognizedTexts = visionText.textBlocks.flatMap { block ->
                block.lines.map { line ->
                    RecognizedText(
                        text = line.text,
                        boundingBox = line.boundingBox ?: Rect(),
                        confidence = line.confidence ?: 0f
                    )
                }
            }
            
            Result.success(recognizedTexts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recognizeTextInRegions(
        bitmap: Bitmap,
        regions: List<Rect>
    ): Result<List<RegionTextResult>> {
        return try {
            // Validate input parameters
            if (bitmap.isRecycled) {
                return Result.failure(IllegalArgumentException("Bitmap is recycled"))
            }
            
            if (regions.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val results = mutableListOf<RegionTextResult>()
            
            regions.forEachIndexed { index, region ->
                try {
                    val croppedBitmap = cropBitmap(bitmap, region)
                    if (croppedBitmap.isRecycled || croppedBitmap.width <= 0 || croppedBitmap.height <= 0) {
                        results.add(
                            RegionTextResult(
                                regionIndex = index,
                                region = region,
                                recognizedTexts = emptyList(),
                                success = false
                            )
                        )
                        return@forEachIndexed
                    }
                    
                    val textResult = recognizeText(croppedBitmap)
                    
                    // Clean up cropped bitmap if it's different from original
                    if (croppedBitmap != bitmap && !croppedBitmap.isRecycled) {
                        try {
                            croppedBitmap.recycle()
                        } catch (e: Exception) {
                            // Ignore recycling errors
                        }
                    }
                    
                    results.add(
                        RegionTextResult(
                            regionIndex = index,
                            region = region,
                            recognizedTexts = textResult.getOrElse { emptyList() },
                            success = textResult.isSuccess
                        )
                    )
                } catch (e: Exception) {
                    results.add(
                        RegionTextResult(
                            regionIndex = index,
                            region = region,
                            recognizedTexts = emptyList(),
                            success = false
                        )
                    )
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseCardInformation(recognizedTexts: List<RecognizedText>): CardTextInfo? {
        val allText = recognizedTexts.joinToString(" ") { it.text }
        
        return CardTextInfo(
            cardName = extractCardName(allText),
            collectorNumber = extractCollectorNumber(allText),
            setCode = extractSetCode(allText),
            rarity = extractRarity(allText),
            rawText = allText
        )
    }

    private fun cropBitmap(bitmap: Bitmap, region: Rect): Bitmap {
        return try {
            // Validate input
            if (bitmap.isRecycled) {
                return bitmap
            }
            
            val x = maxOf(0, region.left)
            val y = maxOf(0, region.top)
            val width = minOf(bitmap.width - x, region.width()).coerceAtLeast(1)
            val height = minOf(bitmap.height - y, region.height()).coerceAtLeast(1)
            
            // Ensure we're not trying to crop outside bitmap bounds
            if (x >= bitmap.width || y >= bitmap.height || width <= 0 || height <= 0) {
                return bitmap
            }
            
            Bitmap.createBitmap(bitmap, x, y, width, height)
        } catch (e: OutOfMemoryError) {
            // Return original bitmap if we can't crop due to memory issues
            bitmap
        } catch (e: Exception) {
            // Return original bitmap for any other issues
            bitmap
        }
    }

    private fun extractCardName(text: String): String? {
        // Remove collector numbers, set codes, and rarity indicators
        var cleanText = text
        
        // Remove collector numbers (4 digits or fraction format)
        cleanText = cleanText.replace(Regex("\\b\\d{4}\\b"), "")
        cleanText = cleanText.replace(Regex("\\b\\d{1,3}/\\d{1,3}\\b"), "")
        
        // Remove set codes (3 letters + optional language)
        cleanText = cleanText.replace(Regex("\\b[A-Z]{3}•[A-Z]{2}\\b"), "")
        cleanText = cleanText.replace(Regex("\\b[A-Z]{3}\\b"), "")
        
        // Remove rarity indicators
        cleanText = cleanText.replace(Regex("\\b[CURM]\\b"), "")
        
        // Clean up whitespace
        cleanText = cleanText.trim().replace(Regex("\\s+"), " ")
        
        return if (cleanText.length > 2) cleanText else null
    }

    private fun extractCollectorNumber(text: String): String? {
        // Modern format: 4 digits
        Regex("\\b(\\d{4})\\b").find(text)?.let { 
            return it.groupValues[1] 
        }
        
        // Legacy format: number/total
        Regex("\\b(\\d{1,3})/(\\d{1,3})\\b").find(text)?.let { 
            return it.groupValues[1] 
        }
        
        // Number followed by rarity
        Regex("\\b(\\d{1,4})\\s*[CURM]\\b").find(text)?.let { 
            return it.groupValues[1] 
        }
        
        return null
    }

    private fun extractSetCode(text: String): String? {
        // Modern format: 3-letter code with language
        Regex("\\b([A-Z]{3})•[A-Z]{2}\\b").find(text)?.let { 
            return it.groupValues[1] 
        }
        
        // Just 3-letter code
        Regex("\\b([A-Z]{3})\\b").find(text)?.let { 
            return it.groupValues[1] 
        }
        
        return null
    }

    private fun extractRarity(text: String): String? {
        return when {
            text.contains("C", ignoreCase = false) -> "common"
            text.contains("U", ignoreCase = false) -> "uncommon"
            text.contains("R", ignoreCase = false) -> "rare"
            text.contains("M", ignoreCase = false) -> "mythic"
            else -> null
        }
    }

    fun cleanup() {
        recognizer.close()
    }
}

data class RecognizedText(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float
)

data class RegionTextResult(
    val regionIndex: Int,
    val region: Rect,
    val recognizedTexts: List<RecognizedText>,
    val success: Boolean
)

data class CardTextInfo(
    val cardName: String?,
    val collectorNumber: String?,
    val setCode: String?,
    val rarity: String?,
    val rawText: String
)

// Grid processing utilities
object GridProcessor {
    
    fun createGridRegions(
        imageWidth: Int,
        imageHeight: Int,
        rows: Int,
        cols: Int,
        padding: Float = 0.05f
    ): List<Rect> {
        val regions = mutableListOf<Rect>()
        
        val paddingX = (imageWidth * padding).toInt()
        val paddingY = (imageHeight * padding).toInt()
        
        val usableWidth = imageWidth - (paddingX * 2)
        val usableHeight = imageHeight - (paddingY * 2)
        
        val cellWidth = usableWidth / cols
        val cellHeight = usableHeight / rows
        
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val left = paddingX + (col * cellWidth)
                val top = paddingY + (row * cellHeight)
                val right = left + cellWidth
                val bottom = top + cellHeight
                
                regions.add(Rect(left, top, right, bottom))
            }
        }
        
        return regions
    }
    
    fun optimizeRegionsForCards(
        originalRegions: List<Rect>,
        cardAspectRatio: Float = 0.715f // Standard MTG card ratio
    ): List<Rect> {
        return originalRegions.map { region ->
            val regionAspectRatio = region.width().toFloat() / region.height().toFloat()
            
            if (regionAspectRatio > cardAspectRatio) {
                // Region is too wide, adjust width
                val newWidth = (region.height() * cardAspectRatio).toInt()
                val widthDiff = region.width() - newWidth
                val left = region.left + (widthDiff / 2)
                val right = left + newWidth
                
                Rect(left, region.top, right, region.bottom)
            } else {
                // Region is too tall, adjust height
                val newHeight = (region.width() / cardAspectRatio).toInt()
                val heightDiff = region.height() - newHeight
                val top = region.top + (heightDiff / 2)
                val bottom = top + newHeight
                
                Rect(region.left, top, region.right, bottom)
            }
        }
    }
}