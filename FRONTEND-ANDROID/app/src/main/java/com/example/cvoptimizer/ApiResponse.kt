package com.example.cvoptimizer

data class ApiResponse(
    val resultados: List<Resultado>? = null,
    val error: String? = null
)

data class Resultado(
    val area: String,
    val porcentaje: Double
)