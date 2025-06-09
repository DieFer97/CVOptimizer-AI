package com.example.cvoptimizer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cvoptimizer.databinding.ItemResultBinding

class ResultsAdapter(
    private val results: List<AnalysisResult>,
    private val onDetailClick: (AnalysisResult) -> Unit
) : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {

    inner class ResultViewHolder(val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]
        with(holder.binding) {
            areaIcon.setImageResource(result.iconRes)
            areaTitle.text = result.area
            areaLevel.text = getLevelFromPercentage(result.percentage)
            percentageValue.text = "${result.percentage}%"
            percentageValue.setTextColor(Color.parseColor(result.color))
            progressBar.progress = result.percentage
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(result.color))
            detailButton.setOnClickListener { onDetailClick(result) }
        }
    }

    override fun getItemCount(): Int = results.size

    private fun getLevelFromPercentage(percentage: Int): String {
        return when {
            percentage >= 85 -> "Excelente"
            percentage >= 70 -> "Muy bueno"
            percentage >= 50 -> "Bueno"
            percentage >= 30 -> "Regular"
            else -> "Necesita mejorar"
        }
    }
}
