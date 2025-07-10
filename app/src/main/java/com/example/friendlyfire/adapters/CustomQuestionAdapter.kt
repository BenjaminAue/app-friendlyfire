// ===== 3. CustomQuestionAdapter amélioré =====
// Fichier: app/src/main/java/com/example/friendlyfire/adapters/CustomQuestionAdapter.kt

package com.example.friendlyfire.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.friendlyfire.R
import com.example.friendlyfire.models.Question

class CustomQuestionAdapter(
    private var questions: List<Question>,
    private val onDeleteClick: (Question) -> Unit,
    private val onEditClick: (Question) -> Unit
) : RecyclerView.Adapter<CustomQuestionAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionTextView: TextView = view.findViewById(R.id.questionTextView)
        val penaltyTextView: TextView = view.findViewById(R.id.penaltyTextView)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]

        holder.questionTextView.text = question.questionText
        holder.penaltyTextView.text = "${question.penalties} pénalités"

        holder.editButton.setOnClickListener {
            onEditClick(question)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(question)
        }
    }

    override fun getItemCount() = questions.size

    fun updateQuestions(newQuestions: List<Question>) {
        questions = newQuestions
        notifyDataSetChanged()
    }
}