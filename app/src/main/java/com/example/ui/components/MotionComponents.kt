package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val xFraction: Float,
    val yFraction: Float,
    val vxPerSec: Float,
    val vyPerSec: Float,
    val sizeDp: Float,
    val color: Color,
    val rotationDeg: Float,
    val rotationSpeedDeg: Float,
    val shapeKind: Int,
)

@Composable
fun rememberBouncyScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.91f,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.48f,
            stiffness = 520f,
        ),
        label = "bouncy_scale",
    )
    return scale
}

fun Modifier.bouncyPress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.91f,
): Modifier = composed {
    val scale = rememberBouncyScale(
        interactionSource = interactionSource,
        pressedScale = pressedScale,
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(22f) }

    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * 36L)
        if (delayMs > 0L) delay(delayMs)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            )
        }
        offsetYAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = offsetYAnim.value
        },
    ) {
        content()
    }
}

fun Modifier.shakeOnTrigger(trigger: Int): Modifier = composed {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        val amplitudes = listOf(-18f, 16f, -12f, 9f, -5f, 0f)
        for (target in amplitudes) {
            offsetX.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 42),
            )
        }
    }
    offset { IntOffset(offsetX.value.roundToInt(), 0) }
}

@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.92f,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BouncyOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.92f,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BouncyFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.92f,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BouncyTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.91f,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.86f,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BouncyFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.86f,
    content: @Composable () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BouncyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.92f,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.bouncyPress(interactionSource, pressedScale),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
    )
}

fun HapticFeedback.tapSoft() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

fun HapticFeedback.tapTick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

fun HapticFeedback.tapConfirm() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

fun HapticFeedback.tapWarning() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun ConfettiOverlay(
    triggerCount: Int,
    modifier: Modifier = Modifier,
) {
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    // Happy Pesisir + Command Center Mint + Ceria/Pop festive palette
    val paletteColors = remember {
        listOf(
            Color(0xFF1677FF), // Pesisir Cobalt Blue
            Color(0xFFEE3E38), // Pesisir Coral Red
            Color(0xFFF97316), // Pesisir Tangerine Orange
            Color(0xFFFACC15), // Pesisir Sunny Amber
            Color(0xFF10B981), // Command Center Fresh Mint
            Color(0xFF0EA5E9), // Sky Blue
            Color(0xFFF43F5E), // Cherry Pink
            Color(0xFF84CC16), // Lime Green
        )
    }

    LaunchedEffect(triggerCount) {
        if (triggerCount <= 0) return@LaunchedEffect
        val rng = Random(System.currentTimeMillis() + triggerCount)
        particles = List(64) { idx ->
            val angle = rng.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 0.28f + rng.nextFloat() * 0.78f
            ConfettiParticle(
                xFraction = 0.18f + rng.nextFloat() * 0.64f,
                yFraction = 0.18f + rng.nextFloat() * 0.14f,
                vxPerSec = cos(angle) * speed * 0.68f,
                vyPerSec = -0.58f - rng.nextFloat() * 0.52f + sin(angle) * 0.20f,
                sizeDp = 6f + rng.nextFloat() * 6.5f,
                color = paletteColors[idx % paletteColors.size],
                rotationDeg = rng.nextFloat() * 360f,
                rotationSpeedDeg = (rng.nextFloat() - 0.5f) * 560f,
                shapeKind = idx % 3,
            )
        }
        elapsedSeconds = 0f
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val sec = (nowNanos - startNanos) / 1_000_000_000f
            elapsedSeconds = sec
            if (sec >= 1.85f) break
        }
        particles = emptyList()
    }

    if (particles.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = elapsedSeconds
        val gravity = 1.38f
        val alpha = (1f - (t / 1.85f).coerceIn(0f, 1f)).let { rem ->
            if (rem < 0.35f) (rem / 0.35f) else 1f
        }
        particles.forEach { p ->
            val drag = (1f - t * 0.18f).coerceAtLeast(0.62f)
            val px = (p.xFraction + p.vxPerSec * t * drag) * size.width
            val py = (p.yFraction + p.vyPerSec * t + 0.5f * gravity * t * t) * size.height
            if (py <= size.height + 48f) {
                val particleSizePx = p.sizeDp * density
                val color = p.color.copy(alpha = alpha)
                when (p.shapeKind) {
                    0 -> {
                        drawCircle(
                            color = color,
                            radius = particleSizePx * 0.42f,
                            center = Offset(px, py),
                        )
                    }
                    1 -> {
                        rotate(
                            degrees = p.rotationDeg + p.rotationSpeedDeg * t,
                            pivot = Offset(px, py),
                        ) {
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(px - particleSizePx * 0.65f, py - particleSizePx * 0.22f),
                                size = Size(particleSizePx * 1.3f, particleSizePx * 0.44f),
                                cornerRadius = CornerRadius(2f * density, 2f * density),
                            )
                        }
                    }
                    else -> {
                        rotate(
                            degrees = p.rotationDeg + p.rotationSpeedDeg * t,
                            pivot = Offset(px, py),
                        ) {
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(px - particleSizePx * 0.48f, py - particleSizePx * 0.36f),
                                size = Size(particleSizePx * 0.96f, particleSizePx * 0.72f),
                                cornerRadius = CornerRadius(2.5f * density, 2.5f * density),
                            )
                        }
                    }
                }
            }
        }
    }
}
