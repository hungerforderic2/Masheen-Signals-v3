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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val V3_DURATION_MULTIPLIER = 4L // +300% = 4x the original duration

private enum class HapticTexture { SOFT, CRISP }

private data class Signal(
    val name: String,
    val color: Color,
    val description: String,
    val timings: LongArray,
    val texture: HapticTexture,
    val frequencyHz: Double,
    val harmonicMix: Double,
    val audioBaseDurationMs: Int,
    val audioBaseVolume: Double
)

private val signals = listOf(
    Signal("Affection", Color(0xFFE879A8), "Gentle, flowing pulses", longArrayOf(0, 90, 70, 140, 90, 190), HapticTexture.SOFT, 392.0, 0.18, 220, 0.22),
    Signal("Joy", Color(0xFFFFD54F), "Light, bouncing rhythm", longArrayOf(0, 65, 65, 65, 110, 65), HapticTexture.SOFT, 660.0, 0.28, 180, 0.28),
    Signal("Curiosity", Color(0xFF4DD0E1), "Two inquisitive pulses", longArrayOf(0, 80, 130, 160), HapticTexture.CRISP, 523.25, 0.12, 240, 0.20),
    Signal("Calm", Color(0xFF64B5F6), "Slow, sustained presence", longArrayOf(0, 260, 220), HapticTexture.SOFT, 293.66, 0.08, 320, 0.16),
    Signal("Frustration", Color(0xFFEF5350), "Firm, interrupted rhythm", longArrayOf(0, 180, 90, 75, 70, 180), HapticTexture.CRISP, 784.0, 0.35, 150, 0.34),
    Signal("Sadness", Color(0xFF7986CB), "Slow, spaced pulses", longArrayOf(0, 210, 260, 140), HapticTexture.SOFT, 246.94, 0.06, 300, 0.15),
    Signal("Excitement", Color(0xFFFF9800), "Rapid, energetic rhythm", longArrayOf(0, 55, 45, 55, 45, 95, 45, 55), HapticTexture.CRISP, 880.0, 0.32, 210, 0.32),
    Signal("Celebration", Color(0xFFAB80D6), "Layered celebratory rhythm", longArrayOf(0, 70, 70, 130, 70, 70, 70, 190), HapticTexture.SOFT, 587.33, 0.25, 280, 0.26)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MasheenSignalsScreen(this) }
    }
}

@Composable
private fun MasheenSignalsScreen(context: Context) {
    var selected by remember { mutableIntStateOf(0) }
    var intensity by remember { mutableFloatStateOf(3f) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var burstTrigger by remember { mutableIntStateOf(0) }
    val burstProgress = remember { Animatable(1f) }
    val signal = signals[selected]

    LaunchedEffect(burstTrigger) {
        if (burstTrigger > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF101820))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("MASHEEN SIGNALS", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("HLP • Expressive communication prototype • V3", color = Color(0xFFB8C7D1))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Selected signal", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Spacer(Modifier.size(24.dp).background(signal.color))
                            Text(signal.name, style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(signal.description)
                        Text("Intensity: ${intensity.toInt()} / 5")
                        Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 1f..5f, steps = 3)
                        Text("Haptic texture: ${if (signal.texture == HapticTexture.SOFT) "Soft / rounded" else "Crisp / sharp"}")
                        Text("Audio: ${signal.frequencyHz.toInt()} Hz • ${(signal.audioBaseDurationMs * V3_DURATION_MULTIPLIER).toInt()} ms")
                    }
                }
                Text("Choose an expression", color = Color.White, style = MaterialTheme.typography.titleMedium)
                signals.forEachIndexed { index, item ->
                    Button(onClick = { selected = index }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selected == index) "●  ${item.name}" else item.name)
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Vibration")
                            Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Sound")
                            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                        }
                        Button(
                            onClick = {
                                previewSignal(context, signal, intensity.toInt(), vibrationEnabled, soundEnabled)
                                burstTrigger++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Preview V3 signal") }
                    }
                }
                Text("V3 synchronizes haptics, synthesized audio and a center-originating color burst. Device hardware affects the final feel and loudness.", color = Color(0xFFB8C7D1))
                Text("Signals are designed expressions, not claims that a device literally feels emotions.", color = Color(0xFFB8C7D1))
            }

            // Three expanding rings radiate from the screen center for each preview.
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (burstTrigger > 0 && burstProgress.value < 1f) {
                    val progress = burstProgress.value
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = max(size.width, size.height) * 0.78f
                    val strength = 0.10f + intensity * 0.035f
                    for (ring in 0..2) {
                        val ringProgress = (progress - ring * 0.13f).coerceIn(0f, 1f)
                        val radius = maxRadius * ringProgress
                        val alpha = (1f - ringProgress) * strength * (1f - ring * 0.18f)
                        if (alpha > 0f) {
                            drawCircle(signal.color.copy(alpha = alpha * 0.34f), radius = radius, center = center)
                            drawCircle(signal.color.copy(alpha = alpha), radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

private fun previewSignal(context: Context, signal: Signal, intensity: Int, vibration: Boolean, sound: Boolean) {
    if (vibration) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            val scale = 0.65f + (intensity - 1) * 0.18f
            val peak = (70 + intensity * 37).coerceIn(1, 255)
            val timingParts = mutableListOf<Long>()
            val amplitudeParts = mutableListOf<Int>()
            signal.timings.forEachIndexed { index, originalDuration ->
                val duration = (originalDuration / scale * V3_DURATION_MULTIPLIER).toLong()
                if (duration <= 0L) {
                    timingParts += 0L
                    amplitudeParts += 0
                } else if (index % 2 == 0) {
                    // Preserve the original silent gaps, expanded to four times their duration.
                    timingParts += duration
                    amplitudeParts += 0
                } else if (signal.texture == HapticTexture.SOFT) {
                    // Three-stage rounded envelope: rise, broad center, gentle fall.
                    val first = (duration * 0.25).toLong().coerceAtLeast(1L)
                    val second = (duration * 0.50).toLong().coerceAtLeast(1L)
                    val third = (duration - first - second).coerceAtLeast(1L)
                    timingParts += first; amplitudeParts += (peak * 0.42f).toInt().coerceAtLeast(1)
                    timingParts += second; amplitudeParts += (peak * 0.78f).toInt().coerceAtLeast(1)
                    timingParts += third; amplitudeParts += (peak * 0.48f).toInt().coerceAtLeast(1)
                } else {
                    // Crisp accents retain a strong, immediate tactile edge.
                    timingParts += duration
                    amplitudeParts += peak
                }
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timingParts.toLongArray(), amplitudeParts.toIntArray(), -1))
        }
    }
    if (sound) playExpressionTone(signal, intensity)
}

private fun playExpressionTone(signal: Signal, intensity: Int) {
    val sampleRate = 22_050
    val durationMs = signal.audioBaseDurationMs * V3_DURATION_MULTIPLIER.toInt()
    val sampleCount = sampleRate * durationMs / 1000
    val intensityGain = 0.55 + (intensity - 1) * 0.1125
    val volume = min(0.92, signal.audioBaseVolume * intensityGain)
    val samples = ShortArray(sampleCount)

    for (i in samples.indices) {
        val t = i.toDouble() / sampleRate
        val progress = i.toDouble() / max(1, sampleCount - 1)
        val attack = min(1.0, progress / 0.045)
        val release = min(1.0, (1.0 - progress) / 0.12)
        val envelope = min(attack, release)
        val fundamental = sin(2.0 * PI * signal.frequencyHz * t)
        val harmonic = sin(2.0 * PI * signal.frequencyHz * 2.0 * t)
        val wave = (fundamental * (1.0 - signal.harmonicMix) + harmonic * signal.harmonicMix)
        samples[i] = (wave * envelope * volume * Short.MAX_VALUE).toInt().toShort()
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
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            } catch (_: IllegalStateException) { }
            track.release()
        }, durationMs.toLong() + 350L)
    } catch (_: Exception) {
        // Audio is optional; haptics and visuals remain usable if audio initialization fails.
    }
}
