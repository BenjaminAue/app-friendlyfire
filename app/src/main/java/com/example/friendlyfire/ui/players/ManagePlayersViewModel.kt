// Fichier: app/src/main/java/com/example/friendlyfire/ui/players/ManagePlayersViewModel.kt

package com.example.friendlyfire.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friendlyfire.data.repository.PlayerRepository
import com.example.friendlyfire.data.repository.PlayerAlreadyExistsException
import com.example.friendlyfire.data.repository.PlayerNotFoundException
import com.example.friendlyfire.data.repository.PlayerRepositoryException
import com.example.friendlyfire.data.repository.PlayerValidationException
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
        _viewState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                playerRepository.getAllPlayers().collect { players ->
                    _viewState.update {
                        it.copy(
                            players = players,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur lors du chargement des joueurs. Veuillez réessayer."
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

        _viewState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                playerRepository.addPlayer(Player(name))
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Joueur \"$name\" ajouté avec succès !"
                    )
                }
            } catch (e: PlayerValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Le nom du joueur n'est pas valide"
                    )
                }
            } catch (e: PlayerAlreadyExistsException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Un joueur avec ce nom existe déjà"
                    )
                }
            } catch (e: PlayerRepositoryException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de l'ajout du joueur"
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Une erreur inattendue s'est produite. Veuillez réessayer."
                    )
                }
            }
        }
    }

    fun removePlayer(player: Player) {
        _viewState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                playerRepository.removePlayer(player)
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Joueur \"${player.name}\" supprimé avec succès !"
                    )
                }
            } catch (e: PlayerNotFoundException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Joueur non trouvé"
                    )
                }
            } catch (e: PlayerValidationException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Données du joueur invalides"
                    )
                }
            } catch (e: PlayerRepositoryException) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la suppression du joueur"
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Une erreur inattendue s'est produite. Veuillez réessayer."
                    )
                }
            }
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _viewState.update { it.copy(successMessage = null) }
    }

    fun retryOperation() {
        loadPlayers()
    }
}

data class ManagePlayersViewState(
    val players: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)