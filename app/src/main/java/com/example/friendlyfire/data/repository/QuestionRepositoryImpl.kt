package com.example.friendlyfire.data.repository

import android.content.Context
import com.example.friendlyfire.R
import com.example.friendlyfire.models.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject

class QuestionRepositoryImpl @Inject constructor(
    private val context: Context
) : QuestionRepository {

    private val questionsFile = File(context.filesDir, "questions.txt")

    override fun getAllQuestions(): Flow<List<Question>> = flow {
        emit(loadQuestionsFromFile())
    }

    override suspend fun addQuestion(question: Question) {
        val currentQuestions = loadQuestionsFromFile().toMutableList()
        currentQuestions.add(question)
        saveQuestionsToFile(currentQuestions)
    }

    override suspend fun removeQuestion(question: Question) {
        val currentQuestions = loadQuestionsFromFile().toMutableList()
        currentQuestions.removeAll { it.questionText == question.questionText }
        saveQuestionsToFile(currentQuestions)
    }

    override suspend fun updateQuestion(question: Question) {
        val currentQuestions = loadQuestionsFromFile().toMutableList()
        val index = currentQuestions.indexOfFirst { it.questionText == question.questionText }
        if (index != -1) {
            currentQuestions[index] = question
            saveQuestionsToFile(currentQuestions)
        }
    }

    override suspend fun clearAllQuestions() {
        saveQuestionsToFile(emptyList())
    }

    override suspend fun importQuestionsFromRaw(): Boolean {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.new_questions)
            val reader = BufferedReader(InputStreamReader(inputStream))

            val newQuestions = mutableListOf<Question>()
            reader.forEachLine { line ->
                if (line.isNotEmpty()) {
                    val parts = line.split("|")
                    if (parts.size == 2) {
                        val questionText = parts[0].trim()
                        val penalties = parts[1].trim().toIntOrNull() ?: 0
                        if (questionText.isNotEmpty()) {
                            newQuestions.add(Question(questionText, penalties))
                        }
                    }
                }
            }

            val currentQuestions = loadQuestionsFromFile().toMutableList()
            newQuestions.forEach { newQuestion ->
                if (currentQuestions.none { it.questionText == newQuestion.questionText }) {
                    currentQuestions.add(newQuestion)
                }
            }

            saveQuestionsToFile(currentQuestions)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun loadQuestionsFromFile(): List<Question> {
        return try {
            if (!questionsFile.exists()) {
                return emptyList()
            }

            questionsFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size == 2) {
                        val questionText = parts[0].trim()
                        val penalties = parts[1].trim().toIntOrNull() ?: 0
                        if (questionText.isNotEmpty()) {
                            Question(questionText, penalties)
                        } else null
                    } else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveQuestionsToFile(questions: List<Question>) {
        try {
            questionsFile.printWriter().use { writer ->
                questions.forEach { question ->
                    writer.println("${question.questionText}|${question.penalties}")
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}