package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.ui.JadwalNihUiState
import com.example.ui.components.CameraZoomControl
import com.example.ui.components.CameraZoomUiMode
import com.example.ui.components.buildCameraZoomPresets
import com.example.ui.components.formatZoomLabel
import com.example.ui.screens.RekapScreen
import com.example.ui.theme.JadwalNihTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RekapGalleryAndZoomTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `zoom presets builder caps at 3_7x and provides 1x 2x 3_7x defaults`() {
        val fullRange = buildCameraZoomPresets(6.0f)
        assertEquals(3, fullRange.size)
        assertEquals(1f, fullRange[0], 0.01f)
        assertEquals(2f, fullRange[1], 0.01f)
        assertEquals(3.7f, fullRange[2], 0.01f)
        assertEquals("3.7x", formatZoomLabel(fullRange[2]))

        val midRange = buildCameraZoomPresets(2.5f)
        assertEquals(listOf("1x", "2x", "2.5x"), midRange.map { formatZoomLabel(it) })
    }

    @Test
    fun `camera zoom control expands 1x 2x 3_7x presets, switches to gear dial, and auto collapses`() {
        var zoomRatio by mutableFloatStateOf(1f)
        var mode by mutableStateOf(CameraZoomUiMode.COLLAPSED)
        var tick by mutableIntStateOf(0)
        var autoCollapsedCount = 0

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.setContent {
                JadwalNihTheme(darkTheme = true) {
                    CameraZoomControl(
                        zoomRatio = zoomRatio,
                        minZoomRatio = 1f,
                        maxZoomRatio = 3.7f,
                        mode = mode,
                        interactionTick = tick,
                        onExpandPresets = {
                            mode = CameraZoomUiMode.PRESETS
                            tick += 1
                        },
                        onSelectPreset = { target ->
                            zoomRatio = target
                            tick += 1
                        },
                        onAutoCollapse = {
                            autoCollapsedCount += 1
                            mode = CameraZoomUiMode.COLLAPSED
                        },
                        autoCollapsePresetsMs = 800L,
                        autoCollapsePinchMs = 500L,
                    )
                }
            }
            composeRule.mainClock.advanceTimeBy(100)

            // Collapsed button displays 1x and expands to 1x / 2x / 3.7x on click
            composeRule.onNodeWithTag("camera_zoom_button").assertIsDisplayed()
            composeRule.onNodeWithText("1x").assertIsDisplayed()
            composeRule.onNodeWithTag("camera_zoom_button").performClick()
            composeRule.mainClock.advanceTimeBy(250)

            composeRule.onNodeWithTag("camera_zoom_presets").assertIsDisplayed()
            composeRule.onNodeWithTag("camera_zoom_preset_1x").assertIsDisplayed()
            composeRule.onNodeWithTag("camera_zoom_preset_2x").assertIsDisplayed()
            composeRule.onNodeWithTag("camera_zoom_preset_3_7x").assertIsDisplayed()

            // Selecting 3.7x updates ratio
            composeRule.onNodeWithTag("camera_zoom_preset_3_7x").performClick()
            composeRule.mainClock.advanceTimeBy(250)
            assertEquals(3.7f, zoomRatio, 0.01f)

            // Simulate pinch -> gear dial mode appears with live zoom readout
            zoomRatio = 2.4f
            mode = CameraZoomUiMode.GEAR_DIAL
            tick += 1
            composeRule.mainClock.advanceTimeBy(250)

            composeRule.onNodeWithTag("camera_zoom_gear_dial").assertIsDisplayed()
            composeRule.onNodeWithText("2.4x").assertIsDisplayed()

            // After idle pause, gear dial automatically collapses back to compact button
            composeRule.mainClock.advanceTimeBy(650)
            assertTrue(autoCollapsedCount >= 1)
            composeRule.onNodeWithTag("camera_zoom_button").assertIsDisplayed()
            composeRule.onNodeWithText("2.4x").assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `rekap screen switches to gallery grid, filters by date, and opens lightbox preview`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val todayIso = DateTimeHelper.todayIso()
        val yesterdayIso = DateTimeHelper.addDays(todayIso, -1)

        val photoDir = File(context.cacheDir, "test_recap_gallery").apply { mkdirs() }
        val todayFile1 = File(photoDir, "today_1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val todayFile2 = File(photoDir, "today_2.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
        val yesterdayFile = File(photoDir, "yesterday_1.jpg").apply { writeBytes(byteArrayOf(7, 8, 9)) }

        try {
            val recaps = mapOf(
                todayIso to PhotoRecap(
                    dateIso = todayIso,
                    imagePaths = listOf(todayFile1.absolutePath, todayFile2.absolutePath),
                    title = "Progres Pagi",
                    caption = "Fokus beres 2 sesi",
                    updatedAtEpochMs = 1_700_000_000_000L,
                ),
                yesterdayIso to PhotoRecap(
                    dateIso = yesterdayIso,
                    imagePaths = listOf(yesterdayFile.absolutePath),
                    title = "Rekap Kemarin",
                    caption = "Review materi",
                    updatedAtEpochMs = 1_699_900_000_000L,
                ),
            )

            composeRule.setContent {
                JadwalNihTheme(darkTheme = true) {
                    RekapScreen(
                        state = JadwalNihUiState(
                            currentDateIso = todayIso,
                            selectedRecapDateIso = todayIso,
                            photoRecaps = recaps,
                        ),
                        onSelectDate = {},
                        onAddPhotos = { _, _ -> },
                        onSaveStyledPhoto = { _, _, _, _, _, _, _ -> },
                        onRemovePhoto = { _, _ -> },
                        onSaveMeta = { _, _, _ -> },
                        onDeleteRecap = {},
                        onExportCollage = {},
                        onNotifyMessage = {},
                        onOverlayVisibilityChange = {},
                    )
                }
            }
            composeRule.waitForIdle()

            // Switch from Camera to Gallery Grid
            composeRule.onNodeWithTag("rekap_mode_camera").assertIsDisplayed()
            composeRule.onNodeWithTag("rekap_mode_gallery").assertIsDisplayed().performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("rekap_gallery_view").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_day_card_$todayIso").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_day_card_$yesterdayIso").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_photo_tile_${todayIso}_0").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_photo_tile_${todayIso}_1").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_add_tile_$todayIso").assertIsDisplayed()

            // Filter by Hari Ini hides yesterday's card
            composeRule.onNodeWithTag("gallery_filter_today").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("gallery_day_card_$todayIso").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_day_card_$yesterdayIso").assertDoesNotExist()

            // Tap first tile opens Lightbox dialog
            composeRule.onNodeWithTag("gallery_photo_tile_${todayIso}_0").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("gallery_lightbox_dialog").assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_lightbox_delete").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("gallery_lightbox_close").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("gallery_lightbox_dialog").assertDoesNotExist()
        } finally {
            photoDir.deleteRecursively()
        }
    }

    @Test
    fun `rekap gallery shows empty state when no photos saved yet`() {
        val todayIso = DateTimeHelper.todayIso()
        composeRule.setContent {
            JadwalNihTheme(darkTheme = true) {
                RekapScreen(
                    state = JadwalNihUiState(
                        currentDateIso = todayIso,
                        selectedRecapDateIso = todayIso,
                        photoRecaps = emptyMap(),
                    ),
                    onSelectDate = {},
                    onAddPhotos = { _, _ -> },
                    onSaveStyledPhoto = { _, _, _, _, _, _, _ -> },
                    onRemovePhoto = { _, _ -> },
                    onSaveMeta = { _, _, _ -> },
                    onDeleteRecap = {},
                    onExportCollage = {},
                    onNotifyMessage = {},
                    onOverlayVisibilityChange = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rekap_mode_gallery").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("gallery_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("Galeri Rekap Masih Kosong").assertIsDisplayed()
    }
}
