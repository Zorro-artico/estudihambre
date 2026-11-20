package com.example.domain.usecase

import com.example.domain.model.ContainerShape
import kotlin.math.PI
import kotlin.math.pow

class ContainerGeometryUseCase {

    fun calculateTotalVolumeMl(
        shape: ContainerShape,
        radiusCm: Double,
        heightCm: Double,
        lengthCm: Double,
        widthCm: Double
    ): Double {
        val raw = when (shape) {
            ContainerShape.CYLINDRICAL -> {
                if (radiusCm <= 0.0 || heightCm <= 0.0) 0.0
                else PI * radiusCm.pow(2) * heightCm
            }
            ContainerShape.RECTANGULAR -> {
                if (lengthCm <= 0.0 || widthCm <= 0.0 || heightCm <= 0.0) 0.0
                else lengthCm * widthCm * heightCm
            }
            ContainerShape.HEMISPHERICAL -> {
                if (radiusCm <= 0.0) 0.0
                else (2.0 / 3.0) * PI * radiusCm.pow(3)
            }
        }
        return raw.coerceAtLeast(0.0)
    }

    fun calculateOccupiedVolumeMl(totalVolumeMl: Double, fillPercent: Int): Double {
        val clamped = fillPercent.coerceIn(0, 100)
        return totalVolumeMl * (clamped / 100.0)
    }
}
