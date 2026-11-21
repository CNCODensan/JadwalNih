package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalAtmospherePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashMotionOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    var visible by remember { mutableStateOf(true) }
    var hasNotifiedFinish by remember { mutableStateOf(false) }

    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.55f) }
    val iconRotation = remember { Animatable(-10f) }
    val haloScale = remember { Animatable(0.7f) }
    val haloAlpha = remember { Animatable(0.65f) }
    val labelAlpha = remember { Animatable(0f) }
    val labelOffsetY = remember { Animatable(24f) }

    fun finishOnce() {
        if (!hasNotifiedFinish) {
            hasNotifiedFinish = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            iconAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            )
        }
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            iconRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            haloScale.animateTo(
                targetValue = 1.45f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
        launch {
            haloAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
        launch {
            delay(140L)
            launch {
                labelAlpha.animateTo(1f, animationSpec = tween(durationMillis = 320))
            }
            labelOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            )
        }
        delay(980L)
        visible = false
        delay(280L)
        finishOnce()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 280)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(atmo.bgTop)
                .pointerInput(Unit) {
                    detectTapGestures {
                        visible = false
                        finishOnce()
                    }
                }
                .semantics(mergeDescendants = false) {
                    onClick(label = "Lewati splash screen") {
                        visible = false
                        finishOnce()
                        true
                    }
                }
                .testTag("splash_motion_overlay"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer {
                                scaleX = haloScale.value
                                scaleY = haloScale.value
                                alpha = haloAlpha.value
                            }
                            .clip(CircleShape)
                            .background(atmo.accentCobalt.copy(alpha = 0.25f)),
                    )

                    Image(
                        painter = painterResource(id = R.drawable.app_brand_icon),
                        contentDescription = "Ikon aplikasi JadwalNih",
                        modifier = Modifier
                            .size(116.dp)
                            .graphicsLayer {
                                alpha = iconAlpha.value
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                                rotationZ = iconRotation.value
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .testTag("splash_app_icon"),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = labelAlpha.value
                        translationY = labelOffsetY.value
                    },
                ) {
                    Text(
                        text = "JadwalNih",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            atmo.accentCobalt,
                            atmo.accentCoral,
                            atmo.accentAmber,
                            atmo.accentMint,
                        ).forEach { dotColor ->
                            Surface(
                                color = dotColor,
                                shape = CircleShape,
                                modifier = Modifier.size(8.dp),
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
