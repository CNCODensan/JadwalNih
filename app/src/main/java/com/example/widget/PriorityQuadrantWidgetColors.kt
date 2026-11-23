package com.example.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.example.domain.PriorityQuadrant
import com.example.domain.TaskStatus
import com.example.ui.theme.DarkAtmospherePalette
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOnBackground
import com.example.ui.theme.DarkOnSurface
import com.example.ui.theme.DarkOnSurfaceVariant
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkStatusPalette
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LightAtmospherePalette
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightOnBackground
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightOutlineVariant
import com.example.ui.theme.LightStatusPalette
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant

object PriorityQuadrantWidgetColors {
    private fun pair(day: Color, night: Color): ColorProvider =
        androidx.glance.color.ColorProvider(day = day, night = night)

    val widgetBackground: ColorProvider = pair(LightBackground, DarkBackground)
    val cardSurface: ColorProvider = pair(LightSurface, DarkSurface)
    val cardAltSurface: ColorProvider = pair(LightSurfaceVariant, DarkSurfaceVariant)
    val subtleSurface: ColorProvider = pair(
        LightAtmospherePalette.surfaceSubtle,
        DarkAtmospherePalette.surfaceSubtle,
    )
    val borderOutline: ColorProvider = pair(LightOutlineVariant, DarkOutlineVariant)

    val textPrimary: ColorProvider = pair(LightOnBackground, DarkOnBackground)
    val textOnCard: ColorProvider = pair(LightOnSurface, DarkOnSurface)
    val textSecondary: ColorProvider = pair(LightOnSurfaceVariant, DarkOnSurfaceVariant)

    val accentCobalt: ColorProvider = pair(
        LightAtmospherePalette.accentCobalt,
        DarkAtmospherePalette.accentCobalt,
    )
    val onAccentCobalt: ColorProvider = pair(
        LightAtmospherePalette.onAccentCobalt,
        DarkAtmospherePalette.onAccentCobalt,
    )
    val accentMint: ColorProvider = pair(
        LightAtmospherePalette.accentMint,
        DarkAtmospherePalette.accentMint,
    )
    val accentCoral: ColorProvider = pair(
        LightAtmospherePalette.accentCoral,
        DarkAtmospherePalette.accentCoral,
    )
    val accentOrange: ColorProvider = pair(
        LightAtmospherePalette.accentOrange,
        DarkAtmospherePalette.accentOrange,
    )

    val filterSelectedBg: ColorProvider = pair(Color(0xFFE0ECFF), Color(0xFF1D355E))
    val filterSelectedFg: ColorProvider = pair(Color(0xFF0B3B8C), Color(0xFFDBEAFE))

    fun quadrantBg(quadrant: PriorityQuadrant): ColorProvider = when (quadrant) {
        PriorityQuadrant.Q1_IMPORTANT_URGENT -> pair(LightStatusPalette.redBg, DarkStatusPalette.redBg)
        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> pair(LightStatusPalette.blueBg, DarkStatusPalette.blueBg)
        PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> pair(LightStatusPalette.yellowBg, DarkStatusPalette.yellowBg)
        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> pair(LightStatusPalette.greenBg, DarkStatusPalette.greenBg)
    }

    fun quadrantFg(quadrant: PriorityQuadrant): ColorProvider = when (quadrant) {
        PriorityQuadrant.Q1_IMPORTANT_URGENT -> pair(LightStatusPalette.redFg, DarkStatusPalette.redFg)
        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> pair(LightStatusPalette.blueFg, DarkStatusPalette.blueFg)
        PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> pair(LightStatusPalette.yellowFg, DarkStatusPalette.yellowFg)
        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> pair(LightStatusPalette.greenFg, DarkStatusPalette.greenFg)
    }

    fun quadrantAccent(quadrant: PriorityQuadrant): ColorProvider = when (quadrant) {
        PriorityQuadrant.Q1_IMPORTANT_URGENT -> accentCoral
        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> accentCobalt
        PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> accentOrange
        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> accentMint
    }

    fun statusBg(status: TaskStatus): ColorProvider = when (status) {
        TaskStatus.COMPLETED_GREEN -> pair(LightStatusPalette.greenBg, DarkStatusPalette.greenBg)
        TaskStatus.CRITICAL_RED -> pair(LightStatusPalette.redBg, DarkStatusPalette.redBg)
        TaskStatus.APPROACHING_YELLOW -> pair(LightStatusPalette.yellowBg, DarkStatusPalette.yellowBg)
        TaskStatus.FLEXIBLE_BLUE -> pair(LightStatusPalette.blueBg, DarkStatusPalette.blueBg)
        TaskStatus.PENDING_GRAY -> pair(LightStatusPalette.grayBg, DarkStatusPalette.grayBg)
        TaskStatus.OVERDUE_DARK -> pair(LightStatusPalette.darkBg, DarkStatusPalette.darkBg)
    }

    fun statusFg(status: TaskStatus): ColorProvider = when (status) {
        TaskStatus.COMPLETED_GREEN -> pair(LightStatusPalette.greenFg, DarkStatusPalette.greenFg)
        TaskStatus.CRITICAL_RED -> pair(LightStatusPalette.redFg, DarkStatusPalette.redFg)
        TaskStatus.APPROACHING_YELLOW -> pair(LightStatusPalette.yellowFg, DarkStatusPalette.yellowFg)
        TaskStatus.FLEXIBLE_BLUE -> pair(LightStatusPalette.blueFg, DarkStatusPalette.blueFg)
        TaskStatus.PENDING_GRAY -> pair(LightStatusPalette.grayFg, DarkStatusPalette.grayFg)
        TaskStatus.OVERDUE_DARK -> pair(LightStatusPalette.darkFg, DarkStatusPalette.darkFg)
    }
}
