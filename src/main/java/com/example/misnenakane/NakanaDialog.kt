package com.example.misnenakane

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NakanaDialog(private val context: Context, val datum: String, val onSave: (String, String?) -> Unit) {
    private val db = Firebase.firestore

    fun show() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_nakana, null)
        val input = view.findViewById<EditText>(R.id.editTextNakana)
        val timeInput = view.findViewById<EditText>(R.id.editTextSat)

        db.collection("nakane").document(datum).get().addOnSuccessListener {
            input.setText(it.getString("tekst") ?: "")
            timeInput.setText(it.getString("sat") ?: "")
        }

        AlertDialog.Builder(context)
            .setTitle("Misna nakana za $datum")
            .setView(view)
            .setPositiveButton("Spremi") { _, _ ->
                onSave(input.text.toString(), timeInput.text.toString().ifBlank { null })
            }
            .setNegativeButton("Odustani", null)
            .show()
    }
}
