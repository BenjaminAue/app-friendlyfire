// ===== 3. GameConfigActivity =====
// Fichier: app/src/main/java/com/example/friendlyfire/GameConfigActivity.kt

package com.example.friendlyfire

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.friendlyfire.ui.gameconfig.GameConfigViewModel
import com.example.friendlyfire.ui.gameconfig.GameConfigViewState
import com.example.friendlyfire.ui.gameconfig.QuestionTheme
import com.example.friendlyfire.ui.home.GameInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GameConfigActivity : AppCompatActivity() {

    private val viewModel: GameConfigViewModel by viewModels()

    // Views
    private lateinit var gameNameTextView: TextView
    private lateinit var gameDescriptionTextView: TextView
    private lateinit var rulesTextView: TextView
    private lateinit var turnsSpinner: Spinner
    private lateinit var themeSpinner: Spinner
    private lateinit var configSummaryTextView: TextView
    private lateinit var startGameButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_config)

        // Récupérer les informations du jeu
        val gameInfo = intent.getSerializableExtra("GAME_INFO") as? GameInfo
        if (gameInfo == null) {
            finish()
            return
        }

        initializeViews()
        setupSpinners()
        setupClickListeners()
        observeViewModel()

        // Initialiser avec les informations du jeu
        viewModel.initializeConfig(gameInfo)
    }

    private fun initializeViews() {
        gameNameTextView = findViewById(R.id.gameNameTextView)
        gameDescriptionTextView = findViewById(R.id.gameDescriptionTextView)
        rulesTextView = findViewById(R.id.rulesTextView)
        turnsSpinner = findViewById(R.id.turnsSpinner)
        themeSpinner = findViewById(R.id.themeSpinner)
        configSummaryTextView = findViewById(R.id.configSummaryTextView)
        startGameButton = findViewById(R.id.startGameButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupSpinners() {
        // Spinner pour les tours
        val turnsOptions = viewModel.getTurnsOptions()
        val turnsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, turnsOptions)
        turnsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        turnsSpinner.adapter = turnsAdapter
        turnsSpinner.setSelection(1) // Par défaut 10 tours

        turnsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.selectTurns(turnsOptions[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Spinner pour les thèmes
        val themeOptions = QuestionTheme.values().map { "${it.emoji} ${it.displayName}" }
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeOptions)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.selectTheme(QuestionTheme.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ===== 4. Mise à jour GameConfigActivity =====
// Modification dans setupClickListeners()

    private fun setupClickListeners() {
        startGameButton.setOnClickListener {
            val state = viewModel.viewState.value
            startGame(state)
        }

        backButton.setOnClickListener {
            finish()
        }

        // Bouton pour les questions personnalisées avec passage des paramètres
        val addCustomQuestionsButton = findViewById<Button>(R.id.addCustomQuestionsButton)
        addCustomQuestionsButton.setOnClickListener {
            val gameInfo = viewModel.viewState.value.gameInfo
            if (gameInfo != null) {
                val intent = Intent(this, ManageQuestionsActivity::class.java)
                intent.putExtra("GAME_ID", gameInfo.id)
                intent.putExtra("GAME_NAME", gameInfo.name)
                startActivity(intent)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: GameConfigViewState) {
        state.gameInfo?.let { gameInfo ->
            gameNameTextView.text = gameInfo.name
            gameDescriptionTextView.text = gameInfo.description
            rulesTextView.text = viewModel.getGameRules()
        }

        configSummaryTextView.text = viewModel.getConfigSummary()
        startGameButton.isEnabled = !state.isLoading
    }

    private fun startGame(state: GameConfigViewState) {
        val gameInfo = state.gameInfo ?: return

        when (gameInfo.id) {
            "friendly_fire" -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("TOTAL_TURNS", state.selectedTurns)
                intent.putExtra("GAME_ID", gameInfo.id)
                intent.putExtra("QUESTION_THEME", state.selectedTheme.name)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "Ce jeu n'est pas encore disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }
}