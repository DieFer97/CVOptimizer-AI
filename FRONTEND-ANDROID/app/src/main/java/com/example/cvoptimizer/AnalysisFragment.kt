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

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0
    private var status = "Iniciando análisis..."
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

        val args: AnalysisFragmentArgs by navArgs()
        val fileName = args.fileName
        binding.fileName.text = fileName

        updateUI()

        startAnalysis()

        binding.actionButton.setOnClickListener {
            if (progress >= 100) {
                findNavController().navigate(R.id.action_analysis_to_results)
            } else {
                analyzing = false
                handler.removeCallbacksAndMessages(null)
                findNavController().navigate(R.id.action_analysis_to_main)
            }
        }
    }

    private fun startAnalysis() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!analyzing) return

                progress += 2
                if (progress >= 100) {
                    progress = 100
                    status = "¡Análisis completado!"
                    analyzing = false
                } else {
                    val statusIndex = (progress / 20).coerceAtMost(statuses.size - 1)
                    status = statuses[statusIndex]
                    handler.postDelayed(this, 200)
                }
                updateUI()
            }
        }, 200)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        binding.progressBar.progress = progress
        binding.progressText.text = "${progress}%"
        binding.statusText.text = status
        binding.statusIcon.setImageResource(
            if (progress >= 100) R.drawable.ic_check_circle else R.drawable.ic_hourglass
        )
        binding.actionButton.setBackgroundResource(
            if (progress >= 100) R.drawable.button_complete else R.drawable.button_cancel
        )
        binding.actionButton.text = if (progress >= 100) "Continuar" else "Cancelar análisis"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}