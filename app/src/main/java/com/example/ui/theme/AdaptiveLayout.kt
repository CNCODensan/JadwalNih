package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AdaptiveLayoutSpec(
    val availableWidthDp: Dp = 400.dp,
    val isUltraCompact: Boolean = false,
    val isCompactWidth: Boolean = false,
    val isExpandedWidth: Boolean = false,
    val screenHorizontalPadding: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val cardInnerSpacing: Dp = 12.dp,
    val sectionSpacing: Dp = 14.dp,
    val summaryRingSize: Dp = 68.dp,
    val navLabelFontSizeSp: Float = 10.5f,
    val navIconSize: Dp = 21.dp,
    val titleScaleFactor: Float = 1f,
    val bottomNavClearance: Dp = 16.dp,
    val fabContentClearance: Dp = 84.dp,
) {
    companion object {
        val Default = AdaptiveLayoutSpec()

        fun fromWidth(width: Dp): AdaptiveLayoutSpec {
            val ultra = width < 345.dp
            val compact = width < 380.dp
            val expanded = width >= 600.dp
            return AdaptiveLayoutSpec(
                availableWidthDp = width,
                isUltraCompact = ultra,
                isCompactWidth = compact,
                isExpandedWidth = expanded,
                screenHorizontalPadding = when {
                    ultra -> 12.dp
                    compact -> 14.dp
                    expanded -> 20.dp
                    else -> 16.dp
                },
                cardPadding = when {
                    ultra -> 12.dp
                    compact -> 14.dp
                    else -> 16.dp
                },
                cardInnerSpacing = when {
                    ultra -> 8.dp
                    compact -> 10.dp
                    else -> 12.dp
                },
                sectionSpacing = if (compact) 12.dp else 14.dp,
                summaryRingSize = when {
                    ultra -> 54.dp
                    compact -> 60.dp
                    else -> 68.dp
                },
                navLabelFontSizeSp = when {
                    ultra -> 8.8f
                    compact -> 9.6f
                    else -> 10.5f
                },
                navIconSize = when {
                    ultra -> 18.dp
                    compact -> 19.dp
                    else -> 21.dp
                },
                titleScaleFactor = when {
                    ultra -> 0.88f
                    compact -> 0.94f
                    else -> 1f
                },
                bottomNavClearance = if (expanded) 20.dp else 16.dp,
                fabContentClearance = 84.dp,
            )
        }
    }
}

val LocalAdaptiveSpec = staticCompositionLocalOf { AdaptiveLayoutSpec.Default }

@Composable
fun ProvideAdaptiveLayoutSpec(
    availableWidth: Dp,
    content: @Composable () -> Unit,
) {
    val spec = remember(availableWidth) { AdaptiveLayoutSpec.fromWidth(availableWidth) }
    CompositionLocalProvider(LocalAdaptiveSpec provides spec) {
        content()
    }
}
