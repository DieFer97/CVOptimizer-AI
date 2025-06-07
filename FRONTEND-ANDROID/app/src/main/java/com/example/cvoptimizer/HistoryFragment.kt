package com.example.cvoptimizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cvoptimizer.databinding.FragmentHistoryBinding

data class HistoryItem(
    val id: String,
    val fileName: String,
    val date: String,
    val topArea: String,
    val score: Int
)

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón de retroceso
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_history_to_main)
        }

        // Datos simulados
        val historyData = listOf(
            HistoryItem("1", "curriculum_2023.pdf", "15/05/2023", "Recursos Humanos", 85),
            HistoryItem("2", "cv_empresarial.pdf", "03/03/2023", "Finanzas", 78),
            HistoryItem("3", "hoja_de_vida_2022.pdf", "22/11/2022", "Sistemas", 91),
            HistoryItem("4", "cv_ingles.pdf", "10/08/2022", "Marketing", 72),
            HistoryItem("5", "resume_tech.pdf", "05/06/2022", "Sistemas", 88)
        )

        if (historyData.isNotEmpty()) {
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.historyRecyclerView.adapter = HistoryAdapter(historyData) {
                findNavController().navigate(R.id.action_history_to_results)
            }
        } else {
            binding.historyRecyclerView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.newAnalysisButton.setOnClickListener {
                findNavController().navigate(R.id.action_history_to_main)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}