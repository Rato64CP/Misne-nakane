package com.example.misnenakane

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class NakanaDialog(
    private val context: Context,
    val datum: String,
    val onSave: (String, String?, String?, String?) -> Unit
) {
    private val db = Firebase.firestore

    fun show() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_nakana, null)
        val input = view.findViewById<EditText>(R.id.editTextNakana)
        val timeInput = view.findViewById<EditText>(R.id.editTextSat)
        val input2 = view.findViewById<EditText>(R.id.editTextNakana2)
        val timeInput2 = view.findViewById<EditText>(R.id.editTextSat2)

        if (!isSunday()) {
            input2.visibility = View.GONE
            timeInput2.visibility = View.GONE
        }

        db.collection("nakane").document(datum).get().addOnSuccessListener {
            input.setText(it.getString("tekst") ?: "")
            timeInput.setText(it.getString("sat") ?: "")
            input2.setText(it.getString("tekst2") ?: "")
            timeInput2.setText(it.getString("sat2") ?: "")
        }

        AlertDialog.Builder(context)
            .setTitle("Misna nakana za $datum")
            .setView(view)
            .setPositiveButton("Spremi") { _, _ ->
                onSave(
                    input.text.toString(),
                    timeInput.text.toString().ifBlank { null },
                    input2.text.toString(),
                    timeInput2.text.toString().ifBlank { null }
                )
            }
            .setNegativeButton("Odustani", null)
            .show()
    }

    private fun isSunday(): Boolean {
        val parts = datum.split("-")
        if (parts.size != 3) return false
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }
}
