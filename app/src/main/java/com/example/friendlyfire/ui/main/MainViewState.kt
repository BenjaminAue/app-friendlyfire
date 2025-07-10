// ===== 1. ViewState pour MainActivity =====
// Fichier: app/src/main/java/com/example/friendlyfire/ui/main/MainViewState.kt

package com.example.friendlyfire.ui.main

import com.example.friendlyfire.models.Player
import com.example.friendlyfire.models.Question

data class MainViewState(
    val isLoading: Boolean = false,
    val players: List<Player> = emptyList(),
    val availableQuestions: List<Question> = emptyList(),
    val currentPlayer: Player? = null,
    val currentQuestion: Question? = null,
    val currentTurn: Int = 0,
    val totalTurns: Int = 10,
    val gamePhase: GamePhase = GamePhase.SETUP,
    val selectedPlayer: Player? = null,
    val coinResult: CoinResult? = null,
    val error: String? = null,
    val gameStats: GameStats? = null
)

enum class GamePhase {
    SETUP,           // Initialisation du jeu
    WAITING_QUESTION, // Attente affichage question
    QUESTION_SHOWN,   // Question affichée, sélection joueur
    COIN_TOSS,        // Lancer de pièce
    GAME_OVER         // Fin de partie
}

enum class CoinResult {
    HEADS,  // Face
    TAILS   // Pile
}

data class GameStats(
    val playerStats: Map<Player, PlayerStats> = emptyMap(),
    val totalQuestionsAsked: Int = 0,
    val averagePenalties: Double = 0.0
)

data class PlayerStats(
    val questionsReceived: List<Question> = emptyList(),
    val totalPenalties: Int = 0,
    val questionsGiven: Int = 0  // Nombre de questions que ce joueur a attribuées
)