// Fichier: app/src/main/java/com/example/friendlyfire/data/repository/QuestionRepositoryImpl.kt

package com.example.friendlyfire.data.repository

import android.content.Context
import com.example.friendlyfire.R
import com.example.friendlyfire.models.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject

class QuestionRepositoryImpl @Inject constructor(
    private val context: Context
) : QuestionRepository {

    private val questionsFile = File(context.filesDir, "questions.txt")

    // StateFlow pour les questions custom par jeu
    private val customQuestionsFlows = mutableMapOf<String, MutableStateFlow<List<Question>>>()

    // ===== Méthodes existantes =====

    override fun getAllQuestions(): Flow<List<Question>> = kotlinx.coroutines.flow.flow {
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

    // ===== Méthodes pour les questions custom avec StateFlow =====

    override fun getCustomQuestionsForGame(gameId: String): Flow<List<Question>> {
        // Créer ou récupérer le StateFlow pour ce jeu
        if (!customQuestionsFlows.containsKey(gameId)) {
            customQuestionsFlows[gameId] = MutableStateFlow(loadCustomQuestionsForGame(gameId))
        }
        return customQuestionsFlows[gameId]!!.asStateFlow()
    }

    override suspend fun addCustomQuestionForGame(gameId: String, question: Question) {
        val currentQuestions = loadCustomQuestionsForGame(gameId).toMutableList()
        currentQuestions.add(question.copy(isCustom = true))
        saveCustomQuestionsForGame(gameId, currentQuestions)

        // Mettre à jour le StateFlow
        updateCustomQuestionsFlow(gameId, currentQuestions)
    }

    override suspend fun deleteCustomQuestionForGame(gameId: String, question: Question) {
        val currentQuestions = loadCustomQuestionsForGame(gameId).toMutableList()
        currentQuestions.removeAll { it.questionText == question.questionText }
        saveCustomQuestionsForGame(gameId, currentQuestions)

        // Mettre à jour le StateFlow
        updateCustomQuestionsFlow(gameId, currentQuestions)
    }

    override suspend fun updateCustomQuestion(gameId: String, oldQuestion: Question, newQuestionText: String, newPenalties: Int) {
        val currentQuestions = loadCustomQuestionsForGame(gameId).toMutableList()
        val index = currentQuestions.indexOfFirst { it.questionText == oldQuestion.questionText }
        if (index != -1) {
            currentQuestions[index] = currentQuestions[index].copy(
                questionText = newQuestionText,
                penalties = newPenalties
            )
            saveCustomQuestionsForGame(gameId, currentQuestions)

            // Mettre à jour le StateFlow
            updateCustomQuestionsFlow(gameId, currentQuestions)
        }
    }

    // ===== SYSTÈME DE PRIORITÉ DES QUESTIONS =====
    override suspend fun getQuestionsForGameWithPriority(gameId: String, totalTurns: Int): List<Question> {
        val baseQuestions = loadQuestionsFromFile()
        val customQuestions = loadCustomQuestionsForGame(gameId)

        // Mélanger les questions avec priorité aux custom
        val prioritizedQuestions = mutableListOf<Question>()

        // 1. Ajouter TOUTES les questions custom en premier (garanties d'apparaître)
        prioritizedQuestions.addAll(customQuestions.shuffled())

        // 2. Compléter avec les questions de base si nécessaire
        val remainingSlots = (totalTurns - customQuestions.size).coerceAtLeast(0)
        if (remainingSlots > 0) {
            val shuffledBaseQuestions = baseQuestions.shuffled()
            prioritizedQuestions.addAll(shuffledBaseQuestions.take(remainingSlots))
        }

        // 3. Mélanger légèrement tout en gardant les custom en priorité
        return prioritizedQuestions.shuffled()
    }

    // ===== Méthodes privées =====

    private fun updateCustomQuestionsFlow(gameId: String, questions: List<Question>) {
        customQuestionsFlows[gameId]?.value = questions
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
                            Question(questionText, penalties, isCustom = false)
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

    private fun loadCustomQuestionsForGame(gameId: String): List<Question> {
        val file = File(context.filesDir, "custom_questions_$gameId.txt")
        return try {
            if (!file.exists()) return emptyList()

            file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size == 2) {
                        val questionText = parts[0].trim()
                        val penalties = parts[1].trim().toIntOrNull() ?: 0
                        if (questionText.isNotEmpty()) {
                            Question(
                                questionText = questionText,
                                penalties = penalties,
                                isCustom = true
                            )
                        } else null
                    } else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomQuestionsForGame(gameId: String, questions: List<Question>) {
        val file = File(context.filesDir, "custom_questions_$gameId.txt")
        try {
            file.printWriter().use { writer ->
                questions.forEach { question ->
                    writer.println("${question.questionText}|${question.penalties}")
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}