package com.example.friendlyfire.data.repository

import com.example.friendlyfire.models.Player
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getAllPlayers(): Flow<List<Player>>
    suspend fun addPlayer(player: Player)
    suspend fun removePlayer(player: Player)
    suspend fun updatePlayer(player: Player)
    suspend fun clearAllPlayers()
}