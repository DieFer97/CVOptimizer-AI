package com.example.cvoptimizer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cvoptimizer.databinding.FragmentResultsBinding
import com.google.gson.Gson

data class AnalysisResult(
    val area: String,
    val percentage: Int,
    val confianza: String,
    val iconRes: Int,
    val color: String
)

class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!
    private val args: ResultsFragmentArgs by navArgs()

    private val areaIcons = mapOf(
        "legal"          to android.R.drawable.ic_menu_manage,
        "finanzas"       to android.R.drawable.ic_menu_agenda,
        "marketing"      to android.R.drawable.ic_menu_send,
        "sistemas"       to android.R.drawable.ic_menu_compass,
        "administrativo" to android.R.drawable.ic_menu_sort_by_size,
        "rrhh"           to android.R.drawable.ic_menu_myplaces
    )

    private val areaColors = mapOf(
        "legal" to "#4CAF50",
        "finanzas" to "#FF9800",
        "marketing" to "#E91E63",
        "sistemas" to "#9C27B0",
        "administrativo" to "#2196F3",
        "rrhh" to "#FFC107"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiResponse = Gson().fromJson(args.apiResponseJson, ApiResponse::class.java)

        val results = buildResultsList(apiResponse)

        setupRecyclerView(results)
        setupFooterButtons(results)
    }

    private fun buildResultsList(apiResponse: ApiResponse?): List<AnalysisResult> {
        return apiResponse?.todas_las_areas?.map { area ->
            val key = area.area.lowercase()
            AnalysisResult(
                area = area.area,
                percentage = area.porcentaje,
                confianza = area.confianza,
                iconRes = areaIcons[key] ?: android.R.drawable.ic_menu_help,
                color = areaColors[key] ?: "#757575"
            )
        } ?: emptyList()
    }

    private fun setupRecyclerView(results: List<AnalysisResult>) {
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ResultsAdapter(results) { area ->
                Toast.makeText(context, "Ver detalles de ${area.area}", Toast.LENGTH_SHORT).show()
                // Aquí podrías abrir un diálogo o pantalla de detalle más adelante
            }
        }
    }

    private fun setupFooterButtons(results: List<AnalysisResult>) {
        binding.homeButton.setOnClickListener {
            findNavController().navigate(R.id.action_results_to_main)
        }

        binding.shareButton.setOnClickListener {
            if (results.isEmpty()) {
                Toast.makeText(context, "No hay resultados para compartir", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shareText = buildShareText(results)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "Compartir resultados"))
        }
    }

    private fun buildShareText(results: List<AnalysisResult>): String {
        val header = "📊 Resultados del análisis de CV:\n"
        val body = results.joinToString("\n") {
            "- ${it.area}: ${it.percentage}% (${getLevelFromPercentage(it.percentage)}, Confianza: ${it.confianza})"
        }
        return header + body
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
