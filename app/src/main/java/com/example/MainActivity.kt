package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calendar.CalendarExportHelper
import com.example.domain.DateTimeHelper
import com.example.domain.MainTab
import com.example.domain.QuickGlanceTarget
import com.example.domain.ScheduleTask
import com.example.domain.TaskDraft
import com.example.ui.JadwalNihViewModel
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.JadwalNihTopBar
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SplashMotionOverlay
import com.example.ui.components.TaskEditorSheet
import com.example.ui.components.bouncyPress
import com.example.ui.components.rememberBouncyScale
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.screens.AnalitikScreen
import com.example.ui.screens.FokusScreen
import com.example.ui.screens.JadwalScreen
import com.example.ui.screens.MatriksScreen
import com.example.ui.screens.RekapScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.JadwalNihTheme
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LocalAtmospherePalette

class MainActivity : ComponentActivity() {
    private val viewModel: JadwalNihViewModel by viewModels()

    private val exportJsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportJsonToUri(uri)
        }
    }

    private val importJsonLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importJsonFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            var showSplash by rememberSaveable { mutableStateOf(true) }
            var showEditorSheet by remember { mutableStateOf(false) }
            var editingTask by remember { mutableStateOf<ScheduleTask?>(null) }
            var editingDraft by remember { mutableStateOf<TaskDraft?>(null) }
            var editorDefaultHour by remember { mutableStateOf<Int?>(null) }
            var editorDefaultDateIso by remember { mutableStateOf<String?>(null) }
            var showSettingsSheet by remember { mutableStateOf(false) }

            var isRekapOverlayOpen by remember { mutableStateOf(false) }
            LaunchedEffect(uiState.selectedTab) {
                if (uiState.selectedTab != MainTab.REKAP) {
                    isRekapOverlayOpen = false
                }
            }

            val effectiveDarkTheme = uiState.isDarkMode || uiState.selectedTab == MainTab.REKAP

            LaunchedEffect(uiState.snackbarMessage) {
                val msg = uiState.snackbarMessage
                if (msg != null) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearSnackbar()
                }
            }

            var previousDarkTheme by remember { mutableStateOf(effectiveDarkTheme) }
            var themeFadeOverlayColor by remember { mutableStateOf(LightBackground) }
            val themeFadeAlpha = remember { Animatable(0f) }

            LaunchedEffect(effectiveDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (effectiveDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (effectiveDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                )
                if (effectiveDarkTheme != previousDarkTheme) {
                    themeFadeOverlayColor = if (previousDarkTheme) DarkBackground else LightBackground
                    previousDarkTheme = effectiveDarkTheme
                    themeFadeAlpha.snapTo(0.92f)
                    themeFadeAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                    )
                }
            }

            JadwalNihTheme(darkTheme = effectiveDarkTheme) {
                val atmo = LocalAtmospherePalette.current
                val haptics = LocalHapticFeedback.current
                LaunchedEffect(uiState.celebrationTick) {
                    if (uiState.celebrationTick > 0) {
                        haptics.tapConfirm()
                    }
                }
                val isOverlayOpen = showEditorSheet || showSettingsSheet || isRekapOverlayOpen
                val backdropBlur by animateDpAsState(
                    targetValue = if (isOverlayOpen) 16.dp else 0.dp,
                    animationSpec = tween(durationMillis = 220),
                    label = "modal_backdrop_blur",
                )
                AtmosphericBackground {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useNavigationRail = maxWidth >= 600.dp

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (backdropBlur > 0.dp) {
                                        Modifier.blur(
                                            radius = backdropBlur,
                                            edgeTreatment = BlurredEdgeTreatment.Rectangle,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                            containerColor = atmo.bgTop,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = {
                                JadwalNihTopBar(
                                    onOpenSettings = { showSettingsSheet = true },
                                )
                            },
                            floatingActionButton = {
                                if (uiState.selectedTab == MainTab.JADWAL || uiState.selectedTab == MainTab.MATRIKS) {
                                    val fabInteraction = remember { MutableInteractionSource() }
                                    val fabScale = rememberBouncyScale(fabInteraction, pressedScale = 0.88f)
                                    FloatingActionButton(
                                        onClick = {
                                            haptics.tapSoft()
                                            editingTask = null
                                            editingDraft = null
                                            editorDefaultHour = null
                                            editorDefaultDateIso = uiState.selectedDateIso
                                            showEditorSheet = true
                                        },
                                        interactionSource = fabInteraction,
                                        shape = CircleShape,
                                        containerColor = atmo.accentAmber,
                                        contentColor = atmo.onAccentAmber,
                                        elevation = FloatingActionButtonDefaults.elevation(
                                            defaultElevation = 6.dp,
                                            pressedElevation = 10.dp,
                                        ),
                                        modifier = Modifier
                                            .size(58.dp)
                                            .graphicsLayer {
                                                scaleX = fabScale
                                                scaleY = fabScale
                                            }
                                            .testTag("add_task_fab"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Tambah Jadwal Baru",
                                            tint = atmo.onAccentAmber,
                                        )
                                    }
                                }
                            },
                            bottomBar = {
                                if (!useNavigationRail) {
                                    FloatingPillNavBar(
                                        selectedTab = uiState.selectedTab,
                                        onSelectTab = viewModel::selectTab,
                                        tabIconProvider = { tab, selected -> tabIcon(tab, selected) },
                                    )
                                }
                            },
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            ) {
                                if (useNavigationRail) {
                                    NavigationRail(
                                        modifier = Modifier.fillMaxHeight(),
                                        containerColor = atmo.navBarFill,
                                    ) {
                                        MainTab.entries.forEach { tab ->
                                            val selected = uiState.selectedTab == tab
                                            val (tabBg, tabFg) = atmo.tabAccent(tab)
                                            val railInteraction = remember { MutableInteractionSource() }
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = {
                                                    haptics.tapSoft()
                                                    viewModel.selectTab(tab)
                                                },
                                                interactionSource = railInteraction,
                                                colors = NavigationRailItemDefaults.colors(
                                                    indicatorColor = tabBg,
                                                    selectedIconColor = tabFg,
                                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                ),
                                                icon = {
                                                    Icon(
                                                        imageVector = tabIcon(tab, selected),
                                                        contentDescription = tab.label,
                                                    )
                                                },
                                                label = { Text(tab.label) },
                                                modifier = Modifier
                                                    .bouncyPress(railInteraction, pressedScale = 0.90f)
                                                    .testTag("nav_rail_${tab.name.lowercase()}"),
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    val contentModifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 900.dp)

                                    AnimatedContent(
                                        targetState = uiState.selectedTab,
                                        transitionSpec = {
                                            val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                            (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                                                slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 8 * dir } +
                                                scaleIn(initialScale = 0.96f, animationSpec = tween(240)))
                                                .togetherWith(
                                                    fadeOut(tween(180)) +
                                                        slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 10 * dir } +
                                                        scaleOut(targetScale = 0.98f, animationSpec = tween(180)),
                                                )
                                        },
                                        label = "main_tab_transition",
                                    ) { currentTab ->
                                        when (currentTab) {
                                            MainTab.JADWAL -> JadwalScreen(
                                                state = uiState,
                                                onSelectScope = viewModel::selectScope,
                                                onSelectDate = viewModel::selectDate,
                                                onStepDate = viewModel::stepDate,
                                                onJumpToToday = viewModel::jumpToToday,
                                                onSelectStatusFilter = viewModel::setStatusFilter,
                                                onToggleComplete = viewModel::toggleTaskComplete,
                                                onToggleSubTask = viewModel::toggleSubTask,
                                                onMoveToTomorrow = viewModel::moveTaskToTomorrow,
                                                onExportCalendar = { task ->
                                                    val launched = CalendarExportHelper.launchCalendarInsert(context, task)
                                                    if (!launched) {
                                                        viewModel.showMessage("Aplikasi kalender tidak ditemukan di perangkat ini.")
                                                    }
                                                },
                                                onEditTask = { task ->
                                                    editingTask = task
                                                    editingDraft = null
                                                    editorDefaultHour = null
                                                    editorDefaultDateIso = task.dateIso
                                                    showEditorSheet = true
                                                },
                                                onDeleteTask = viewModel::deleteTask,
                                                onQuickGlanceClick = {
                                                    val targetTab = when (uiState.quickGlanceTarget) {
                                                        QuickGlanceTarget.ANALITIK -> MainTab.ANALITIK
                                                        QuickGlanceTarget.MATRIKS -> MainTab.MATRIKS
                                                    }
                                                    viewModel.selectTab(targetTab)
                                                },
                                                onResumeDraft = { draft ->
                                                    editingTask = null
                                                    editingDraft = draft
                                                    editorDefaultHour = null
                                                    editorDefaultDateIso = draft.dateIso
                                                    showEditorSheet = true
                                                },
                                                onDiscardDraft = viewModel::clearTaskDraft,
                                                onConfirmAttendance = viewModel::confirmTaskAttendance,
                                                onOverrideAttended = viewModel::overrideMissedTaskAsAttended,
                                                onOpenFocusTab = { viewModel.selectTab(MainTab.FOKUS) },
                                                onOpenRecapTab = { viewModel.selectTab(MainTab.REKAP) },
                                                onOpenMatrixTab = { viewModel.selectTab(MainTab.MATRIKS) },
                                                onOpenAnalyticsTab = { viewModel.selectTab(MainTab.ANALITIK) },
                                                modifier = contentModifier,
                                            )

                                            MainTab.MATRIKS -> MatriksScreen(
                                                state = uiState,
                                                onSelectScope = viewModel::setMatrixScope,
                                                onSelectQuadrantFilter = viewModel::setMatrixQuadrantFilter,
                                                onStepDate = { delta ->
                                                    viewModel.selectDate(DateTimeHelper.addDays(uiState.selectedDateIso, delta))
                                                },
                                                onJumpToToday = viewModel::jumpToToday,
                                                onSelectSharedDate = viewModel::selectSharedCalendarDate,
                                                onAddTaskAtHour = { dateIso, hour ->
                                                    editingTask = null
                                                    editingDraft = null
                                                    editorDefaultDateIso = dateIso
                                                    editorDefaultHour = hour
                                                    showEditorSheet = true
                                                },
                                                onOpenRecapForDate = { dateIso ->
                                                    viewModel.selectSharedCalendarDate(dateIso)
                                                    viewModel.selectTab(MainTab.REKAP)
                                                },
                                                onToggleComplete = viewModel::toggleTaskComplete,
                                                onMoveToTomorrow = viewModel::moveTaskToTomorrow,
                                                onChangeQuadrant = viewModel::changeTaskQuadrant,
                                                onEditTask = { task ->
                                                    editingTask = task
                                                    editingDraft = null
                                                    editorDefaultHour = null
                                                    editorDefaultDateIso = task.dateIso
                                                    showEditorSheet = true
                                                },
                                                modifier = contentModifier,
                                            )

                                            MainTab.FOKUS -> FokusScreen(
                                                state = uiState,
                                                onMinutesInputChange = viewModel::updateCustomFocusMinutes,
                                                onStepMinutes = viewModel::stepCustomFocusMinutes,
                                                onSelectTask = viewModel::selectFocusTask,
                                                onStart = viewModel::startFocusTimer,
                                                onPause = viewModel::pauseFocusTimer,
                                                onResume = viewModel::resumeFocusTimer,
                                                onReset = viewModel::resetFocusTimer,
                                                onStopAndSave = viewModel::stopAndSaveFocusTimer,
                                                onToggleDistractionFree = viewModel::toggleDistractionFreeMode,
                                                modifier = contentModifier,
                                            )

                                            MainTab.REKAP -> RekapScreen(
                                                state = uiState,
                                                onSelectDate = viewModel::selectSharedCalendarDate,
                                                onAddPhotos = viewModel::addPhotosToRecap,
                                                onSaveStyledPhoto = viewModel::saveCapturedPhotoToRecap,
                                                onRemovePhoto = viewModel::removePhotoFromRecap,
                                                onSaveMeta = viewModel::saveRecapMeta,
                                                onDeleteRecap = viewModel::deleteRecap,
                                                onExportCollage = { dateIso ->
                                                    viewModel.exportRecapCollage(dateIso) { shareUri ->
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "image/jpeg"
                                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        try {
                                                            context.startActivity(
                                                                Intent.createChooser(shareIntent, "Bagikan Kolase Rekap"),
                                                            )
                                                        } catch (_: ActivityNotFoundException) {
                                                            viewModel.showMessage(
                                                                "Kolase tersimpan ke Galeri, tetapi tidak ada aplikasi berbagi.",
                                                            )
                                                        }
                                                    }
                                                },
                                                onNotifyMessage = viewModel::showMessage,
                                                onOverlayVisibilityChange = { isRekapOverlayOpen = it },
                                                modifier = contentModifier,
                                            )

                                            MainTab.ANALITIK -> AnalitikScreen(
                                                state = uiState,
                                                onSelectScope = viewModel::setAnalyticsScope,
                                                modifier = contentModifier,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (themeFadeAlpha.value > 0.005f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(themeFadeOverlayColor.copy(alpha = themeFadeAlpha.value)),
                            )
                        }

                        ConfettiOverlay(triggerCount = uiState.celebrationTick)

                        if (showEditorSheet) {
                            TaskEditorSheet(
                                initialTask = editingTask,
                                initialDraft = editingDraft,
                                defaultDateIso = editorDefaultDateIso ?: uiState.selectedDateIso,
                                defaultStartHour = editorDefaultHour,
                                existingTasks = uiState.allTasksWithStatus.map { it.task },
                                onSaveDraftAndDismiss = { draft ->
                                    viewModel.saveTaskDraft(draft)
                                    viewModel.showMessage("Draft jadwal disimpan sementara.")
                                    showEditorSheet = false
                                },
                                onDiscardAndDismiss = {
                                    viewModel.clearTaskDraft()
                                    showEditorSheet = false
                                },
                                onSave = { saved ->
                                    viewModel.saveTask(saved)
                                    showEditorSheet = false
                                },
                                onNotifyPermissionDenied = {
                                    viewModel.showMessage("Izin notifikasi ditolak; pengingat sistem dinonaktifkan.")
                                },
                            )
                        }

                        if (showSettingsSheet) {
                            SettingsSheet(
                                userName = uiState.userName,
                                isDarkMode = uiState.isDarkMode,
                                quickGlanceTarget = uiState.quickGlanceTarget,
                                checkInNotificationsEnabled = uiState.checkInNotificationsEnabled,
                                isBusyBackup = uiState.isBusyBackup,
                                onUserNameChange = viewModel::updateUserName,
                                onDarkModeChange = viewModel::setDarkMode,
                                onQuickGlanceTargetChange = viewModel::setQuickGlanceTarget,
                                onCheckInNotificationsChange = viewModel::setCheckInNotificationsEnabled,
                                onExportJson = {
                                    exportJsonLauncher.launch("jadwalnih_backup_${DateTimeHelper.todayIso()}.json")
                                },
                                onImportJson = {
                                    importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                onResetDataConfirmed = viewModel::resetAllData,
                                onNotifyPermissionDenied = {
                                    viewModel.showMessage("Izin notifikasi ditolak.")
                                },
                                onDismiss = { showSettingsSheet = false },
                            )
                        }

                        if (showSplash) {
                            SplashMotionOverlay(
                                onFinished = { showSplash = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector = when (tab) {
    MainTab.JADWAL -> if (selected) Icons.Filled.CalendarViewDay else Icons.Outlined.CalendarViewDay
    MainTab.MATRIKS -> if (selected) Icons.Filled.GridView else Icons.Outlined.GridView
    MainTab.FOKUS -> if (selected) Icons.Filled.Timer else Icons.Outlined.Timer
    MainTab.REKAP -> if (selected) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary
    MainTab.ANALITIK -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
}
