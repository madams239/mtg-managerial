package com.mtginventory.app.data.api

import com.mtginventory.app.data.mlkit.CardTextInfo
import com.mtginventory.app.model.ScryfallCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScryfallRepository @Inject constructor(
    private val apiService: ScryfallApiService
) {
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    suspend fun identifyCard(cardInfo: CardTextInfo): Result<ScryfallCard?> {
        return try {
            // Try collector number + set code first (most accurate)
            if (!cardInfo.setCode.isNullOrBlank() && !cardInfo.collectorNumber.isNullOrBlank()) {
                getCardByCollectorNumber(cardInfo.setCode, cardInfo.collectorNumber)
                    .getOrNull()?.let { return Result.success(it) }
            }

            // Fallback to name search
            if (!cardInfo.cardName.isNullOrBlank()) {
                searchCardByName(cardInfo.cardName).getOrNull()?.let { 
                    return Result.success(it) 
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun identifyCards(cardInfoList: List<CardTextInfo>): Result<List<ScryfallCard?>> {
        return try {
            val results = mutableListOf<ScryfallCard?>()
            
            cardInfoList.forEach { cardInfo ->
                val result = identifyCard(cardInfo)
                results.add(result.getOrNull())
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun searchCardByName(name: String): Result<ScryfallCard?> {
        return withRateLimit {
            try {
                val query = "!\"$name\""
                val response = apiService.searchCards(query)
                
                if (response.isSuccessful) {
                    val searchResult = response.body()
                    val card = searchResult?.data?.firstOrNull()
                    Result.success(card)
                } else {
                    Result.failure(ApiException("Search failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun getCardByCollectorNumber(
        setCode: String, 
        collectorNumber: String
    ): Result<ScryfallCard?> {
        return withRateLimit {
            try {
                val response = apiService.getCardByCollectorNumber(setCode, collectorNumber)
                
                if (response.isSuccessful) {
                    Result.success(response.body())
                } else {
                    Result.failure(ApiException("Card not found: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchCards(query: String): Result<List<ScryfallCard>> {
        return withRateLimit {
            try {
                val response = apiService.searchCards(query)
                
                if (response.isSuccessful) {
                    val searchResult = response.body()
                    Result.success(searchResult?.data ?: emptyList())
                } else {
                    Result.failure(ApiException("Search failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getRandomCard(): Result<ScryfallCard> {
        return withRateLimit {
            try {
                val response = apiService.getRandomCard()
                
                if (response.isSuccessful) {
                    val card = response.body()
                    if (card != null) {
                        Result.success(card)
                    } else {
                        Result.failure(ApiException("No card returned"))
                    }
                } else {
                    Result.failure(ApiException("Random card failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun <T> withRateLimit(block: suspend () -> T): T {
        return rateLimitMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRequest = currentTime - lastRequestTime
            
            if (timeSinceLastRequest < ScryfallApiService.RATE_LIMIT_MS) {
                val delayTime = ScryfallApiService.RATE_LIMIT_MS - timeSinceLastRequest
                delay(delayTime)
            }
            
            lastRequestTime = System.currentTimeMillis()
            block()
        }
    }
}

class ApiException(message: String) : Exception(message)

// Utility functions for building search queries
object ScryfallQueryBuilder {
    
    fun exactName(name: String): String = "!\"$name\""
    
    fun nameAndSet(name: String, setCode: String): String = 
        "!\"$name\" set:$setCode"
    
    fun collectorNumberAndSet(collectorNumber: String, setCode: String): String = 
        "cn:$collectorNumber set:$setCode"
    
    fun rarity(rarity: String): String = "rarity:$rarity"
    
    fun colors(colors: List<String>): String = 
        "c:${colors.joinToString("")}"
    
    fun manaValue(value: Int): String = "mv:$value"
    
    fun type(type: String): String = "t:$type"
    
    fun format(format: String): String = "f:$format"
    
    fun priceRange(min: Double, max: Double): String = 
        "usd>=$min usd<=$max"
    
    fun combine(vararg queries: String): String = 
        queries.filter { it.isNotBlank() }.joinToString(" ")
}