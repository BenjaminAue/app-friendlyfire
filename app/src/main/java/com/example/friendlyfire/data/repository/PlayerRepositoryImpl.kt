// ===== 3. Repository Implementations =====
// Fichier: app/src/main/java/com/example/friendlyfire/data/repository/PlayerRepositoryImpl.kt

package com.example.friendlyfire.data.repository

import android.content.Context
import com.example.friendlyfire.models.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val context: Context
) : PlayerRepository {

    private val playersFile = File(context.filesDir, "players.txt")

    override fun getAllPlayers(): Flow<List<Player>> = flow {
        emit(loadPlayersFromFile())
    }

    override suspend fun addPlayer(player: Player) {
        val currentPlayers = loadPlayersFromFile().toMutableList()
        if (currentPlayers.none { it.name == player.name }) {
            currentPlayers.add(player)
            savePlayersToFile(currentPlayers)
        }
    }

    override suspend fun removePlayer(player: Player) {
        val currentPlayers = loadPlayersFromFile().toMutableList()
        currentPlayers.removeAll { it.name == player.name }
        savePlayersToFile(currentPlayers)
    }

    override suspend fun updatePlayer(player: Player) {
        val currentPlayers = loadPlayersFromFile().toMutableList()
        val index = currentPlayers.indexOfFirst { it.name == player.name }
        if (index != -1) {
            currentPlayers[index] = player
            savePlayersToFile(currentPlayers)
        }
    }

    override suspend fun clearAllPlayers() {
        savePlayersToFile(emptyList())
    }

    private fun loadPlayersFromFile(): List<Player> {
        return try {
            if (!playersFile.exists()) return emptyList()

            playersFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { Player(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlayersToFile(players: List<Player>) {
        try {
            playersFile.printWriter().use { writer ->
                players.forEach { player ->
                    writer.println(player.name)
                }
            }
        } catch (e: Exception) {
            // Log error - sera géré par le ViewModel
        }
    }
}