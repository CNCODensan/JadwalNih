package com.example

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.example.data.JsonBackupManager
import com.example.ui.components.BouncyButton
import com.example.ui.components.BouncyTextButton
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
import com.example.domain.HolidayThemeType
import com.example.domain.MainTab
import com.example.domain.QuickGlanceTarget
import com.example.domain.ScheduleTask
import com.example.domain.TaskDraft
import com.example.domain.ThemeMode
import com.example.ui.JadwalNihViewModel
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.JadwalNihTopBar
import com.example.ui.components.NotificationCenterSheet
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
import com.example.ui.screens.OnboardingStartingScreen
import com.example.ui.screens.RekapScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.JadwalNihTheme
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LocalAtmospherePalette
import com.example.widget.WidgetSyncHelper

class MainActivity : ComponentActivity() {
    private val viewModel: JadwalNihViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenTabIntent(intent)
    }

    private fun handleOpenTabIntent(intent: Intent?) {
        val rawTab = intent?.getStringExtra(WidgetSyncHelper.EXTRA_OPEN_TAB) ?: return
        val tab = MainTab.entries.firstOrNull { it.name == rawTab } ?: return
        viewModel.selectTab(tab)
    }

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
        handleOpenTabIntent(intent)
        val launchedFromWidget = intent?.hasExtra(WidgetSyncHelper.EXTRA_OPEN_TAB) == true
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            var showSplash by rememberSaveable { mutableStateOf(!launchedFromWidget) }
            var showEditorSheet by remember { mutableStateOf(false) }
            var editingTask by remember { mutableStateOf<ScheduleTask?>(null) }
            var editingDraft by remember { mutableStateOf<TaskDraft?>(null) }
            var editorDefaultHour by remember { mutableStateOf<Int?>(null) }
            var editorDefaultDateIso by remember { mutableStateOf<String?>(null) }
            var showSettingsSheet by remember { mutableStateOf(false) }
            var showNotificationSheet by remember { mutableStateOf(false) }

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                viewModel.refreshDynamicServices(tryGpsLocation = granted)
            }

            LaunchedEffect(showSplash, uiState.isPreferencesLoaded, uiState.hasPromptedInitialLocation) {
                if (!showSplash && uiState.isPreferencesLoaded && !uiState.hasPromptedInitialLocation) {
                    viewModel.markInitialLocationPrompted()
                    val alreadyGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (alreadyGranted) {
                        viewModel.refreshDynamicServices(tryGpsLocation = true)
                    } else {
                        runCatching {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }.onFailure {
                            viewModel.refreshDynamicServices(tryGpsLocation = false)
                        }
                    }
                }
            }

            var isRekapOverlayOpen by remember { mutableStateOf(false) }
            LaunchedEffect(uiState.selectedTab) {
                if (uiState.selectedTab != MainTab.REKAP) {
                    isRekapOverlayOpen = false
                }
            }

            val systemInDark = isSystemInDarkTheme()
            val isUserDarkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> systemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val effectiveDarkTheme = isUserDarkTheme || uiState.selectedTab == MainTab.REKAP
            val effectiveHolidayTheme = if (uiState.selectedTab == MainTab.REKAP) {
                HolidayThemeType.NONE
            } else {
                uiState.activeHolidayTheme
            }

            LaunchedEffect(uiState.snackbarMessage, uiState.canUndoDelete) {
                val msg = uiState.snackbarMessage
                if (msg != null) {
                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = if (uiState.canUndoDelete) "Urungkan" else null,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed && uiState.canUndoDelete) {
                        viewModel.undoLastDelete()
                    }
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

            JadwalNihTheme(
                darkTheme = effectiveDarkTheme,
                holidayThemeType = effectiveHolidayTheme,
            ) {
                val atmo = LocalAtmospherePalette.current
                val haptics = LocalHapticFeedback.current
                LaunchedEffect(uiState.celebrationTick) {
                    if (uiState.celebrationTick > 0) {
                        haptics.tapConfirm()
                    }
                }
                AtmosphericBackground {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useNavigationRail = maxWidth >= 600.dp
                        val visibleTabs = remember(uiState.isSimpleMode) {
                            if (uiState.isSimpleMode) {
                                listOf(MainTab.JADWAL, MainTab.FOKUS, MainTab.REKAP)
                            } else {
                                MainTab.entries
                            }
                        }

                        if (uiState.isPreferencesLoaded && !uiState.hasCompletedOnboarding) {
                            OnboardingStartingScreen(
                                initialName = uiState.userName,
                                initialSimpleMode = uiState.isSimpleMode,
                                initialThemeMode = uiState.themeMode,
                                onPreviewThemeMode = viewModel::setThemeMode,
                                onComplete = viewModel::completeOnboarding,
                            )
                        } else if (uiState.isPreferencesLoaded) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = atmo.bgTop,
                                contentColor = MaterialTheme.colorScheme.onBackground,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                                topBar = {
                                    JadwalNihTopBar(
                                        onOpenSettings = { showSettingsSheet = true },
                                        holidayThemeType = effectiveHolidayTheme,
                                        unreadNotificationCount = uiState.unreadNotificationCount,
                                        onOpenNotifications = { showNotificationSheet = true },
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
                                            visibleTabs = visibleTabs,
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
                                            visibleTabs.forEach { tab ->
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
                                            .widthIn(max = 600.dp)

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
                                                    onDeleteTask = { task ->
                                                        viewModel.deleteTask(task, deleteAllSeries = false)
                                                    },
                                                    onDeleteRecurringSeries = { task ->
                                                        viewModel.deleteTask(task, deleteAllSeries = true)
                                                    },
                                                    onQuickGlanceClick = {
                                                        if (!uiState.isSimpleMode) {
                                                            val targetTab = when (uiState.quickGlanceTarget) {
                                                                QuickGlanceTarget.ANALITIK -> MainTab.ANALITIK
                                                                QuickGlanceTarget.MATRIKS -> MainTab.MATRIKS
                                                            }
                                                            viewModel.selectTab(targetTab)
                                                        }
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
                                                    onRescheduleYesterday = viewModel::rescheduleYesterdayTasksToToday,
                                                    onSkipYesterday = viewModel::skipYesterdayReschedule,
                                                    onOpenFocusTab = { viewModel.selectTab(MainTab.FOKUS) },
                                                    onOpenRecapTab = { viewModel.selectTab(MainTab.REKAP) },
                                                    onOpenMatrixTab = { viewModel.selectTab(MainTab.MATRIKS) },
                                                    onOpenAnalyticsTab = { viewModel.selectTab(MainTab.ANALITIK) },
                                                    onDismissWelcomeCard = viewModel::dismissV1WelcomeCard,
                                                    onToggleWorshipComplete = viewModel::toggleWorshipComplete,
                                                    onActivateForeshadowEarly = { occ ->
                                                        viewModel.activateForeshadowEarly(occ) { taskToEdit ->
                                                            editingTask = taskToEdit
                                                            editingDraft = null
                                                            editorDefaultHour = null
                                                            editorDefaultDateIso = taskToEdit.dateIso
                                                            showEditorSheet = true
                                                        }
                                                    },
                                                    onSkipForeshadow = viewModel::skipForeshadowOccurrence,
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
                                                    onDismissWorshipPrompt = viewModel::dismissGentleFocusWorshipPrompt,
                                                    onMarkWorshipDone = { item ->
                                                        viewModel.toggleWorshipComplete(item)
                                                        viewModel.dismissGentleFocusWorshipPrompt()
                                                    },
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
                                onDraftChanged = viewModel::updateLiveDraft,
                                onSaveDraftAndDismiss = { draft ->
                                    viewModel.saveTaskDraft(draft)
                                    viewModel.showMessage(
                                        "Draf disimpan ke Pusat Notifikasi & Beranda. Ketuk Lanjut Edit kapan saja.",
                                    )
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
                                onSaveBatch = { tasks, conflicts ->
                                    viewModel.saveBatchTasks(tasks, conflicts)
                                    showEditorSheet = false
                                },
                                onSaveBatchWithResolution = { tasks, conflicts, resolution ->
                                    viewModel.saveBatchTasks(tasks, conflicts, resolution)
                                    showEditorSheet = false
                                },
                                defaultLocationName = uiState.currentLocationName,
                                defaultLocationLat = uiState.currentLatitude,
                                defaultLocationLng = uiState.currentLongitude,
                                onDetectLocation = { viewModel.detectCurrentLocationNow(showFeedback = true) },
                                onNotifyMessage = viewModel::showMessage,
                                onNotifyPermissionDenied = {
                                    viewModel.showMessage("Izin notifikasi ditolak; pengingat sistem dinonaktifkan.")
                                },
                            )
                        }

                        if (showNotificationSheet) {
                            NotificationCenterSheet(
                                logs = uiState.notificationLogs,
                                onResumeDraft = { draft ->
                                    showNotificationSheet = false
                                    editingTask = null
                                    editingDraft = draft
                                    editorDefaultHour = null
                                    editorDefaultDateIso = draft.dateIso
                                    showEditorSheet = true
                                },
                                onDeleteLog = viewModel::deleteNotificationLogWithUndo,
                                onClearAllLogs = viewModel::clearAllNotificationsWithUndo,
                                onMarkAllRead = viewModel::markAllNotificationsRead,
                                onDismiss = { showNotificationSheet = false },
                            )
                        }

                        if (showSettingsSheet) {
                            SettingsSheet(
                                userName = uiState.userName,
                                themeMode = uiState.themeMode,
                                systemInDarkTheme = systemInDark,
                                isSimpleMode = uiState.isSimpleMode,
                                quickGlanceTarget = uiState.quickGlanceTarget,
                                checkInNotificationsEnabled = uiState.checkInNotificationsEnabled,
                                isBusyBackup = uiState.isBusyBackup,
                                onUserNameChange = viewModel::updateUserName,
                                onThemeModeChange = viewModel::setThemeMode,
                                onSimpleModeChange = viewModel::setSimpleMode,
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
                                religion = uiState.religion,
                                prayerReminderEnabled = uiState.prayerReminderEnabled,
                                worshipNotificationSoundEnabled = uiState.worshipNotificationSoundEnabled,
                                holidayThemeEnabled = uiState.holidayThemeEnabled,
                                holidayThemePreview = uiState.holidayThemePreview,
                                currentLocationName = uiState.currentLocationName,
                                onReligionChange = viewModel::setReligion,
                                onPrayerReminderEnabledChange = viewModel::setPrayerReminderEnabled,
                                onWorshipSoundChange = viewModel::setWorshipNotificationSoundEnabled,
                                onHolidayThemeEnabledChange = viewModel::setHolidayThemeEnabled,
                                onHolidayThemePreviewChange = viewModel::setHolidayThemePreview,
                                onDetectLocation = { viewModel.detectCurrentLocationNow(showFeedback = true) },
                                onManualLocationSave = viewModel::updateManualLocation,
                            )
                        }

                        val importPreview = uiState.pendingImportPreview
                        if (importPreview != null) {
                            AlertDialog(
                                onDismissRequest = { viewModel.dismissImportPreview() },
                                shape = MaterialTheme.shapes.large,
                                containerColor = atmo.sheetBg,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                title = {
                                    Text(
                                        text = "Impor Cadangan ${importPreview.appName}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (importPreview.appName == "TugasNih") {
                                            Text(
                                                "Terdeteksi cadangan TugasNih:\n" +
                                                    "• ${importPreview.taskCount} tugas sekolah\n" +
                                                    "• ${importPreview.lessonCount} jadwal pelajaran (berulang mingguan)",
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        } else {
                                            Text(
                                                "Terdeteksi cadangan JadwalNih:\n" +
                                                    "• ${importPreview.taskCount} jadwal kegiatan\n" +
                                                    "• ${importPreview.sessionCount} sesi fokus\n" +
                                                    "• ${importPreview.recapCount} rekap foto",
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                        Text(
                                            text = "Pilih cara memasukkan data ke JadwalNih:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                },
                                confirmButton = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BouncyButton(
                                            onClick = {
                                                haptics.tapConfirm()
                                                viewModel.confirmImport(JsonBackupManager.ImportStrategy.MERGE)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = atmo.accentCobalt,
                                                contentColor = atmo.onAccentCobalt,
                                            ),
                                            modifier = Modifier.testTag("import_merge_button"),
                                        ) {
                                            Text("Gabungkan Data")
                                        }
                                        BouncyButton(
                                            onClick = {
                                                haptics.tapConfirm()
                                                viewModel.confirmImport(JsonBackupManager.ImportStrategy.REPLACE)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError,
                                            ),
                                            modifier = Modifier.testTag("import_replace_button"),
                                        ) {
                                            Text("Timpa Bersih")
                                        }
                                    }
                                },
                                dismissButton = {
                                    BouncyTextButton(
                                        onClick = {
                                            haptics.tapSoft()
                                            viewModel.dismissImportPreview()
                                        },
                                        modifier = Modifier.testTag("import_cancel_button"),
                                    ) {
                                        Text("Batal")
                                    }
                                },
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
