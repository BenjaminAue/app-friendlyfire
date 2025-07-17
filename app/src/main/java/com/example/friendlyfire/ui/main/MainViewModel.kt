// Fichier: app/src/main/java/com/example/friendlyfire/ui/main/MainViewModel.kt

package com.example.friendlyfire.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.domain.usecase.GameUseCase
import com.example.friendlyfire.models.Player
import com.example.friendlyfire.models.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val gameUseCase: GameUseCase
) : ViewModel() {

    private val _viewState = MutableStateFlow(MainViewState())
    val viewState: StateFlow<MainViewState> = _viewState.asStateFlow()

    private val _availableQuestions = mutableListOf<Question>()

    fun initializeGame(totalTurns: Int, gameId: String = "friendly_fire") {
        _viewState.update { it.copy(isLoading = true, totalTurns = totalTurns) }

        viewModelScope.launch {
            try {
                // Assurer que les questions sont chargées
                gameUseCase.ensureQuestionsLoaded()

                // Charger les questions avec priorité
                val prioritizedQuestions = gameUseCase.getQuestionsWithPriority(gameId, totalTurns)
                _availableQuestions.clear()
                _availableQuestions.addAll(prioritizedQuestions)

                // Charger les joueurs
                gameUseCase.getGameData().collect { (players, _) ->
                    when {
                        players.isEmpty() -> {
                            _viewState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Aucun joueur enregistré. Veuillez ajouter des joueurs."
                                )
                            }
                        }
                        players.size < 2 -> {
                            _viewState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Il faut au moins 2 joueurs pour jouer. Ajoutez d'autres joueurs."
                                )
                            }
                        }
                        prioritizedQuestions.isEmpty() -> {
                            _viewState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Aucune question disponible. Veuillez en ajouter."
                                )
                            }
                        }
                        else -> {
                            // Initialiser le jeu avec un joueur aléatoire
                            val startingPlayer = players.random()
                            _viewState.update {
                                it.copy(
                                    isLoading = false,
                                    players = players,
                                    availableQuestions = prioritizedQuestions,
                                    currentPlayer = startingPlayer,
                                    gamePhase = GamePhase.WAITING_QUESTION,
                                    error = null
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur lors de l'initialisation du jeu: ${e.message}"
                    )
                }
            }
        }
    }

    fun showQuestion() {
        val currentState = _viewState.value
        val currentPlayer = currentState.currentPlayer ?: return

        val question = gameUseCase.getRandomQuestion(_availableQuestions, currentPlayer)

        if (question == null) {
            // Plus de questions disponibles, terminer le jeu
            endGame()
            return
        }

        // Retirer la question des disponibles
        _availableQuestions.removeAll { it.questionText == question.questionText }

        // Créer la liste des joueurs sélectionnables (exclure le joueur actuel)
        val selectablePlayers = currentState.players.filter { player ->
            player.name != currentPlayer.name
        }

        _viewState.update {
            it.copy(
                currentQuestion = question,
                gamePhase = GamePhase.QUESTION_SHOWN,
                availableQuestions = _availableQuestions.toList(),
                selectablePlayers = selectablePlayers
            )
        }
    }

    fun selectPlayer(player: Player?) {
        _viewState.update {
            it.copy(selectedPlayer = player)
        }
    }

    fun validatePlayerSelection() {
        val currentState = _viewState.value
        val selectedPlayer = currentState.selectedPlayer
        val currentQuestion = currentState.currentQuestion

        if (selectedPlayer == null || currentQuestion == null) {
            _viewState.update { it.copy(error = "Veuillez sélectionner un joueur !") }
            return
        }

        // Ajouter la question au joueur sélectionné
        selectedPlayer.questions.add(currentQuestion)

        // Le joueur sélectionné deviendra le joueur actuel APRÈS le lancer de pièce
        _viewState.update {
            it.copy(
                gamePhase = GamePhase.COIN_TOSS,
                selectedPlayer = selectedPlayer,
                selectablePlayers = emptyList(), // Reset la liste sélectionnable
                error = null
            )
        }
    }

    fun tossCoin() {
        val currentState = _viewState.value
        val currentQuestion = currentState.currentQuestion ?: return
        val selectedPlayer = currentState.selectedPlayer ?: return

        val coinResult = gameUseCase.tossCoin()

        when (coinResult) {
            CoinResult.HEADS -> {
                // Face : le joueur doit répondre à la question
                // Pas de pénalité supplémentaire
            }
            CoinResult.TAILS -> {
                // Pile : le joueur prend les pénalités
                selectedPlayer.penalties += currentQuestion.penalties
            }
        }

        // Le joueur sélectionné devient maintenant le joueur actuel
        _viewState.update {
            it.copy(
                currentPlayer = selectedPlayer,
                coinResult = coinResult,
                gamePhase = GamePhase.COIN_TOSS
            )
        }
    }

    fun nextTurn() {
        val currentState = _viewState.value
        val newTurn = currentState.currentTurn + 1

        if (newTurn >= currentState.totalTurns) {
            endGame()
        } else {
            _viewState.update {
                it.copy(
                    currentTurn = newTurn,
                    gamePhase = GamePhase.WAITING_QUESTION,
                    currentQuestion = null,
                    coinResult = null,
                    selectedPlayer = null,
                    selectablePlayers = emptyList() // Reset la liste sélectionnable
                )
            }
        }
    }

    fun endGame() {
        val currentState = _viewState.value
        val gameStats = GameStats(
            playerStats = currentState.players.associateWith { player ->
                PlayerStats(
                    questionsReceived = player.questions.toList(),
                    totalPenalties = player.totalPenalties() + player.penalties
                )
            },
            totalQuestionsAsked = currentState.currentTurn,
            averagePenalties = currentState.players.map { it.totalPenalties() + it.penalties }.average()
        )

        _viewState.update {
            it.copy(
                gamePhase = GamePhase.GAME_OVER,
                gameStats = gameStats,
                selectablePlayers = emptyList() // Reset la liste sélectionnable
            )
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            // Réinitialiser les pénalités des joueurs
            _viewState.value.players.forEach { player ->
                player.penalties = 0
                player.questions.clear()
            }

            // Recharger les questions
            gameUseCase.getGameData().collect { (players, questions) ->
                _availableQuestions.clear()
                _availableQuestions.addAll(questions)

                val startingPlayer = players.random()
                _viewState.update {
                    MainViewState(
                        players = players,
                        availableQuestions = questions,
                        currentPlayer = startingPlayer,
                        totalTurns = it.totalTurns,
                        gamePhase = GamePhase.WAITING_QUESTION,
                        selectablePlayers = emptyList()
                    )
                }
            }
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    fun getCurrentTurnText(): String {
        val currentState = _viewState.value
        return "Tour ${currentState.currentTurn + 1}/${currentState.totalTurns}"
    }

    fun getPlayerTurnText(): String {
        val currentState = _viewState.value
        val currentPlayer = currentState.currentPlayer?.name ?: "Joueur inconnu"

        return when (currentState.gamePhase) {
            GamePhase.WAITING_QUESTION -> "C'est à $currentPlayer de jouer !"
            GamePhase.QUESTION_SHOWN -> "Sélectionnez un joueur pour la question"
            GamePhase.COIN_TOSS -> {
                if (currentState.coinResult == null) {
                    val selectedPlayer = currentState.selectedPlayer?.name ?: "Joueur"
                    "$selectedPlayer, lance la pièce !"
                } else {
                    "Résultat :"
                }
            }
            GamePhase.GAME_OVER -> "Fin de partie ! Merci d'avoir joué !"
            else -> currentPlayer
        }
    }

    fun getCoinResultText(): String {
        val currentState = _viewState.value
        val selectedPlayer = currentState.selectedPlayer?.name ?: "Joueur"
        val currentQuestion = currentState.currentQuestion

        return when (currentState.coinResult) {
            CoinResult.HEADS -> "🎉 Face ! $selectedPlayer doit répondre à la question :\n\n\"${currentQuestion?.questionText}\""
            CoinResult.TAILS -> "😅 Pile ! $selectedPlayer prend ${currentQuestion?.penalties} pénalités."
            null -> ""
        }
    }

    fun getQuestionText(): String {
        val currentQuestion = _viewState.value.currentQuestion
        return currentQuestion?.let {
            "${it.questionText} (Pénalité: ${it.penalties})"
        } ?: ""
    }

    fun getGameStatsText(): String {
        val gameStats = _viewState.value.gameStats ?: return ""

        val statsBuilder = StringBuilder("Stats de la partie:\n")

        gameStats.playerStats.forEach { (player, stats) ->
            statsBuilder.append("${player.name}:\n")

            if (stats.questionsReceived.isNotEmpty()) {
                stats.questionsReceived.forEach { question ->
                    val playerWhoAssigned = question.player?.name ?: "Joueur inconnu"
                    statsBuilder.append("- ${question.questionText} (${question.penalties}), par: $playerWhoAssigned\n")
                }
            } else {
                statsBuilder.append("- Aucune question attribuée.\n")
            }

            statsBuilder.append("Total des pénalités: ${stats.totalPenalties}\n\n")
        }

        return statsBuilder.toString()
    }
}