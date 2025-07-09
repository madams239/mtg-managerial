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
        CollectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MTGInventoryDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun collectionDao(): CollectionDao

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

