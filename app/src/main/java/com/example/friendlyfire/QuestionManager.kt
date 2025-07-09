package com.example.friendlyfire

import com.example.friendlyfire.models.Question
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

object QuestionManager {

    // Liste des questions partagées dans toute l'application
    val questionsList = mutableListOf<Question>()

    // Charger les questions à partir d'un fichier externe
    fun loadQuestionsFromExternal(file: File) {
        if (file.exists()) {
            val loadedQuestions = mutableListOf<Question>()
            file.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val questionText = parts[0].trim()
                    val penalties = parts[1].trim().toIntOrNull() ?: 0
                    loadedQuestions.add(Question(questionText, penalties))
                }
            }
            questionsList.clear()
            questionsList.addAll(loadedQuestions)
        }
    }

    // Importer les questions depuis les ressources RAW
    fun importQuestionsFromRaw(context: android.content.Context) {
        try {
            val inputStream = context.resources.openRawResource(R.raw.new_questions)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val newQuestions = mutableListOf<Question>()

            reader.forEachLine { line ->
                if (line.isNotEmpty()) {
                    val parts = line.split("|")
                    if (parts.size == 2) {
                        val questionText = parts[0].trim()
                        val penalties = parts[1].trim().toIntOrNull() ?: 0
                        newQuestions.add(Question(questionText, penalties))
                    }
                }
            }
            questionsList.clear()
            questionsList.addAll(newQuestions)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sauvegarder les questions dans un fichier externe
    fun saveQuestionsToExternal(file: File) {
        PrintWriter(file).use { writer ->
            questionsList.forEach { question ->
                writer.println("${question.questionText}|${question.penalties}")
            }
        }
    }
}
