package com.mtginventory.app.data.api

import com.mtginventory.app.model.ScryfallCard
import com.mtginventory.app.model.ScryfallSearchResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ScryfallApiService {

    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): Response<ScryfallSearchResult>

    @GET("cards/{set}/{collector_number}")
    suspend fun getCardByCollectorNumber(
        @Path("set") setCode: String,
        @Path("collector_number") collectorNumber: String
    ): Response<ScryfallCard>

    @GET("cards/{id}")
    suspend fun getCardById(
        @Path("id") scryfallId: String
    ): Response<ScryfallCard>

    @GET("cards/random")
    suspend fun getRandomCard(): Response<ScryfallCard>

    @GET("sets")
    suspend fun getAllSets(): Response<List<Any>> // Simplified for now

    @GET("sets/{code}")
    suspend fun getSet(
        @Path("code") setCode: String
    ): Response<Any> // Simplified for now

    companion object {
        const val BASE_URL = "https://api.scryfall.com/"
        const val RATE_LIMIT_MS = 100L // 100ms between requests
    }
}