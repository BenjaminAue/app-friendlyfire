// Fichier: app/src/main/java/com/example/friendlyfire/di/AppModule.kt

package com.example.friendlyfire.di

import android.content.Context
import com.example.friendlyfire.data.database.dao.PlayerDao
import com.example.friendlyfire.data.database.dao.QuestionDao
import com.example.friendlyfire.data.migration.MigrationHelper
import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.data.repository.PlayerRepositoryRoom
import com.example.friendlyfire.data.repository.QuestionRepository
import com.example.friendlyfire.data.repository.QuestionRepositoryRoom
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePlayerRepository(
        playerDao: PlayerDao
    ): PlayerRepository = PlayerRepositoryRoom(playerDao)

    @Provides
    @Singleton
    fun provideQuestionRepository(
        @ApplicationContext context: Context,
        questionDao: QuestionDao
    ): QuestionRepository = QuestionRepositoryRoom(context, questionDao)

    @Provides
    @Singleton
    fun provideMigrationHelper(
        @ApplicationContext context: Context,
        playerDao: PlayerDao,
        questionDao: QuestionDao
    ): MigrationHelper = MigrationHelper(context, playerDao, questionDao)
}