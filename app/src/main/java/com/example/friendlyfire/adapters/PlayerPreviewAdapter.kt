// ===== 2. PlayerPreviewAdapter =====
// Fichier: app/src/main/java/com/example/friendlyfire/adapters/PlayerPreviewAdapter.kt

package com.example.friendlyfire.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.R
import com.example.friendlyfire.models.Player

class PlayerPreviewAdapter(
    private var players: List<Player>
) : RecyclerView.Adapter<PlayerPreviewAdapter.PlayerPreviewViewHolder>() {

    class PlayerPreviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerNameTextView: TextView = view.findViewById(R.id.playerNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerPreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_preview, parent, false)
        return PlayerPreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerPreviewViewHolder, position: Int) {
        val player = players[position]
        holder.playerNameTextView.text = player.name
    }

    override fun getItemCount() = players.size

    fun updatePlayers(newPlayers: List<Player>) {
        players = newPlayers
        notifyDataSetChanged()
    }
}