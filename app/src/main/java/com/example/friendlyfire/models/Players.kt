package com.example.friendlyfire.models

data class Player(val name: String) {
    val questions: MutableList<Question> = mutableListOf() // Liste des questions attribuées à ce joueur
    var penalties : Int = 0

    // Calcul du total des pénalités pour ce joueur
    fun totalPenalties(): Int {
        return questions.sumOf { it.penalties }
    }
    fun addPenalties(penalty: Int) {
        // Ajoute les pénalités au joueur
        this.questions.add(Question("Pénalité", penalty, null))
    }
    fun reset() {
        questions.clear() // Efface les questions associées au joueur
    }

}
