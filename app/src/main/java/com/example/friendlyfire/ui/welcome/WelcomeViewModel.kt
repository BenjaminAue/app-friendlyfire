// ===== 1. WelcomeViewModel =====
// Fichier: app/src/main/java/com/example/friendlyfire/ui/welcome/WelcomeViewModel.kt

package com.example.friendlyfire.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(WelcomeViewState())
    val viewState: StateFlow<WelcomeViewState> = _viewState.asStateFlow()

    init {
        loadGameData()
    }

    private fun loadGameData() {
        viewModelScope.launch {
            // Assurer que les questions sont importées
            questionRepository.importQuestionsFromRaw()

            // Charger les joueurs
            playerRepository.getAllPlayers().collect { players ->
                _viewState.update {
                    it.copy(
                        playerCount = players.size,
                        canStartGame = players.isNotEmpty()
                    )
                }
            }
        }
    }

    fun startGame(totalTurns: Int) {
        _viewState.update { it.copy(selectedTurns = totalTurns) }
    }
}

data class WelcomeViewState(
    val playerCount: Int = 0,
    val canStartGame: Boolean = false,
    val selectedTurns: Int? = null
)