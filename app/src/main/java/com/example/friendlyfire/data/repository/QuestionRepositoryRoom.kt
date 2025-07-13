// Fichier: app/src/main/java/com/example/friendlyfire/data/repository/QuestionRepositoryRoom.kt

package com.example.friendlyfire.data.repository

import android.content.Context
import android.util.Log
import com.example.friendlyfire.R
import com.example.friendlyfire.data.database.dao.QuestionDao
import com.example.friendlyfire.data.database.entities.QuestionEntity
import com.example.friendlyfire.data.database.mappers.toQuestion
import com.example.friendlyfire.data.database.mappers.toQuestionEntity
import com.example.friendlyfire.data.database.mappers.toQuestions
import com.example.friendlyfire.models.Question
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
// Ajoutez ces imports dans PlayerRepositoryRoom.kt
import com.example.friendlyfire.data.security.InputSanitizer
import com.example.friendlyfire.data.security.SanitizedInput
import com.example.friendlyfire.data.security.SecurityValidationException

class QuestionRepositoryRoom @Inject constructor(
    private val context: Context,
    private val questionDao: QuestionDao
) : QuestionRepository {

    companion object {
        private const val TAG = "QuestionRepositoryRoom"
        private const val MAX_RETRIES = 3
        private const val MIN_QUESTION_LENGTH = 10
        private const val MAX_QUESTION_LENGTH = 500
        private const val MIN_PENALTIES = 1
        private const val MAX_PENALTIES = 20
    }

    override fun getAllQuestions(): Flow<List<Question>> {
        return questionDao.getBaseQuestions()
            .map { entities -> entities.toQuestions() }
            .catch { exception ->
                Log.e(TAG, "Error loading base questions", exception)
                emit(emptyList()) // Fallback
            }
    }

    override suspend fun addQuestion(question: Question) {
        try {
            val validatedQuestion = validateQuestion(question)

            // Vérifier si la question existe déjà (pour éviter les doublons)
            if (questionAlreadyExists(validatedQuestion, null)) {
                throw QuestionAlreadyExistsException("Cette question existe déjà")
            }

            retryOperation(MAX_RETRIES) {
                val questionEntity = validatedQuestion.toQuestionEntity()
                questionDao.insertQuestion(questionEntity)
            }

            Log.d(TAG, "Successfully added base question: ${validatedQuestion.questionText.take(50)}...")

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when adding question", e)
            throw e
        } catch (e: QuestionAlreadyExistsException) {
            Log.e(TAG, "Question already exists", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when adding question", e)
            throw QuestionRepositoryException("Impossible d'ajouter la question", e)
        }
    }

    override suspend fun removeQuestion(question: Question) {
        try {
            val validatedQuestion = validateQuestion(question)

            // Pour les questions de base, on cherche par texte (pas optimal mais fonctionnel)
            retryOperation(MAX_RETRIES) {
                val questionEntity = validatedQuestion.toQuestionEntity()
                questionDao.deleteQuestion(questionEntity)
            }

            Log.d(TAG, "Successfully removed question: ${validatedQuestion.questionText.take(50)}...")

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when removing question", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when removing question", e)
            throw QuestionRepositoryException("Impossible de supprimer la question", e)
        }
    }

    override suspend fun updateQuestion(question: Question) {
        try {
            val validatedQuestion = validateQuestion(question)

            retryOperation(MAX_RETRIES) {
                val questionEntity = validatedQuestion.toQuestionEntity()
                questionDao.updateQuestion(questionEntity)
            }

            Log.d(TAG, "Successfully updated question: ${validatedQuestion.questionText.take(50)}...")

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when updating question", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when updating question", e)
            throw QuestionRepositoryException("Impossible de modifier la question", e)
        }
    }

    override suspend fun clearAllQuestions() {
        try {
            retryOperation(MAX_RETRIES) {
                questionDao.deleteAllBaseQuestions()
            }
            Log.d(TAG, "Successfully cleared all base questions")

        } catch (e: Exception) {
            Log.e(TAG, "Database error when clearing all questions", e)
            throw QuestionRepositoryException("Impossible de supprimer toutes les questions", e)
        }
    }

    override suspend fun importQuestionsFromRaw(): Boolean {
        return try {
            // Vérifier si on a déjà des questions de base
            val existingCount = getBaseQuestionCount()
            if (existingCount > 0) {
                Log.d(TAG, "Base questions already imported ($existingCount questions)")
                return true
            }

            val inputStream = context.resources.openRawResource(R.raw.new_questions)
            val reader = BufferedReader(InputStreamReader(inputStream))

            val questionEntities = mutableListOf<QuestionEntity>()
            var lineNumber = 0
            var validQuestions = 0
            var invalidQuestions = 0

            reader.forEachLine { line ->
                lineNumber++
                if (line.isNotEmpty()) {
                    try {
                        val parts = line.split("|")
                        if (parts.size == 2) {
                            val questionText = parts[0].trim()
                            val penalties = parts[1].trim().toIntOrNull()

                            if (questionText.isNotEmpty() && penalties != null) {
                                // Validation de la question importée
                                if (isValidQuestionText(questionText) && isValidPenalties(penalties)) {
                                    questionEntities.add(
                                        QuestionEntity(
                                            questionText = questionText,
                                            penalties = penalties,
                                            isCustom = false,
                                            gameId = null
                                        )
                                    )
                                    validQuestions++
                                } else {
                                    Log.w(TAG, "Invalid question at line $lineNumber: $questionText (penalties: $penalties)")
                                    invalidQuestions++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing line $lineNumber: $line", e)
                        invalidQuestions++
                    }
                }
            }

            reader.close()

            if (questionEntities.isNotEmpty()) {
                retryOperation(MAX_RETRIES) {
                    questionDao.insertQuestions(questionEntities)
                }
                Log.d(TAG, "Successfully imported $validQuestions base questions from RAW (skipped $invalidQuestions invalid)")
            }

            true

        } catch (e: Exception) {
            Log.e(TAG, "Error importing questions from RAW", e)
            false
        }
    }

    override fun getCustomQuestionsForGame(gameId: String): Flow<List<Question>> {
        return questionDao.getCustomQuestionsForGame(gameId)
            .map { entities -> entities.toQuestions() }
            .catch { exception ->
                Log.e(TAG, "Error loading custom questions for game: $gameId", exception)
                emit(emptyList()) // Fallback
            }
    }

    override suspend fun addCustomQuestionForGame(gameId: String, question: Question) {
        try {
            val validatedGameId = validateGameId(gameId)
            val validatedQuestion = validateQuestion(question)

            // Vérifier si la question existe déjà pour ce jeu
            if (questionAlreadyExists(validatedQuestion, validatedGameId)) {
                throw QuestionAlreadyExistsException("Cette question existe déjà pour ce jeu")
            }

            retryOperation(MAX_RETRIES) {
                val questionEntity = validatedQuestion.toQuestionEntity(gameId = validatedGameId)
                questionDao.insertQuestion(questionEntity)
            }

            Log.d(TAG, "Successfully added custom question for game $validatedGameId: ${validatedQuestion.questionText.take(50)}...")

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when adding custom question", e)
            throw e
        } catch (e: QuestionAlreadyExistsException) {
            Log.e(TAG, "Custom question already exists", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when adding custom question for game: $gameId", e)
            throw QuestionRepositoryException("Impossible d'ajouter la question personnalisée", e)
        }
    }

    override suspend fun deleteCustomQuestionForGame(gameId: String, question: Question) {
        try {
            val validatedGameId = validateGameId(gameId)
            val validatedQuestion = validateQuestion(question)

            val customQuestions = questionDao.getCustomQuestionsForGame(validatedGameId).first()
            val questionToDelete = customQuestions.find { it.questionText == validatedQuestion.questionText }

            if (questionToDelete == null) {
                throw QuestionNotFoundException("Question personnalisée non trouvée")
            }

            retryOperation(MAX_RETRIES) {
                questionDao.deleteQuestion(questionToDelete)
            }

            Log.d(TAG, "Successfully deleted custom question for game $validatedGameId: ${validatedQuestion.questionText.take(50)}...")

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when deleting custom question", e)
            throw e
        } catch (e: QuestionNotFoundException) {
            Log.e(TAG, "Custom question not found for deletion", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when deleting custom question for game: $gameId", e)
            throw QuestionRepositoryException("Impossible de supprimer la question personnalisée", e)
        }
    }

    override suspend fun updateCustomQuestion(
        gameId: String,
        oldQuestion: Question,
        newQuestionText: String,
        newPenalties: Int
    ) {
        try {
            val validatedGameId = validateGameId(gameId)
            val validatedOldQuestion = validateQuestion(oldQuestion)
            val validatedNewQuestion = validateQuestion(
                Question(newQuestionText, newPenalties, isCustom = true)
            )

            val customQuestions = questionDao.getCustomQuestionsForGame(validatedGameId).first()
            val questionToUpdate = customQuestions.find { it.questionText == validatedOldQuestion.questionText }

            if (questionToUpdate == null) {
                throw QuestionNotFoundException("Question personnalisée non trouvée")
            }

            retryOperation(MAX_RETRIES) {
                val updatedEntity = questionToUpdate.copy(
                    questionText = validatedNewQuestion.questionText,
                    penalties = validatedNewQuestion.penalties
                )
                questionDao.updateQuestion(updatedEntity)
            }

            Log.d(TAG, "Successfully updated custom question for game $validatedGameId")

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when updating custom question", e)
            throw e
        } catch (e: QuestionNotFoundException) {
            Log.e(TAG, "Custom question not found for update", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when updating custom question for game: $gameId", e)
            throw QuestionRepositoryException("Impossible de modifier la question personnalisée", e)
        }
    }

    override suspend fun getQuestionsForGameWithPriority(gameId: String, totalTurns: Int): List<Question> {
        return try {
            val validatedGameId = validateGameId(gameId)

            if (totalTurns <= 0) {
                throw QuestionValidationException("Le nombre de tours doit être positif")
            }

            // Récupérer les questions de base
            val baseQuestionsFlow = questionDao.getBaseQuestions()
            val baseQuestions = baseQuestionsFlow.first().toQuestions()

            // Récupérer les questions custom pour ce jeu
            val customQuestionsFlow = questionDao.getCustomQuestionsForGame(validatedGameId)
            val customQuestions = customQuestionsFlow.first().toQuestions()

            // Créer la liste priorisée
            val prioritizedQuestions = mutableListOf<Question>()

            // Ajouter TOUTES les questions custom en premier (priorité max)
            prioritizedQuestions.addAll(customQuestions.shuffled())

            // Compléter avec les questions de base si nécessaire
            val remainingSlots = (totalTurns - customQuestions.size).coerceAtLeast(0)
            if (remainingSlots > 0 && baseQuestions.isNotEmpty()) {
                val shuffledBaseQuestions = baseQuestions.shuffled()
                prioritizedQuestions.addAll(shuffledBaseQuestions.take(remainingSlots))
            }

            // Mélanger légèrement tout en gardant les custom en priorité
            val finalList = prioritizedQuestions.shuffled()

            Log.d(TAG, "Generated ${finalList.size} questions for game $validatedGameId (${customQuestions.size} custom + ${finalList.size - customQuestions.size} base)")

            finalList

        } catch (e: QuestionValidationException) {
            Log.e(TAG, "Validation error when getting questions with priority", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error getting questions with priority for game: $gameId", e)
            emptyList() // Fallback pour ne pas crasher le jeu
        }
    }

    // Méthodes supplémentaires
    suspend fun getBaseQuestionCount(): Int {
        return try {
            questionDao.getBaseQuestionCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting base question count", e)
            0
        }
    }

    suspend fun getCustomQuestionCount(gameId: String): Int {
        return try {
            val validatedGameId = validateGameId(gameId)
            questionDao.getCustomQuestionCount(validatedGameId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting custom question count for game: $gameId", e)
            0
        }
    }

    // Fonctions de validation
    private fun validateQuestion(question: Question): Question {
        // Valider le texte de la question
        val sanitizedText = InputSanitizer.sanitizeQuestionText(question.questionText)
        val sanitizedPenalties = InputSanitizer.sanitizePenalties(question.penalties)

        return when {
            sanitizedText is SanitizedInput.Invalid -> {
                InputSanitizer.logSecurityEvent(
                    eventType = "INVALID_QUESTION_TEXT",
                    input = question.questionText
                )
                throw QuestionValidationException(sanitizedText.reason)
            }
            sanitizedPenalties is SanitizedInput.Invalid -> {
                InputSanitizer.logSecurityEvent(
                    eventType = "INVALID_PENALTIES",
                    input = question.penalties.toString()
                )
                throw QuestionValidationException(sanitizedPenalties.reason)
            }
            else -> {
                question.copy(
                    questionText = (sanitizedText as SanitizedInput.Valid).cleanInput,
                    penalties = question.penalties
                )
            }
        }
    }

    private fun isValidQuestionText(text: String): Boolean {
        return text.length in MIN_QUESTION_LENGTH..MAX_QUESTION_LENGTH
    }

    private fun isValidPenalties(penalties: Int): Boolean {
        return penalties in MIN_PENALTIES..MAX_PENALTIES
    }

    private fun validateGameId(gameId: String): String {
        val trimmedGameId = gameId.trim()
        if (trimmedGameId.isBlank()) {
            throw QuestionValidationException("L'ID du jeu ne peut pas être vide")
        }
        if (!trimmedGameId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw QuestionValidationException("L'ID du jeu contient des caractères non autorisés")
        }
        return trimmedGameId
    }

    private suspend fun questionAlreadyExists(question: Question, gameId: String?): Boolean {
        return try {
            if (gameId == null) {
                // Question de base
                val baseQuestions = questionDao.getBaseQuestions().first()
                baseQuestions.any { it.questionText.equals(question.questionText, ignoreCase = true) }
            } else {
                // Question custom
                val customQuestions = questionDao.getCustomQuestionsForGame(gameId).first()
                customQuestions.any { it.questionText.equals(question.questionText, ignoreCase = true) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if question exists", e)
            false // En cas d'erreur, on assume qu'elle n'existe pas
        }
    }

    // Fonction retry avec délai exponentiel
    private suspend fun <T> retryOperation(
        maxAttempts: Int,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block() // Dernière tentative, on laisse l'exception remonter
    }
}

// Exceptions spécifiques pour les questions
open class QuestionRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

class QuestionValidationException(message: String) : QuestionRepositoryException(message)
class QuestionAlreadyExistsException(message: String) : QuestionRepositoryException(message)
class QuestionNotFoundException(message: String) : QuestionRepositoryException(message)