package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ContainerShape
import com.example.domain.model.DeficitProjectionPoint
import com.example.domain.model.MetabolicBuffStatus
import com.example.domain.model.WeightEntry
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.HyruleSage
import com.example.ui.theme.LeatherBorder
import com.example.ui.theme.MutedParchment
import com.example.ui.theme.ParchmentCream

@Composable
fun MetabolicBuffBadge(
    status: MetabolicBuffStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (status) {
        MetabolicBuffStatus.ELIXIR_ANABOLICO -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            HyruleSage,
            Icons.Default.AutoAwesome
        )
        MetabolicBuffStatus.FORJA_ACTIVA -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            HoneyBrothGold,
            Icons.Default.LocalFireDepartment
        )
        MetabolicBuffStatus.ESCUDO_TECHO -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            CrystalWaterBlue,
            Icons.Default.Shield
        )
        MetabolicBuffStatus.MALEFICIO_CATABOLICO, MetabolicBuffStatus.EXCEDENTE -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            EmberCrimson,
            Icons.Default.Warning
        )
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.titleEs,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.titleEs,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OptimalRangeGauge(
    intakeKcal: Int,
    floorKcal: Int,
    ceilingKcal: Int,
    tdeeKcal: Int,
    modifier: Modifier = Modifier
) {
    val maxScale = maxOf(tdeeKcal, ceilingKcal + 300, intakeKcal + 100).coerceAtLeast(1500)
    val progressTarget = (intakeKcal.toFloat() / maxScale.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 280f),
        label = "calGauge"
    )
    val floorFrac = (floorKcal.toFloat() / maxScale.toFloat()).coerceIn(0.05f, 0.95f)
    val ceilingFrac = (ceilingKcal.toFloat() / maxScale.toFloat()).coerceIn(floorFrac + 0.02f, 0.98f)

    val barColor = when {
        intakeKcal < floorKcal -> HoneyBrothGold
        intakeKcal <= ceilingKcal -> HyruleSage
        else -> EmberCrimson
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            val h = size.height
            val w = size.width
            val trackY = h * 0.25f
            val trackH = h * 0.5f

            // Base track
            drawRoundRect(
                color = LeatherBorder,
                topLeft = Offset(0f, trackY),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Optimal zone highlight between floor and ceiling
            drawRect(
                color = HyruleSage.copy(alpha = 0.22f),
                topLeft = Offset(w * floorFrac, trackY),
                size = Size((w * (ceilingFrac - floorFrac)).coerceAtLeast(4f), trackH)
            )

            // Active fill bar
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(barColor.copy(alpha = 0.8f), barColor)
                ),
                topLeft = Offset(0f, trackY),
                size = Size((w * animatedProgress).coerceAtLeast(0f), trackH),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Floor marker
            val floorX = w * floorFrac
            drawLine(
                color = ParchmentCream,
                start = Offset(floorX, 0f),
                end = Offset(floorX, h),
                strokeWidth = 3f
            )

            // Ceiling marker
            val ceilingX = w * ceilingFrac
            drawLine(
                color = EmberCrimson,
                start = Offset(ceilingX, 0f),
                end = Offset(ceilingX, h),
                strokeWidth = 3f
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Piso: $floorKcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentCream
            )
            Text(
                text = "Techo: $ceilingKcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = EmberCrimson
            )
            Text(
                text = "Gasto: $tdeeKcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MutedParchment
            )
        }
    }
}

@Composable
fun ContainerGeometryDiagram(
    shape: ContainerShape,
    fillPercent: Int,
    modifier: Modifier = Modifier
) {
    val fillFrac = (fillPercent.coerceIn(0, 100) / 100f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(92.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round)
            val liquidColor = CrystalWaterBlue.copy(alpha = 0.35f)
            val wireColor = HoneyBrothGold

            when (shape) {
                ContainerShape.CYLINDRICAL -> {
                    val left = w * 0.22f
                    val right = w * 0.78f
                    val top = h * 0.18f
                    val bottom = h * 0.84f
                    val bodyW = right - left
                    val bodyH = bottom - top
                    val fillTop = bottom - (bodyH * fillFrac)

                    if (fillFrac > 0f) {
                        drawRect(
                            color = liquidColor,
                            topLeft = Offset(left, fillTop),
                            size = Size(bodyW, bottom - fillTop)
                        )
                    }
                    drawOval(
                        color = wireColor,
                        topLeft = Offset(left, top - 8f),
                        size = Size(bodyW, 16f),
                        style = stroke
                    )
                    drawLine(wireColor, Offset(left, top), Offset(left, bottom), strokeWidth = 3.5f)
                    drawLine(wireColor, Offset(right, top), Offset(right, bottom), strokeWidth = 3.5f)
                    drawOval(
                        color = wireColor,
                        topLeft = Offset(left, bottom - 8f),
                        size = Size(bodyW, 16f),
                        style = stroke
                    )
                }
                ContainerShape.RECTANGULAR -> {
                    val left = w * 0.16f
                    val top = h * 0.24f
                    val boxW = w * 0.58f
                    val boxH = h * 0.56f
                    val fillH = boxH * fillFrac

                    if (fillFrac > 0f) {
                        drawRect(
                            color = liquidColor,
                            topLeft = Offset(left, top + boxH - fillH),
                            size = Size(boxW, fillH)
                        )
                    }
                    drawRect(
                        color = wireColor,
                        topLeft = Offset(left, top),
                        size = Size(boxW, boxH),
                        style = stroke
                    )
                    val dx = w * 0.12f
                    val dy = -h * 0.10f
                    drawLine(wireColor, Offset(left, top), Offset(left + dx, top + dy), strokeWidth = 3f)
                    drawLine(wireColor, Offset(left + boxW, top), Offset(left + boxW + dx, top + dy), strokeWidth = 3f)
                    drawLine(wireColor, Offset(left + boxW, top + boxH), Offset(left + boxW + dx, top + boxH + dy), strokeWidth = 3f)
                    drawLine(wireColor, Offset(left + dx, top + dy), Offset(left + boxW + dx, top + dy), strokeWidth = 3f)
                    drawLine(wireColor, Offset(left + boxW + dx, top + dy), Offset(left + boxW + dx, top + boxH + dy), strokeWidth = 3f)
                }
                ContainerShape.HEMISPHERICAL -> {
                    val left = w * 0.14f
                    val right = w * 0.86f
                    val top = h * 0.34f
                    val bowlW = right - left
                    val bowlH = h * 0.50f

                    val bowlPath = Path().apply {
                        moveTo(left, top)
                        quadraticTo(w * 0.5f, top + bowlH * 1.45f, right, top)
                    }
                    if (fillFrac > 0f) {
                        val liquidTop = top + (bowlH * 0.72f) * (1f - fillFrac)
                        val fillPath = Path().apply {
                            moveTo(left + (1f - fillFrac) * 14f, liquidTop)
                            quadraticTo(w * 0.5f, top + bowlH * 1.42f, right - (1f - fillFrac) * 14f, liquidTop)
                            close()
                        }
                        drawPath(fillPath, color = liquidColor)
                    }
                    drawPath(bowlPath, color = wireColor, style = stroke)
                    drawOval(
                        color = wireColor,
                        topLeft = Offset(left, top - 8f),
                        size = Size(bowlW, 16f),
                        style = stroke
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectionAndHistoryChart(
    weightsAsc: List<WeightEntry>,
    projectionPoints: List<DeficitProjectionPoint>,
    modifier: Modifier = Modifier
) {
    val allWeights = buildList {
        weightsAsc.forEach {
            add(it.weightKg)
            add(it.theoreticalDryWeightKg)
            add(it.movingAverage7dKg)
        }
        projectionPoints.forEach { add(it.projectedWeightKg) }
    }

    if (allWeights.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Registra un pesaje o ajusta el simulador para ver la curva.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedParchment
            )
        }
        return
    }

    val minW = (allWeights.minOrNull() ?: 60.0) - 1.0
    val maxW = (allWeights.maxOrNull() ?: 80.0) + 1.0
    val spanW = (maxW - minW).coerceAtLeast(2.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val padLeft = 18f
            val padRight = 18f
            val padTop = 16f
            val padBottom = 22f
            val plotW = (size.width - padLeft - padRight).coerceAtLeast(10f)
            val plotH = (size.height - padTop - padBottom).coerceAtLeast(10f)

            fun yFor(weight: Double): Float {
                val norm = ((weight - minW) / spanW).toFloat().coerceIn(0f, 1f)
                return padTop + plotH * (1f - norm)
            }

            // Grid lines
            for (i in 0..3) {
                val gy = padTop + (plotH * i / 3f)
                drawLine(
                    color = LeatherBorder.copy(alpha = 0.6f),
                    start = Offset(padLeft, gy),
                    end = Offset(padLeft + plotW, gy),
                    strokeWidth = 1.5f
                )
            }

            // Projected curve (HoneyBrothGold dashed)
            if (projectionPoints.size >= 2) {
                val projPath = Path()
                projectionPoints.forEachIndexed { idx, pt ->
                    val x = padLeft + plotW * (idx.toFloat() / (projectionPoints.size - 1).coerceAtLeast(1))
                    val y = yFor(pt.projectedWeightKg)
                    if (idx == 0) projPath.moveTo(x, y) else projPath.lineTo(x, y)
                }
                drawPath(
                    path = projPath,
                    color = HoneyBrothGold,
                    style = Stroke(
                        width = 4.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )
            }

            // Historical curves if >= 2 weigh-ins
            if (weightsAsc.size >= 2) {
                val dryPath = Path()
                val maPath = Path()
                val denom = (weightsAsc.size - 1).coerceAtLeast(1).toFloat()

                weightsAsc.forEachIndexed { idx, item ->
                    val x = padLeft + plotW * (idx / denom)
                    val yDry = yFor(item.theoreticalDryWeightKg)
                    val yMa = yFor(item.movingAverage7dKg)
                    if (idx == 0) {
                        dryPath.moveTo(x, yDry)
                        maPath.moveTo(x, yMa)
                    } else {
                        dryPath.lineTo(x, yDry)
                        maPath.lineTo(x, yMa)
                    }
                }
                drawPath(dryPath, color = CrystalWaterBlue, style = Stroke(width = 4f, cap = StrokeCap.Round))
                drawPath(maPath, color = HyruleSage, style = Stroke(width = 4f, cap = StrokeCap.Round))

                weightsAsc.forEachIndexed { idx, item ->
                    val x = padLeft + plotW * (idx / denom)
                    val yReal = yFor(item.weightKg)
                    drawCircle(
                        color = if (item.isWhooshDetected) CrystalWaterBlue else ParchmentCream,
                        radius = if (item.isWhooshDetected) 7f else 5.5f,
                        center = Offset(x, yReal)
                    )
                }
            } else if (weightsAsc.size == 1) {
                drawCircle(
                    color = ParchmentCream,
                    radius = 7f,
                    center = Offset(padLeft, yFor(weightsAsc.first().weightKg))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot(color = HoneyBrothGold, label = "Proyección")
            LegendDot(color = HyruleSage, label = "Media 7d")
            LegendDot(color = CrystalWaterBlue, label = "Peso Seco")
            LegendDot(color = ParchmentCream, label = "Real")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedParchment)
    }
}
