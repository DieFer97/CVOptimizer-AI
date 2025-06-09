package com.example.cvoptimizer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.cvoptimizer.databinding.FragmentMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

// --- MODELO INTERNO PARA INFO DEL ARCHIVO ---
data class FileData(val name: String, val size: String, val type: String)

// --- FRAGMENTO PRINCIPAL ---
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private var selectedFileUri: Uri? = null
    private var apiResponse: ApiResponse? = null

    private val uploadIconUrl = "https://api.a0.dev/upload-icon.png"
    private val historyIconUrl = "https://api.a0.dev/history-icon.png"
    private val fileIconUrl    = "https://api.a0.dev/file-icon.png"

    private val stats = listOf("10,000+", "87%", "25+")
    private val statLabels = listOf("Análisis realizados", "Tasa de mejora", "Sectores cubiertos")

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data!!.data?.let { uri ->
                val info = getFileInfo(uri)
                if (isValidFileType(info.type)) {
                    selectedFileUri = uri
                    binding.fileName.text = info.name
                    binding.fileDetails.text = info.size
                    binding.selectedFileContainer.isVisible = true
                } else {
                    showError("Formato no soportado. Usa PDF, DOCX o TXT.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ya no necesitas registro para permisos de almacenamiento especial
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIcons()
        setupClickListeners()
        setupStats()
        updateFileSelectionUI()
    }

    private fun setupIcons() {
        Glide.with(this).load(uploadIconUrl).into(binding.uploadIcon)
        Glide.with(this).load(historyIconUrl).into(binding.historyIcon)
        Glide.with(this).load(fileIconUrl).into(binding.fileIcon)
    }

    private fun setupClickListeners() {
        binding.uploadCard.setOnClickListener {
            // Solo abre el selector de archivos, sin pedir permisos extras
            showFileSelectionDialog()
        }

        binding.historyCard.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_history)
        }
        binding.continueButton.setOnClickListener { handleContinue() }
    }

    private fun showFileSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_file_selection, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.option_device).setOnClickListener {
            startActivityPicker()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.option_cloud).setOnClickListener {
            // mock demo
            selectedFileUri = null
            binding.fileName.text = "cv_cloud_2023.pdf"
            binding.fileDetails.text = "2.4 MB"
            binding.selectedFileContainer.isVisible = true
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun startActivityPicker() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            showError("No se pudo abrir el selector de archivos")
        }
    }

    private fun handleContinue() {
        selectedFileUri?.let { uri ->
            CoroutineScope(Dispatchers.Main).launch {
                uploadCV(uri)
            }
        } ?: showError("Por favor selecciona un archivo primero")
    }

    private suspend fun uploadCV(uri: Uri) {
        val file = getFileFromUri(uri) ?: run {
            showError("No se pudo procesar el archivo")
            return
        }
        val info = getFileInfo(uri)
        val requestBody = file.asRequestBody(info.type.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)

        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.uploadCV(part)
            }
            apiResponse = response
            if (response.success && response.todas_las_areas != null) {
                val action = MainFragmentDirections.actionMainToAnalysis(
                    fileName = info.name,
                    fileSize = info.size,
                    fileType = info.type,
                    apiResponse = response
                )
                findNavController().navigate(action)
            } else {
                showError(response.error ?: "Error desconocido en la respuesta")
            }
        } catch (e: Exception) {
            showError("Error de conexión: ${e.message}")
        }
    }

    private fun getFileInfo(uri: Uri): FileData {
        var name = "desconocido"
        var size = 0L
        var type = "application/octet-stream"

        context?.contentResolver?.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        type = context?.contentResolver?.getType(uri) ?: type
        val sizeString = if (size < 1024 * 1024) "${size / 1024} KB" else "${size / (1024 * 1024)} MB"

        return FileData(name, sizeString, type)
    }

    private fun getFileFromUri(uri: Uri): File? {
        val info = getFileInfo(uri)
        val tempFile = File(requireContext().cacheDir, info.name)
        return try {
            context?.contentResolver?.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidFileType(type: String): Boolean {
        return type in listOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
        )
    }

    private fun setupStats() {
        binding.statValue1.text = stats[0]
        binding.statLabel1.text = statLabels[0]
        binding.statValue2.text = stats[1]
        binding.statLabel2.text = statLabels[1]
        binding.statValue3.text = stats[2]
        binding.statLabel3.text = statLabels[2]
    }

    private fun updateFileSelectionUI() {
        binding.selectedFileContainer.isVisible = selectedFileUri != null
    }

    private fun showError(msg: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
