// Fichier: app/src/main/java/com/example/friendlyfire/MainActivity.kt

package com.example.friendlyfire

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.friendlyfire.models.Player
import com.example.friendlyfire.ui.main.GamePhase
import com.example.friendlyfire.ui.main.MainViewModel
import com.example.friendlyfire.ui.main.CoinResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Views
    private lateinit var turnTextView: TextView
    private lateinit var turnCounterTextView: TextView
    private lateinit var showQuestionButton: Button
    private lateinit var questionTextView: TextView
    private lateinit var playersLayout: LinearLayout
    private lateinit var validateButton: Button
    private lateinit var statsTextView: TextView
    private lateinit var quitButton: Button
    private lateinit var mainLayout: LinearLayout
    private lateinit var replayButton: Button

    private var selectedCheckBox: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        observeViewModel()

        // Initialiser le jeu avec le nombre de tours
        val totalTurns = intent.getIntExtra("TOTAL_TURNS", 10)
        viewModel.initializeGame(totalTurns)
    }

    private fun initializeViews() {
        turnTextView = findViewById(R.id.turnTextView)
        turnCounterTextView = findViewById(R.id.turnCounterTextView)
        showQuestionButton = findViewById(R.id.showQuestionButton)
        questionTextView = findViewById(R.id.questionTextView)
        playersLayout = findViewById(R.id.playersLayout)
        validateButton = findViewById(R.id.validateButton)
        statsTextView = findViewById(R.id.statsTextView)
        quitButton = findViewById(R.id.quitButton)
        mainLayout = findViewById(R.id.mainLayout)
        replayButton = findViewById(R.id.replayButton)
    }

    private fun setupClickListeners() {
        showQuestionButton.setOnClickListener {
            viewModel.showQuestion()
        }

        validateButton.setOnClickListener {
            viewModel.validatePlayerSelection()
        }

        quitButton.setOnClickListener {
            showQuitDialog()
        }

        replayButton.setOnClickListener {
            viewModel.resetGame()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.example.friendlyfire.ui.main.MainViewState) {
        // Gestion du loading
        if (state.isLoading) {
            showLoadingState()
            return
        }

        // Gestion des erreurs
        state.error?.let { error ->
            showErrorState(error)
            return
        }

        // Mise à jour des textes
        turnTextView.text = viewModel.getPlayerTurnText()
        turnCounterTextView.text = viewModel.getCurrentTurnText()

        // Gestion des phases de jeu
        when (state.gamePhase) {
            GamePhase.SETUP -> showSetupPhase()
            GamePhase.WAITING_QUESTION -> showWaitingQuestionPhase()
            GamePhase.QUESTION_SHOWN -> showQuestionShownPhase(state)
            GamePhase.COIN_TOSS -> showCoinTossPhase(state)
            GamePhase.GAME_OVER -> showGameOverPhase(state)
        }
    }

    private fun showLoadingState() {
        turnTextView.text = "Chargement..."
        showQuestionButton.visibility = View.GONE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE
        mainLayout.removeAllViews()
    }

    private fun showErrorState(error: String) {
        turnTextView.text = error
        showQuestionButton.visibility = View.GONE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE
        mainLayout.removeAllViews()

        if (error.contains("joueur")) {
            // Ajouter un bouton pour aller à la gestion des joueurs
            val goToPlayersButton = Button(this).apply {
                text = "Gérer les joueurs"
                setOnClickListener {
                    finish() // Retourner à l'activité précédente
                }
            }
            mainLayout.addView(goToPlayersButton)
        }
    }

    private fun showSetupPhase() {
        showQuestionButton.visibility = View.GONE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE
        mainLayout.removeAllViews()
    }

    private fun showWaitingQuestionPhase() {
        showQuestionButton.visibility = View.VISIBLE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE
        mainLayout.removeAllViews()
        replayButton.visibility = View.GONE
        statsTextView.visibility = View.GONE
    }

    private fun showQuestionShownPhase(state: com.example.friendlyfire.ui.main.MainViewState) {
        showQuestionButton.visibility = View.GONE
        questionTextView.text = viewModel.getQuestionText()
        questionTextView.visibility = View.VISIBLE

        setupPlayerSelection(state.players)
        playersLayout.visibility = View.VISIBLE
        validateButton.visibility = View.VISIBLE
        mainLayout.removeAllViews()
    }

    private fun showCoinTossPhase(state: com.example.friendlyfire.ui.main.MainViewState) {
        showQuestionButton.visibility = View.GONE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE

        mainLayout.removeAllViews()

        if (state.coinResult == null) {
            // Afficher le texte pour le joueur qui va lancer la pièce
            val selectedPlayer = state.selectedPlayer?.name ?: "Joueur"
            val instructionText = TextView(this).apply {
                text = "$selectedPlayer, lance la pièce !"
                textSize = 18f
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
            }
            mainLayout.addView(instructionText)

            // Afficher le bouton pour lancer la pièce
            val tossCoinButton = Button(this).apply {
                text = "Lancer la pièce"
                setOnClickListener {
                    viewModel.tossCoin()
                }
            }
            mainLayout.addView(tossCoinButton)
        } else {
            // Afficher le résultat
            val resultText = TextView(this).apply {
                text = viewModel.getCoinResultText()
                textSize = 16f
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
            }
            mainLayout.addView(resultText)

            val nextTurnButton = Button(this).apply {
                text = "Tour suivant"
                setOnClickListener {
                    viewModel.nextTurn()
                }
            }
            mainLayout.addView(nextTurnButton)
        }
    }

    private fun showGameOverPhase(state: com.example.friendlyfire.ui.main.MainViewState) {
        showQuestionButton.visibility = View.GONE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE
        mainLayout.removeAllViews()

        // Afficher les statistiques
        statsTextView.text = viewModel.getGameStatsText()
        statsTextView.visibility = View.VISIBLE

        // Afficher le bouton rejouer
        replayButton.visibility = View.VISIBLE
    }

    private fun setupPlayerSelection(players: List<Player>) {
        playersLayout.removeAllViews()
        selectedCheckBox = null

        players.forEach { player ->
            val checkBox = CheckBox(this).apply {
                text = player.name
                id = View.generateViewId()
                setBackgroundColor(resources.getColor(android.R.color.transparent))

                setOnClickListener {
                    handlePlayerSelection(this, player)
                }
            }
            playersLayout.addView(checkBox)
        }
    }

    private fun handlePlayerSelection(checkBox: CheckBox, player: Player) {
        if (selectedCheckBox == checkBox) {
            // Désélectionner
            checkBox.isChecked = false
            checkBox.setBackgroundColor(resources.getColor(android.R.color.transparent))
            selectedCheckBox = null
            viewModel.selectPlayer(null as Player?)
        } else {
            // Désélectionner l'ancien
            selectedCheckBox?.apply {
                isChecked = false
                setBackgroundColor(resources.getColor(android.R.color.transparent))
            }

            // Sélectionner le nouveau
            checkBox.isChecked = true
            checkBox.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            selectedCheckBox = checkBox
            viewModel.selectPlayer(player)
        }
    }

    private fun showQuitDialog() {
        AlertDialog.Builder(this)
            .setMessage("Voulez-vous quitter le jeu ?")
            .setCancelable(false)
            .setPositiveButton("Oui") { _, _ ->
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showQuitDialog()
    }


}