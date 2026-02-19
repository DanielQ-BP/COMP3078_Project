package com.comp3074_101384549.projectui.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.comp3074_101384549.projectui.model.ListingEntity

/**
 * The Room Database holder. Defines entities and provides access to DAOs.
 */
@Database(
    entities = [ListingEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parkspot_database"
                )
                .fallbackToDestructiveMigration() // For development: clears old data when schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}