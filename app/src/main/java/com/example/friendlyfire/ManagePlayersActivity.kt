// ===== 4. ManagePlayersActivity refactorisée =====
// Fichier: app/src/main/java/com/example/friendlyfire/ManagePlayersActivity.kt

package com.example.friendlyfire

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.adapters.PlayerAdapter
import com.example.friendlyfire.models.Player
import com.example.friendlyfire.ui.players.ManagePlayersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagePlayersActivity : AppCompatActivity() {

    private val viewModel: ManagePlayersViewModel by viewModels()

    private lateinit var playersRecyclerView: RecyclerView
    private lateinit var addPlayerButton: Button
    private lateinit var playerAdapter: PlayerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_players)

        initializeViews()
        setupRecyclerView()
        observeViewModel()
    }

    private fun initializeViews() {
        playersRecyclerView = findViewById(R.id.playersRecyclerView)
        addPlayerButton = findViewById(R.id.addPlayerButton)

        addPlayerButton.setOnClickListener {
            showAddPlayerDialog()
        }
    }

    private fun setupRecyclerView() {
        playerAdapter = PlayerAdapter(emptyList()) { player ->
            viewModel.removePlayer(player)
        }
        playersRecyclerView.layoutManager = LinearLayoutManager(this)
        playersRecyclerView.adapter = playerAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.example.friendlyfire.ui.players.ManagePlayersViewState) {
        // Mettre à jour l'adapter avec les nouveaux joueurs
        playerAdapter = PlayerAdapter(state.players) { player ->
            viewModel.removePlayer(player)
        }
        playersRecyclerView.adapter = playerAdapter

        // Gérer les erreurs
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun showAddPlayerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_player, null)
        val playerNameEditText = dialogView.findViewById<EditText>(R.id.playerNameEditText)

        AlertDialog.Builder(this)
            .setTitle("Ajouter un joueur")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val playerName = playerNameEditText.text.toString()
                viewModel.addPlayer(playerName)
            }
            .setNegativeButton("Annuler", null)
            .create()
            .show()
    }
}