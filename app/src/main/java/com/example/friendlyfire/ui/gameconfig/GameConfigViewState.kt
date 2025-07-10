package com.example.friendlyfire.ui.gameconfig

import com.example.friendlyfire.ui.home.GameInfo

data class GameConfigViewState(
    val gameInfo: GameInfo? = null,
    val selectedTurns: Int = 10,
    val selectedTheme: QuestionTheme = QuestionTheme.RANDOM,
    val availableThemes: List<QuestionTheme> = QuestionTheme.values().toList(),
    val playerCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class QuestionTheme(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    RANDOM("Aléatoire", "Mélange de toutes les questions", "🎲"),
    CHILL("Chill", "Questions détendues pour une ambiance cool", "😎"),
    HARD("Hard", "Questions intenses pour les plus courageux", "🔥"),
    HOT("Hot", "Questions épicées pour pimenter la soirée", "🌶️")
}