package com.mtginventory.app

import android.graphics.Bitmap
import android.graphics.Rect
import com.mtginventory.app.data.api.ScryfallRepository
import com.mtginventory.app.data.mlkit.CardTextInfo
import com.mtginventory.app.data.mlkit.TextRecognitionService
import com.mtginventory.app.data.mlkit.RegionTextResult
import com.mtginventory.app.data.mlkit.RecognizedText
import com.mtginventory.app.data.processing.CardProcessingPipeline
import com.mtginventory.app.data.processing.ProcessingResult
import com.mtginventory.app.model.ScryfallCard
import com.mtginventory.app.model.Prices
import com.mtginventory.app.ui.scanner.GridMode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class CardProcessingPipelineTest {

    @Mock
    private lateinit var textRecognitionService: TextRecognitionService

    @Mock
    private lateinit var scryfallRepository: ScryfallRepository

    @Mock
    private lateinit var mockBitmap: Bitmap

    private lateinit var cardProcessingPipeline: CardProcessingPipeline

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        cardProcessingPipeline = CardProcessingPipeline(
            textRecognitionService,
            scryfallRepository
        )

        // Mock bitmap properties
        whenever(mockBitmap.width) doReturn 800
        whenever(mockBitmap.height) doReturn 600
        whenever(mockBitmap.isRecycled) doReturn false
    }

    @Test
    fun `processGridImage should handle successful 3x3 grid processing`() = runBlocking {
        // Mock text recognition results
        val mockRegionResults = listOf(
            createMockRegionResult(0, "Lightning Bolt DMU•EN 123 C"),
            createMockRegionResult(1, "Counterspell DMU•EN 456 U"),
            createMockRegionResult(2, "Black Lotus VIN•EN 1 R")
        )
        
        whenever(textRecognitionService.recognizeTextInRegions(any(), any()))
            .thenReturn(Result.success(mockRegionResults))

        // Mock card info parsing
        val cardInfos = listOf(
            CardTextInfo("Lightning Bolt", "123", "DMU", "common", "Lightning Bolt DMU•EN 123 C"),
            CardTextInfo("Counterspell", "456", "DMU", "uncommon", "Counterspell DMU•EN 456 U"),
            CardTextInfo("Black Lotus", "1", "VIN", "rare", "Black Lotus VIN•EN 1 R")
        )
        
        cardInfos.forEachIndexed { index, cardInfo ->
            whenever(textRecognitionService.parseCardInformation(mockRegionResults[index].recognizedTexts))
                .thenReturn(cardInfo)
        }

        // Mock Scryfall API responses
        val mockCards = listOf(
            createMockScryfallCard("Lightning Bolt", "DMU", "123", "common", 0.25),
            createMockScryfallCard("Counterspell", "DMU", "456", "uncommon", 0.50),
            createMockScryfallCard("Black Lotus", "VIN", "1", "rare", 1000.0)
        )
        
        cardInfos.forEachIndexed { index, cardInfo ->
            whenever(scryfallRepository.identifyCard(cardInfo))
                .thenReturn(Result.success(mockCards[index]))
        }

        // Execute the pipeline
        val results = cardProcessingPipeline.processGridImage(
            bitmap = mockBitmap,
            gridMode = GridMode.GRID_3X3
        ).toList()

        // Verify results
        assert(results.isNotEmpty())
        
        val completedResult = results.filterIsInstance<ProcessingResult.Completed>().firstOrNull()
        assert(completedResult != null)
        assert(completedResult!!.scannedCards.size == 3)
        
        val scannedCards = completedResult.scannedCards
        assert(scannedCards[0].name == "Lightning Bolt")
        assert(scannedCards[1].name == "Counterspell")
        assert(scannedCards[2].name == "Black Lotus")
        
        assert(scannedCards[0].price == 0.25)
        assert(scannedCards[1].price == 0.50)
        assert(scannedCards[2].price == 1000.0)
    }

    @Test
    fun `processGridImage should handle partial failures gracefully`() = runBlocking {
        // Mock text recognition with some failures
        val mockRegionResults = listOf(
            createMockRegionResult(0, "Lightning Bolt DMU•EN 123 C"),
            createMockRegionResult(1, "", success = false), // Failed text recognition
            createMockRegionResult(2, "Invalid text content")
        )
        
        whenever(textRecognitionService.recognizeTextInRegions(any(), any()))
            .thenReturn(Result.success(mockRegionResults))

        // Mock parsing results
        whenever(textRecognitionService.parseCardInformation(mockRegionResults[0].recognizedTexts))
            .thenReturn(CardTextInfo("Lightning Bolt", "123", "DMU", "common", "Lightning Bolt DMU•EN 123 C"))
        whenever(textRecognitionService.parseCardInformation(mockRegionResults[1].recognizedTexts))
            .thenReturn(null) // Failed parsing
        whenever(textRecognitionService.parseCardInformation(mockRegionResults[2].recognizedTexts))
            .thenReturn(null) // Failed parsing

        // Mock successful identification for the first card only
        whenever(scryfallRepository.identifyCard(any()))
            .thenReturn(Result.success(createMockScryfallCard("Lightning Bolt", "DMU", "123", "common", 0.25)))

        // Execute the pipeline
        val results = cardProcessingPipeline.processGridImage(
            bitmap = mockBitmap,
            gridMode = GridMode.GRID_3X3
        ).toList()

        // Verify results - should get at least one successful card
        val completedResult = results.filterIsInstance<ProcessingResult.Completed>().firstOrNull()
        assert(completedResult != null)
        assert(completedResult!!.scannedCards.isNotEmpty())
        assert(completedResult.scannedCards.any { it.name == "Lightning Bolt" })
    }

    @Test
    fun `processGridImage should handle complete API failure`() = runBlocking {
        // Mock text recognition success
        val mockRegionResults = listOf(
            createMockRegionResult(0, "Lightning Bolt DMU•EN 123 C")
        )
        
        whenever(textRecognitionService.recognizeTextInRegions(any(), any()))
            .thenReturn(Result.success(mockRegionResults))

        whenever(textRecognitionService.parseCardInformation(any()))
            .thenReturn(CardTextInfo("Lightning Bolt", "123", "DMU", "common", "Lightning Bolt DMU•EN 123 C"))

        // Mock API failure
        whenever(scryfallRepository.identifyCard(any()))
            .thenReturn(Result.failure(Exception("Network error")))

        // Execute the pipeline
        val results = cardProcessingPipeline.processGridImage(
            bitmap = mockBitmap,
            gridMode = GridMode.GRID_3X3
        ).toList()

        // Should still complete but with placeholder cards or empty results
        val completedResult = results.filterIsInstance<ProcessingResult.Completed>().firstOrNull()
        assert(completedResult != null)
        // Pipeline should handle gracefully - either with placeholder cards or empty list
    }

    private fun createMockRegionResult(index: Int, text: String, success: Boolean = true): RegionTextResult {
        val recognizedTexts = if (success && text.isNotEmpty()) {
            listOf(RecognizedText(text, Rect(0, 0, 100, 100), 0.9f))
        } else {
            emptyList()
        }
        
        return RegionTextResult(
            regionIndex = index,
            region = Rect(index * 100, 0, (index + 1) * 100, 100),
            recognizedTexts = recognizedTexts,
            success = success
        )
    }

    private fun createMockScryfallCard(
        name: String,
        setCode: String,
        collectorNumber: String,
        rarity: String,
        price: Double
    ): ScryfallCard {
        return ScryfallCard(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            uri = "https://api.scryfall.com/cards/$setCode/$collectorNumber",
            scryfallUri = "https://scryfall.com/card/$setCode/$collectorNumber",
            layout = "normal",
            manaCost = "{R}",
            cmc = 1.0,
            typeLine = "Instant",
            oracleText = "Test card",
            power = null,
            toughness = null,
            colors = listOf("R"),
            colorIdentity = listOf("R"),
            keywords = emptyList(),
            rarity = rarity,
            flavorText = null,
            artist = "Test Artist",
            illustrationId = null,
            borderColor = "black",
            frame = "2015",
            fullArt = false,
            textless = false,
            booster = true,
            storySpotlight = false,
            promo = false,
            variation = false,
            setCode = setCode,
            setName = "Test Set",
            setType = "expansion",
            setUri = "https://api.scryfall.com/sets/$setCode",
            setSearchUri = "https://api.scryfall.com/cards/search?order=set&q=e%3A$setCode",
            scryfallSetUri = "https://scryfall.com/sets/$setCode",
            releasedAt = "2023-01-01",
            cardBackId = null,
            collectorNumber = collectorNumber,
            digital = false,
            reprint = false,
            lang = "en",
            mtgoId = null,
            mtgoFoilId = null,
            tcgplayerId = null,
            cardmarketId = null,
            imageUris = null,
            cardFaces = null,
            prices = Prices(
                usd = price.toString(),
                usdFoil = null,
                usdEtched = null,
                eur = null,
                eurFoil = null,
                tix = null
            ),
            purchaseUris = null,
            relatedUris = null,
            legalities = null
        )
    }
}