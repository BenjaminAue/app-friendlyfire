// Fichier: app/src/main/java/com/example/friendlyfire/ui/questions/CustomQuestionsViewModel.kt

package com.example.friendlyfire.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.data.repository.QuestionRepository
import com.example.friendlyfire.data.repository.QuestionAlreadyExistsException
import com.example.friendlyfire.data.repository.QuestionNotFoundException
import com.example.friendlyfire.data.repository.QuestionRepositoryException
import com.example.friendlyfire.data.repository.QuestionValidationException
import com.example.friendlyfire.data.security.SecurityValidationException
import com.example.friendlyfire.models.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomQuestionsViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(CustomQuestionsViewState())
    val viewState: StateFlow<CustomQuestionsViewState> = _viewState.asStateFlow()

    private var currentGameId: String = ""

    fun initializeForGame(gameId: String) {
        currentGameId = gameId
        _viewState.update { it.copy(isLoading = true, error = null) }
        loadCustomQuestionsForGame(gameId)
    }

    private fun loadCustomQuestionsForGame(gameId: String) {
        viewModelScope.launch {
            try {
                questionRepository.getCustomQuestionsForGame(gameId).collect { questions ->
                    _viewState.update {
                        it.copy(
                            customQuestions = questions,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur lors du chargement des questions. Veuillez réessayer."
                    )
                }
            }
        }
    }

    fun addQuestionForGame(gameId: String, questionText: String, penalties: Int) {
        // Validation côté ViewModel pour feedback immédiat
        if (questionText.isBlank()) {
            _viewState.update { it.copy(error = "La question ne peut pas être vide") }
            return
        }

        if (questionText.length < 10) {
            _viewState.update { it.copy(error = "La question doit contenir au moins 10 caractères") }
            return
        }

        if (penalties < 1 || penalties > 20) {
            _viewState.update { it.copy(error = "Les pénalités doivent être entre 1 et 20") }
            return
        }

        _viewState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val question = Question(
                    questionText = questionText,
                    penalties = penalties,
                    isCustom = true
                )
                questionRepository.addCustomQuestionForGame(gameId, question)
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Question ajoutée avec succès !"
                    )
                }
            } catch (e: QuestionValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "La question n'est pas valide"
                    )
                }
            } catch (e: SecurityValidationException) {
                // Gestion spécifique des erreurs de sécurité InputSanitizer
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "La question contient du contenu non autorisé"
                    )
                }
            } catch (e: QuestionAlreadyExistsException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Cette question existe déjà"
                    )
                }
            } catch (e: QuestionRepositoryException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de l'ajout de la question"
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Une erreur inattendue s'est produite. Veuillez réessayer."
                    )
                }
            }
        }
    }

    fun deleteQuestion(question: Question) {
        _viewState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                questionRepository.deleteCustomQuestionForGame(currentGameId, question)
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Question supprimée avec succès !"
                    )
                }
            } catch (e: QuestionNotFoundException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Question non trouvée"
                    )
                }
            } catch (e: QuestionValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Données de la question invalides"
                    )
                }
            } catch (e: SecurityValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Données de sécurité invalides"
                    )
                }
            } catch (e: QuestionRepositoryException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la suppression de la question"
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Une erreur inattendue s'est produite. Veuillez réessayer."
                    )
                }
            }
        }
    }

    fun updateQuestion(question: Question, newQuestionText: String, newPenalties: Int) {
        // Validation côté ViewModel
        if (newQuestionText.isBlank()) {
            _viewState.update { it.copy(error = "La question ne peut pas être vide") }
            return
        }

        if (newQuestionText.length < 10) {
            _viewState.update { it.copy(error = "La question doit contenir au moins 10 caractères") }
            return
        }

        if (newPenalties < 1 || newPenalties > 20) {
            _viewState.update { it.copy(error = "Les pénalités doivent être entre 1 et 20") }
            return
        }

        _viewState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                questionRepository.updateCustomQuestion(
                    gameId = currentGameId,
                    oldQuestion = question,
                    newQuestionText = newQuestionText,
                    newPenalties = newPenalties
                )
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Question modifiée avec succès !"
                    )
                }
            } catch (e: QuestionNotFoundException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Question non trouvée"
                    )
                }
            } catch (e: QuestionValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Les nouvelles données ne sont pas valides"
                    )
                }
            } catch (e: SecurityValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "La question contient du contenu non autorisé"
                    )
                }
            } catch (e: QuestionAlreadyExistsException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Une question avec ce texte existe déjà"
                    )
                }
            } catch (e: QuestionRepositoryException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la modification de la question"
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Une erreur inattendue s'est produite. Veuillez réessayer."
                    )
                }
            }
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _viewState.update { it.copy(successMessage = null) }
    }

    fun retryOperation() {
        initializeForGame(currentGameId)
    }
}

data class CustomQuestionsViewState(
    val customQuestions: List<Question> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)