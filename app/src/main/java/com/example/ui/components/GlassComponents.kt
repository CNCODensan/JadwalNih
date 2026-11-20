package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.MainTab
import com.example.ui.theme.LocalAtmospherePalette

@Composable
fun AtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(atmo.bgTop, atmo.bgBottom),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(atmo.orbBlue, Color.Transparent),
                        center = Offset(size.width * 0.14f, size.height * 0.08f),
                        radius = size.maxDimension * 0.55f,
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.14f, size.height * 0.08f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(atmo.orbPurple, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.24f),
                        radius = size.maxDimension * 0.50f,
                    ),
                    radius = size.maxDimension * 0.50f,
                    center = Offset(size.width * 0.88f, size.height * 0.24f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(atmo.orbBlue.copy(alpha = atmo.orbBlue.alpha * 0.65f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.92f),
                        radius = size.maxDimension * 0.5f,
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = Offset(size.width * 0.5f, size.height * 0.92f),
                )
            },
        content = content,
    )
}

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    borderBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBorder = borderBrush ?: Brush.linearGradient(
        colors = listOf(atmo.glassBorderStart, atmo.glassBorderEnd),
    )

    Surface(
        modifier = modifier
            .then(
                if (atmo.isDark) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        ambientColor = atmo.accentBlue.copy(alpha = 0.12f),
                        spotColor = atmo.accentPurple.copy(alpha = 0.14f),
                    )
                },
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, effectiveBorder),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(atmo.glassFillTop, atmo.glassFillBottom),
                ),
            ),
        ) {
            content()
        }
    }
}

@Composable
fun FloatingPillNavBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    tabIconProvider: (MainTab, Boolean) -> ImageVector,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val pillShape = RoundedCornerShape(34.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (atmo.isDark) 18.dp else 14.dp,
                    shape = pillShape,
                    ambientColor = atmo.accentPurple.copy(alpha = 0.25f),
                    spotColor = atmo.accentBlue.copy(alpha = 0.3f),
                ),
            shape = pillShape,
            color = atmo.navBarFill,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(atmo.glassBorderStart, atmo.glassBorderEnd),
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) atmo.heroOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_color_${tab.name}",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(26.dp))
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(atmo.heroGradStart, atmo.heroGradEnd),
                                        ),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectTab(tab) },
                            )
                            .padding(vertical = 7.dp, horizontal = 2.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = tabIconProvider(tab, selected),
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
