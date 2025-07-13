// Fichier: app/src/main/java/com/example/friendlyfire/data/database/entities/QuestionEntity.kt

package com.example.friendlyfire.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["isCustom"]),
        Index(value = ["gameId", "isCustom"])
    ]
)
data class QuestionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val questionText: String,
    val penalties: Int,
    val isCustom: Boolean = false,
    val gameId: String? = null, // null pour questions de base, gameId pour questions custom
    val createdAt: Long = System.currentTimeMillis(),
    val timesUsed: Int = 0 // Pour futures statistiques
)