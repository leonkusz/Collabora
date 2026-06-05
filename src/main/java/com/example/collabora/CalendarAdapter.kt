package com.example.collabora

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val days: List<String>,
    private val deadlineDates: Set<String>,
    private var selectedDay: String = "",
    private val onDateClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayText: TextView = view.findViewById(R.id.tvDayText)
        val cardDayBackground: CardView = view.findViewById(R.id.cardDayBackground)
        val viewDeadlineDot: View = view.findViewById(R.id.viewDeadlineDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]
        holder.tvDayText.text = day

        if (day.isNotEmpty()) {
            // 1. Indikator Deadline (Dot Merah Besar di Atas Kanan)
            if (deadlineDates.contains(day)) {
                holder.viewDeadlineDot.visibility = View.VISIBLE
            } else {
                holder.viewDeadlineDot.visibility = View.INVISIBLE
            }

            // 2. Indikator Seleksi (Background Abu Tipis)
            if (day == selectedDay) {
                holder.cardDayBackground.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
                holder.tvDayText.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                holder.cardDayBackground.setCardBackgroundColor(Color.TRANSPARENT)
                holder.tvDayText.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            holder.itemView.setOnClickListener {
                val oldSelected = selectedDay
                selectedDay = day
                
                // Refresh item lama dan baru agar animasi seleksi pindah
                notifyItemChanged(position)
                val oldPos = days.indexOf(oldSelected)
                if (oldPos != -1) notifyItemChanged(oldPos)
                
                onDateClick(day)
            }
        } else {
            holder.viewDeadlineDot.visibility = View.INVISIBLE
            holder.cardDayBackground.setCardBackgroundColor(Color.TRANSPARENT)
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = days.size
}
