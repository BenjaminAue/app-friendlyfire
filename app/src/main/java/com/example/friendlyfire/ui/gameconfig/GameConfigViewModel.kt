// ===== 2. GameConfigViewModel =====
// Fichier: app/src/main/java/com/example/friendlyfire/ui/gameconfig/GameConfigViewModel.kt

package com.example.friendlyfire.ui.gameconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.ui.home.GameInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameConfigViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(GameConfigViewState())
    val viewState: StateFlow<GameConfigViewState> = _viewState.asStateFlow()

    fun initializeConfig(gameInfo: GameInfo) {
        _viewState.update { it.copy(gameInfo = gameInfo, isLoading = true) }

        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                _viewState.update {
                    it.copy(
                        playerCount = players.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectTurns(turns: Int) {
        _viewState.update { it.copy(selectedTurns = turns) }
    }

    fun selectTheme(theme: QuestionTheme) {
        _viewState.update { it.copy(selectedTheme = theme) }
    }

    fun startGame() {
        val currentState = _viewState.value
        // La navigation sera gérée par l'Activity
    }

    fun getGameRules(): String {
        return when (_viewState.value.gameInfo?.id) {
            "friendly_fire" -> """
                🎯 Règles de Friendly Fire :
                
                1. Un joueur est choisi aléatoirement pour commencer
                2. Il voit une question et choisit qui doit y répondre
                3. La personne désignée lance une pièce :
                   • Face = Répond à la question
                   • Pile = Prend les pénalités
                4. C'est au tour de la personne désignée
                5. Le joueur avec le moins de pénalités gagne !
                
                🎮 Prêt à jouer ?
            """.trimIndent()
            else -> "Règles non définies pour ce jeu"
        }
    }

    fun getTurnsOptions(): List<Int> {
        return listOf(5, 10, 15, 20, 25, 30)
    }

    fun getConfigSummary(): String {
        val state = _viewState.value
        return """
            👥 ${state.playerCount} joueurs
            🎲 ${state.selectedTurns} tours
            ${state.selectedTheme.emoji} ${state.selectedTheme.displayName}
        """.trimIndent()
    }
}
