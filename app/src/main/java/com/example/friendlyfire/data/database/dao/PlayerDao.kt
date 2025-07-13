// Fichier: app/src/main/java/com/example/friendlyfire/data/database/dao/PlayerDao.kt

package com.example.friendlyfire.data.database.dao

import androidx.room.*
import com.example.friendlyfire.data.database.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players ORDER BY lastUsed DESC, name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayerById(playerId: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE name = :name LIMIT 1")
    suspend fun getPlayerByName(name: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayerById(playerId: String)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    @Query("UPDATE players SET lastUsed = :timestamp WHERE id = :playerId")
    suspend fun updatePlayerLastUsed(playerId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int
}