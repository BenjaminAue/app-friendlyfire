// ===== 3. Mise à jour du modèle Question =====
// Fichier: app/src/main/java/com/example/friendlyfire/models/Questions.kt

package com.example.friendlyfire.models

data class Question(
    val questionText: String,
    val penalties: Int,
    val player: Player? = null,
    var used: Boolean = false,
    val isCustom: Boolean = false
)