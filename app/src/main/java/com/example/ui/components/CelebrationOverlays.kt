package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AchievementBanner
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.HyruleSage
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ParchmentCream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MinecraftAdvancementToast(
    banner: AchievementBanner?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (banner != null) {
            val isIdealWeight = banner.isIdealWeight
            val headerColor = if (isIdealWeight) Color(0xFFFFD700) else Color(0xFF55FF55)
            val borderColor = if (isIdealWeight) HoneyBrothGold else Color(0xFF555555)

            Surface(
                color = Color(0xFF212121),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(2.dp, borderColor),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable(onClick = onDismiss)
                    .testTag("minecraft_advancement_toast")
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF212121))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF333333),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF777777)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isIdealWeight) Icons.Default.EmojiEvents else Icons.Default.Stars,
                                contentDescription = banner.subtitleEs,
                                tint = headerColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isIdealWeight) "¡Desafío Completado!" else banner.titleEs,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = headerColor
                        )
                        Text(
                            text = banner.subtitleEs,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val xFrac: Float,
    val startYFrac: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float,
    val spinSpeed: Float,
    val swayPhase: Float,
    val color: Color
)

@Composable
fun ConfettiCelebrationOverlay(
    visible: Boolean,
    intense: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val colors = remember {
        listOf(
            HoneyBrothGold,
            HyruleSage,
            CrystalWaterBlue,
            EmberCrimson,
            ParchmentCream,
            Color(0xFFFFD700)
        )
    }

    val pieces = remember(intense) {
        val count = if (intense) 95 else 42
        val rng = Random(System.currentTimeMillis())
        List(count) {
            ConfettiPiece(
                xFrac = rng.nextFloat(),
                startYFrac = -rng.nextFloat() * 0.45f,
                vx = (rng.nextFloat() - 0.5f) * 0.22f,
                vy = 0.28f + rng.nextFloat() * 0.38f,
                sizeDp = 7f + rng.nextFloat() * 7f,
                spinSpeed = (rng.nextFloat() - 0.5f) * 360f,
                swayPhase = rng.nextFloat() * (2f * PI.toFloat()),
                color = colors[it % colors.size]
            )
        }
    }

    var elapsedSec by remember(visible, intense) { mutableFloatStateOf(0f) }

    LaunchedEffect(visible, intense) {
        var startNanos = 0L
        while (elapsedSec < 4.2f) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                elapsedSec = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val fade = (1f - ((elapsedSec - 3.0f).coerceAtLeast(0f) / 1.2f)).coerceIn(0f, 1f)

        pieces.forEach { piece ->
            val sway = sin(elapsedSec * 4.5f + piece.swayPhase) * 28f
            val x = ((piece.xFrac + piece.vx * elapsedSec) * w + sway).mod(w)
            val y = (piece.startYFrac + piece.vy * elapsedSec + 0.08f * elapsedSec * elapsedSec) * h
            if (y in -40f..(h + 40f)) {
                val angle = piece.spinSpeed * elapsedSec
                val pxSize = piece.sizeDp.dp.toPx()
                val scaleY = cos(elapsedSec * 6f + piece.swayPhase).coerceIn(0.3f, 1f)
                rotate(degrees = angle, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = fade),
                        topLeft = Offset(x - pxSize / 2f, y - (pxSize * scaleY) / 2f),
                        size = Size(pxSize, pxSize * 0.65f * scaleY)
                    )
                }
            }
        }
    }
}
