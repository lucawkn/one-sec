package com.luca.appinterceptor.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val BackgroundColor = Color(0xF50E1116)
private val AccentColor = Color(0xFF4FC3F7)

@Composable
fun OverlayScreen(
    appLabel: String,
    countdownSeconds: Int,
    onOpen: () -> Unit,
    onGoHome: () -> Unit,
) {
    var secondsLeft by remember { mutableIntStateOf(countdownSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        if (secondsLeft > 0) {
            BreathingPhase(appLabel = appLabel, secondsLeft = secondsLeft)
        } else {
            DecisionPhase(appLabel = appLabel, onOpen = onOpen, onGoHome = onGoHome)
        }
    }
}

@Composable
private fun BreathingPhase(appLabel: String, secondsLeft: Int) {
    val scale = remember { Animatable(0.55f) }
    var inhale by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            inhale = true
            scale.animateTo(1f, tween(4000, easing = FastOutSlowInEasing))
            inhale = false
            scale.animateTo(0.55f, tween(4000, easing = FastOutSlowInEasing))
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Einen Moment. Du wolltest gerade $appLabel öffnen.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(40.dp))
        Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(AccentColor.copy(alpha = 0.45f), Color.Transparent)
                        ),
                        CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .background(AccentColor.copy(alpha = 0.9f), CircleShape),
            )
            Text(
                text = if (inhale) "Ein…" else "Aus…",
                color = Color(0xFF0E1116),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = "$secondsLeft",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Sekunden bis zur Entscheidung",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun DecisionPhase(
    appLabel: String,
    onOpen: () -> Unit,
    onGoHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Möchtest du",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp,
        )
        Text(
            text = appLabel,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "wirklich öffnen?",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(48.dp))
        // "Nein" bewusst als prominenter Primär-Button (sanfter Nudge)
        Button(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color(0xFF0E1116)),
        ) {
            Text("Nein, zurück zum Homescreen", fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp))
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ja, $appLabel öffnen", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}
