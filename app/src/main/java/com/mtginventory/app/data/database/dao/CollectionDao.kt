package com.mtginventory.app.data.database.dao

import androidx.room.*
import com.mtginventory.app.data.database.entities.CollectionEntity
import com.mtginventory.app.data.database.entities.PriceHistoryEntity
import com.mtginventory.app.data.database.entities.ScanSessionEntity
import com.mtginventory.app.data.database.entities.MTGSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY updatedAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE type = :type ORDER BY name ASC")
    fun getCollectionsByType(type: String): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE name = :name AND type = :type")
    suspend fun getCollectionByName(name: String, type: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollectionById(collectionId: String)

    // Update collection stats
    @Query("UPDATE collections SET cardCount = :cardCount, totalValue = :totalValue, lastCalculated = :timestamp WHERE id = :collectionId")
    suspend fun updateCollectionStats(
        collectionId: String, 
        cardCount: Int, 
        totalValue: Double, 
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM collections WHERE type = :type")
    suspend fun getCollectionCountByType(type: String): Int
}

@Dao
interface PriceHistoryDao {

    @Query("SELECT * FROM price_history WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getPriceHistoryForCard(cardId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE cardId = :cardId AND source = :source ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPriceForCard(cardId: String, source: String): PriceHistoryEntity?

    @Query("SELECT * FROM price_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getPriceHistorySince(startTime: Long): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(priceHistory: PriceHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistories(priceHistories: List<PriceHistoryEntity>): List<Long>

    @Query("DELETE FROM price_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldPriceHistory(cutoffTime: Long)

    @Query("DELETE FROM price_history WHERE cardId = :cardId")
    suspend fun deletePriceHistoryForCard(cardId: String)

    // Statistics
    @Query("SELECT AVG(price) FROM price_history WHERE cardId = :cardId AND source = :source")
    suspend fun getAveragePriceForCard(cardId: String, source: String): Double?

    @Query("SELECT MAX(price) FROM price_history WHERE cardId = :cardId AND source = :source")
    suspend fun getMaxPriceForCard(cardId: String, source: String): Double?

    @Query("SELECT MIN(price) FROM price_history WHERE cardId = :cardId AND source = :source")
    suspend fun getMinPriceForCard(cardId: String, source: String): Double?
}

@Dao
interface ScanSessionDao {

    @Query("SELECT * FROM scan_sessions ORDER BY timestamp DESC")
    fun getAllScanSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getScanSessionsSince(startTime: Long): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    suspend fun getScanSessionById(sessionId: String): ScanSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanSession(session: ScanSessionEntity): Long

    @Update
    suspend fun updateScanSession(session: ScanSessionEntity)

    @Delete
    suspend fun deleteScanSession(session: ScanSessionEntity)

    @Query("DELETE FROM scan_sessions WHERE timestamp < :cutoffTime")
    suspend fun deleteOldScanSessions(cutoffTime: Long)

    // Statistics
    @Query("SELECT COUNT(*) FROM scan_sessions")
    suspend fun getTotalScanSessions(): Int

    @Query("SELECT SUM(cardsScanned) FROM scan_sessions")
    suspend fun getTotalCardsScanned(): Int

    @Query("SELECT SUM(cardsIdentified) FROM scan_sessions")
    suspend fun getTotalCardsIdentified(): Int

    @Query("SELECT AVG(CAST(cardsIdentified AS FLOAT) / CAST(cardsScanned AS FLOAT)) FROM scan_sessions WHERE cardsScanned > 0")
    suspend fun getAverageIdentificationRate(): Double?

    @Query("SELECT gridMode, COUNT(*) as count FROM scan_sessions GROUP BY gridMode")
    suspend fun getGridModeUsage(): Map<String, Int>
}

@Dao
interface MTGSetDao {

    @Query("SELECT * FROM mtg_sets ORDER BY releaseDate DESC")
    fun getAllSets(): Flow<List<MTGSetEntity>>

    @Query("SELECT * FROM mtg_sets WHERE code = :setCode")
    suspend fun getSetByCode(setCode: String): MTGSetEntity?

    @Query("SELECT * FROM mtg_sets WHERE scryfallId = :scryfallId")
    suspend fun getSetByScryfallId(scryfallId: String): MTGSetEntity?

    @Query("SELECT * FROM mtg_sets WHERE name LIKE '%' || :searchQuery || '%' OR code LIKE '%' || :searchQuery || '%'")
    fun searchSets(searchQuery: String): Flow<List<MTGSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: MTGSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<MTGSetEntity>): List<Long>

    @Update
    suspend fun updateSet(set: MTGSetEntity)

    @Delete
    suspend fun deleteSet(set: MTGSetEntity)

    @Query("DELETE FROM mtg_sets WHERE id = :setId")
    suspend fun deleteSetById(setId: String)

    @Query("SELECT COUNT(*) FROM mtg_sets")
    suspend fun getSetCount(): Int

    @Query("SELECT * FROM mtg_sets WHERE setType = :setType ORDER BY releaseDate DESC")
    fun getSetsByType(setType: String): Flow<List<MTGSetEntity>>
}