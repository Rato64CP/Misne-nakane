package com.example.misnenakane

import android.os.Bundle
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val dateKey = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            NakanaDialog(this, dateKey) { tekst ->
                if (tekst.isNotBlank()) {
                    val data = hashMapOf("tekst" to tekst)
                    db.collection("nakane").document(dateKey).set(data)
                }
            }.show()
        }
    }
}
