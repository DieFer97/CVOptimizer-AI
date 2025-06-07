package com.example.cvoptimizer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cvoptimizer.databinding.FragmentResultsBinding

data class AnalysisResult(
    val area: String,
    val iconUrl: String,
    val percentage: Int,
    val color: String
)

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        val results = listOf(
            AnalysisResult("Recursos Humanos", "https://api.a0.dev/assets/image?text=HR%20Icon&aspect=1:1&seed=hr123", 85, "#4CAF50"),
            AnalysisResult("Finanzas", "https://api.a0.dev/assets/image?text=Finance%20Icon&aspect=1:1&seed=finance456", 62, "#FF9800"),
            AnalysisResult("Administrativo", "https://api.a0.dev/assets/image?text=Admin%20Icon&aspect=1:1&seed=admin789", 78, "#2196F3"),
            AnalysisResult("Marketing", "https://api.a0.dev/assets/image?text=Marketing%20Icon&aspect=1:1&seed=marketing101", 45, "#E91E63"),
            AnalysisResult("Sistemas", "https://api.a0.dev/assets/image?text=IT%20Icon&aspect=1:1&seed=it202", 91, "#9C27B0")
        )

        binding.resultsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.resultsRecyclerView.adapter = ResultsAdapter(results) { area ->
            Toast.makeText(context, "Ver detalles de $area", Toast.LENGTH_SHORT).show()
        }

        // Configurar botones del footer
        binding.homeButton.setOnClickListener {
            findNavController().navigate(R.id.action_results_to_main)
        }

        binding.shareButton.setOnClickListener {
            val shareText = results.joinToString("\n") { "${it.area}: ${it.percentage}% (${getLevelFromPercentage(it.percentage)})" }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Resultados del análisis de CV:\n$shareText")
            }
            startActivity(Intent.createChooser(intent, "Compartir resultados"))
        }
    }

    private fun getLevelFromPercentage(percentage: Int): String {
        return when {
            percentage >= 85 -> "Excelente"
            percentage >= 70 -> "Muy bueno"
            percentage >= 50 -> "Bueno"
            percentage >= 30 -> "Regular"
            else -> "Necesita mejorar"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}