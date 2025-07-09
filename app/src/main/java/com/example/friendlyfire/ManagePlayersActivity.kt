package com.example.friendlyfire

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.adapters.PlayerAdapter
import com.example.friendlyfire.models.Player
import java.io.File

class ManagePlayersActivity : AppCompatActivity() {

    private lateinit var playersRecyclerView: RecyclerView
    private lateinit var addPlayerButton: Button
    private val players = mutableListOf<Player>()  // Liste des joueurs
    private lateinit var playerAdapter: PlayerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_players)

        // Initialisation des vues
        playersRecyclerView = findViewById(R.id.playersRecyclerView)
        addPlayerButton = findViewById(R.id.addPlayerButton)

        // Charger les joueurs enregistrés localement
        loadPlayers()

        // Configurer le RecyclerView
        playerAdapter = PlayerAdapter(players) { player -> removePlayer(player) }
        playersRecyclerView.layoutManager = LinearLayoutManager(this)
        playersRecyclerView.adapter = playerAdapter

        // Bouton pour ajouter un joueur
        addPlayerButton.setOnClickListener {
            showAddPlayerDialog()
        }
    }

    // Afficher un pop-up pour ajouter un joueur
    private fun showAddPlayerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_player, null)
        val playerNameEditText = dialogView.findViewById<EditText>(R.id.playerNameEditText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ajouter un joueur")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val playerName = playerNameEditText.text.toString()
                if (playerName.isNotBlank()) {
                    addPlayer(playerName)
                } else {
                    Toast.makeText(this, "Le nom du joueur ne peut pas être vide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()
    }

    // Ajouter un joueur
    private fun addPlayer(name: String) {
        val newPlayer = Player(name)
        players.add(newPlayer)
        playerAdapter.notifyItemInserted(players.size - 1)  // Mettre à jour la liste
        savePlayers()  // Sauvegarder les joueurs dans un fichier texte
    }

    // Supprimer un joueur
    private fun removePlayer(player: Player) {
        val position = players.indexOf(player)
        if (position != -1) {
            players.removeAt(position)
            playerAdapter.notifyItemRemoved(position)
            savePlayers()  // Sauvegarder les modifications dans le fichier texte
        }
    }

    // Sauvegarder les joueurs dans un fichier texte
    private fun savePlayers() {
        val file = File(filesDir, "players.txt")
        file.printWriter().use { writer ->
            for (player in players) {
                writer.println(player.name)  // Sauvegarder uniquement le nom du joueur
            }
        }
    }

    // Charger les joueurs depuis un fichier texte
    private fun loadPlayers() {
        val file = File(filesDir, "players.txt")
        if (file.exists()) {
            file.forEachLine { line ->
                players.add(Player(line))  // Créer un objet Player pour chaque nom
            }
        }
    }


}
