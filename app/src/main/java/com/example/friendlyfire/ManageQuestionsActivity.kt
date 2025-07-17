// Fichier: app/src/main/java/com/example/friendlyfire/ManageQuestionsActivity.kt

package com.example.friendlyfire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.adapters.CustomQuestionAdapter
import com.example.friendlyfire.models.Question
import com.example.friendlyfire.ui.questions.CustomQuestionsViewModel
import com.example.friendlyfire.ui.common.SecureInputTextWatcher
import com.example.friendlyfire.ui.common.addSecureValidation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageQuestionsActivity : AppCompatActivity() {

    private val viewModel: CustomQuestionsViewModel by viewModels()

    private lateinit var questionsRecyclerView: RecyclerView
    private lateinit var addQuestionButton: Button
    private lateinit var questionsCountTextView: TextView
    private lateinit var backButton: Button
    private lateinit var gameNameTextView: TextView
    private lateinit var customQuestionAdapter: CustomQuestionAdapter

    private var gameId: String = "friendly_fire"
    private var gameName: String = "Friendly Fire"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_questions)

        // Récupérer les infos du jeu depuis l'intent (si appelé depuis GameConfig)
        gameId = intent.getStringExtra("GAME_ID") ?: "friendly_fire"
        gameName = intent.getStringExtra("GAME_NAME") ?: "Friendly Fire"

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Initialiser pour ce jeu spécifique
        viewModel.initializeForGame(gameId)
    }

    private fun initializeViews() {
        questionsRecyclerView = findViewById(R.id.questionRecyclerView)
        addQuestionButton = findViewById(R.id.addQuestionButton)
        questionsCountTextView = findViewById(R.id.questionsCountTextView)
        backButton = findViewById(R.id.backButton)
        gameNameTextView = findViewById(R.id.gameNameTextView)

        // Personnaliser l'interface pour le jeu
        gameNameTextView.text = "Questions pour $gameName"
        addQuestionButton.text = "➕ Ajouter une question personnalisée"
    }

    private fun setupRecyclerView() {
        customQuestionAdapter = CustomQuestionAdapter(
            questions = emptyList(),
            onDeleteClick = { question -> showDeleteConfirmation(question) },
            onEditClick = { question -> showEditQuestionDialog(question) }
        )
        questionsRecyclerView.adapter = customQuestionAdapter
        questionsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        addQuestionButton.setOnClickListener {
            showAddQuestionDialog()
        }

        backButton.setOnClickListener {
            val intent = Intent(this, GameConfigActivity::class.java)
            intent.putExtra("GAME_INFO", createGameInfoFromId(gameId, gameName))
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun createGameInfoFromId(gameId: String, gameName: String): com.example.friendlyfire.ui.home.GameInfo {
        return com.example.friendlyfire.ui.home.GameInfo(
            id = gameId,
            name = gameName,
            description = "Jeu de questions entre amis",
            minPlayers = 2,
            maxPlayers = 10,
            isAvailable = true
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun showAddQuestionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        val questionEditText = dialogView.findViewById<EditText>(R.id.questionEditText)
        val penaltySpinner = dialogView.findViewById<Spinner>(R.id.penaltySpinner)

        // Variables pour validation
        var isQuestionValid = true
        var questionValidationError: String? = null

        // Configurer le spinner
        setupPenaltySpinner(penaltySpinner)

        // Placeholder spécifique au jeu
        questionEditText.hint = when (gameId) {
            "friendly_fire" -> "Ex: Qui a le plus de chance de devenir célèbre ?"
            else -> "Entrez votre question..."
        }

        // Ajouter validation sécurisée en temps réel pour la question
        questionEditText.addSecureValidation(
            SecureInputTextWatcher.InputType.QUESTION_TEXT
        ) { isValid, errorMessage ->
            isQuestionValid = isValid
            questionValidationError = errorMessage
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("✨ Nouvelle question pour $gameName")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val questionText = questionEditText.text.toString().trim()
                val penalties = (penaltySpinner.selectedItem as Int)

                // Validation finale
                if (!isQuestionValid) {
                    Toast.makeText(this, questionValidationError ?: "Question invalide", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (questionText.isEmpty()) {
                    Toast.makeText(this, "Veuillez entrer une question", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (questionText.length < 10) {
                    Toast.makeText(this, "La question doit contenir au moins 10 caractères", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addQuestionForGame(gameId, questionText, penalties)
                Toast.makeText(this, "Question ajoutée !", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()

        // Gérer l'état du bouton OK
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { okButton ->
            questionEditText.addSecureValidation(
                SecureInputTextWatcher.InputType.QUESTION_TEXT
            ) { isValid, _ ->
                val text = questionEditText.text.toString().trim()
                okButton.isEnabled = isValid && text.isNotEmpty() && text.length >= 10
            }
        }
    }

    private fun showEditQuestionDialog(question: Question) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        val questionEditText = dialogView.findViewById<EditText>(R.id.questionEditText)
        val penaltySpinner = dialogView.findViewById<Spinner>(R.id.penaltySpinner)

        // Variables pour validation
        var isQuestionValid = true
        var questionValidationError: String? = null

        // Pré-remplir avec les valeurs actuelles
        questionEditText.setText(question.questionText)
        setupPenaltySpinner(penaltySpinner)

        // Sélectionner la pénalité actuelle
        val penaltyPosition = (question.penalties - 1).coerceIn(0, 9)
        penaltySpinner.setSelection(penaltyPosition)

        // Ajouter validation sécurisée
        questionEditText.addSecureValidation(
            SecureInputTextWatcher.InputType.QUESTION_TEXT
        ) { isValid, errorMessage ->
            isQuestionValid = isValid
            questionValidationError = errorMessage
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("✏️ Modifier la question")
            .setView(dialogView)
            .setPositiveButton("Modifier") { _, _ ->
                val questionText = questionEditText.text.toString().trim()
                val penalties = (penaltySpinner.selectedItem as Int)

                // Validation finale
                if (!isQuestionValid) {
                    Toast.makeText(this, questionValidationError ?: "Question invalide", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (questionText.isEmpty()) {
                    Toast.makeText(this, "Veuillez entrer une question", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (questionText.length < 10) {
                    Toast.makeText(this, "La question doit contenir au moins 10 caractères", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.updateQuestion(question, questionText, penalties)
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()

        // Gérer l'état du bouton OK
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { okButton ->
            questionEditText.addSecureValidation(
                SecureInputTextWatcher.InputType.QUESTION_TEXT
            ) { isValid, _ ->
                val text = questionEditText.text.toString().trim()
                okButton.isEnabled = isValid && text.isNotEmpty() && text.length >= 10
            }
        }
    }

    private fun showDeleteConfirmation(question: Question) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Supprimer la question")
            .setMessage("Voulez-vous vraiment supprimer cette question ?\n\n\"${question.questionText}\"")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteQuestion(question)
            }
            .setNegativeButton("Annuler", null)
            .create()
            .show()
    }

    private fun setupPenaltySpinner(spinner: Spinner) {
        val penalties = (1..10).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, penalties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(2) // Par défaut 3 pénalités
    }

    private fun updateUI(state: com.example.friendlyfire.ui.questions.CustomQuestionsViewState) {
        // Mettre à jour la liste
        customQuestionAdapter.updateQuestions(state.customQuestions)
        customQuestionAdapter.notifyDataSetChanged()

        // Mettre à jour le compteur
        val count = state.customQuestions.size
        questionsCountTextView.text = when (count) {
            0 -> "🎯 Aucune question personnalisée"
            1 -> "🎯 1 question personnalisée"
            else -> "🎯 $count questions personnalisées"
        }

        // Gérer les erreurs
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }

        // Gérer les messages de succès
        state.successMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.initializeForGame(gameId)
    }
}