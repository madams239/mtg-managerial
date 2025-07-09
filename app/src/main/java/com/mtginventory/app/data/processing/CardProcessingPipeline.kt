package com.mtginventory.app.data.processing

import android.graphics.Bitmap
import com.mtginventory.app.data.api.ScryfallRepository
import com.mtginventory.app.data.mlkit.CardTextInfo
import com.mtginventory.app.data.mlkit.GridProcessor
import com.mtginventory.app.data.mlkit.TextRecognitionService
import com.mtginventory.app.model.ScannedCard
import com.mtginventory.app.model.ScryfallCard
import com.mtginventory.app.ui.scanner.GridMode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardProcessingPipeline @Inject constructor(
    private val textRecognitionService: TextRecognitionService,
    private val scryfallRepository: ScryfallRepository
) {

    suspend fun processGridImage(
        bitmap: Bitmap,
        gridMode: GridMode,
        onProgress: (Float) -> Unit = {}
    ): Flow<ProcessingResult> = flow {
        try {
            emit(ProcessingResult.Started(gridMode.cardCount))
            onProgress(0.1f)

            // Step 1: Create grid regions with memory-efficient processing
            val regions = GridProcessor.createGridRegions(
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                rows = gridMode.rows,
                cols = gridMode.cols
            )

            val optimizedRegions = GridProcessor.optimizeRegionsForCards(regions)
            emit(ProcessingResult.GridDetected(optimizedRegions.size))
            onProgress(0.2f)
            
            // Early validation to avoid unnecessary processing
            if (optimizedRegions.isEmpty()) {
                emit(ProcessingResult.Error("No valid card regions detected"))
                return@flow
            }

            // Step 2: Extract text from each region
            val textResults = textRecognitionService.recognizeTextInRegions(bitmap, optimizedRegions)
            
            if (textResults.isFailure) {
                emit(ProcessingResult.Error("Text recognition failed: ${textResults.exceptionOrNull()?.message}"))
                return@flow
            }

            val regionTexts = textResults.getOrNull() ?: emptyList()
            emit(ProcessingResult.TextExtracted(regionTexts.size))
            onProgress(0.4f)

            // Step 3: Parse card information from text
            val cardInfoList = regionTexts.mapIndexed { index, regionResult ->
                val cardInfo = if (regionResult.success && regionResult.recognizedTexts.isNotEmpty()) {
                    textRecognitionService.parseCardInformation(regionResult.recognizedTexts)
                } else {
                    null
                }
                
                IndexedCardInfo(index, cardInfo)
            }

            emit(ProcessingResult.CardInfoParsed(cardInfoList.count { it.cardInfo != null }))
            onProgress(0.6f)

            // Step 4: Identify cards via Scryfall API with retry logic
            val identificationResults = identifyCardsInParallelWithRetry(cardInfoList) { progress ->
                onProgress(0.6f + (progress * 0.3f))
            }

            emit(ProcessingResult.CardsIdentified(identificationResults.count { it.scryfallCard != null }))
            onProgress(0.9f)

            // Step 5: Create final scanned cards
            val scannedCards = createScannedCards(identificationResults)
            
            emit(ProcessingResult.Completed(scannedCards))
            onProgress(1.0f)

        } catch (e: Exception) {
            emit(ProcessingResult.Error("Processing failed: ${e.message}"))
        }
    }

    private suspend fun identifyCardsInParallel(
        cardInfoList: List<IndexedCardInfo>,
        onProgress: (Float) -> Unit
    ): List<IdentificationResult> = coroutineScope {
        val results = mutableListOf<IdentificationResult>()
        var completed = 0

        val deferredResults = cardInfoList.map { indexedInfo ->
            async {
                val result = if (indexedInfo.cardInfo != null) {
                    val scryfallResult = scryfallRepository.identifyCard(indexedInfo.cardInfo)
                    IdentificationResult(
                        index = indexedInfo.index,
                        cardInfo = indexedInfo.cardInfo,
                        scryfallCard = scryfallResult.getOrNull(),
                        success = scryfallResult.isSuccess,
                        error = scryfallResult.exceptionOrNull()?.message
                    )
                } else {
                    IdentificationResult(
                        index = indexedInfo.index,
                        cardInfo = null,
                        scryfallCard = null,
                        success = false,
                        error = "No card info extracted"
                    )
                }

                completed++
                onProgress(completed.toFloat() / cardInfoList.size)
                result
            }
        }

        deferredResults.awaitAll()
    }

    private suspend fun identifyCardsInParallelWithRetry(
        cardInfoList: List<IndexedCardInfo>,
        onProgress: (Float) -> Unit,
        maxRetries: Int = 3
    ): List<IdentificationResult> = coroutineScope {
        val results = mutableListOf<IdentificationResult>()
        var completed = 0

        val deferredResults = cardInfoList.map { indexedInfo ->
            async {
                val result = if (indexedInfo.cardInfo != null) {
                    // Retry logic for API calls
                    var attempts = 0
                    var lastError: Exception? = null
                    var scryfallResult: Result<ScryfallCard?> = Result.failure(Exception("No attempts made"))

                    while (attempts < maxRetries) {
                        try {
                            scryfallResult = scryfallRepository.identifyCard(indexedInfo.cardInfo)
                            if (scryfallResult.isSuccess) {
                                break // Success, exit retry loop
                            }
                        } catch (e: Exception) {
                            lastError = e
                        }
                        attempts++
                        
                        // Exponential backoff for retries
                        if (attempts < maxRetries) {
                            kotlinx.coroutines.delay(100L * attempts)
                        }
                    }

                    IdentificationResult(
                        index = indexedInfo.index,
                        cardInfo = indexedInfo.cardInfo,
                        scryfallCard = scryfallResult.getOrNull(),
                        success = scryfallResult.isSuccess,
                        error = scryfallResult.exceptionOrNull()?.message ?: lastError?.message
                    )
                } else {
                    IdentificationResult(
                        index = indexedInfo.index,
                        cardInfo = null,
                        scryfallCard = null,
                        success = false,
                        error = "No card info extracted"
                    )
                }

                completed++
                onProgress(completed.toFloat() / cardInfoList.size)
                result
            }
        }

        deferredResults.awaitAll()
    }

    private fun createScannedCards(identificationResults: List<IdentificationResult>): List<ScannedCard> {
        return identificationResults.mapNotNull { result ->
            if (result.scryfallCard != null) {
                ScannedCard(
                    name = result.scryfallCard.name,
                    setName = result.scryfallCard.setName,
                    collectorNumber = result.scryfallCard.collectorNumber,
                    rarity = result.scryfallCard.rarity,
                    price = result.scryfallCard.displayPrice(),
                    scryfallCard = result.scryfallCard
                )
            } else if (result.cardInfo != null) {
                // Create a placeholder card for failed identifications
                ScannedCard(
                    name = result.cardInfo.cardName ?: "Unknown Card",
                    setName = "Unknown Set",
                    collectorNumber = result.cardInfo.collectorNumber ?: "???",
                    rarity = result.cardInfo.rarity ?: "unknown",
                    price = 0.0,
                    scryfallCard = null
                )
            } else {
                null
            }
        }
    }

    suspend fun processSingleCard(
        cardInfo: CardTextInfo
    ): Result<ScannedCard?> {
        return try {
            val scryfallResult = scryfallRepository.identifyCard(cardInfo)
            
            if (scryfallResult.isSuccess && scryfallResult.getOrNull() != null) {
                val scryfallCard = scryfallResult.getOrNull()!!
                val scannedCard = ScannedCard(
                    name = scryfallCard.name,
                    setName = scryfallCard.setName,
                    collectorNumber = scryfallCard.collectorNumber,
                    rarity = scryfallCard.rarity,
                    price = scryfallCard.displayPrice(),
                    scryfallCard = scryfallCard
                )
                Result.success(scannedCard)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cleanup() {
        textRecognitionService.cleanup()
    }
}

// Data classes for processing pipeline
data class IndexedCardInfo(
    val index: Int,
    val cardInfo: CardTextInfo?
)

data class IdentificationResult(
    val index: Int,
    val cardInfo: CardTextInfo?,
    val scryfallCard: ScryfallCard?,
    val success: Boolean,
    val error: String? = null
)

sealed class ProcessingResult {
    data class Started(val expectedCards: Int) : ProcessingResult()
    data class GridDetected(val regionsFound: Int) : ProcessingResult()
    data class TextExtracted(val regionsProcessed: Int) : ProcessingResult()
    data class CardInfoParsed(val cardsFound: Int) : ProcessingResult()
    data class CardsIdentified(val cardsIdentified: Int) : ProcessingResult()
    data class Completed(val scannedCards: List<ScannedCard>) : ProcessingResult()
    data class Error(val message: String) : ProcessingResult()
}

// Processing statistics
data class ProcessingStats(
    val totalRegions: Int,
    val textExtracted: Int,
    val cardsParsed: Int,
    val cardsIdentified: Int,
    val successRate: Float,
    val processingTime: Long
) {
    val textExtractionRate: Float
        get() = if (totalRegions > 0) textExtracted.toFloat() / totalRegions else 0f
    
    val identificationRate: Float
        get() = if (cardsParsed > 0) cardsIdentified.toFloat() / cardsParsed else 0f
}