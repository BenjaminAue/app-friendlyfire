package com.example.friendlyfire.data.repository

import com.example.friendlyfire.models.Question
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getAllQuestions(): Flow<List<Question>>
    suspend fun addQuestion(question: Question)
    suspend fun removeQuestion(question: Question)
    suspend fun updateQuestion(question: Question)
    suspend fun clearAllQuestions()
    suspend fun importQuestionsFromRaw(): Boolean
}