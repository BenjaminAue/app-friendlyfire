// Fichier: app/src/main/java/com/example/friendlyfire/di/DatabaseModule.kt

package com.example.friendlyfire.di

import android.content.Context
import androidx.room.Room
import com.example.friendlyfire.data.database.FriendlyFireDatabase
import com.example.friendlyfire.data.database.dao.PlayerDao
import com.example.friendlyfire.data.database.dao.QuestionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFriendlyFireDatabase(
        @ApplicationContext context: Context
    ): FriendlyFireDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FriendlyFireDatabase::class.java,
            FriendlyFireDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Pour le développement uniquement
            .build()
    }

    @Provides
    fun providePlayerDao(database: FriendlyFireDatabase): PlayerDao {
        return database.playerDao()
    }

    @Provides
    fun provideQuestionDao(database: FriendlyFireDatabase): QuestionDao {
        return database.questionDao()
    }
}