package com.mtginventory.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.*

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["scryfallId"], unique = true),
        Index(value = ["setCode", "collectorNumber"], unique = true),
        Index(value = ["name"]),
        Index(value = ["rarity"]),
        Index(value = ["setCode"])
    ]
)
data class CardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Scryfall data
    val scryfallId: String,
    val name: String,
    val setCode: String,
    val setName: String,
    val collectorNumber: String,
    val rarity: String,
    
    // Card details
    val manaCost: String? = null,
    val convertedManaCost: Int = 0,
    val typeLine: String? = null,
    val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val colors: String? = null, // JSON string
    val colorIdentity: String? = null, // JSON string
    val keywords: String? = null, // JSON string
    
    // Metadata
    val artist: String? = null,
    val flavorText: String? = null,
    val imageUri: String? = null,
    val language: String = "en",
    val foil: Boolean = false,
    val promo: Boolean = false,
    val digital: Boolean = false,
    
    // Collection data
    val quantity: Int = 1,
    val condition: String = "NM", // NM, LP, MP, HP, DMG
    val collectionId: String? = null,
    val deckId: String? = null,
    
    // Pricing
    val lastPriceUpdate: Long? = null,
    val currentPrice: Double? = null,
    val priceSource: String? = null, // "scryfall", "tcgplayer", etc.
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)