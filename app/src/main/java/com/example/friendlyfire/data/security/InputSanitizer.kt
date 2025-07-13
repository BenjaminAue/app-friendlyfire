// Fichier: app/src/main/java/com/example/friendlyfire/data/security/InputSanitizer.kt

package com.example.friendlyfire.data.security

import android.util.Log
import java.util.regex.Pattern

object InputSanitizer {

    private const val TAG = "InputSanitizer"

    // Patterns dangereux à détecter
    private val DANGEROUS_PATTERNS = listOf(
        // Scripts et code
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE),

        // HTML potentiellement dangereux
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<form[^>]*>", Pattern.CASE_INSENSITIVE),

        // SQL Injection basique
        Pattern.compile("(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("('|(\\-\\-)|(;)|(\\|)|(\\*)|(%)|(<)|(>)|(\\?)|(\\[)|(\\])|(\\{)|(\\}))", Pattern.CASE_INSENSITIVE),

        // Autres patterns suspects
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("url\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE)
    )

    // Caractères autorisés pour les noms de joueurs
    private val PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s\\-_']+$")

    // Caractères autorisés pour les questions (plus permissif mais sécurisé)
    private val QUESTION_TEXT_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s\\-_'\".,!?()\\[\\]:;/+=]+$")

    /**
     * Valide et nettoie un nom de joueur
     */
    fun sanitizePlayerName(input: String): SanitizedInput {
        val trimmed = input.trim()

        // Vérifications de base
        if (trimmed.isBlank()) {
            return SanitizedInput.Invalid("Le nom ne peut pas être vide")
        }

        if (trimmed.length > 50) {
            return SanitizedInput.Invalid("Le nom ne peut pas dépasser 50 caractères")
        }

        // Détecter les patterns dangereux
        val dangerousPattern = detectDangerousPatterns(trimmed)
        if (dangerousPattern != null) {
            Log.w(TAG, "Dangerous pattern detected in player name: $dangerousPattern")
            return SanitizedInput.Invalid("Le nom contient des caractères non autorisés")
        }

        // Vérifier les caractères autorisés
        if (!PLAYER_NAME_PATTERN.matcher(trimmed).matches()) {
            return SanitizedInput.Invalid("Le nom contient des caractères non autorisés")
        }

        return SanitizedInput.Valid(trimmed)
    }

    /**
     * Valide et nettoie une question
     */
    fun sanitizeQuestionText(input: String): SanitizedInput {
        val trimmed = input.trim()

        // Vérifications de base
        if (trimmed.isBlank()) {
            return SanitizedInput.Invalid("La question ne peut pas être vide")
        }

        if (trimmed.length < 10) {
            return SanitizedInput.Invalid("La question doit contenir au moins 10 caractères")
        }

        if (trimmed.length > 500) {
            return SanitizedInput.Invalid("La question ne peut pas dépasser 500 caractères")
        }

        // Détecter les patterns dangereux
        val dangerousPattern = detectDangerousPatterns(trimmed)
        if (dangerousPattern != null) {
            Log.w(TAG, "Dangerous pattern detected in question: $dangerousPattern")
            return SanitizedInput.Invalid("La question contient du contenu non autorisé")
        }

        // Vérifier les caractères autorisés (plus permissif pour les questions)
        if (!QUESTION_TEXT_PATTERN.matcher(trimmed).matches()) {
            return SanitizedInput.Invalid("La question contient des caractères non autorisés")
        }

        return SanitizedInput.Valid(trimmed)
    }

    /**
     * Valide un ID de jeu
     */
    fun sanitizeGameId(input: String): SanitizedInput {
        val trimmed = input.trim()

        if (trimmed.isBlank()) {
            return SanitizedInput.Invalid("L'ID du jeu ne peut pas être vide")
        }

        if (trimmed.length > 50) {
            return SanitizedInput.Invalid("L'ID du jeu ne peut pas dépasser 50 caractères")
        }

        // IDs de jeu très stricts : seulement alphanumériques, tirets et underscores
        if (!Pattern.compile("^[a-zA-Z0-9\\-_]+$").matcher(trimmed).matches()) {
            return SanitizedInput.Invalid("L'ID du jeu contient des caractères non autorisés")
        }

        return SanitizedInput.Valid(trimmed)
    }

    /**
     * Valide une valeur de pénalité
     */
    fun sanitizePenalties(input: Int): SanitizedInput {
        when {
            input < 1 -> return SanitizedInput.Invalid("Les pénalités doivent être d'au moins 1")
            input > 20 -> return SanitizedInput.Invalid("Les pénalités ne peuvent pas dépasser 20")
            else -> return SanitizedInput.Valid(input.toString())
        }
    }

    /**
     * Détecte les patterns dangereux dans le texte
     */
    private fun detectDangerousPatterns(input: String): String? {
        DANGEROUS_PATTERNS.forEach { pattern ->
            if (pattern.matcher(input).find()) {
                return pattern.pattern()
            }
        }
        return null
    }

    /**
     * Escape HTML pour affichage sécurisé
     */
    fun escapeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }

    /**
     * Log des tentatives d'attaque pour monitoring
     */
    fun logSecurityEvent(eventType: String, input: String, userId: String? = null) {
        Log.w(TAG, "SECURITY EVENT: $eventType - Input: ${input.take(100)} - User: $userId")
        // TODO: En production, envoyer vers un service de monitoring externe
    }
}

/**
 * Résultat de la validation d'entrée
 */
sealed class SanitizedInput {
    data class Valid(val cleanInput: String) : SanitizedInput()
    data class Invalid(val reason: String) : SanitizedInput()

    fun getValueOrThrow(): String = when (this) {
        is Valid -> cleanInput
        is Invalid -> throw SecurityValidationException(reason)
    }
}

/**
 * Exception pour les erreurs de validation de sécurité
 */
class SecurityValidationException(message: String) : Exception(message)