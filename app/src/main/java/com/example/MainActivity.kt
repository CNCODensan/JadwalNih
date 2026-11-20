package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calendar.CalendarExportHelper
import com.example.domain.DateTimeHelper
import com.example.domain.MainTab
import com.example.domain.ScheduleTask
import com.example.ui.JadwalNihViewModel
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.DataManagementDialog
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.JadwalNihTopBar
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.AnalitikScreen
import com.example.ui.screens.FokusScreen
import com.example.ui.screens.JadwalScreen
import com.example.ui.screens.MatriksScreen
import com.example.ui.screens.RekapScreen
import com.example.ui.theme.JadwalNihTheme
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

            var showEditorSheet by remember { mutableStateOf(false) }
            var editingTask by remember { mutableStateOf<ScheduleTask?>(null) }
            var showDataDialog by remember { mutableStateOf(false) }

            LaunchedEffect(uiState.snackbarMessage) {
                val msg = uiState.snackbarMessage
                if (msg != null) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearSnackbar()
                }
            }

            JadwalNihTheme(darkTheme = uiState.isDarkMode) {
                val atmo = LocalAtmospherePalette.current
                AtmosphericBackground {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useNavigationRail = maxWidth >= 600.dp

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = {
                                JadwalNihTopBar(
                                    isDarkMode = uiState.isDarkMode,
                                    onToggleTheme = viewModel::toggleTheme,
                                    onOpenDataDialog = { showDataDialog = true },
                                )
                            },
                            floatingActionButton = {
                                if (uiState.selectedTab == MainTab.JADWAL || uiState.selectedTab == MainTab.MATRIKS) {
                                    FloatingActionButton(
                                        onClick = {
                                            editingTask = null
                                            showEditorSheet = true
                                        },
                                        shape = CircleShape,
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White,
                                        elevation = FloatingActionButtonDefaults.elevation(
                                            defaultElevation = 6.dp,
                                            pressedElevation = 10.dp,
                                        ),
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(atmo.heroGradStart, atmo.heroGradEnd),
                                                ),
                                            )
                                            .testTag("add_task_fab"),
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Tambah Jadwal Baru",
                                                tint = Color.White,
                                            )
                                        }
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
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { viewModel.selectTab(tab) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tabIcon(tab, selected),
                                                        contentDescription = tab.label,
                                                    )
                                                },
                                                label = { Text(tab.label) },
                                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
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

                                    when (uiState.selectedTab) {
                                        MainTab.JADWAL -> JadwalScreen(
                                            state = uiState,
                                            onSelectScope = viewModel::selectScope,
                                            onSelectDate = viewModel::selectDate,
                                            onStepDate = viewModel::stepDate,
                                            onJumpToToday = viewModel::jumpToToday,
                                            onSelectStatusFilter = viewModel::setStatusFilter,
                                            onToggleComplete = viewModel::toggleTaskComplete,
                                            onMoveToTomorrow = viewModel::moveTaskToTomorrow,
                                            onExportCalendar = { task ->
                                                val launched = CalendarExportHelper.launchCalendarInsert(context, task)
                                                if (!launched) {
                                                    viewModel.showMessage("Aplikasi kalender tidak ditemukan di perangkat ini.")
                                                }
                                            },
                                            onEditTask = { task ->
                                                editingTask = task
                                                showEditorSheet = true
                                            },
                                            onDeleteTask = viewModel::deleteTask,
                                            onOpenFocusTab = { viewModel.selectTab(MainTab.FOKUS) },
                                            onOpenRecapTab = { viewModel.selectTab(MainTab.REKAP) },
                                            modifier = contentModifier,
                                        )

                                        MainTab.MATRIKS -> MatriksScreen(
                                            state = uiState,
                                            onSelectScope = viewModel::setMatrixScope,
                                            onToggleComplete = viewModel::toggleTaskComplete,
                                            onMoveToTomorrow = viewModel::moveTaskToTomorrow,
                                            onChangeQuadrant = viewModel::changeTaskQuadrant,
                                            onEditTask = { task ->
                                                editingTask = task
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
                                            onSelectDate = viewModel::selectRecapDate,
                                            onAddPhotos = viewModel::addPhotosToRecap,
                                            onRemovePhoto = viewModel::removePhotoFromRecap,
                                            onSaveMeta = viewModel::saveRecapMeta,
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

                        if (showEditorSheet) {
                            TaskEditorSheet(
                                initialTask = editingTask,
                                defaultDateIso = uiState.selectedDateIso,
                                onDismiss = { showEditorSheet = false },
                                onSave = { saved ->
                                    viewModel.saveTask(saved)
                                    showEditorSheet = false
                                },
                                onNotifyPermissionDenied = {
                                    viewModel.showMessage("Izin notifikasi ditolak; pengingat sistem dinonaktifkan.")
                                },
                            )
                        }

                        if (showDataDialog) {
                            DataManagementDialog(
                                isBusy = uiState.isBusyBackup,
                                onDismiss = { showDataDialog = false },
                                onExportJson = {
                                    exportJsonLauncher.launch("jadwalnih_backup_${DateTimeHelper.todayIso()}.json")
                                },
                                onImportJson = {
                                    importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                onResetDataConfirmed = viewModel::resetAllData,
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
