package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

/** Resultado de un reconocimiento de placa por el microservicio IA. */
@Serializable
data class LprResultDto(
    val plate: String,
    val confidence: Double,
    val processingMs: Long,
    val boundingBox: BoundingBoxDto? = null,
    val alternatives: List<LprAlternative> = emptyList(),
)

@Serializable
data class BoundingBoxDto(val x: Int, val y: Int, val width: Int, val height: Int)

@Serializable
data class LprAlternative(val plate: String, val confidence: Double)
