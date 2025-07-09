package com.mtginventory.app.di

import android.content.Context
import com.mtginventory.app.data.api.ScryfallApiService
import com.mtginventory.app.data.api.ScryfallRepository
import com.mtginventory.app.data.database.MTGInventoryDatabase
import com.mtginventory.app.data.database.dao.*
import com.mtginventory.app.data.mlkit.TextRecognitionService
import com.mtginventory.app.data.processing.CardProcessingPipeline
import com.mtginventory.app.data.repository.CardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MTGInventoryDatabase {
        return MTGInventoryDatabase.getDatabase(context)
    }

    @Provides
    fun provideCardDao(database: MTGInventoryDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    fun provideCollectionDao(database: MTGInventoryDatabase): CollectionDao {
        return database.collectionDao()
    }

    @Provides
    fun providePriceHistoryDao(database: MTGInventoryDatabase): PriceHistoryDao {
        return database.priceHistoryDao()
    }

    @Provides
    fun provideScanSessionDao(database: MTGInventoryDatabase): ScanSessionDao {
        return database.scanSessionDao()
    }

    @Provides
    fun provideMTGSetDao(database: MTGInventoryDatabase): MTGSetDao {
        return database.mtgSetDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "MTGInventoryApp/1.0")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ScryfallApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideScryfallApiService(retrofit: Retrofit): ScryfallApiService {
        return retrofit.create(ScryfallApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideScryfallRepository(apiService: ScryfallApiService): ScryfallRepository {
        return ScryfallRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideTextRecognitionService(): TextRecognitionService {
        return TextRecognitionService()
    }

    @Provides
    @Singleton
    fun provideCardProcessingPipeline(
        textRecognitionService: TextRecognitionService,
        scryfallRepository: ScryfallRepository
    ): CardProcessingPipeline {
        return CardProcessingPipeline(textRecognitionService, scryfallRepository)
    }

    @Provides
    @Singleton
    fun provideCardRepository(
        cardDao: CardDao,
        scryfallRepository: ScryfallRepository,
        processingPipeline: CardProcessingPipeline
    ): CardRepository {
        return CardRepository(cardDao, scryfallRepository, processingPipeline)
    }
}