package com.mtginventory.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.mtginventory.app.data.database.dao.*
import com.mtginventory.app.data.database.entities.*

@Database(
    entities = [
        CardEntity::class,
        CollectionEntity::class,
        PriceHistoryEntity::class,
        ScanSessionEntity::class,
        MTGSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MTGInventoryDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun collectionDao(): CollectionDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun mtgSetDao(): MTGSetDao

    companion object {
        @Volatile
        private var INSTANCE: MTGInventoryDatabase? = null

        fun getDatabase(context: Context): MTGInventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MTGInventoryDatabase::class.java,
                    "mtg_inventory_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    // Add any type converters needed for complex data types
    // For now, we're using JSON strings for lists/maps
}