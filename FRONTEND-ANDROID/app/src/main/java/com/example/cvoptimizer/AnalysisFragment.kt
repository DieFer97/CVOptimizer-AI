package com.example.cvoptimizer

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.cvoptimizer.databinding.FragmentAnalysisBinding
import com.google.gson.Gson

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private val args: AnalysisFragmentArgs by navArgs()

    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0
    private var analyzing = true

    private val statuses = listOf(
        "Cargando documento...",
        "Extrayendo texto...",
        "Analizando habilidades técnicas...",
        "Evaluando experiencia laboral...",
        "Identificando áreas de mejora...",
        "Preparando resultados..."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileName.text = args.fileName

        startSimulatedAnalysis()

        binding.actionButton.setOnClickListener {
            if (progress >= 100) {
                navigateToResults()
            } else {
                cancelAnalysis()
            }
        }
    }

    private fun startSimulatedAnalysis() {
        val response = args.apiResponse

        if (response?.success == true && response.todas_las_areas != null) {
            simulateProgress()
        } else {
            showErrorState("Error en el análisis")
        }
    }

    private fun simulateProgress() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!analyzing) return

                progress += 2

                if (progress >= 100) {
                    progress = 100
                    analyzing = false
                } else {
                    handler.postDelayed(this, 200)
                }

                updateUI()
            }
        }, 200)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        binding.progressBar.progress = progress
        binding.progressText.text = "$progress%"

        val statusText = when {
            progress >= 100 -> "¡Análisis completado!"
            else -> {
                val index = (progress / 20).coerceAtMost(statuses.size - 1)
                statuses[index]
            }
        }
        binding.statusText.text = statusText

        binding.statusIcon.setImageResource(
            if (progress >= 100) R.drawable.ic_check_circle else R.drawable.ic_hourglass
        )

        binding.actionButton.apply {
            setBackgroundResource(
                if (progress >= 100) R.drawable.button_complete else R.drawable.button_cancel
            )
            text = if (progress >= 100) "Continuar" else "Cancelar análisis"
        }
    }

    private fun cancelAnalysis() {
        analyzing = false
        handler.removeCallbacksAndMessages(null)
        findNavController().navigate(R.id.action_analysis_to_main)
    }

    private fun showErrorState(message: String) {
        progress = 0
        analyzing = false
        binding.statusText.text = message
        updateUI()
        handler.postDelayed({
            findNavController().navigate(R.id.action_analysis_to_main)
        }, 2000)
    }

    private fun navigateToResults() {
        val apiResponseJson = Gson().toJson(args.apiResponse)
        val action = AnalysisFragmentDirections.actionAnalysisToResults(
            fileName = args.fileName,
            apiResponseJson = apiResponseJson
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
