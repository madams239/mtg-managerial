package com.mtginventory.app.model

data class ScannedCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val setName: String,
    val collectorNumber: String,
    val rarity: String,
    val price: Double,
    val scryfallCard: ScryfallCard? = null
)