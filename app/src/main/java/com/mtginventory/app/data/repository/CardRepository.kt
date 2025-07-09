package com.mtginventory.app.data.repository

import com.mtginventory.app.data.api.ScryfallRepository
import com.mtginventory.app.data.database.dao.CardDao
import com.mtginventory.app.data.database.entities.CardEntity
import com.mtginventory.app.data.processing.CardProcessingPipeline
import com.mtginventory.app.model.ScannedCard
import com.mtginventory.app.model.ScryfallCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val scryfallRepository: ScryfallRepository,
    private val processingPipeline: CardProcessingPipeline
) {
    
    suspend fun saveCard(card: ScannedCard): Result<Unit> {
        return try {
            val cardEntity = card.toCardEntity()
            cardDao.insertCard(cardEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveCards(cards: List<ScannedCard>): Result<Unit> {
        return try {
            val cardEntities = cards.map { it.toCardEntity() }
            cardDao.insertCards(cardEntities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteCard(cardId: String): Result<Unit> {
        return try {
            cardDao.deleteCardById(cardId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getAllCards(): Flow<List<ScannedCard>> {
        return cardDao.getAllCards().map { entities ->
            entities.map { it.toScannedCard() }
        }
    }
    
    fun getCardsByCollection(collectionId: String): Flow<List<ScannedCard>> {
        return cardDao.getCardsByCollection(collectionId).map { entities ->
            entities.map { it.toScannedCard() }
        }
    }
    
    suspend fun searchCards(query: String): Flow<List<ScannedCard>> {
        return cardDao.searchCards(query).map { entities ->
            entities.map { it.toScannedCard() }
        }
    }
    
    suspend fun getCollectionValue(): Double {
        return cardDao.getTotalCollectionValue()
    }
    
    suspend fun getTotalCardCount(): Int {
        return cardDao.getTotalCardCount()
    }
    
    suspend fun getRarityDistribution(): Map<String, Int> {
        return cardDao.getRarityDistribution().associate { it.rarity to it.count }
    }
    
    suspend fun getTopSets(limit: Int = 10): Map<String, Int> {
        return cardDao.getTopSets(limit).associate { it.setCode to it.count }
    }
    
    suspend fun updateCardQuantity(cardId: String, quantity: Int): Result<Unit> {
        return try {
            cardDao.updateCardQuantity(cardId, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateCardPrice(cardId: String, price: Double, source: String): Result<Unit> {
        return try {
            cardDao.updateCardPrice(cardId, price, source)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Conversion functions
    private fun ScannedCard.toCardEntity(): CardEntity {
        return CardEntity(
            scryfallId = this.scryfallCard?.id ?: "",
            name = this.name,
            setCode = this.scryfallCard?.setCode ?: "",
            setName = this.setName,
            collectorNumber = this.collectorNumber,
            rarity = this.rarity,
            manaCost = this.scryfallCard?.manaCost,
            convertedManaCost = this.scryfallCard?.cmc?.toInt() ?: 0,
            typeLine = this.scryfallCard?.typeLine,
            oracleText = this.scryfallCard?.oracleText,
            power = this.scryfallCard?.power,
            toughness = this.scryfallCard?.toughness,
            colors = this.scryfallCard?.colors?.joinToString(","),
            colorIdentity = this.scryfallCard?.colorIdentity?.joinToString(","),
            artist = this.scryfallCard?.artist,
            flavorText = this.scryfallCard?.flavorText,
            imageUri = this.scryfallCard?.primaryImageUrl(),
            currentPrice = this.price,
            priceSource = "scryfall"
        )
    }
    
    private fun CardEntity.toScannedCard(): ScannedCard {
        return ScannedCard(
            id = this.id,
            name = this.name,
            setName = this.setName,
            collectorNumber = this.collectorNumber,
            rarity = this.rarity,
            price = this.currentPrice ?: 0.0,
            scryfallCard = null // Would need to reconstruct ScryfallCard if needed
        )
    }
}