package com.mtginventory.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.*

@Entity(
    tableName = "collections",
    indices = [
        Index(value = ["name"]),
        Index(value = ["type"]),
        Index(value = ["createdAt"])
    ]
)
data class CollectionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val name: String,
    val description: String? = null,
    val type: String, // "collection", "deck", "wishlist", "tradelist"
    
    // Deck specific
    val format: String? = null, // "standard", "modern", "commander", etc.
    val colors: String? = null, // JSON string of deck colors
    val isComplete: Boolean = false,
    
    // Metadata
    val imageUri: String? = null,
    val tags: String? = null, // JSON string
    
    // Stats (cached for performance)
    val cardCount: Int = 0,
    val totalValue: Double = 0.0,
    val lastCalculated: Long? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "price_history",
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["timestamp"]),
        Index(value = ["source"])
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val cardId: String,
    val price: Double,
    val source: String, // "scryfall", "tcgplayer", etc.
    val currency: String = "USD",
    val foil: Boolean = false,
    
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scan_sessions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["gridMode"])
    ]
)
data class ScanSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val gridMode: String, // "3x3", "4x3"
    val cardsScanned: Int = 0,
    val cardsIdentified: Int = 0,
    val totalValue: Double = 0.0,
    val duration: Long = 0, // milliseconds
    
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mtg_sets",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["name"]),
        Index(value = ["releaseDate"])
    ]
)
data class MTGSetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val scryfallId: String,
    val code: String,
    val name: String,
    val setType: String,
    val releaseDate: String? = null,
    val cardCount: Int = 0,
    val digital: Boolean = false,
    val iconUri: String? = null,
    
    val createdAt: Long = System.currentTimeMillis()
)