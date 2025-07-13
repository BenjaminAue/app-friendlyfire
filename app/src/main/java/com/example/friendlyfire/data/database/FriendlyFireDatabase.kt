// Fichier: app/src/main/java/com/example/friendlyfire/data/database/FriendlyFireDatabase.kt

package com.example.friendlyfire.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.friendlyfire.data.database.dao.PlayerDao
import com.example.friendlyfire.data.database.dao.QuestionDao
import com.example.friendlyfire.data.database.entities.PlayerEntity
import com.example.friendlyfire.data.database.entities.QuestionEntity

@Database(
    entities = [
        PlayerEntity::class,
        QuestionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FriendlyFireDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun questionDao(): QuestionDao

    companion object {
        const val DATABASE_NAME = "friendlyfire_database"

        @Volatile
        private var INSTANCE: FriendlyFireDatabase? = null

        fun getDatabase(context: Context): FriendlyFireDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FriendlyFireDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // Pour le développement uniquement
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}