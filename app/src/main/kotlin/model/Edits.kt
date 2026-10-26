package model

import kotlinx.serialization.Serializable

@Serializable
data class Adjustments(
    val exposure: Double = 0.0,
    val temperature: Double = 0.0,
    val tint: Double = 0.0,
    val contrast: Double = 0.0,
    val saturation: Double = 0.0,
)

@Serializable
data class Grade(
    val lift: List<Double> = listOf(0.0, 0.0, 0.0),
    val gamma: List<Double> = listOf(0.0, 0.0, 0.0),
    val gain: List<Double> = listOf(0.0, 0.0, 0.0),
    val lut_path: String? = null,
    val lut_intensity: Double = 1.0,
)

@Serializable
data class PreviewRequest(
    val path: String,
    val max_size: Int = 1600,
    val quality: Int = 90,
    val adjustments: Adjustments = Adjustments(),
    val grade: Grade = Grade(),
)
