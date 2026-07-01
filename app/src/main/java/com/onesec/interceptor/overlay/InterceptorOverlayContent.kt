package com.onesec.interceptor.overlay

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onesec.interceptor.ui.theme.CalmBlue
import com.onesec.interceptor.ui.theme.OverlayScrim
import kotlinx.coroutines.delay

private enum class OverlayPhase { BREATHING, CONFIRM }

@Composable
fun InterceptorOverlayContent(
    appLabel: String,
    breathingSeconds: Int,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    var phase by remember { mutableStateOf(OverlayPhase.BREATHING) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayScrim.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = phase, label = "overlay-phase") { current ->
            when (current) {
                OverlayPhase.BREATHING -> BreathingScreen(
                    totalSeconds = breathingSeconds,
                    onFinished = { phase = OverlayPhase.CONFIRM }
                )
                OverlayPhase.CONFIRM -> ConfirmScreen(
                    appLabel = appLabel,
                    onAllow = onAllow,
                    onDeny = onDeny
                )
            }
        }
    }
}

@Composable
private fun BreathingScreen(totalSeconds: Int, onFinished: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var breatheInLabel by remember { mutableStateOf(true) }

    // Drives the pulsating circle: 4s inhale, 4s exhale, repeating.
    val scale = remember { Animatable(0.65f) }
    LaunchedEffect(Unit) {
        while (true) {
            breatheInLabel = true
            scale.animateTo(1f, animationSpec = tween(4000, easing = FastOutSlowInEasing))
            breatheInLabel = false
            scale.animateTo(0.65f, animationSpec = tween(4000, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(totalSeconds) {
        for (remaining in totalSeconds downTo 1) {
            secondsLeft = remaining
            delay(1000)
        }
        onFinished()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CalmBlue, CalmBlue.copy(alpha = 0.15f))
                    )
                )
        )
        Spacer(Modifier.height(40.dp))
        Text(
            text = if (breatheInLabel) "Einatmen …" else "Ausatmen …",
            color = Color.White,
            fontSize = 24.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = secondsLeft.toString(),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ConfirmScreen(appLabel: String, onAllow: () -> Unit, onDeny: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Möchtest du $appLabel wirklich öffnen?",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row {
                OutlinedButton(onClick = onDeny) {
                    Text("Nein, zurück zum Homescreen")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onAllow,
                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                ) {
                    Text("Ja, öffnen")
                }
            }
        }
    }
}
