
package com.hungyplusthamasheen.masheensignals

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val V3_DURATION_MULTIPLIER = 4L

private enum class HapticTexture { SOFT, CRISP }
private enum class BurstShape { CIRCLE, SQUARE, DIAMOND }

private data class Signal(
    val name: String,
    val color: Color,
    val description: String,
    val timings: LongArray,
    val texture: HapticTexture,
    val frequencyHz: Double,
    val harmonicMix: Double,
    val audioBaseVolume: Double
)

private data class Pulse(
    val startMs: Long,
    val durationMs: Long,
    val strength: Float
)

private val signals = listOf(
    Signal("Affection", Color(0xFFE879A8), "Gentle, flowing pulses",
        longArrayOf(0, 90, 70, 140, 90, 190),
        HapticTexture.SOFT, 392.0, 0.18, 0.22),

    Signal("Joy", Color(0xFFFFD54F), "Light, bouncing rhythm",
        longArrayOf(0, 65, 65, 65, 110, 65),
        HapticTexture.SOFT, 660.0, 0.28, 0.28),

    Signal("Curiosity", Color(0xFF4DD0E1), "Two inquisitive pulses",
        longArrayOf(0, 80, 130, 160),
        HapticTexture.CRISP, 523.25, 0.12, 0.20),

    Signal("Calm", Color(0xFF64B5F6), "Slow, sustained presence",
        longArrayOf(0, 260, 220),
        HapticTexture.SOFT, 293.66, 0.08, 0.16),

    Signal("Frustration", Color(0xFFEF5350), "Firm, interrupted rhythm",
        longArrayOf(0, 180, 90, 75, 70, 180),
        HapticTexture.CRISP, 784.0, 0.35, 0.34),

    Signal("Sadness", Color(0xFF7986CB), "Slow, spaced pulses",
        longArrayOf(0, 210, 260, 140),
        HapticTexture.SOFT, 246.94, 0.06, 0.15),

    Signal("Excitement", Color(0xFFFF9800), "Rapid, energetic rhythm",
        longArrayOf(0, 55, 45, 55, 45, 95, 45, 55),
        HapticTexture.CRISP, 880.0, 0.32, 0.32),

    Signal("Celebration", Color(0xFFAB80D6), "Layered celebratory rhythm",
        longArrayOf(0, 70, 70, 130, 70, 70, 70, 190),
        HapticTexture.SOFT, 587.33, 0.25, 0.26)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MasheenSignalsScreen(this) }
    }
}

private fun buildPulseTimeline(
    signal: Signal,
    durationMultiplier: Float
): List<Pulse> {
    val pulses = mutableListOf<Pulse>()
    var cursor = 0L

    val pulseCount = signal.timings.countIndexed { index, duration ->
        index % 2 == 1 && duration > 0
    }

    var pulseIndex = 0

    signal.timings.forEachIndexed { index, originalDuration ->
        val duration = (originalDuration * durationMultiplier).toLong()

        if (index % 2 == 1 && duration > 0) {
            val position = if (pulseCount <= 1) {
                0.5f
            } else {
                pulseIndex.toFloat() / (pulseCount - 1)
            }

            // Intensity rises toward the middle and falls toward the end.
            val strength = 0.55f + 0.45f * (1f - abs(2f * position - 1f))

            pulses += Pulse(cursor, duration, strength)
            pulseIndex++
        }

        cursor += duration
    }

    return pulses
}

private inline fun LongArray.countIndexed(
    predicate: (Int, Long) -> Boolean
): Int {
    var count = 0
    forEachIndexed { index, value ->
        if (predicate(index, value)) count++
    }
    return count
}

@Composable
private fun MasheenSignalsScreen(context: Context) {
    var selected by remember { mutableIntStateOf(0) }

    var hapticStrength by remember { mutableFloatStateOf(3f) }
    var durationMultiplier by remember { mutableFloatStateOf(4f) }
    var audioVolume by remember { mutableFloatStateOf(1f) }
    var fadeAmount by remember { mutableFloatStateOf(0.12f) }
    var colorIntensity by remember { mutableFloatStateOf(3f) }

    var burstCount by remember { mutableFloatStateOf(3f) }
    var burstSize by remember { mutableFloatStateOf(1f) }
    var screenCoverage by remember { mutableFloatStateOf(1f) }
    var burstShape by remember { mutableStateOf(BurstShape.CIRCLE) }

    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    var previewTrigger by remember { mutableIntStateOf(0) }

    val burstProgress = remember { Animatable(1f) }
    val signal = signals[selected]

    val pulses = remember(signal, durationMultiplier) {
        buildPulseTimeline(signal, durationMultiplier)
    }

    val totalDurationMs = remember(signal, durationMultiplier) {
        signal.timings.sumOf {
            (it * durationMultiplier).toLong()
        }.coerceAtLeast(1L)
    }

    LaunchedEffect(previewTrigger, totalDurationMs) {
        if (previewTrigger > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = totalDurationMs.coerceAtMost(12000L).toInt()
                )
            )
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "MASHEEN SIGNALS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Text(
                    "HLP • Expressive communication prototype • V4",
                    color = Color(0xFFB8C7D1)
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Selected expression",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(
                                Modifier
                                    .size(24.dp)
                                    .background(signal.color)
                            )
                            Text(
                                signal.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Text(signal.description)

                        Text("Haptic strength: ${hapticStrength.toInt()} / 5")
                        Slider(
                            value = hapticStrength,
                            onValueChange = { hapticStrength = it },
                            valueRange = 1f..5f,
                            steps = 3
                        )

                        Text("Haptic duration: ${durationMultiplier}×")
                        Slider(
                            value = durationMultiplier,
                            onValueChange = { durationMultiplier = it },
                            valueRange = 2f..6f,
                            steps = 7
                        )

                        Text("Tone: ${signal.frequencyHz.toInt()} Hz")
                        Text("Visual intensity: ${colorIntensity.toInt()}×")
                    }
                }

                Text(
                    "Choose an expression",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                signals.forEachIndexed { index, item ->
                    Button(
                        onClick = { selected = index },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selected == index) "●  ${item.name}"
                            else item.name
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Output controls",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Vibration")
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { vibrationEnabled = it }
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sound")
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it }
                            )
                        }

                        Text("Audio volume: ${(audioVolume * 100).toInt()}%")
                        Slider(
                            value = audioVolume,
                            onValueChange = { audioVolume = it },
                            valueRange = 0f..1f,
                            steps = 9
                        )

                        Text("Fade-in / fade-out: ${(fadeAmount * 100).toInt()}%")
                        Slider(
                            value = fadeAmount,
                            onValueChange = { fadeAmount = it },
                            valueRange = 0f..0.4f,
                            steps = 7
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Visual controls",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Color intensity: ${colorIntensity.toInt()}×")
                        Slider(
                            value = colorIntensity,
                            onValueChange = { colorIntensity = it },
                            valueRange = 1f..3f,
                            steps = 3
                        )

                        Text("Burst count: ${burstCount.toInt()}")
                        Slider(
                            value = burstCount,
                            onValueChange = { burstCount = it },
                            valueRange = 1f..6f,
                            steps = 4
                        )

                        Text("Burst size: ${(burstSize * 100).toInt()}%")
                        Slider(
                            value = burstSize,
                            onValueChange = { burstSize = it },
                            valueRange = 0.5f..1.5f,
                            steps = 9
                        )

                        Text("Screen coverage: ${(screenCoverage * 100).toInt()}%")
                        Slider(
                            value = screenCoverage,
                            onValueChange = { screenCoverage = it },
                            valueRange = 0.5f..1.5f,
                            steps = 9
                        )

                        Text("Burst shape: ${burstShape.name.lowercase()}")

                        Button(
                            onClick = {
                                burstShape = when (burstShape) {
                                    BurstShape.CIRCLE -> BurstShape.SQUARE
                                    BurstShape.SQUARE -> BurstShape.DIAMOND
                                    BurstShape.DIAMOND -> BurstShape.CIRCLE
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change burst shape")
                        }
                    }
                }

                Button(
                    onClick = {
                        previewSignal(
                            context = context,
                            signal = signal,
                            pulses = pulses,
                            hapticStrength = hapticStrength.toInt(),
                            durationMultiplier = durationMultiplier,
                            audioVolume = audioVolume,
                            fadeAmount = fadeAmount,
                            vibration = vibrationEnabled,
                            sound = soundEnabled
                        )
                        previewTrigger++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PREVIEW V4 EXPRESSION")
                }

                Text(
                    "V4 uses one shared timing and intensity envelope to coordinate vibration, audio and visuals.",
                    color = Color(0xFFB8C7D1)
                )

                Text(
                    "Expressions are designed communication signals, not claims that a device literally experiences emotions.",
                    color = Color(0xFFB8C7D1)
                )

                Spacer(Modifier.height(24.dp))
            }

            // Visual bursts follow the same pulse timeline as audio and haptics.
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (previewTrigger > 0 && burstProgress.value < 1f) {
                    val elapsed = burstProgress.value * totalDurationMs
                    val activePulse = pulses.firstOrNull {
                        elapsed >= it.startMs &&
                            elapsed < it.startMs + it.durationMs
                    }

                    if (activePulse != null) {
                        val localProgress = (
                            (elapsed - activePulse.startMs) /
                                activePulse.durationMs
                            ).toFloat().coerceIn(0f, 1f)

                        val envelope = sin(PI * localProgress).toFloat()
                        val center = Offset(size.width / 2f, size.height / 2f)

                        val maxRadius = max(size.width, size.height) *
                            0.55f * screenCoverage * burstSize

                        val visualStrength = (
                            envelope * activePulse.strength
                            ).coerceIn(0f, 1f)

                        val alpha = (
                            0.10f +
                                0.65f * visualStrength *
                                (colorIntensity / 3f)
                            ).coerceIn(0f, 0.85f)

                        for (ring in 0 until burstCount.toInt()) {
                            val ringOffset = ring * 0.12f
                            val ringProgress = (
                                (localProgress - ringOffset) /
                                    (1f - ringOffset)
                                ).coerceIn(0f, 1f)

                            val radius = maxRadius *
                                (0.12f + 0.88f * ringProgress)

                            val ringAlpha = alpha *
                                (1f - ring * 0.10f)

                            val ringColor = signal.color.copy(
                                alpha = ringAlpha
                            )

                            when (burstShape) {
                                BurstShape.CIRCLE -> {
                                    drawCircle(
                                        color = ringColor,
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = 7.dp.toPx())
                                    )
                                    drawCircle(
                                        color = ringColor.copy(alpha = ringAlpha * 0.18f),
                                        radius = radius,
                                        center = center
                                    )
                                }

                                BurstShape.SQUARE -> {
                                    drawRect(
                                        color = ringColor,
                                        topLeft = Offset(
                                            center.x - radius,
                                            center.y - radius
                                        ),
                                        size = Size(radius * 2f, radius * 2f),
                                        style = Stroke(width = 7.dp.toPx())
                                    )
                                }

                                BurstShape.DIAMOND -> {
                                    val path = Path().apply {
                                        moveTo(center.x, center.y - radius)
                                        lineTo(center.x + radius, center.y)
                                        lineTo(center.x, center.y + radius)
                                        lineTo(center.x - radius, center.y)
                                        close()
                                    }

                                    drawPath(
                                        path = path,
                                        color = ringColor,
                                        style = Stroke(width = 7.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun previewSignal(
    context: Context,
    signal: Signal,
    pulses: List<Pulse>,
    hapticStrength: Int,
    durationMultiplier: Float,
    audioVolume: Float,
    fadeAmount: Float,
    vibration: Boolean,
    sound: Boolean
) {
    if (vibration) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            val peak = (50 + (hapticStrength - 1) * 51.25f)
                .roundToInt()
                .coerceIn(1, 255)

            val timingParts = mutableListOf<Long>()
            val amplitudeParts = mutableListOf<Int>()

            var pulseIndex = 0
            val pulseCount = pulses.size

            signal.timings.forEachIndexed { index, originalDuration ->
                val duration = (originalDuration * durationMultiplier).toLong()

                if (index % 2 == 0) {
                    // Silent gaps retain the same timing as the shared envelope.
                    timingParts += duration
                    amplitudeParts += 0
                } else {
                    val pulse = pulses.getOrNull(pulseIndex)
                    val pulseStrength = pulse?.strength ?: 1f
                    val pulsePeak = (peak * pulseStrength)
                        .roundToInt()
                        .coerceIn(1, 255)

                    if (signal.texture == HapticTexture.SOFT) {
                        val first = (duration * 0.25f).toLong().coerceAtLeast(1L)
                        val second = (duration * 0.50f).toLong().coerceAtLeast(1L)
                        val third = (duration - first - second).coerceAtLeast(1L)

                        timingParts += first
                        amplitudeParts += (pulsePeak * 0.42f).roundToInt().coerceIn(1, 255)

                        timingParts += second
                        amplitudeParts += (pulsePeak * 0.78f).roundToInt().coerceIn(1, 255)

                        timingParts += third
                        amplitudeParts += (pulsePeak * 0.48f).roundToInt().coerceIn(1, 255)
                    } else {
                        // Crisp pulses keep a strong, immediate tactile edge.
                        timingParts += duration
                        amplitudeParts += pulsePeak
                    }

                    pulseIndex++
                }
            }

            vibrator.cancel()

            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    timingParts.toLongArray(),
                    amplitudeParts.toIntArray(),
                    -1
                )
            )
        }
    }

    if (sound) {
        playExpressionTone(
            signal = signal,
            pulses = pulses,
            totalDurationMs = signal.timings.sumOf {
                (it * durationMultiplier).toLong()
            },
            hapticStrength = hapticStrength,
            audioVolume = audioVolume,
            fadeAmount = fadeAmount
        )
    }
}

private var activeAudioTrack: AudioTrack? = null

private fun playExpressionTone(
    signal: Signal,
    pulses: List<Pulse>,
    totalDurationMs: Long,
    hapticStrength: Int,
    audioVolume: Float,
    fadeAmount: Float
) {
    val sampleRate = 22_050
    val safeDuration = totalDurationMs.coerceAtLeast(1L)
    val sampleCount = (sampleRate * safeDuration / 1000L)
        .coerceAtLeast(1L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

    val intensityGain = 0.5 + (hapticStrength - 1) * 0.125
    val volume = min(
        0.95,
        signal.audioBaseVolume * intensityGain * audioVolume
    )

    // The sample buffer begins silent. Sound is added only during haptic pulses.
    val samples = ShortArray(sampleCount)

    pulses.forEach { pulse ->
        val startSample = (
            pulse.startMs * sampleRate / 1000L
            ).toInt().coerceIn(0, sampleCount)

        val endSample = (
            (pulse.startMs + pulse.durationMs) * sampleRate / 1000L
            ).toInt().coerceIn(0, sampleCount)

        val pulseSamples = endSample - startSample
        if (pulseSamples <= 0) return@forEach

        for (sample in startSample until endSample) {
            val localProgress = (
                (sample - startSample).toDouble() /
                    max(1, pulseSamples - 1)
                )

            val fade = fadeAmount.toDouble()
            val attack = if (fade <= 0.0) {
                1.0
            } else {
                min(1.0, localProgress / fade)
            }

            val release = if (fade <= 0.0) {
                1.0
            } else {
                min(1.0, (1.0 - localProgress) / fade)
            }

            val envelope = min(attack, release) * pulse.strength

            val time = sample.toDouble() / sampleRate
            val fundamental = sin(2.0 * PI * signal.frequencyHz * time)
            val harmonic = sin(2.0 * PI * signal.frequencyHz * 2.0 * time)

            val wave = fundamental * (1.0 - signal.harmonicMix) +
                harmonic * signal.harmonicMix

            samples[sample] = (
                wave * envelope * volume * Short.MAX_VALUE
                ).toInt().toShort()
        }
    }

    val format = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    try {
        // Stop a previous preview before starting another.
        activeAudioTrack?.let { previous ->
            try {
                previous.stop()
            } catch (_: IllegalStateException) {
            }
            previous.release()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()

        activeAudioTrack = track

        track.write(samples, 0, samples.size)
        track.play()

        Handler(Looper.getMainLooper()).postDelayed({
            if (activeAudioTrack === track) {
                activeAudioTrack = null

                try {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                } catch (_: IllegalStateException) {
                }

                track.release()
            }
        }, safeDuration + 500L)

    } catch (_: Exception) {
        // Audio is optional; vibration and visuals remain available.
    }
}
