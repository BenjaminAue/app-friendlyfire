// Fichier: app/src/main/java/com/example/friendlyfire/data/database/dao/QuestionDao.kt

package com.example.friendlyfire.data.database.dao

import androidx.room.*
import com.example.friendlyfire.data.database.entities.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    // Questions de base (non custom)
    @Query("SELECT * FROM questions WHERE isCustom = 0 ORDER BY questionText ASC")
    fun getBaseQuestions(): Flow<List<QuestionEntity>>

    // Questions custom pour un jeu spécifique
    @Query("SELECT * FROM questions WHERE isCustom = 1 AND gameId = :gameId ORDER BY createdAt DESC")
    fun getCustomQuestionsForGame(gameId: String): Flow<List<QuestionEntity>>

    // Toutes les questions pour un jeu (base + custom)
    @Query("SELECT * FROM questions WHERE isCustom = 0 OR (isCustom = 1 AND gameId = :gameId) ORDER BY isCustom DESC, createdAt DESC")
    fun getAllQuestionsForGame(gameId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :questionId")
    suspend fun deleteQuestionById(questionId: String)

    @Query("DELETE FROM questions WHERE isCustom = 1 AND gameId = :gameId")
    suspend fun deleteAllCustomQuestionsForGame(gameId: String)

    @Query("DELETE FROM questions WHERE isCustom = 0")
    suspend fun deleteAllBaseQuestions()

    @Query("SELECT COUNT(*) FROM questions WHERE isCustom = 0")
    suspend fun getBaseQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE isCustom = 1 AND gameId = :gameId")
    suspend fun getCustomQuestionCount(gameId: String): Int

    @Query("UPDATE questions SET timesUsed = timesUsed + 1 WHERE id = :questionId")
    suspend fun incrementQuestionUsage(questionId: String)

    // Pour les statistiques futures
    @Query("SELECT * FROM questions WHERE timesUsed > 0 ORDER BY timesUsed DESC LIMIT :limit")
    suspend fun getMostUsedQuestions(limit: Int = 10): List<QuestionEntity>
}