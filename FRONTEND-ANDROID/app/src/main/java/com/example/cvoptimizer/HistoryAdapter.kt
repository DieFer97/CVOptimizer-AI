package com.example.cvoptimizer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cvoptimizer.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val historyData: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyData[position]
        with(holder.binding) {
            Glide.with(root.context)
                .load("https://api.a0.dev/assets/image?text=PDF%20File&aspect=1:1&seed=pdf123")
                .into(fileIcon)
            fileName.text = item.fileName
            fileDate.text = "Analizado el ${item.date}"
            areaText.text = item.topArea
            scoreText.text = "${item.score}%"
            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount(): Int = historyData.size
}