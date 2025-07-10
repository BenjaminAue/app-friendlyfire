// ===== 1. HomeActivity =====
// Fichier: app/src/main/java/com/example/friendlyfire/HomeActivity.kt

package com.example.friendlyfire

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.adapters.GameAdapter
import com.example.friendlyfire.adapters.PlayerPreviewAdapter
import com.example.friendlyfire.ui.home.HomeViewModel
import com.example.friendlyfire.ui.home.HomeViewState
import com.example.friendlyfire.ui.home.GameInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()

    // Views
    private lateinit var titleTextView: TextView
    private lateinit var playersCountTextView: TextView
    private lateinit var playersRecyclerView: RecyclerView
    private lateinit var managePlayersButton: Button
    private lateinit var gamesRecyclerView: RecyclerView
    //private lateinit var manageQuestionsButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView

    // Adapters
    private lateinit var playerPreviewAdapter: PlayerPreviewAdapter
    private lateinit var gameAdapter: GameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeViews()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchir les données quand on revient sur l'écran
        viewModel.refreshPlayers()
    }

    private fun initializeViews() {
        titleTextView = findViewById(R.id.titleTextView)
        playersCountTextView = findViewById(R.id.playersCountTextView)
        playersRecyclerView = findViewById(R.id.playersRecyclerView)
        managePlayersButton = findViewById(R.id.managePlayersButton)
        gamesRecyclerView = findViewById(R.id.gamesRecyclerView)
        //manageQuestionsButton = findViewById(R.id.manageQuestionsButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        errorTextView = findViewById(R.id.errorTextView)
    }

    private fun setupRecyclerViews() {
        // RecyclerView pour les joueurs (aperçu horizontal)
        playerPreviewAdapter = PlayerPreviewAdapter(emptyList())
        playersRecyclerView.adapter = playerPreviewAdapter
        playersRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // RecyclerView pour les jeux
        gameAdapter = GameAdapter(
            games = emptyList(),
            onGameClick = { gameInfo -> viewModel.selectGame(gameInfo) },
            onPlayClick = { gameInfo -> startGame(gameInfo) }
        )
        gamesRecyclerView.adapter = gameAdapter
        gamesRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        managePlayersButton.setOnClickListener {
            val intent = Intent(this, ManagePlayersActivity::class.java)
            startActivity(intent)
        }

        //manageQuestionsButton.setOnClickListener {
          //  val intent = Intent(this, ManageQuestionsActivity::class.java)
            //startActivity(intent)
        //}

        errorTextView.setOnClickListener {
            viewModel.clearError()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: HomeViewState) {
        // Gestion du loading
        loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Gestion des erreurs
        if (state.error != null) {
            errorTextView.text = state.error
            errorTextView.visibility = View.VISIBLE
        } else {
            errorTextView.visibility = View.GONE
        }

        // Mise à jour du nombre de joueurs
        playersCountTextView.text = viewModel.getPlayersCountText()

        // Mise à jour des joueurs
        playerPreviewAdapter.updatePlayers(state.players)

        // Mise à jour des jeux
        gameAdapter.updateGames(state.availableGames, state.players.size)

        // Gérer l'état des boutons
        managePlayersButton.isEnabled = !state.isLoading
        //manageQuestionsButton.isEnabled = !state.isLoading
    }

    private fun startGame(gameInfo: GameInfo) {
        val intent = Intent(this, GameConfigActivity::class.java)
        intent.putExtra("GAME_INFO", gameInfo)
        startActivity(intent)
    }
}