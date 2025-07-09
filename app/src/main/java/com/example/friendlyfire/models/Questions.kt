package com.example.friendlyfire.models

data class Question(
    val questionText: String,
    val penalties: Int,
    val player: Player? = null,
    var used: Boolean = false // Indique si la question a été utilisée
)
