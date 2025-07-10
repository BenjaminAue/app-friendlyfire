// ===== 2. HomeViewModel =====
// Fichier: app/src/main/java/com/example/friendlyfire/ui/home/HomeViewModel.kt

package com.example.friendlyfire.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.R
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
class HomeViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(HomeViewState())
    val viewState: StateFlow<HomeViewState> = _viewState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        _viewState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Assurer que les questions sont chargées
                questionRepository.importQuestionsFromRaw()

                // Charger les joueurs
                playerRepository.getAllPlayers().collect { players ->
                    val games = getAvailableGames()
                    val canStart = players.size >= 2 // Minimum 2 joueurs

                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            players = players,
                            availableGames = games,
                            canStartGame = canStart,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur lors du chargement: ${e.message}"
                    )
                }
            }
        }
    }

    private fun getAvailableGames(): List<GameInfo> {
        return listOf(
            GameInfo(
                id = "friendly_fire",
                name = "Friendly Fire",
                description = "Le jeu de questions entre amis ! Qui sera désigné pour chaque question ?",
                minPlayers = 2,
                maxPlayers = 10,
                iconResId = R.drawable.ic_launcher_foreground,
                isAvailable = true
            ),
            GameInfo(
                id = "truth_or_dare",
                name = "Vérité ou Action",
                description = "Classique ! Vérité ou action pour pimenter la soirée",
                minPlayers = 2,
                maxPlayers = 8,
                iconResId = R.drawable.ic_launcher_foreground,
                isAvailable = false // Pas encore développé
            ),
            GameInfo(
                id = "never_have_i_ever",
                name = "Je n'ai jamais",
                description = "Découvrez les secrets de vos amis avec ce jeu de révélations",
                minPlayers = 3,
                maxPlayers = 10,
                iconResId = R.drawable.ic_launcher_foreground,
                isAvailable = false // Pas encore développé
            )
        )
    }

    fun selectGame(gameInfo: GameInfo) {
        val currentState = _viewState.value

        when {
            !gameInfo.isAvailable -> {
                _viewState.update { it.copy(error = "Ce jeu n'est pas encore disponible !") }
            }
            currentState.players.size < gameInfo.minPlayers -> {
                _viewState.update {
                    it.copy(error = "Il faut au minimum ${gameInfo.minPlayers} joueurs pour ce jeu")
                }
            }
            currentState.players.size > gameInfo.maxPlayers -> {
                _viewState.update {
                    it.copy(error = "Ce jeu accepte maximum ${gameInfo.maxPlayers} joueurs")
                }
            }
            else -> {
                _viewState.update { it.copy(selectedGame = gameInfo, error = null) }
            }
        }
    }

    fun startSelectedGame() {
        val selectedGame = _viewState.value.selectedGame
        if (selectedGame != null) {
            // L'activity gérera la navigation vers le jeu
            // Ici on peut ajouter des analytics, logs, etc.
        }
    }

    fun clearGameSelection() {
        _viewState.update { it.copy(selectedGame = null) }
    }

    fun refreshPlayers() {
        loadHomeData()
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    fun getPlayersCountText(): String {
        val count = _viewState.value.players.size
        return when (count) {
            0 -> "Aucun joueur"
            1 -> "1 joueur"
            else -> "$count joueurs"
        }
    }

    fun getGameStatusText(gameInfo: GameInfo): String {
        val playerCount = _viewState.value.players.size

        return when {
            !gameInfo.isAvailable -> "Bientôt disponible"
            playerCount < gameInfo.minPlayers -> "Minimum ${gameInfo.minPlayers} joueurs requis"
            playerCount > gameInfo.maxPlayers -> "Maximum ${gameInfo.maxPlayers} joueurs"
            else -> "Prêt à jouer !"
        }
    }

    fun canPlayGame(gameInfo: GameInfo): Boolean {
        val playerCount = _viewState.value.players.size
        return gameInfo.isAvailable &&
                playerCount >= gameInfo.minPlayers &&
                playerCount <= gameInfo.maxPlayers
    }
}