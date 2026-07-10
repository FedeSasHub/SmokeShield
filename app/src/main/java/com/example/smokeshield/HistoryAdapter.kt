package com.example.smokeshield

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

data class HistoryItem(val data: String, val punteggio: Int, val dateObj: Date, val livello: String)

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_item_date)
        val tvScore: TextView = view.findViewById(R.id.tv_item_score)
        val tvLevel: TextView = view.findViewById(R.id.tv_item_level)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvDate.text = item.data
        holder.tvScore.text = "Punti: ${item.punteggio}"
        holder.tvLevel.text = "Livello: ${item.livello}"
    }

    override fun getItemCount(): Int = historyList.size
}