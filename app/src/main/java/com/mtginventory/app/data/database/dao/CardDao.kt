package com.mtginventory.app.data.database.dao

import androidx.room.*
import com.mtginventory.app.data.database.entities.CardEntity
import kotlinx.coroutines.flow.Flow

// Data classes for query results
data class RarityCount(
    val rarity: String,
    val count: Int
)

data class SetCount(
    val setCode: String,
    val count: Int
)

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE collectionId = :collectionId ORDER BY name ASC")
    fun getCardsByCollection(collectionId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY name ASC")
    fun getCardsByDeck(deckId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE name LIKE '%' || :searchQuery || '%' OR setName LIKE '%' || :searchQuery || '%'")
    fun searchCards(searchQuery: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE rarity = :rarity ORDER BY name ASC")
    fun getCardsByRarity(rarity: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE setCode = :setCode ORDER BY collectorNumber ASC")
    fun getCardsBySet(setCode: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: String): CardEntity?

    @Query("SELECT * FROM cards WHERE scryfallId = :scryfallId")
    suspend fun getCardByScryfallId(scryfallId: String): CardEntity?

    @Query("SELECT * FROM cards WHERE setCode = :setCode AND collectorNumber = :collectorNumber")
    suspend fun getCardByCollectorNumber(setCode: String, collectorNumber: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: CardEntity)

    @Update
    suspend fun updateCards(cards: List<CardEntity>)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: String)

    @Query("DELETE FROM cards WHERE collectionId = :collectionId")
    suspend fun deleteCardsByCollection(collectionId: String)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeck(deckId: String)

    // Statistics queries
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getTotalCardCount(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE collectionId = :collectionId")
    suspend fun getCardCountByCollection(collectionId: String): Int

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    suspend fun getCardCountByDeck(deckId: String): Int

    @Query("SELECT SUM(quantity * COALESCE(currentPrice, 0.0)) FROM cards")
    suspend fun getTotalCollectionValue(): Double

    @Query("SELECT SUM(quantity * COALESCE(currentPrice, 0.0)) FROM cards WHERE collectionId = :collectionId")
    suspend fun getCollectionValue(collectionId: String): Double

    @Query("SELECT SUM(quantity * COALESCE(currentPrice, 0.0)) FROM cards WHERE deckId = :deckId")
    suspend fun getDeckValue(deckId: String): Double

    @Query("SELECT rarity, COUNT(*) as count FROM cards GROUP BY rarity")
    suspend fun getRarityDistribution(): List<RarityCount>

    @Query("SELECT setCode, COUNT(*) as count FROM cards GROUP BY setCode ORDER BY count DESC LIMIT :limit")
    suspend fun getTopSets(limit: Int = 10): List<SetCount>

    // Update operations
    @Query("UPDATE cards SET quantity = :quantity, updatedAt = :timestamp WHERE id = :cardId")
    suspend fun updateCardQuantity(cardId: String, quantity: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cards SET currentPrice = :price, lastPriceUpdate = :timestamp, priceSource = :source WHERE id = :cardId")
    suspend fun updateCardPrice(cardId: String, price: Double, source: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cards SET collectionId = :collectionId, updatedAt = :timestamp WHERE id IN (:cardIds)")
    suspend fun moveCardsToCollection(cardIds: List<String>, collectionId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cards SET deckId = :deckId, updatedAt = :timestamp WHERE id IN (:cardIds)")
    suspend fun moveCardsToDeck(cardIds: List<String>, deckId: String?, timestamp: Long = System.currentTimeMillis())

    // Cleanup operations
    @Query("DELETE FROM cards WHERE updatedAt < :cutoffTime")
    suspend fun deleteOldCards(cutoffTime: Long)

    @Query("UPDATE cards SET currentPrice = NULL, lastPriceUpdate = NULL WHERE lastPriceUpdate < :cutoffTime")
    suspend fun clearOldPrices(cutoffTime: Long)
}