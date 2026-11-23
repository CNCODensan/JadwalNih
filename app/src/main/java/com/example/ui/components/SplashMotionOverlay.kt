package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalAtmospherePalette
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashMotionOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    var visible by remember { mutableStateOf(true) }
    var hasNotifiedFinish by remember { mutableStateOf(false) }

    val timeGreeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..10 -> "Selamat Pagi! Yuk mulai hari dengan tenang"
            in 11..14 -> "Selamat Siang! Tetap fokus di prioritasmu"
            in 15..18 -> "Selamat Sore! Cek jadwal & waktu ibadahmu"
            else -> "Selamat Malam! Rekap harimu dan siapkan esok"
        }
    }

    val statusSteps = remember {
        listOf(
            "Menyiapkan jadwal harian & mingguan...",
            "Memuat kalender hari raya & tema...",
            "Menyesuaikan jadwal sholat & ibadah...",
            "Menyegarkan fokus & rekap fotomu...",
        )
    }

    val helpfulTips = remember {
        listOf(
            "Tips: Tempel banyak jadwal sekaligus dari AI di tombol +.",
            "Tips: Pilih ikon favorit biar jadwal makin gampang dikenali.",
            "Tips: Ketuk kartu bayangan minggu depan buat edit lebih awal.",
            "Tips: Ketuk layar ini kapan saja kalau mau langsung masuk.",
        )
    }

    var stepIndex by remember { mutableIntStateOf(0) }

    // Icon choreography states
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.48f) }
    val iconRotation = remember { Animatable(-14f) }
    val iconFloatY = remember { Animatable(0f) }
    val sheenProgress = remember { Animatable(-0.6f) }

    // Dual staggered expanding wave rings
    val wave1Progress = remember { Animatable(0f) }
    val wave2Progress = remember { Animatable(0f) }

    // Orbital track & revolving colored dots
    val orbitAlpha = remember { Animatable(0f) }
    val orbitAngleDeg = remember { Animatable(-35f) }

    // Title + tagline + progress + bouncing dots
    val labelAlpha = remember { Animatable(0f) }
    val labelOffsetY = remember { Animatable(26f) }
    val taglineAlpha = remember { Animatable(0f) }
    val dotBouncePhase = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    fun finishOnce() {
        if (!hasNotifiedFinish) {
            hasNotifiedFinish = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Phase 1: Icon bouncy entrance + first wave ring
        launch {
            iconAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            )
        }
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
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
            wave1Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            )
        }
        launch {
            delay(280L)
            wave2Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            )
        }

        // Smooth 3.85s progress bar animation
        launch {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3850, easing = LinearEasing),
            )
        }

        // Staged loading status & tips updates
        launch {
            delay(980L)
            stepIndex = 1
            delay(1000L)
            stepIndex = 2
            delay(1000L)
            stepIndex = 3
        }

        // Phase 2: Orbital track fade-in & smooth rotation
        launch {
            delay(180L)
            launch {
                orbitAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                )
            }
            orbitAngleDeg.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            )
        }

        // Phase 3: Title, tagline, diagonal icon sheen, and gentle float settle
        launch {
            delay(300L)
            launch {
                labelAlpha.animateTo(1f, animationSpec = tween(durationMillis = 460))
            }
            labelOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            delay(500L)
            taglineAlpha.animateTo(1f, animationSpec = tween(durationMillis = 420))
        }
        launch {
            delay(650L)
            sheenProgress.animateTo(
                targetValue = 1.6f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
        }
        launch {
            delay(850L)
            iconFloatY.animateTo(
                targetValue = -8f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
            iconFloatY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }

        // Phase 4: Staggered 4-dot playful wave bounce
        launch {
            delay(450L)
            dotBouncePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3200, easing = LinearEasing),
            )
        }

        // Total ~4.27s (3950ms active loading motion + 320ms exit transition)
        delay(3950L)
        visible = false
        delay(320L)
        finishOnce()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 320)) +
            scaleOut(targetScale = 0.96f, animationSpec = tween(durationMillis = 320)),
        modifier = modifier,
    ) {
        val accentColors = remember(atmo) {
            listOf(atmo.accentCobalt, atmo.accentCoral, atmo.accentAmber, atmo.accentMint)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(atmo.bgTop, atmo.surfaceSubtle, atmo.bgTop),
                    ),
                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(216.dp),
                ) {
                    // Ambient radial halo + expanding wave rings + rotating orbital track & nodes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension * 0.25f

                        // Soft ambient glow behind icon
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    atmo.accentCobalt.copy(alpha = 0.22f * iconAlpha.value),
                                    atmo.accentMint.copy(alpha = 0.10f * iconAlpha.value),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.minDimension * 0.48f,
                            ),
                            radius = size.minDimension * 0.48f,
                            center = center,
                        )

                        // Expanding wave ring 1
                        val w1 = wave1Progress.value
                        if (w1 in 0.01f..0.99f) {
                            val r1 = baseRadius * (0.95f + w1 * 1.10f)
                            drawCircle(
                                color = atmo.accentCobalt.copy(alpha = (1f - w1) * 0.42f),
                                radius = r1,
                                center = center,
                                style = Stroke(width = (3.5f - w1 * 2f).dp.toPx()),
                            )
                        }

                        // Expanding wave ring 2
                        val w2 = wave2Progress.value
                        if (w2 in 0.01f..0.99f) {
                            val r2 = baseRadius * (0.95f + w2 * 1.15f)
                            drawCircle(
                                color = atmo.accentMint.copy(alpha = (1f - w2) * 0.36f),
                                radius = r2,
                                center = center,
                                style = Stroke(width = (3.2f - w2 * 1.8f).dp.toPx()),
                            )
                        }

                        // Subtle dashed orbital ring + 4 revolving colored nodes
                        val orbAlpha = orbitAlpha.value
                        if (orbAlpha > 0.01f) {
                            val orbitRadius = size.minDimension * 0.38f
                            drawCircle(
                                color = atmo.accentCobalt.copy(alpha = 0.20f * orbAlpha),
                                radius = orbitRadius,
                                center = center,
                                style = Stroke(
                                    width = 1.6.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f),
                                    cap = StrokeCap.Round,
                                ),
                            )

                            accentColors.forEachIndexed { idx, nodeColor ->
                                val angleRad = ((orbitAngleDeg.value + idx * 90f) * PI / 180.0).toFloat()
                                val nx = center.x + cos(angleRad) * orbitRadius
                                val ny = center.y + sin(angleRad) * orbitRadius
                                drawCircle(
                                    color = nodeColor.copy(alpha = 0.24f * orbAlpha),
                                    radius = 9.dp.toPx(),
                                    center = Offset(nx, ny),
                                )
                                drawCircle(
                                    color = nodeColor.copy(alpha = 0.92f * orbAlpha),
                                    radius = 4.5.dp.toPx(),
                                    center = Offset(nx, ny),
                                )
                            }
                        }
                    }

                    // Brand icon with diagonal sheen sweep clipped to smooth rounded square
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .graphicsLayer {
                                alpha = iconAlpha.value
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                                rotationZ = iconRotation.value
                                translationY = iconFloatY.value
                            }
                            .clip(RoundedCornerShape(30.dp)),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_brand_icon),
                            contentDescription = "Ikon aplikasi JadwalNih",
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("splash_app_icon"),
                        )

                        val sheen = sheenProgress.value
                        if (sheen in -0.55f..1.55f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val startX = size.width * (sheen - 0.35f)
                                val endX = size.width * (sheen + 0.35f)
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.34f),
                                            Color.Transparent,
                                        ),
                                        start = Offset(startX, 0f),
                                        end = Offset(endX, size.height),
                                    ),
                                )
                            }
                        }
                    }
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

                    Surface(
                        color = atmo.accentCobalt.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.graphicsLayer {
                            alpha = taglineAlpha.value
                        },
                    ) {
                        Text(
                            text = "Atur Hari Lebih Tenang",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = atmo.accentCobalt,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = timeGreeting,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progressAnim.value.coerceIn(0f, 1f) },
                        color = atmo.accentCobalt,
                        trackColor = atmo.accentCobalt.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .testTag("splash_progress_bar"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedContent(
                        targetState = stepIndex,
                        transitionSpec = { fadeIn(tween(260)).togetherWith(fadeOut(tween(200))) },
                        label = "splash_status_transition",
                    ) { idx ->
                        Text(
                            text = statusSteps.getOrElse(idx) { statusSteps.last() },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("splash_status_text"),
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = helpfulTips.getOrElse(stepIndex) { helpfulTips.last() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val phase = dotBouncePhase.value
                        accentColors.forEachIndexed { idx, dotColor ->
                            val waveArg = ((phase * 3.8f) - (idx * 0.16f)).mod(1.5f)
                            val bounce = if (waveArg in 0f..1f) {
                                -sin(waveArg * PI.toFloat()) * 10f
                            } else {
                                0f
                            }
                            val dotScale = if (waveArg in 0f..1f) {
                                1f + sin(waveArg * PI.toFloat()) * 0.24f
                            } else {
                                1f
                            }
                            Surface(
                                color = dotColor,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer {
                                        translationY = bounce
                                        scaleX = dotScale
                                        scaleY = dotScale
                                    },
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
