package com.svetemise

import android.os.Bundle
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import org.json.JSONObject
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.svetemise.R
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    private val intentDates = mutableSetOf<CalendarDay>()
    private val weekButtons = mutableListOf<Pair<String, Button>>()
    private val feasts = mutableMapOf<String, String>()
    private val feastDates = mutableSetOf<CalendarDay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resources.openRawResource(R.raw.blagdani).bufferedReader().use { reader ->
            val obj = JSONObject(reader.readText())
            obj.keys().forEach { key ->
                val name = obj.getString(key)
                feasts[key] = name
                val parts = key.split("-")
                if (parts.size == 3) {
                    val y = parts[0].toInt()
                    val m = parts[1].toInt()
                    val d = parts[2].toInt()
                    feastDates.add(CalendarDay.from(y, m, d))
                }
            }
        }

        val calendarView = findViewById<MaterialCalendarView>(R.id.calendarView)
        val weekLayout = findViewById<LinearLayout>(R.id.weekLayout)

        setupWeekView(weekLayout)

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
            updateWeekButtons(snapshot)
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

            NakanaDialog(this, dateKey) { tekst, sat, tekst2, sat2 ->
                if (tekst.isNotBlank() || !tekst2.isNullOrBlank()) {
                    val data = hashMapOf<String, Any>()
                    if (tekst.isNotBlank()) {
                        data["tekst"] = tekst
                        sat?.let { data["sat"] = it }
                    }
                    if (!tekst2.isNullOrBlank()) {
                        data["tekst2"] = tekst2
                        sat2?.let { data["sat2"] = it }
                    }
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
            override fun shouldDecorate(day: CalendarDay) = feastDates.contains(day)
            override fun decorate(view: DayViewFacade) {
                view.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#FFCCCC")))
            }
        })
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay) = intentDates.contains(day)
            override fun decorate(view: DayViewFacade) {
                view.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#FFFF99")))
            }
        })
    }

    private fun setupWeekView(layout: LinearLayout) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        for (i in 0..6) {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val dateKey = String.format(
                "%04d-%02d-%02d",
                dayCal.get(Calendar.YEAR),
                dayCal.get(Calendar.MONTH) + 1,
                dayCal.get(Calendar.DAY_OF_MONTH)
            )
            val btn = Button(this)
            btn.setOnClickListener {
                NakanaDialog(this, dateKey) { t, s, t2, s2 ->
                    if (t.isNotBlank() || !t2.isNullOrBlank()) {
                        val data = hashMapOf<String, Any>()
                        if (t.isNotBlank()) {
                            data["tekst"] = t
                            s?.let { data["sat"] = it }
                        }
                        if (!t2.isNullOrBlank()) {
                            data["tekst2"] = t2
                            s2?.let { data["sat2"] = it }
                        }
                        db.collection("nakane").document(dateKey).set(data)
                    }
                }.show()
            }
            layout.addView(btn)
            weekButtons.add(dateKey to btn)
        }
    }

    private fun updateWeekButtons(snapshot: com.google.firebase.firestore.QuerySnapshot?) {
        val docs = snapshot?.documents?.associateBy { it.id } ?: emptyMap()
        weekButtons.forEach { (key, btn) ->
            val parts = key.split("-")
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
            var text = "$dayName: "
            feasts[key]?.let { feast ->
                text += "$feast "
            }
            docs[key]?.let { doc ->
                val items = mutableListOf<String>()
                doc.getString("tekst")?.let { t ->
                    val time = doc.getString("sat")
                    items.add(t + (time?.let { " ($it)" } ?: ""))
                }
                doc.getString("tekst2")?.let { t ->
                    val time = doc.getString("sat2")
                    items.add(t + (time?.let { " ($it)" } ?: ""))
                }
                text += items.joinToString(" | ")
            }
            btn.text = text
        }
    }
}
