// ===== 3. ManagePlayersViewModel =====
// Fichier: app/src/main/java/com/example/friendlyfire/ui/players/ManagePlayersViewModel.kt

package com.example.friendlyfire.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.models.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagePlayersViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(ManagePlayersViewState())
    val viewState: StateFlow<ManagePlayersViewState> = _viewState.asStateFlow()

    init {
        loadPlayers()
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                _viewState.update {
                    it.copy(
                        players = players,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addPlayer(name: String) {
        if (name.isBlank()) {
            _viewState.update { it.copy(error = "Le nom du joueur ne peut pas être vide") }
            return
        }

        val currentPlayers = _viewState.value.players
        if (currentPlayers.any { it.name.equals(name, ignoreCase = true) }) {
            _viewState.update { it.copy(error = "Ce joueur existe déjà") }
            return
        }

        viewModelScope.launch {
            try {
                playerRepository.addPlayer(Player(name))
                _viewState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _viewState.update { it.copy(error = "Erreur lors de l'ajout du joueur") }
            }
        }
    }

    fun removePlayer(player: Player) {
        viewModelScope.launch {
            try {
                playerRepository.removePlayer(player)
                _viewState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _viewState.update { it.copy(error = "Erreur lors de la suppression du joueur") }
            }
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }
}

data class ManagePlayersViewState(
    val players: List<Player> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)