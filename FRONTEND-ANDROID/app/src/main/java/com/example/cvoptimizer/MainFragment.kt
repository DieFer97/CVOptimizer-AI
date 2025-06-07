package com.example.cvoptimizer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.cvoptimizer.databinding.FragmentMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import ApiClient
import ApiService

data class FileData(val name: String, val size: String, val type: String)

data class StatData(val value: String, val label: String)

data class FeatureData(
    val id: Int,
    val title: String,
    val description: String,
    val iconRes: Int
)

interface ApiService {
    @Multipart
    @POST("predict")
    fun uploadCV(@Part file: MultipartBody.Part): Call<ApiResponse>
}

object ApiClient {
    private const val BASE_URL = "http://192.168.137.1:8000/"

    val apiService: ApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var selectedFileUri: Uri? = null

    private val uploadIconUrl = "https://api.a0.dev/assets/image?text=Upload%20Icon&aspect=1:1&seed=upload123"
    private val historyIconUrl = "https://api.a0.dev/assets/image?text=History%20Icon&aspect=1:1&seed=history456"
    private val fileIconUrl = "https://api.a0.dev/assets/image?text=File%20Icon&aspect=1:1&seed=file789"

    private val stats = listOf(
        StatData("10,000+", "Análisis realizados"),
        StatData("87%", "Tasa de mejora"),
        StatData("25+", "Sectores cubiertos")
    )

    private val features = listOf(
        FeatureData(
            id = 1,
            title = "Análisis de Competencias",
            description = "Identifica las habilidades clave en tu CV",
            iconRes = R.drawable.ic_lightbulb
        ),
        FeatureData(
            id = 2,
            title = "Palabras Clave",
            description = "Optimiza tu CV con términos de alto impacto",
            iconRes = R.drawable.ic_auto_awesome
        ),
        FeatureData(
            id = 3,
            title = "Sugerencias Personalizadas",
            description = "Recibe recomendaciones específicas para tu sector",
            iconRes = R.drawable.ic_psychology
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupIcons()
        setupClickListeners()
        setupStats()
        setupFeatures()
        updateFileSelectionUI()
    }

    private fun setupIcons() {
        Glide.with(this)
            .load(uploadIconUrl)
            .into(binding.uploadIcon)

        Glide.with(this)
            .load(historyIconUrl)
            .into(binding.historyIcon)

        Glide.with(this)
            .load(fileIconUrl)
            .into(binding.fileIcon)
    }

    private fun setupClickListeners() {
        binding.uploadCard.setOnClickListener {
            showFileSelectionDialog()
        }

        binding.historyCard.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_history)
        }

        binding.continueButton.setOnClickListener {
            handleContinue()
        }
    }

    private fun showFileSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_selection, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<View>(R.id.option_device).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                }
                startActivityForResult(intent, 1)
            } catch (e: ActivityNotFoundException) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("No se pudo abrir el selector de archivos")
                    .setPositiveButton("OK", null)
                    .show()
            }
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.option_cloud).setOnClickListener {
            handleFileSelect("cloud")
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.data
            selectedFileUri?.let { uri ->
                val cursor = context?.contentResolver?.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        val name = it.getString(nameIndex ?: 0)
                        val size = it.getLong(sizeIndex ?: 0)
                        val type = context?.contentResolver?.getType(uri) ?: "application/octet-stream"

                        binding.fileName.text = name
                        binding.fileDetails.text = "${size / 1024} KB"
                        binding.selectedFileContainer.isVisible = true
                    }
                }
            }
        }
    }

    private fun handleFileSelect(source: String) {
        selectedFileUri = null
        val mockFileName = if (source == "cloud") "cv_cloud_2023.pdf" else "mi_curriculum.pdf"
        val mockFileSize = "2.4 MB"

        binding.fileName.text = mockFileName
        binding.fileDetails.text = mockFileSize
        binding.selectedFileContainer.isVisible = true
    }

    private fun handleContinue() {
        if (selectedFileUri != null) {
            uploadCV(selectedFileUri!!)
        } else {
            val fileName = binding.fileName.text.toString()
            if (fileName.isNotEmpty()) {
                val action = MainFragmentDirections.actionMainToAnalysis(
                    fileName = fileName,
                    fileSize = binding.fileDetails.text.toString(),
                    fileType = "application/pdf"
                )
                findNavController().navigate(action)
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Por favor selecciona un archivo primero")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun uploadCV(uri: Uri) {
        val file = File(getRealPathFromURI(uri))
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("archivo", file.name, requestFile)

        val call = ApiClient.apiService.uploadCV(body)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    apiResponse?.resultados?.let { resultados ->
                        val action = MainFragmentDirections.actionMainToAnalysis(
                            fileName = file.name,
                            fileSize = "${file.length() / 1024} KB",
                            fileType = context?.contentResolver?.getType(uri) ?: "application/octet-stream"
                        )
                        findNavController().navigate(action)
                    } ?: run {
                        apiResponse?.error?.let { error -> showError(error) }
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showError("Error al comunicarse con el servidor")
            }
        })
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = context?.contentResolver?.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return it.getString(columnIndex ?: 0)
            }
        }
        return ""
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupStats() {
        binding.statValue1.text = stats[0].value
        binding.statLabel1.text = stats[0].label

        binding.statValue2.text = stats[1].value
        binding.statLabel2.text = stats[1].label

        binding.statValue3.text = stats[2].value
        binding.statLabel3.text = stats[2].label
    }

    private fun setupFeatures() {
        features.forEachIndexed { index, feature ->
            when (index) {
                0 -> {
                    binding.feature1Title.text = feature.title
                    binding.feature1Description.text = feature.description
                    binding.feature1Icon.setImageResource(feature.iconRes)
                }
                1 -> {
                    binding.feature2Title.text = feature.title
                    binding.feature2Description.text = feature.description
                    binding.feature2Icon.setImageResource(feature.iconRes)
                }
                2 -> {
                    binding.feature3Title.text = feature.title
                    binding.feature3Description.text = feature.description
                    binding.feature3Icon.setImageResource(feature.iconRes)
                }
            }
        }
    }

    private fun updateFileSelectionUI() {
        binding.selectedFileContainer.isVisible = (selectedFileUri != null || binding.fileName.text.isNotEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}