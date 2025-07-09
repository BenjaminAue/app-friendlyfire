package com.example.friendlyfire.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.R
import com.example.friendlyfire.models.Player

class PlayerAdapter(
    private val players: List<Player>,
    private val onDeleteClicked: (Player) -> Unit
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerNameTextView: TextView = view.findViewById(R.id.playerNameTextView)
        val deleteButton: Button = view.findViewById(R.id.deletePlayerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]
        holder.playerNameTextView.text = player.name
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(player)
        }
    }

    override fun getItemCount() = players.size
}
