// ===== 3. GameAdapter =====
// Fichier: app/src/main/java/com/example/friendlyfire/adapters/GameAdapter.kt

package com.example.friendlyfire.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.R
import com.example.friendlyfire.ui.home.GameInfo

class GameAdapter(
    private var games: List<GameInfo>,
    private val onGameClick: (GameInfo) -> Unit,
    private val onPlayClick: (GameInfo) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    private var playerCount: Int = 0

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameNameTextView: TextView = view.findViewById(R.id.gameNameTextView)
        val gameDescriptionTextView: TextView = view.findViewById(R.id.gameDescriptionTextView)
        val gamePlayersTextView: TextView = view.findViewById(R.id.gamePlayersTextView)
        val gameStatusTextView: TextView = view.findViewById(R.id.gameStatusTextView)
        val playButton: Button = view.findViewById(R.id.playButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]

        holder.gameNameTextView.text = game.name
        holder.gameDescriptionTextView.text = game.description
        holder.gamePlayersTextView.text = game.getPlayersRangeText()

        // Statut du jeu
        val canPlay = game.isAvailable &&
                playerCount >= game.minPlayers &&
                playerCount <= game.maxPlayers

        holder.gameStatusTextView.text = when {
            !game.isAvailable -> "Bientôt disponible"
            playerCount < game.minPlayers -> "Minimum ${game.minPlayers} joueurs requis"
            playerCount > game.maxPlayers -> "Maximum ${game.maxPlayers} joueurs"
            else -> "Prêt à jouer !"
        }

        // Couleur du statut
        val statusColor = if (canPlay) {
            holder.itemView.context.getColor(android.R.color.holo_green_dark)
        } else {
            holder.itemView.context.getColor(android.R.color.holo_red_dark)
        }
        holder.gameStatusTextView.setTextColor(statusColor)

        // Bouton play
        holder.playButton.isEnabled = canPlay
        holder.playButton.text = if (canPlay) "Jouer" else "Indisponible"

        // Click listeners
        holder.itemView.setOnClickListener { onGameClick(game) }
        holder.playButton.setOnClickListener {
            if (canPlay) onPlayClick(game)
        }
    }

    override fun getItemCount() = games.size

    fun updateGames(newGames: List<GameInfo>, newPlayerCount: Int) {
        games = newGames
        playerCount = newPlayerCount
        notifyDataSetChanged()
    }
}

// Extension pour GameInfo (si pas déjà créée)
private fun GameInfo.getPlayersRangeText(): String {
    return if (minPlayers == maxPlayers) {
        "$minPlayers joueurs"
    } else {
        "$minPlayers-$maxPlayers joueurs"
    }
}