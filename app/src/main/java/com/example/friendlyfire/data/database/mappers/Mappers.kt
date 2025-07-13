// Fichier: app/src/main/java/com/example/friendlyfire/data/database/mappers/Mappers.kt

package com.example.friendlyfire.data.database.mappers

import com.example.friendlyfire.data.database.entities.PlayerEntity
import com.example.friendlyfire.data.database.entities.QuestionEntity
import com.example.friendlyfire.models.Player
import com.example.friendlyfire.models.Question

// Extension functions pour PlayerEntity
fun PlayerEntity.toPlayer(): Player {
    return Player(name = this.name)
}

fun Player.toPlayerEntity(id: String? = null): PlayerEntity {
    return PlayerEntity(
        id = id ?: java.util.UUID.randomUUID().toString(),
        name = this.name,
        lastUsed = System.currentTimeMillis()
    )
}

fun List<PlayerEntity>.toPlayers(): List<Player> {
    return this.map { it.toPlayer() }
}

fun List<Player>.toPlayerEntities(): List<PlayerEntity> {
    return this.map { it.toPlayerEntity() }
}

// Extension functions pour QuestionEntity
fun QuestionEntity.toQuestion(): Question {
    return Question(
        questionText = this.questionText,
        penalties = this.penalties,
        isCustom = this.isCustom
    )
}

fun Question.toQuestionEntity(
    id: String? = null,
    gameId: String? = null
): QuestionEntity {
    return QuestionEntity(
        id = id ?: java.util.UUID.randomUUID().toString(),
        questionText = this.questionText,
        penalties = this.penalties,
        isCustom = this.isCustom,
        gameId = gameId
    )
}

fun List<QuestionEntity>.toQuestions(): List<Question> {
    return this.map { it.toQuestion() }
}

fun List<Question>.toQuestionEntities(gameId: String? = null): List<QuestionEntity> {
    return this.map { it.toQuestionEntity(gameId = gameId) }
}