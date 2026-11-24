package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MainTab
import com.example.ui.theme.AppThemeWallpaperLayer
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalHolidayThemeType
import com.example.ui.theme.LocalIsBirthdayTheme

@Composable
fun AtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val holidayTheme = LocalHolidayThemeType.current
    val isBirthday = LocalIsBirthdayTheme.current
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(atmo.bgTop),
        ) {
            AppThemeWallpaperLayer(
                holidayThemeType = holidayTheme,
                isBirthdayTheme = isBirthday,
                isDark = atmo.isDark,
            )
            content()
        }
    }
}

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBorder = borderColor ?: atmo.cardBorder
    val cardInteraction = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .bouncyPress(cardInteraction, pressedScale = 0.96f)
                        .clip(shape)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier.clip(shape)
                },
            ),
        shape = shape,
        color = atmo.cardBg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, effectiveBorder),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
    visibleTabs: List<MainTab> = MainTab.entries,
) {
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val pillShape = RoundedCornerShape(36.dp)
    val outerHorizontal = if (spec.isUltraCompact) 8.dp else if (spec.isCompactWidth) 10.dp else 14.dp
    val outerVertical = if (spec.isCompactWidth) 8.dp else 10.dp
    val innerHorizontal = if (spec.isUltraCompact) 3.dp else 5.dp
    val innerVertical = if (spec.isUltraCompact) 4.dp else 6.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = outerHorizontal, vertical = outerVertical),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = pillShape,
            color = atmo.navBarFill,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = if (atmo.isDark) 8.dp else 6.dp,
            border = BorderStroke(1.dp, atmo.cardBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = innerHorizontal, vertical = innerVertical),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val haptics = LocalHapticFeedback.current
                visibleTabs.forEach { tab ->
                    val selected = selectedTab == tab
                    val (activeBg, activeFg) = atmo.tabAccent(tab)
                    val interactionSource = remember { MutableInteractionSource() }
                    val scale = rememberBouncyScale(interactionSource, pressedScale = 0.90f)
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) activeBg else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_bg_${tab.name}",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_color_${tab.name}",
                    )
                    val iconPopScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "nav_icon_pop_${tab.name}",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(pillBg)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    haptics.tapSoft()
                                    onSelectTab(tab)
                                },
                            )
                            .padding(vertical = if (spec.isUltraCompact) 6.dp else 7.dp, horizontal = 1.dp)
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
                                modifier = Modifier
                                    .size(spec.navIconSize)
                                    .graphicsLayer {
                                        scaleX = iconPopScale
                                        scaleY = iconPopScale
                                    },
                            )
                            val fontScaleGuard = LocalDensity.current.fontScale.coerceIn(1f, 1.3f)
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = (spec.navLabelFontSizeSp / fontScaleGuard).sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
