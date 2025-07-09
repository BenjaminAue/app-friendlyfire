package com.example.friendlyfire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import com.example.friendlyfire.MainActivity

import androidx.appcompat.app.AlertDialog
import com.example.friendlyfire.models.Question
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter


class WelcomeActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Boutons
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


    private fun showTurnSelectionDialog() {
        val roundsOptions = arrayOf("2","5","10", "20", "30", "40", "50")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choisissez le nombre de tours")
            .setItems(roundsOptions) { dialog, which ->
                val selectedRounds = roundsOptions[which].toInt()

                // Passer à MainActivity avec le nombre de tours sélectionné
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("TOTAL_TURNS", selectedRounds)
                startActivity(intent)
            }
            .create()
            .show()
    }
}