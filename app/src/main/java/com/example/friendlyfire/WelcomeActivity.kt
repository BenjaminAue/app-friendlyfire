// ===== 2. WelcomeActivity refactorisée =====
// Fichier: app/src/main/java/com/example/friendlyfire/WelcomeActivity.kt

package com.example.friendlyfire

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.friendlyfire.ui.welcome.WelcomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WelcomeActivity : AppCompatActivity() {

    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        val startGameButton = findViewById<Button>(R.id.startGameButton)
        val managePlayersButton = findViewById<Button>(R.id.managePlayersButton)
        val manageQuestionsButton = findViewById<Button>(R.id.manageQuestionsButton)

        startGameButton.setOnClickListener {
            showTurnSelectionDialog()
        }

        managePlayersButton.setOnClickListener {
            val intent = Intent(this, ManagePlayersActivity::class.java)
            startActivity(intent)
        }

        manageQuestionsButton.setOnClickListener {
            val intent = Intent(this, ManageQuestionsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                // Mettre à jour l'UI si nécessaire
                // Par exemple, afficher le nombre de joueurs

                state.selectedTurns?.let { turns ->
                    val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
                    intent.putExtra("TOTAL_TURNS", turns)
                    startActivity(intent)
                }
            }
        }
    }

    private fun showTurnSelectionDialog() {
        val roundsOptions = arrayOf("2", "5", "10", "20", "30", "40", "50")

        AlertDialog.Builder(this)
            .setTitle("Choisissez le nombre de tours")
            .setItems(roundsOptions) { _, which ->
                val selectedRounds = roundsOptions[which].toInt()
                viewModel.startGame(selectedRounds)
            }
            .create()
            .show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    // Ajoutez un bouton retour dans WelcomeActivity si nécessaire
    override fun onBackPressed() {
        navigateToHome()
    }
}