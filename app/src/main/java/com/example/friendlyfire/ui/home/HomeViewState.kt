package com.example.friendlyfire.ui.home

import com.example.friendlyfire.models.Player
import java.io.Serializable

data class HomeViewState(
    val isLoading: Boolean = false,
    val players: List<Player> = emptyList(),
    val availableGames: List<GameInfo> = emptyList(),
    val selectedGame: GameInfo? = null,
    val canStartGame: Boolean = false,
    val error: String? = null
)

data class GameInfo(
    val id: String,
    val name: String,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val iconResId: Int = 0,
    val isAvailable: Boolean = true
) : Serializable