package com.example.friendlyfire

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.friendlyfire.models.Question
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.PrintWriter

class ManageQuestionsActivity : AppCompatActivity() {

    private val questionsList = mutableListOf<Question>()
    private lateinit var questionsListView: ListView
    private lateinit var addQuestionButton: Button
    private lateinit var questionsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_questions)

        questionsListView = findViewById(R.id.questionRecyclerView)
        addQuestionButton = findViewById(R.id.addQuestionButton)

        // Initialiser l'adaptateur avant d'appeler loadQuestions()
        questionsListView = findViewById(R.id.questionRecyclerView)
        questionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, questionsList.map { it.questionText })
        questionsListView.adapter = questionsAdapter

        // Charger les questions enregistrées depuis un fichier texte
        loadQuestions()
        saveQuestionsToExternal()
        importQuestionsFromRaw()
        //clearQuestionsFile()

        // Bouton pour ajouter une question
        addQuestionButton.setOnClickListener { showAddQuestionDialog() }

        // Suppression d'une question en appuyant longtemps sur un élément
        questionsListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedQuestion = questionsList[position]
            showDeleteQuestionDialog(selectedQuestion, position)
            true
        }
    }

    // Affiche un pop-up pour ajouter une nouvelle question
    private fun showAddQuestionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)

        val editTextQuestion = dialogView.findViewById<EditText>(R.id.editTextQuestion)
        val spinnerPenaltyPoints = dialogView.findViewById<Spinner>(R.id.spinnerPenaltyPoints)

        // Configurer le Spinner
        val penaltyOptions = (1..10).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, penaltyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPenaltyPoints.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ajouter une question")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val questionText = editTextQuestion.text.toString()
                val penaltyPoints = spinnerPenaltyPoints.selectedItem as Int

                if (questionText.isNotEmpty()) {
                    val newQuestion = Question(questionText, penaltyPoints)
                    questionsList.add(newQuestion)
                    questionsAdapter.clear()
                    questionsAdapter.addAll(questionsList.map { it.questionText })
                    questionsAdapter.notifyDataSetChanged()
                    saveQuestions()
                } else {
                    Toast.makeText(this, "Veuillez entrer une question", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()
    }

    // Affiche un pop-up pour confirmer la suppression d'une question
    private fun showDeleteQuestionDialog(question: Question, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer une question")
            .setMessage("Voulez-vous supprimer cette question ?\n\n${question.questionText}")
            .setPositiveButton("Supprimer") { _, _ ->
                questionsList.removeAt(position)
                saveQuestions()
                refreshQuestionsList()
            }
            .setNegativeButton("Annuler", null)
            .create()
            .show()
    }

    // Met à jour l'affichage des questions
    private fun refreshQuestionsList() {
        questionsAdapter.clear()
        questionsAdapter.addAll(questionsList.map { it.questionText })
        questionsAdapter.notifyDataSetChanged()
    }

    private fun saveQuestions() {
        val file = File(filesDir, "questions.txt")
        PrintWriter(file).use { writer ->
            questionsList.forEach { question ->
                writer.println("${question.questionText}|${question.penalties}")
            }
        }
    }
    private fun saveQuestionsToExternal() {
        val file = File(getExternalFilesDir(null), "questions.txt")
        PrintWriter(file).use { writer ->
            questionsList.forEach { question ->
                writer.println("${question.questionText}|${question.penalties}")
            }
        }
    }


    // Charger les questions depuis le fichier texte
    private fun loadQuestions() {
        val file = File(filesDir, "questions.txt")
        if (file.exists()) {
            try {
                val bufferedReader = file.bufferedReader()
                bufferedReader.forEachLine { line ->
                    // Ignore les lignes vides
                    if (line.isNotBlank()) {
                        // Diviser la ligne par le séparateur '|'
                        val parts = line.split("|")
                        if (parts.size == 2) {
                            val questionText = parts[0].trim()
                            val penalties = parts[1].toIntOrNull() ?: 0
                            // Ajouter la question à la liste
                            questionsList.add(Question(questionText, penalties))
                        }
                    }
                }
                // Rafraîchir la liste des questions après le chargement
                refreshQuestionsList()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }


    private fun importQuestionsFromRaw() {
        try {
            val inputStream = resources.openRawResource(R.raw.new_questions)
            val reader = BufferedReader(InputStreamReader(inputStream))

            val newQuestions = mutableListOf<Question>()

            reader.forEachLine { line ->
                if (line.isNotEmpty()) {
                    // Séparer la ligne en deux parties : la question et les pénalités
                    val parts = line.split("|")
                    if (parts.size == 2) {
                        val questionText = parts[0].trim()
                        val penalties = parts[1].trim().toIntOrNull() ?: 0
                        newQuestions.add(Question(questionText, penalties))
                    } else {

                    }
                }
            }
            questionsList.addAll(newQuestions)
            saveQuestions()
            refreshQuestionsList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun clearQuestionsFile() {
        val file = File(filesDir, "questions.txt")
        if (file.exists()) {
            PrintWriter(file).use { writer ->
                writer.print("") // Écrire une chaîne vide pour vider le fichier
            }
            Toast.makeText(this, "Le fichier questions.txt a été vidé.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Le fichier questions.txt n'existe pas.", Toast.LENGTH_SHORT).show()
        }
    }

}