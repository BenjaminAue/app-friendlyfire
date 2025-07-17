// Fichier: app/src/main/java/com/example/friendlyfire/ui/common/SecureInputTextWatcher.kt

package com.example.friendlyfire.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.example.friendlyfire.data.security.InputSanitizer
import com.example.friendlyfire.data.security.SanitizedInput

/**
 * TextWatcher sécurisé qui valide les inputs en temps réel
 */
class SecureInputTextWatcher(
    private val editText: EditText,
    private val inputType: InputType,
    private val onValidationResult: (isValid: Boolean, errorMessage: String?) -> Unit = { _, _ -> }
) : TextWatcher {

    enum class InputType {
        PLAYER_NAME,
        QUESTION_TEXT,
        GAME_ID
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Ne rien faire avant la modification
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Ne rien faire pendant la modification
    }

    override fun afterTextChanged(s: Editable?) {
        val input = s?.toString() ?: ""
        validateInput(input)
    }

    private fun validateInput(input: String) {
        if (input.isEmpty()) {
            onValidationResult(true, null) // Input vide = valide (sera géré par la validation finale)
            return
        }

        val sanitizedResult = when (inputType) {
            InputType.PLAYER_NAME -> InputSanitizer.sanitizePlayerName(input)
            InputType.QUESTION_TEXT -> InputSanitizer.sanitizeQuestionText(input)
            InputType.GAME_ID -> InputSanitizer.sanitizeGameId(input)
        }

        when (sanitizedResult) {
            is SanitizedInput.Valid -> {
                onValidationResult(true, null)
                updateEditTextAppearance(isValid = true)
            }
            is SanitizedInput.Invalid -> {
                onValidationResult(false, sanitizedResult.reason)
                updateEditTextAppearance(isValid = false)
            }
        }
    }

    private fun updateEditTextAppearance(isValid: Boolean) {
        // Changer la couleur du border pour feedback visuel
        val context = editText.context
        val colorRes = if (isValid) {
            android.R.color.holo_green_light
        } else {
            android.R.color.holo_red_light
        }

        // Note: Vous pourriez créer des drawables custom pour un meilleur rendu
        editText.setBackgroundColor(context.getColor(colorRes))
    }
}

/**
 * Extension function pour faciliter l'usage
 */
fun EditText.addSecureValidation(
    inputType: SecureInputTextWatcher.InputType,
    onValidationResult: (isValid: Boolean, errorMessage: String?) -> Unit = { _, _ -> }
) {
    this.addTextChangedListener(
        SecureInputTextWatcher(this, inputType, onValidationResult)
    )
}