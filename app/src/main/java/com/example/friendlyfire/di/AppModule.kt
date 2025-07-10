// ===== 2. Module de Dependency Injection =====
// Fichier: app/src/main/java/com/example/friendlyfire/di/AppModule.kt

package com.example.friendlyfire.di

import android.content.Context
import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.data.repository.PlayerRepositoryImpl
import com.example.friendlyfire.data.repository.QuestionRepository
import com.example.friendlyfire.data.repository.QuestionRepositoryImpl
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
        @ApplicationContext context: Context
    ): PlayerRepository = PlayerRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideQuestionRepository(
        @ApplicationContext context: Context
    ): QuestionRepository = QuestionRepositoryImpl(context)
}