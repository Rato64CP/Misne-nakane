package com.example.misnenakane

import android.os.Bundle
import android.graphics.Color
import android.widget.Toast
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    private val intentDates = mutableSetOf<CalendarDay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val calendarView = findViewById<MaterialCalendarView>(R.id.calendarView)

        db.collection("nakane").addSnapshotListener { snapshot, _ ->
            intentDates.clear()
            snapshot?.documents?.forEach { doc ->
                val parts = doc.id.split("-")
                if (parts.size == 3) {
                    val y = parts[0].toInt()
                    val m = parts[1].toInt()
                    val d = parts[2].toInt()
                    intentDates.add(CalendarDay.from(y, m, d))
                }
            }
            refreshDecorators(calendarView)
        }

        calendarView.setOnDateChangedListener { _, date, _ ->
            val selectedCal = Calendar.getInstance().apply {
                set(date.year, date.month - 1, date.day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (selectedCal.before(today)) {
                Toast.makeText(this, "Ne može se mijenjati prošli dan", Toast.LENGTH_SHORT).show()
                return@setOnDateChangedListener
            }

            val dateKey = String.format("%04d-%02d-%02d", date.year, date.month, date.day)

            NakanaDialog(this, dateKey) { tekst, sat ->
                if (tekst.isNotBlank()) {
                    val data = hashMapOf<String, Any>("tekst" to tekst)
                    sat?.let { data["sat"] = it }
                    db.collection("nakane").document(dateKey).set(data)
                }
            }.show()
        }
    }

    private fun refreshDecorators(calendarView: MaterialCalendarView) {
        calendarView.removeDecorators()
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay) = true
            override fun decorate(view: DayViewFacade) {
                view.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#CCFFCC")))
            }
        })
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay) = intentDates.contains(day)
            override fun decorate(view: DayViewFacade) {
                view.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#FFFF99")))
            }
        })
    }
}
