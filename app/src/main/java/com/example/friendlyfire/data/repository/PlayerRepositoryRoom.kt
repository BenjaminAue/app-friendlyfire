// Fichier: app/src/main/java/com/example/friendlyfire/data/repository/PlayerRepositoryRoom.kt

package com.example.friendlyfire.data.repository

import android.util.Log
import com.example.friendlyfire.data.database.dao.PlayerDao
import com.example.friendlyfire.data.database.mappers.toPlayer
import com.example.friendlyfire.data.database.mappers.toPlayerEntity
import com.example.friendlyfire.data.database.mappers.toPlayers
import com.example.friendlyfire.models.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
// Ajoutez ces imports dans PlayerRepositoryRoom.kt
import com.example.friendlyfire.data.security.InputSanitizer
import com.example.friendlyfire.data.security.SanitizedInput
import com.example.friendlyfire.data.security.SecurityValidationException

class PlayerRepositoryRoom @Inject constructor(
    private val playerDao: PlayerDao
) : PlayerRepository {

    companion object {
        private const val TAG = "PlayerRepositoryRoom"
        private const val MAX_RETRIES = 3
    }

    override fun getAllPlayers(): Flow<List<Player>> {
        return playerDao.getAllPlayers()
            .map { entities ->
                entities.toPlayers()
            }
            .catch { exception ->
                Log.e(TAG, "Error loading players", exception)
                // Fallback : émettre une liste vide plutôt que crasher
                emit(emptyList())
            }
    }

    override suspend fun addPlayer(player: Player) {
        try {
            // Validation des données
            val validatedPlayer = validatePlayer(player)

            // Vérifier si le joueur existe déjà
            val existingPlayer = playerDao.getPlayerByName(validatedPlayer.name)
            if (existingPlayer != null) {
                Log.w(TAG, "Player ${validatedPlayer.name} already exists")
                throw PlayerAlreadyExistsException("Un joueur avec ce nom existe déjà")
            }

            // Retry logic pour les opérations critiques
            retryOperation(MAX_RETRIES) {
                val playerEntity = validatedPlayer.toPlayerEntity()
                playerDao.insertPlayer(playerEntity)
            }

            Log.d(TAG, "Successfully added player: ${validatedPlayer.name}")

        } catch (e: PlayerValidationException) {
            Log.e(TAG, "Validation error when adding player", e)
            throw e
        } catch (e: PlayerAlreadyExistsException) {
            Log.e(TAG, "Player already exists", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when adding player: ${player.name}", e)
            throw PlayerRepositoryException("Impossible d'ajouter le joueur", e)
        }
    }

    override suspend fun removePlayer(player: Player) {
        try {
            val validatedPlayer = validatePlayer(player)

            val existingPlayer = playerDao.getPlayerByName(validatedPlayer.name)
            if (existingPlayer == null) {
                Log.w(TAG, "Player ${validatedPlayer.name} not found for deletion")
                throw PlayerNotFoundException("Joueur non trouvé")
            }

            retryOperation(MAX_RETRIES) {
                playerDao.deletePlayer(existingPlayer)
            }

            Log.d(TAG, "Successfully removed player: ${validatedPlayer.name}")

        } catch (e: PlayerValidationException) {
            Log.e(TAG, "Validation error when removing player", e)
            throw e
        } catch (e: PlayerNotFoundException) {
            Log.e(TAG, "Player not found for deletion", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when removing player: ${player.name}", e)
            throw PlayerRepositoryException("Impossible de supprimer le joueur", e)
        }
    }

    override suspend fun updatePlayer(player: Player) {
        try {
            val validatedPlayer = validatePlayer(player)

            val existingPlayer = playerDao.getPlayerByName(validatedPlayer.name)
            if (existingPlayer == null) {
                Log.w(TAG, "Player ${validatedPlayer.name} not found for update")
                throw PlayerNotFoundException("Joueur non trouvé")
            }

            retryOperation(MAX_RETRIES) {
                val updatedEntity = existingPlayer.copy(
                    name = validatedPlayer.name,
                    lastUsed = System.currentTimeMillis()
                )
                playerDao.updatePlayer(updatedEntity)
            }

            Log.d(TAG, "Successfully updated player: ${validatedPlayer.name}")

        } catch (e: PlayerValidationException) {
            Log.e(TAG, "Validation error when updating player", e)
            throw e
        } catch (e: PlayerNotFoundException) {
            Log.e(TAG, "Player not found for update", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Database error when updating player: ${player.name}", e)
            throw PlayerRepositoryException("Impossible de modifier le joueur", e)
        }
    }

    override suspend fun clearAllPlayers() {
        try {
            retryOperation(MAX_RETRIES) {
                playerDao.deleteAllPlayers()
            }
            Log.d(TAG, "Successfully cleared all players")

        } catch (e: Exception) {
            Log.e(TAG, "Database error when clearing all players", e)
            throw PlayerRepositoryException("Impossible de supprimer tous les joueurs", e)
        }
    }

    // Méthodes supplémentaires spécifiques à Room
    suspend fun updatePlayerLastUsed(playerName: String) {
        try {
            val player = playerDao.getPlayerByName(playerName)
            if (player != null) {
                retryOperation(MAX_RETRIES) {
                    playerDao.updatePlayerLastUsed(player.id)
                }
                Log.d(TAG, "Updated last used for player: $playerName")
            } else {
                Log.w(TAG, "Player not found for last used update: $playerName")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating last used for player: $playerName", e)
            // Non-critique, on ne throw pas
        }
    }

    suspend fun getPlayerCount(): Int {
        return try {
            playerDao.getPlayerCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting player count", e)
            0 // Fallback
        }
    }

    // Validation des données
    private fun validatePlayer(player: Player): Player {
        // Utiliser InputSanitizer au lieu de validation manuelle
        val sanitizedName = InputSanitizer.sanitizePlayerName(player.name)

        return when (sanitizedName) {
            is SanitizedInput.Valid -> {
                player.copy(name = sanitizedName.cleanInput)
            }
            is SanitizedInput.Invalid -> {
                // Logger l'événement de sécurité
                InputSanitizer.logSecurityEvent(
                    eventType = "INVALID_PLAYER_NAME",
                    input = player.name
                )
                throw PlayerValidationException(sanitizedName.reason)
            }
        }
    }

    // Fonction retry locale avec délai exponentiel
    private suspend fun <T> retryOperation(
        maxAttempts: Int,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block() // Dernière tentative, on laisse l'exception remonter
    }
}

// Exceptions spécifiques pour une meilleure gestion d'erreurs
open class PlayerRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PlayerValidationException(message: String) : PlayerRepositoryException(message)
class PlayerAlreadyExistsException(message: String) : PlayerRepositoryException(message)
class PlayerNotFoundException(message: String) : PlayerRepositoryException(message)