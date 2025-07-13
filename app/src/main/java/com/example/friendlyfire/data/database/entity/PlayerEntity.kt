// Fichier: app/src/main/java/com/example/friendlyfire/data/database/entities/PlayerEntity.kt

package com.example.friendlyfire.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val photoPath: String? = null, // Pour futur ajout de photos
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)