// Fichier: app/src/main/java/com/example/friendlyfire/ui/questions/CustomQuestionsViewModel.kt

package com.example.friendlyfire.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.data.repository.QuestionRepository
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
        _viewState.update { it.copy(isLoading = true) }
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
                        error = "Erreur lors du chargement des questions"
                    )
                }
            }
        }
    }

    fun addQuestionForGame(gameId: String, questionText: String, penalties: Int) {
        if (questionText.isBlank()) {
            _viewState.update { it.copy(error = "La question ne peut pas être vide") }
            return
        }

        viewModelScope.launch {
            try {
                val question = Question(
                    questionText = questionText,
                    penalties = penalties,
                    isCustom = true
                )
                questionRepository.addCustomQuestionForGame(gameId, question)
                // Les données se mettront à jour automatiquement via le Flow
            } catch (e: Exception) {
                _viewState.update { it.copy(error = "Erreur lors de l'ajout de la question") }
            }
        }
    }

    fun deleteQuestion(question: Question) {
        viewModelScope.launch {
            try {
                questionRepository.deleteCustomQuestionForGame(currentGameId, question)
                // Les données se mettront à jour automatiquement via le Flow
            } catch (e: Exception) {
                _viewState.update { it.copy(error = "Erreur lors de la suppression") }
            }
        }
    }

    // CORRECTION : Utiliser la bonne méthode du repository
    fun updateQuestion(question: Question, newQuestionText: String, newPenalties: Int) {
        viewModelScope.launch {
            try {
                questionRepository.updateCustomQuestion(
                    gameId = currentGameId,
                    oldQuestion = question,
                    newQuestionText = newQuestionText,
                    newPenalties = newPenalties
                )
                // Les données se mettront à jour automatiquement via le Flow
            } catch (e: Exception) {
                _viewState.update { it.copy(error = "Erreur lors de la modification") }
            }
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }
}

data class CustomQuestionsViewState(
    val customQuestions: List<Question> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)