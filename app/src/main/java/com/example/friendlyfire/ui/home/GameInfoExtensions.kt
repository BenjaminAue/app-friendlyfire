// ===== 3. Extension pour GameInfo =====
// Fichier: app/src/main/java/com/example/friendlyfire/ui/home/GameInfoExtensions.kt

package com.example.friendlyfire.ui.home

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.friendlyfire.R

fun GameInfo.getStatusColor(context: Context): Int {
    return when {
        !isAvailable -> ContextCompat.getColor(context, R.color.purple_200)
        else -> ContextCompat.getColor(context, R.color.teal_200)
    }
}

fun GameInfo.getPlayersRangeText(): String {
    return if (minPlayers == maxPlayers) {
        "$minPlayers joueurs"
    } else {
        "$minPlayers-$maxPlayers joueurs"
    }
}