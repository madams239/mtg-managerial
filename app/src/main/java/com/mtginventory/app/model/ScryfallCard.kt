package com.mtginventory.app.model

import com.google.gson.annotations.SerializedName

data class ScryfallCard(
    val id: String,
    val name: String,
    val uri: String,
    @SerializedName("scryfall_uri")
    val scryfallUri: String,
    val layout: String,
    @SerializedName("mana_cost")
    val manaCost: String?,
    val cmc: Double,
    @SerializedName("type_line")
    val typeLine: String,
    @SerializedName("oracle_text")
    val oracleText: String?,
    val power: String?,
    val toughness: String?,
    val colors: List<String>?,
    @SerializedName("color_identity")
    val colorIdentity: List<String>?,
    val keywords: List<String>?,
    val rarity: String,
    @SerializedName("flavor_text")
    val flavorText: String?,
    val artist: String?,
    @SerializedName("illustration_id")
    val illustrationId: String?,
    @SerializedName("border_color")
    val borderColor: String,
    val frame: String,
    @SerializedName("full_art")
    val fullArt: Boolean,
    val textless: Boolean,
    val booster: Boolean,
    @SerializedName("story_spotlight")
    val storySpotlight: Boolean,
    val promo: Boolean,
    val variation: Boolean,
    @SerializedName("set")
    val setCode: String,
    @SerializedName("set_name")
    val setName: String,
    @SerializedName("set_type")
    val setType: String,
    @SerializedName("set_uri")
    val setUri: String,
    @SerializedName("set_search_uri")
    val setSearchUri: String,
    @SerializedName("scryfall_set_uri")
    val scryfallSetUri: String,
    @SerializedName("released_at")
    val releasedAt: String,
    @SerializedName("card_back_id")
    val cardBackId: String?,
    @SerializedName("collector_number")
    val collectorNumber: String,
    val digital: Boolean,
    val reprint: Boolean,
    val lang: String,
    @SerializedName("mtgo_id")
    val mtgoId: Int?,
    @SerializedName("mtgo_foil_id")
    val mtgoFoilId: Int?,
    @SerializedName("tcgplayer_id")
    val tcgplayerId: Int?,
    @SerializedName("cardmarket_id")
    val cardmarketId: Int?,
    @SerializedName("image_uris")
    val imageUris: ImageUris?,
    @SerializedName("card_faces")
    val cardFaces: List<CardFace>?,
    val prices: Prices?,
    @SerializedName("purchase_uris")
    val purchaseUris: PurchaseUris?,
    @SerializedName("related_uris")
    val relatedUris: RelatedUris?,
    val legalities: Legalities?
)

data class ImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    val png: String?,
    @SerializedName("art_crop")
    val artCrop: String?,
    @SerializedName("border_crop")
    val borderCrop: String?
)

data class CardFace(
    val name: String,
    @SerializedName("mana_cost")
    val manaCost: String?,
    @SerializedName("type_line")
    val typeLine: String,
    @SerializedName("oracle_text")
    val oracleText: String?,
    val power: String?,
    val toughness: String?,
    val colors: List<String>?,
    @SerializedName("color_indicator")
    val colorIndicator: List<String>?,
    val artist: String?,
    @SerializedName("illustration_id")
    val illustrationId: String?,
    @SerializedName("image_uris")
    val imageUris: ImageUris?,
    @SerializedName("flavor_text")
    val flavorText: String?
)

data class Prices(
    val usd: String?,
    @SerializedName("usd_foil")
    val usdFoil: String?,
    @SerializedName("usd_etched")
    val usdEtched: String?,
    val eur: String?,
    @SerializedName("eur_foil")
    val eurFoil: String?,
    val tix: String?
)

data class PurchaseUris(
    val tcgplayer: String?,
    val cardmarket: String?,
    val cardhoarder: String?
)

data class RelatedUris(
    val gatherer: String?,
    @SerializedName("tcgplayer_infinite_articles")
    val tcgplayerInfiniteArticles: String?,
    @SerializedName("tcgplayer_infinite_decks")
    val tcgplayerInfiniteDecks: String?,
    val edhrec: String?
)

data class Legalities(
    val standard: String?,
    val future: String?,
    val historic: String?,
    val gladiator: String?,
    val pioneer: String?,
    val explorer: String?,
    val modern: String?,
    val legacy: String?,
    val pauper: String?,
    val vintage: String?,
    val penny: String?,
    val commander: String?,
    val brawl: String?,
    val historicbrawl: String?,
    val alchemy: String?,
    val paupercommander: String?,
    val duel: String?,
    val oldschool: String?,
    val premodern: String?
)

data class ScryfallSearchResult(
    val `object`: String,
    @SerializedName("total_cards")
    val totalCards: Int,
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("next_page")
    val nextPage: String?,
    val data: List<ScryfallCard>
)

// Extension functions
fun ScryfallCard.primaryImageUrl(): String? {
    return imageUris?.normal ?: imageUris?.large ?: imageUris?.png
}

fun ScryfallCard.displayPrice(): Double {
    return prices?.usd?.toDoubleOrNull() ?: 0.0
}

fun ScryfallCard.colorString(): String {
    return colors?.joinToString("") ?: "C"
}

fun ScryfallCard.isLegal(format: String): Boolean {
    return when (format.lowercase()) {
        "standard" -> legalities?.standard == "legal"
        "modern" -> legalities?.modern == "legal"
        "legacy" -> legalities?.legacy == "legal"
        "vintage" -> legalities?.vintage == "legal"
        "commander" -> legalities?.commander == "legal"
        "pioneer" -> legalities?.pioneer == "legal"
        "pauper" -> legalities?.pauper == "legal"
        else -> false
    }
}