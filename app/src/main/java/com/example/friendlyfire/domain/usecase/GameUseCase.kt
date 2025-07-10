// Fichier: app/src/main/java/com/example/friendlyfire/domain/usecase/GameUseCase.kt

package com.example.friendlyfire.domain.usecase

import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.data.repository.QuestionRepository
import com.example.friendlyfire.models.Player
import com.example.friendlyfire.models.Question
import com.example.friendlyfire.ui.main.CoinResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.random.Random

class GameUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val questionRepository: QuestionRepository
) {

    fun getGameData(): Flow<Pair<List<Player>, List<Question>>> {
        return combine(
            playerRepository.getAllPlayers(),
            questionRepository.getAllQuestions()
        ) { players, questions ->
            Pair(players, questions)
        }
    }

    fun getRandomQuestion(availableQuestions: List<Question>, currentPlayer: Player): Question? {
        return if (availableQuestions.isNotEmpty()) {
            val randomQuestion = availableQuestions.random()
            randomQuestion.copy(player = currentPlayer)
        } else {
            null
        }
    }

    fun tossCoin(): CoinResult {
        return if (Random.nextBoolean()) CoinResult.HEADS else CoinResult.TAILS
    }

    fun calculateGameStats(players: List<Player>): Map<Player, Int> {
        return players.associateWith { player ->
            player.questions.sumOf { it.penalties } + player.penalties
        }
    }

    suspend fun ensureQuestionsLoaded(): Boolean {
        return questionRepository.importQuestionsFromRaw()
    }
}