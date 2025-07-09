package com.example.friendlyfire

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.friendlyfire.models.Player
import com.example.friendlyfire.models.Question
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val players = mutableListOf<Player>() // Liste des joueurs
    private var currentTurn = 0 // Tour actuel
    private var totalTurns = 2 // Nombre total de tours
    private lateinit var currentPlayer: Player // Joueur actuel
    private lateinit var currentQuestion: Question // Question actuelle
    private lateinit var turnTextView: TextView
    private lateinit var showQuestionButton: Button
    private lateinit var questionTextView: TextView
    private lateinit var playersLayout: LinearLayout
    private lateinit var validateButton: Button
    private lateinit var statsTextView: TextView // TextView pour afficher les statistiques
    private lateinit var turnCounterTextView: TextView
    private lateinit var quitButton: Button



    private val availableQuestions = mutableListOf<Question>() // Liste des questions disponibles




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        totalTurns = intent.getIntExtra("TOTAL_TURNS", 10) // 10 est la valeur par défaut si rien n'est passé

        loadQuestions()

        turnTextView = findViewById(R.id.turnTextView)
        showQuestionButton = findViewById(R.id.showQuestionButton)
        questionTextView = findViewById(R.id.questionTextView)
        playersLayout = findViewById(R.id.playersLayout)
        validateButton = findViewById(R.id.validateButton)
        turnCounterTextView = findViewById(R.id.turnCounterTextView)
        statsTextView = findViewById(R.id.statsTextView) // Initialisation de la TextView pour les stats


        turnCounterTextView.text = "Tour ${currentTurn + 1}/$totalTurns"

        quitButton = findViewById(R.id.quitButton)  // Assure-toi que ce bouton est bien dans ton layout
        quitButton.visibility = View.VISIBLE


        quitButton.setOnClickListener {
            showQuitDialog()
        }

        // Récupérer la liste des joueurs depuis le fichier
        players.clear()
        players.addAll(loadPlayers())

        // Vérifier si des joueurs existent
        if (players.isEmpty()) {
            turnTextView.text = "Aucun joueur enregistré. Veuillez ajouter des joueurs."
            showQuestionButton.visibility = View.GONE
            return
        }

        // Initialiser les pénalités pour chaque joueur


        // Choisir un joueur au hasard pour commencer
        currentPlayer = players.random()
        turnTextView.text = "${currentPlayer.name} commence !"

        // Configurer l'interface pour commencer
        questionTextView.visibility = View.INVISIBLE
        playersLayout.visibility = View.INVISIBLE
        validateButton.visibility = View.INVISIBLE

        // Bouton "Afficher la question"
        showQuestionButton.setOnClickListener {
            showQuestionButton.visibility = View.INVISIBLE // Cacher le bouton

            val question = generateRandomQuestion(currentPlayer)
            if (question == null) {
                Toast.makeText(this, "Toutes les questions ont été utilisées !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentQuestion = question
            questionTextView.text = "${currentQuestion.questionText} (Pénalité: ${currentQuestion.penalties})"
            questionTextView.visibility = View.VISIBLE

            showPlayersForSelection(playersLayout)
            validateButton.visibility = View.VISIBLE
        }


        // Bouton "Valider"
        validateButton.setOnClickListener {
            val selectedPlayer = getSelectedPlayer(playersLayout)
            if (selectedPlayer != null) {
                Toast.makeText(this, "Question attribuée à ${selectedPlayer.name} !", Toast.LENGTH_SHORT).show()

                // Ajouter la question à la liste des questions du joueur sélectionné
                selectedPlayer.questions.add(currentQuestion)

                showQuestionButton.visibility = View.GONE


                // Le joueur sélectionné devient celui qui joue ensuite
                currentPlayer = selectedPlayer

                // Masquer les éléments de l'interface et préparer le pile ou face
                questionTextView.visibility = View.INVISIBLE
                playersLayout.visibility = View.INVISIBLE
                validateButton.visibility = View.INVISIBLE

                // Afficher l'étape de pile ou face
                showTossCoinPhase()
            } else {
                Toast.makeText(this, "Veuillez sélectionner un joueur !", Toast.LENGTH_SHORT).show()
            }
        }


    }

    // Charger les joueurs depuis un fichier texte
    private fun loadPlayers(): List<Player> {
        val playersList = mutableListOf<Player>()
        val file = File(filesDir, "players.txt")
        if (file.exists()) {
            file.forEachLine { line ->
                playersList.add(Player(line))  // Créer un objet Player pour chaque nom
            }
        }
        return playersList
    }

    // Charger les questions depuis un fichier texte
    private fun loadQuestions() {
        val questionsList = mutableListOf<Question>()
        val file = File(filesDir, "questions.txt")
        if (file.exists()) {
            file.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val questionText = parts[0]
                    val penalties = parts[1].toIntOrNull() ?: 0
                    questionsList.add(Question(questionText, penalties))
                }
            }
        }
        availableQuestions.clear()
        availableQuestions.addAll(questionsList)

    }




    // Générer une question aléatoire à partir des questions enregistrées
    private fun generateRandomQuestion(player: Player): Question {
        if (availableQuestions.isEmpty()) {
            // Si plus de questions disponibles
            Toast.makeText(this, "Aucune question disponible !", Toast.LENGTH_SHORT).show()
            return Question("Fin des questions", 0)
        }
        val randomQuestion = availableQuestions.random()
        availableQuestions.remove(randomQuestion)
        return randomQuestion.copy(player = player)

    }





    private fun showPlayersForSelection(layout: LinearLayout) {
        layout.removeAllViews() // Réinitialiser les vues

        var selectedCheckBox: CheckBox? = null // Stocke la case actuellement sélectionnée

        players.forEach { player ->
            val checkBox = CheckBox(this).apply {
                text = player.name
                id = View.generateViewId()

                // Initialiser la couleur de fond
                setBackgroundColor(resources.getColor(android.R.color.transparent))

                // Gestion de la sélection exclusive
                setOnClickListener {
                    // Si cette case est déjà sélectionnée, la désélectionner
                    if (selectedCheckBox == this) {
                        isChecked = false
                        setBackgroundColor(resources.getColor(android.R.color.transparent))
                        selectedCheckBox = null
                    } else {
                        // Sinon, désélectionner l'ancienne case et sélectionner celle-ci
                        selectedCheckBox?.apply {
                            isChecked = false
                            setBackgroundColor(resources.getColor(android.R.color.transparent))
                        }
                        isChecked = true
                        setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                        selectedCheckBox = this
                    }
                }
            }

            // Ajouter la CheckBox au layout
            layout.addView(checkBox)
        }

        layout.visibility = View.VISIBLE
    }

    private fun getSelectedPlayer(layout: LinearLayout): Player? {
        // Récupérer la CheckBox sélectionnée
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                return players.find { it.name == view.text.toString() }
            }
        }
        return null
    }




    private fun showStats() {
        // Afficher le bouton "Rejouer"
        val replayButton: Button = findViewById(R.id.replayButton)
        replayButton.visibility = View.VISIBLE

        // Associer le bouton "Rejouer" à la réinitialisation du jeu
        replayButton.setOnClickListener {
            resetGame()
        }

        // Construire les statistiques
        val stats = StringBuilder("Stats de la partie:\n")
        players.forEach { player ->
            stats.append("${player.name}:\n")

            if (player.questions.isNotEmpty()) {
                player.questions.forEach { question ->
                    val playerWhoAssigned = question.player?.name ?: "Joueur inconnu"
                    stats.append("- Question: ${question.questionText} (Pénalité: ${question.penalties}), Attribuée par: $playerWhoAssigned\n")
                }
            } else {
                stats.append("- Aucune question attribuée.\n")
            }

            stats.append("Total des pénalités: ${player.totalPenalties()}\n\n")
        }

        // Afficher les statistiques
        val statsTextView: TextView = findViewById(R.id.statsTextView)
        statsTextView.text = stats.toString()
        statsTextView.visibility = View.VISIBLE
    }





    private fun endGame() {
        // Masquer les éléments du jeu
        showQuestionButton.visibility = View.GONE
        questionTextView.visibility = View.GONE
        playersLayout.visibility = View.GONE
        validateButton.visibility = View.GONE

        findViewById<LinearLayout>(R.id.mainLayout).removeAllViews()

        turnTextView.text = "Fin de partie ! Merci d'avoir joué !"
        showStats()
    }


    private fun proceedToNextTurn() {
        currentTurn++
        if (currentTurn >= totalTurns) {
            endGame()
        } else {

            turnCounterTextView.text = "Tour ${currentTurn + 1}/$totalTurns"
            showQuestionButton.visibility = View.VISIBLE


            turnTextView.text = "C'est à ${currentPlayer.name} de jouer !"
            questionTextView.visibility = View.INVISIBLE
            playersLayout.visibility = View.INVISIBLE
            validateButton.visibility = View.INVISIBLE
            findViewById<LinearLayout>(R.id.mainLayout).removeAllViews() // Nettoyer les boutons
            showQuestionButton.visibility = View.VISIBLE // Réactiver le bouton pour afficher une question
        }
    }


    private fun showTossCoinPhase() {
        turnTextView.text = "${currentPlayer.name}, lancez la pièce !"

        // Bouton pour lancer la pièce
        val tossCoinButton = Button(this).apply {
            text = "Lancer la pièce"
            setOnClickListener {
                val result = if (Random.nextBoolean()) "face" else "pile"
                handleTossCoinResult(result)
            }
        }

        // Ajouter le bouton dynamiquement
        findViewById<LinearLayout>(R.id.mainLayout).apply {
            removeAllViews() // Supprimer les anciens boutons
            addView(tossCoinButton)
        }
    }

    private fun handleTossCoinResult(result: String) {
        // Supprimer le bouton de la pièce
        findViewById<LinearLayout>(R.id.mainLayout).removeAllViews()

        if (result == "face") {
            turnTextView.text = "Face ! Voici la question : ${currentQuestion.questionText}"
            val nextButton = Button(this).apply {
                text = "Tour suivant"
                setOnClickListener {
                    proceedToNextTurn()
                }
            }
            findViewById<LinearLayout>(R.id.mainLayout).addView(nextButton)
        } else {
            turnTextView.text = "Pile ! ${currentPlayer.name} doit prendre ${currentQuestion.penalties} pénalités."
            currentPlayer.penalties += currentQuestion.penalties
            val nextButton = Button(this).apply {
                text = "Tour suivant"
                setOnClickListener {
                    proceedToNextTurn()
                }
            }
            findViewById<LinearLayout>(R.id.mainLayout).addView(nextButton)
        }

    }

    private fun resetGame() {
        players.forEach { player ->
            player.penalties = 0
            player.questions.clear()

        }
        loadQuestions()
        currentTurn = 0
        val intent = intent
        finish()
        startActivity(intent)
    }



    private fun showQuitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Voulez-vous quitter le jeu ?")
            .setCancelable(false)
            .setPositiveButton("Oui") { _, _ ->
                // Code pour retourner à l'écran d'accueil
                val intent = Intent(this, WelcomeActivity::class.java)  // Remplace AccueilActivity par ton activité d'accueil
                startActivity(intent)
                finish()  // Fin de l'activité actuelle pour revenir à l'accueil
            }
            .setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()  // Ferme le dialog sans rien faire
            }

        val dialog = builder.create()
        dialog.show()
    }


    override fun onBackPressed() {
        showQuitDialog()  // Affiche le dialog de confirmation si le bouton retour est pressé
    }

}