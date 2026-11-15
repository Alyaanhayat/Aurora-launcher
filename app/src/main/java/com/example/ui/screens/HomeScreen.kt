package com.example.ui.screens

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.AppDrawerSide
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.HabitItem
import com.example.model.HomeGridAppPlacement
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.PlacedAppWidget
import com.example.model.PlacedHomeFolder
import com.example.model.TabletFolderSize
import com.example.model.TaskItem
import com.example.model.ThemeMode
import androidx.compose.runtime.collectAsState
import com.example.ui.components.AppIconGraphic
import com.example.ui.components.AppIconView
import com.example.ui.components.DockBar
import com.example.ui.components.EmptyGridCellActionSheet
import com.example.ui.components.FolderExpandedDialog
import com.example.ui.components.HomeScreenMenuSheet
import com.example.ui.components.PlacedAppWidgetCard
import com.example.ui.components.PlacedHomeFolderCard
import com.example.ui.components.PurpleCheckerboardBackdrop
import com.example.ui.components.PurpleFailsafeBanner
import com.example.ui.components.PurpleFailsafeDetailsDialog
import com.example.ui.components.RenderFailsafeMonitor
import com.example.ui.components.WidgetManageDialog
import com.example.ui.components.failsafeDrawBoundary
import com.example.ui.components.getIconShape
import com.example.ui.theme.isLauncherDarkTheme
import com.example.ui.widgets.AtAGlanceWidget
import com.example.ui.widgets.NotesWidget
import com.example.ui.widgets.TasksWidget
import com.example.util.SystemAppWidgetHelper
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragSource {
    HOME,
    DOCK
}

private data class DragSession(
    val app: AppItem,
    val source: DragSource,
    val sourcePage: Int,
    val sourceIndex: Int
)

@Composable
fun HomeScreen(
    preferences: LauncherPreferences,
    allApps: List<AppItem>,
    dockedApps: List<AppItem>,
    homeGridAppsMap: Map<Int, List<Pair<HomeGridAppPlacement, AppItem>>>,
    placedWidgets: List<PlacedAppWidget> = emptyList(),
    folders: List<AppFolder> = emptyList(),
    placedHomeFolders: List<PlacedHomeFolder> = emptyList(),
    notes: List<NoteItem>,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    isDefaultLauncher: Boolean = false,
    isTabletLayout: Boolean = false,
    onSetDefaultLauncher: () -> Unit = {},
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onOpenDrawer: (AppDrawerSide) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddNote: (String) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleHabit: (HabitItem) -> Unit,
    onAddNewPage: () -> Unit = {},
    onRemoveCurrentPage: () -> Unit = {},
    onToggleWidget: (String, Boolean) -> Unit = { _, _ -> },
    onCycleWallpaper: () -> Unit = {},
    onPlaceAppOnHomeGrid: (page: Int, app: AppItem, row: Int, col: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _ -> },
    onMoveAppWithinDock: (AppItem, Int) -> Unit = { _, _ -> },
    onMoveAppHomeToDock: (Int, AppItem, Int) -> Unit = { _, _, _ -> },
    onMoveAppDockToHomeGrid: (app: AppItem, page: Int, row: Int?, col: Int?, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _ -> },
    onRemoveAppFromHomePage: (Int, AppItem) -> Unit = { _, _ -> },
    onRemoveAppFromDock: (AppItem) -> Unit = {},
    onOpenThirdPartyWidgetPicker: (page: Int, preferredRow: Int?, preferredCol: Int?, cols: Int, rows: Int) -> Unit = { _, _, _, _, _ -> },
    onMoveThirdPartyWidget: (appWidgetId: Int, targetPage: Int, targetRow: Int, targetCol: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _ -> },
    onResizeThirdPartyWidget: (appWidgetId: Int, newSpanX: Int, newSpanY: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _ -> },
    onUpdateThirdPartyWidgetBounds: (appWidgetId: Int, targetRow: Int, targetCol: Int, newSpanX: Int, newSpanY: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onConfigureThirdPartyWidget: (appWidgetId: Int) -> Unit = {},
    onRemoveThirdPartyWidget: (appWidgetId: Int) -> Unit = {},
    onPlaceFolderOnHomeGrid: (folderId: String, page: Int, preferredRow: Int?, preferredCol: Int?, spanX: Int, spanY: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onCreateAndPlaceFolderOnHomeGrid: (name: String, page: Int, preferredRow: Int?, preferredCol: Int?, spanX: Int, spanY: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateHomeFolderBounds: (folderId: String, targetRow: Int, targetCol: Int, newSpanX: Int, newSpanY: Int, cols: Int, rows: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onRemoveHomeFolder: (folderId: String) -> Unit = {},
    onRemoveAppFromFolder: (AppFolder, AppItem) -> Unit = { _, _ -> },
    onRenameFolder: (AppFolder, String) -> Unit = { _, _ -> },
    onDeleteFolder: (AppFolder) -> Unit = {},
    onChangeTabletFolderSize: (TabletFolderSize) -> Unit = {},
    onToggleGridGuides: (Boolean) -> Unit = {},
    onCurrentHomePageChanged: (Int) -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidthDp = maxWidth
        val pinShelfOnLeftPane = isTabletLayout &&
            preferences.enableProductivityShelf &&
            availableWidthDp >= 520.dp
        val showShelfAsPagerPage = preferences.enableProductivityShelf && !pinShelfOnLeftPane

        // Total pages: optional Productivity Shelf on the left (index 0) + Home pages (homeStartPagerIndex until totalPages)
        val shelfPagerIndex = if (showShelfAsPagerPage) 0 else -1
        val homeStartPagerIndex = if (showShelfAsPagerPage) 1 else 0
        val totalPages = (preferences.homePageCount + homeStartPagerIndex).coerceAtLeast(1)
        val pagerState = rememberPagerState(initialPage = homeStartPagerIndex, pageCount = { totalPages })
        val coroutineScope = rememberCoroutineScope()
        var showHomeScreenMenu by remember { mutableStateOf(false) }
        var isGridEditMode by remember { mutableStateOf(false) }
        var resizingWidgetId by remember { mutableStateOf<Int?>(null) }
        var resizingFolderId by remember { mutableStateOf<String?>(null) }
        var isDraggingResizeHandle by remember { mutableStateOf(false) }
        var selectedEmptyCell by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
        var activeManageWidget by remember { mutableStateOf<PlacedAppWidget?>(null) }
        var activeExpandedHomeFolder by remember { mutableStateOf<AppFolder?>(null) }
        var prevShowShelfAsPagerPage by remember { mutableStateOf(showShelfAsPagerPage) }
        val visibleAppsByPkg = remember(allApps) {
            allApps.asSequence().filter { !it.isHidden }.associateBy { it.packageName }
        }

        LaunchedEffect(isTabletLayout) {
            if (!isTabletLayout) {
                resizingFolderId = null
            }
        }

        LaunchedEffect(showShelfAsPagerPage, preferences.homePageCount) {
            if (prevShowShelfAsPagerPage != showShelfAsPagerPage) {
                val shifted = if (showShelfAsPagerPage) {
                    (pagerState.currentPage + 1).coerceIn(1, (totalPages - 1).coerceAtLeast(1))
                } else {
                    (pagerState.currentPage - 1).coerceIn(0, (preferences.homePageCount - 1).coerceAtLeast(0))
                }
                prevShowShelfAsPagerPage = showShelfAsPagerPage
                pagerState.scrollToPage(shifted)
            }
        }

        LaunchedEffect(pagerState.currentPage, preferences.homePageCount, showShelfAsPagerPage) {
            val logicalHomePage = pagerState.currentPage - homeStartPagerIndex
            if (logicalHomePage in 0 until preferences.homePageCount) {
                onCurrentHomePageChanged(logicalHomePage)
            } else if (pagerState.currentPage >= totalPages) {
                val safePage = (totalPages - 1).coerceAtLeast(0)
                pagerState.scrollToPage(safePage)
                val clampedHome = (safePage - homeStartPagerIndex).coerceAtLeast(0)
                if (clampedHome < preferences.homePageCount) {
                    onCurrentHomePageChanged(clampedHome)
                }
            }
        }

    val isDark = isLauncherDarkTheme(preferences.themeMode)
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val renderFaults by RenderFailsafeMonitor.faults.collectAsState()
    val softwareFallbackActive by RenderFailsafeMonitor.softwareFallbackActive.collectAsState()
    var showFailsafeDialog by remember { mutableStateOf(false) }

    // Read Android system wallpaper brightness hints (API 27+, zero permissions required)
    var wallpaperFault by remember { mutableStateOf(false) }
    var wallpaperSupportsDarkText by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val res = runCatching {
                    val colors = WallpaperManager.getInstance(context)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    colors != null && (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                }
                res.onFailure { err ->
                    wallpaperFault = true
                    RenderFailsafeMonitor.reportFault(
                        component = "System Wallpaper",
                        reason = "Wallpaper / display driver failed to respond",
                        throwable = err
                    )
                }
                res.getOrDefault(false)
            } else false
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        DisposableEffect(context) {
            val wm = runCatching { WallpaperManager.getInstance(context) }
                .onFailure { err ->
                    wallpaperFault = true
                    RenderFailsafeMonitor.reportFault(
                        component = "System Wallpaper",
                        reason = "Wallpaper service unavailable",
                        throwable = err
                    )
                }
                .getOrNull()
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0) {
                    wallpaperSupportsDarkText = colors != null &&
                        (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                }
            }
            if (wm != null) {
                runCatching {
                    wm.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
                }.onFailure { err ->
                    wallpaperFault = true
                    RenderFailsafeMonitor.reportFault(
                        component = "System Wallpaper",
                        reason = "Wallpaper listener registration failed",
                        throwable = err
                    )
                }
            }
            onDispose {
                if (wm != null) {
                    runCatching { wm.removeOnColorsChangedListener(listener) }
                }
            }
        }
    }

    val useSystemWallpaper = preferences.wallpaperThemeIndex == 0 && !wallpaperFault

    // Wallpaper background gradient when a gradient preset (1..4) is chosen; null = Android system wallpaper
    val wallpaperBrush = remember(preferences.wallpaperThemeIndex, preferences.themeMode, isDark) {
        when {
            preferences.wallpaperThemeIndex == 0 -> null
            preferences.themeMode == ThemeMode.AMOLED_BLACK -> Brush.verticalGradient(
                listOf(Color(0xFF000000), Color(0xFF000000))
            )
            isDark -> when (preferences.wallpaperThemeIndex) {
                1 -> Brush.verticalGradient(
                    listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF0B0E14))
                )
                2 -> Brush.verticalGradient(
                    listOf(Color(0xFF1E102F), Color(0xFF130924), Color(0xFF0A0512))
                )
                3 -> Brush.verticalGradient(
                    listOf(Color(0xFF0A2218), Color(0xFF081912), Color(0xFF05100B))
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF141414), Color(0xFF000000))
                )
            }
            else -> when (preferences.wallpaperThemeIndex) {
                1 -> Brush.verticalGradient(
                    listOf(Color(0xFFEAF0FF), Color(0xFFDCE6FA), Color(0xFFF4F7FF))
                )
                2 -> Brush.verticalGradient(
                    listOf(Color(0xFFF3EBFF), Color(0xFFE8DAFF), Color(0xFFFCEEFF))
                )
                3 -> Brush.verticalGradient(
                    listOf(Color(0xFFE6F8F0), Color(0xFFD4F2E4), Color(0xFFF2FCF7))
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE8EDF5))
                )
            }
        }
    }

    // Drag-and-drop coordinator state
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var isTouchingAppIcon by remember { mutableStateOf(false) }
    var dragSession by remember { mutableStateOf<DragSession?>(null) }
    var dragStartPosInRoot by remember { mutableStateOf(Offset.Zero) }
    var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }
    var hasDraggedSignificantly by remember { mutableStateOf(false) }

    var removeZoneBounds by remember { mutableStateOf(Rect.Zero) }
    var dockBounds by remember { mutableStateOf(Rect.Zero) }
    val dockSlotBounds = remember { mutableStateMapOf<Int, Rect>() }
    val homeGridBounds = remember { mutableStateMapOf<Int, Rect>() }
    val pageGridDimensions = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }

    val resetDrag: () -> Unit = {
        dragSession = null
        dragStartPosInRoot = Offset.Zero
        dragPointerInRoot = Offset.Zero
        hasDraggedSignificantly = false
    }

    val startDrag: (AppItem, DragSource, Int, Int, Offset, Rect) -> Unit =
        { app, source, page, index, localOffset, slotBounds ->
            if (preferences.hapticFeedbackEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            val startInRoot = slotBounds.topLeft + localOffset
            dragSession = DragSession(
                app = app,
                source = source,
                sourcePage = page,
                sourceIndex = index
            )
            dragStartPosInRoot = startInRoot
            dragPointerInRoot = startInRoot
            hasDraggedSignificantly = false
        }

    val onDragDelta: (Offset) -> Unit = { delta ->
        dragPointerInRoot += delta
        if (!hasDraggedSignificantly && (dragPointerInRoot - dragStartPosInRoot).getDistance() > 22f) {
            hasDraggedSignificantly = true
        }
    }

    val activeDockList = dockedApps.take(5)
    val currentPagerPage = pagerState.currentPage
    val currentHomePage = currentPagerPage - homeStartPagerIndex
    val isOnHomePagerPage = currentHomePage in 0 until preferences.homePageCount

    val isOverRemoveZone = dragSession != null &&
        !removeZoneBounds.isEmpty &&
        removeZoneBounds.inflate(28f).contains(dragPointerInRoot)

    val isOverDock = dragSession != null &&
        !isOverRemoveZone &&
        !dockBounds.isEmpty &&
        dockBounds.inflate(20f).contains(dragPointerInRoot)

    val hoveredDockSlotIndex: Int? = if (isOverDock) {
        val validEntries = activeDockList.indices.mapNotNull { idx ->
            dockSlotBounds[idx]?.let { idx to it }
        }
        val exactHit = validEntries.firstOrNull { (_, rect) -> rect.inflate(10f).contains(dragPointerInRoot) }
        if (exactHit != null) {
            exactHit.first
        } else if (activeDockList.size < 5 && validEntries.isNotEmpty() &&
            dragPointerInRoot.x > validEntries.last().second.right
        ) {
            activeDockList.size
        } else {
            validEntries.minByOrNull { (_, rect) -> abs(dragPointerInRoot.x - rect.center.x) }?.first
                ?: activeDockList.size.coerceAtMost(4)
        }
    } else {
        null
    }

    val isOverHomeGrid = dragSession != null &&
        !isOverRemoveZone &&
        !isOverDock &&
        isOnHomePagerPage &&
        homeGridBounds[currentHomePage]?.inflate(24f)?.contains(dragPointerInRoot) == true

    val hoveredHomeCell: Pair<Int, Int>? = if (isOverHomeGrid) {
        val rect = homeGridBounds[currentHomePage]
        val (cols, rows) = pageGridDimensions[currentHomePage] ?: (4 to 4)
        if (rect != null && rect.width > 0f && rect.height > 0f) {
            val relX = (dragPointerInRoot.x - rect.left).coerceIn(0f, rect.width - 1f)
            val relY = (dragPointerInRoot.y - rect.top).coerceIn(0f, rect.height - 1f)
            val c = ((relX / rect.width) * cols).toInt().coerceIn(0, cols - 1)
            val r = ((relY / rect.height) * rows).toInt().coerceIn(0, rows - 1)
            r to c
        } else {
            0 to 0
        }
    } else {
        null
    }

    LaunchedEffect(isOverRemoveZone, preferences.hapticFeedbackEnabled) {
        if (preferences.hapticFeedbackEnabled && isOverRemoveZone) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    LaunchedEffect(hoveredHomeCell, hasDraggedSignificantly, preferences.hapticFeedbackEnabled) {
        if (preferences.hapticFeedbackEnabled && hasDraggedSignificantly && hoveredHomeCell != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    LaunchedEffect(hoveredDockSlotIndex, hasDraggedSignificantly, preferences.hapticFeedbackEnabled) {
        if (preferences.hapticFeedbackEnabled && hasDraggedSignificantly && hoveredDockSlotIndex != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val onDragEnd: () -> Unit = {
        val session = dragSession
        if (session != null) {
            val safeHomePage = currentHomePage.coerceIn(0, (preferences.homePageCount - 1).coerceAtLeast(0))
            val (cols, rows) = pageGridDimensions[safeHomePage] ?: (4 to 4)
            if (!hasDraggedSignificantly) {
                resetDrag()
                onAppLongClick(session.app)
            } else if (isOverRemoveZone) {
                if (preferences.hapticFeedbackEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                if (session.source == DragSource.HOME) {
                    onRemoveAppFromHomePage(session.sourcePage, session.app)
                } else {
                    onRemoveAppFromDock(session.app)
                }
                resetDrag()
            } else if (isOverDock) {
                if (preferences.hapticFeedbackEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                val targetIdx = hoveredDockSlotIndex ?: activeDockList.size.coerceAtMost(4)
                if (session.source == DragSource.DOCK) {
                    onMoveAppWithinDock(session.app, targetIdx)
                } else {
                    onMoveAppHomeToDock(session.sourcePage, session.app, targetIdx)
                }
                resetDrag()
            } else if (isOnHomePagerPage) {
                val targetCell = hoveredHomeCell
                if (session.source == DragSource.HOME && session.sourcePage == safeHomePage) {
                    if (targetCell != null) {
                        if (preferences.hapticFeedbackEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                        onPlaceAppOnHomeGrid(
                            safeHomePage,
                            session.app,
                            targetCell.first,
                            targetCell.second,
                            cols,
                            rows
                        )
                    }
                } else if (session.source == DragSource.DOCK &&
                    (isOverHomeGrid || dragPointerInRoot.y < (dockBounds.top - 24f))
                ) {
                    if (preferences.hapticFeedbackEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                    onMoveAppDockToHomeGrid(
                        session.app,
                        safeHomePage,
                        targetCell?.first,
                        targetCell?.second,
                        cols,
                        rows
                    )
                }
                resetDrag()
            } else {
                resetDrag()
            }
        }
    }

    var totalDragY by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var rootWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootOrigin = coords.boundsInRoot().topLeft
                rootWidthPx = coords.size.width.toFloat()
            }
            .then(
                if (wallpaperBrush != null) Modifier.background(wallpaperBrush)
                else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        // Only open home options when long-pressing empty wallpaper, not an app icon
                        if (!isTouchingAppIcon && dragSession == null) {
                            showHomeScreenMenu = true
                        }
                    }
                )
            }
            .pointerInput(isTabletLayout) {
                detectDragGestures(
                    onDragStart = { offset ->
                        totalDragY = 0f
                        dragStartX = offset.x
                    },
                    onDrag = { change, dragAmount ->
                        if (dragSession == null && !isDraggingResizeHandle) {
                            change.consume()
                            totalDragY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        if (dragSession == null && !isDraggingResizeHandle) {
                            if (totalDragY < -60f) {
                                // Swiped UP -> Left / Right side drawer in Tablet mode, full drawer on Phone
                                val side = if (isTabletLayout) {
                                    val ratio = if (rootWidthPx > 0f) dragStartX / rootWidthPx else 0.5f
                                    when {
                                        ratio < 0.42f -> AppDrawerSide.LEFT
                                        ratio > 0.58f -> AppDrawerSide.RIGHT
                                        else -> AppDrawerSide.FULL
                                    }
                                } else {
                                    AppDrawerSide.FULL
                                }
                                onOpenDrawer(side)
                            } else if (totalDragY > 60f && preferences.enableSmartSearch) {
                                // Swiped DOWN -> Open Smart Search
                                onOpenSearch()
                            }
                        }
                        totalDragY = 0f
                    }
                )
            }
            .failsafeDrawBoundary("Home Screen")
            .testTag("home_screen_root")
    ) {
        if (wallpaperFault && preferences.wallpaperThemeIndex == 0) {
            PurpleCheckerboardBackdrop(
                tileSize = 24.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            AnimatedVisibility(visible = (isGridEditMode || resizingWidgetId != null || resizingFolderId != null) && dragSession == null) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Button(
                        onClick = {
                            isGridEditMode = false
                            resizingWidgetId = null
                            resizingFolderId = null
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("home_grid_done_editing_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done editing",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            var showDefaultBanner by remember { mutableStateOf(true) }
            AnimatedVisibility(visible = !isDefaultLauncher && showDefaultBanner && dragSession == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f))
                        .clickable { onSetDefaultLauncher() }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("home_set_default_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Set Aurora as default home app",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { showDefaultBanner = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = renderFaults.isNotEmpty() && dragSession == null) {
                PurpleFailsafeBanner(
                    faults = renderFaults,
                    softwareFallbackActive = softwareFallbackActive,
                    onOpenDetails = { RenderFailsafeMonitor.openReportPrompt() },
                    onDismiss = {
                        wallpaperFault = false
                        RenderFailsafeMonitor.clearActiveFaults(context)
                    }
                )
            }

            // Tablet 2-Pane Split or Standard Pager + Dock Layout (Productivity Shelf on the Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Tablet Pinned Productivity Shelf on the Left
                if (pinShelfOnLeftPane) {
                    Box(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight()
                            .navigationBarsPadding()
                            .padding(start = 12.dp, bottom = 12.dp)
                    ) {
                        WidgetShelfScreen(
                            preferences = preferences,
                            notes = notes,
                            tasks = tasks,
                            habits = habits,
                            isTabletSidePane = true,
                            onAddNote = onAddNote,
                            onDeleteNote = onDeleteNote,
                            onAddTask = onAddTask,
                            onToggleTask = onToggleTask,
                            onDeleteTask = onDeleteTask,
                            onToggleHabit = onToggleHabit,
                            onOpenSettings = onOpenSettings
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(if (pinShelfOnLeftPane) 0.60f else 1f)
                        .fillMaxHeight()
                ) {
                    // Horizontal Pager: Productivity Shelf on Left (index 0) + Home Pages on Center/Right
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        userScrollEnabled = dragSession == null && !isDraggingResizeHandle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { pagerIndex ->
                        if (showShelfAsPagerPage && pagerIndex == shelfPagerIndex) {
                            // Leftmost Page: Productivity Shelf Dashboard in active launcher theme color
                            WidgetShelfScreen(
                                preferences = preferences,
                                notes = notes,
                                tasks = tasks,
                                habits = habits,
                                onAddNote = onAddNote,
                                onDeleteNote = onDeleteNote,
                                onAddTask = onAddTask,
                                onToggleTask = onToggleTask,
                                onDeleteTask = onDeleteTask,
                                onToggleHabit = onToggleHabit,
                                onOpenSettings = onOpenSettings
                            )
                        } else {
                            val page = (pagerIndex - homeStartPagerIndex)
                                .coerceIn(0, (preferences.homePageCount - 1).coerceAtLeast(0))
                            val pagePlacements = homeGridAppsMap[page] ?: emptyList()
                            val pageWidgets = remember(page, placedWidgets) {
                                placedWidgets.filter { it.page == page }
                            }
                            val pageHomeFolders = remember(page, placedHomeFolders) {
                                placedHomeFolders.filter { it.page == page }
                            }
                            val homeColumns = when {
                                pinShelfOnLeftPane && availableWidthDp >= 840.dp -> 5
                                pinShelfOnLeftPane -> 4
                                isTabletLayout && availableWidthDp >= 600.dp -> 6
                                isTabletLayout -> 5
                                else -> 4
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (page == 0) {
                                    Column {
                                        // Smart At-a-Glance Widget
                                        if ("at_a_glance" in preferences.enabledWidgets) {
                                            AtAGlanceWidget(
                                                onClockClick = {
                                                    allApps.firstOrNull { it.label.contains("Clock", true) }?.let { onAppClick(it) }
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        val showTasksPreview = "tasks" in preferences.enabledWidgets && tasks.isNotEmpty()
                                        val showNotesPreview = "quick_notes" in preferences.enabledWidgets && notes.isNotEmpty()

                                        if (isTabletLayout && !pinShelfOnLeftPane && (showTasksPreview || showNotesPreview)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (showTasksPreview) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        TasksWidget(
                                                            tasks = tasks.take(2),
                                                            onToggleTask = onToggleTask,
                                                            onAddTask = onAddTask,
                                                            onDeleteTask = onDeleteTask
                                                        )
                                                    }
                                                }
                                                if (showNotesPreview) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        NotesWidget(
                                                            notes = notes.take(1),
                                                            onAddNote = onAddNote,
                                                            onDeleteNote = onDeleteNote
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        } else {
                                            if (showTasksPreview) {
                                                TasksWidget(
                                                    tasks = tasks.take(2),
                                                    onToggleTask = onToggleTask,
                                                    onAddTask = onAddTask,
                                                    onDeleteTask = onDeleteTask
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                            } else if (showNotesPreview) {
                                                NotesWidget(
                                                    notes = notes.take(1),
                                                    onAddNote = onAddNote,
                                                    onDeleteNote = onDeleteNote
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }
                                        }
                                    }
                                }

                                // Free-Placement Home Screen Grid (apps & 3rd-party/system AppWidgets anywhere on grid)
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .onGloballyPositioned { coords ->
                                            homeGridBounds[page] = coords.boundsInRoot()
                                        }
                                        .testTag("home_free_grid_$page")
                                ) {
                                    val homeRows = (maxHeight / 84.dp).toInt().coerceIn(3, 6)
                                    LaunchedEffect(page, homeColumns, homeRows) {
                                        pageGridDimensions[page] = homeColumns to homeRows
                                    }

                                    val cellWidth = maxWidth / homeColumns
                                    val cellHeight = maxHeight / homeRows
                                    val showGridOverlay = dragSession != null || isGridEditMode || resizingWidgetId != null || resizingFolderId != null || preferences.showHomeGridGuides
                                    val blockedCells = remember(pageWidgets, pageHomeFolders, homeRows, homeColumns, isTabletLayout) {
                                        buildSet {
                                            pageWidgets.forEach { w ->
                                                for (rr in w.row until (w.row + w.spanY).coerceAtMost(homeRows)) {
                                                    for (cc in w.col until (w.col + w.spanX).coerceAtMost(homeColumns)) {
                                                        add((rr.toLong() shl 16) or cc.toLong())
                                                    }
                                                }
                                            }
                                            pageHomeFolders.forEach { f ->
                                                val fSpanX = if (isTabletLayout) f.spanX else 1
                                                val fSpanY = if (isTabletLayout) f.spanY else 1
                                                for (rr in f.row until (f.row + fSpanY).coerceAtMost(homeRows)) {
                                                    for (cc in f.col until (f.col + fSpanX).coerceAtMost(homeColumns)) {
                                                        add((rr.toLong() shl 16) or cc.toLong())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    val appCells = remember(pagePlacements, homeRows, homeColumns) {
                                        buildSet {
                                            pagePlacements.forEach { (p, _) ->
                                                val rr = p.row.coerceIn(0, homeRows - 1)
                                                val cc = p.col.coerceIn(0, homeColumns - 1)
                                                add((rr.toLong() shl 16) or cc.toLong())
                                            }
                                        }
                                    }

                                    // Layer 1: Interactive Grid Cells (long-press any open cell to place an app, folder, or widget!)
                                    for (r in 0 until homeRows) {
                                        for (c in 0 until homeColumns) {
                                            val cellKey = (r.toLong() shl 16) or c.toLong()
                                            val isHoveredCell = isOverHomeGrid &&
                                                currentHomePage == page &&
                                                hoveredHomeCell == (r to c)
                                            val isCoveredByBlock = cellKey in blockedCells
                                            val hasApp = cellKey in appCells

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .offset(x = cellWidth * c, y = cellHeight * r)
                                                    .size(width = cellWidth, height = cellHeight)
                                                    .padding(3.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .then(
                                                        when {
                                                            isHoveredCell && !isCoveredByBlock -> Modifier
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                                                                .border(
                                                                    width = 2.dp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    shape = RoundedCornerShape(16.dp)
                                                                )
                                                            showGridOverlay -> Modifier
                                                                .background(
                                                                    MaterialTheme.colorScheme.surface.copy(
                                                                        alpha = if (isDark) 0.14f else 0.18f
                                                                    )
                                                                )
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                                    shape = RoundedCornerShape(16.dp)
                                                                )
                                                            else -> Modifier
                                                        }
                                                    )
                                                    .then(
                                                        if (!isCoveredByBlock && !hasApp) {
                                                            Modifier.pointerInput(page, r, c, isGridEditMode, preferences.hapticFeedbackEnabled) {
                                                                detectTapGestures(
                                                                    onTap = {
                                                                        if (isGridEditMode) {
                                                                            selectedEmptyCell = Triple(page, r, c)
                                                                        }
                                                                    },
                                                                    onLongPress = {
                                                                        if (preferences.hapticFeedbackEnabled) {
                                                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                                        }
                                                                        selectedEmptyCell = Triple(page, r, c)
                                                                    }
                                                                )
                                                            }
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                            ) {
                                                if (isGridEditMode && !isCoveredByBlock && !hasApp) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Place app, folder, or widget at row ${r + 1}, col ${c + 1}",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Layer 2: Placed Third-Party & Android System AppWidgets (with fluid interactive resize handles)
                                    pageWidgets.forEach { widget ->
                                        val clampedRow = widget.row.coerceIn(0, (homeRows - 1).coerceAtLeast(0))
                                        val clampedCol = widget.col.coerceIn(0, (homeColumns - 1).coerceAtLeast(0))
                                        val isWidgetResizeActive = isGridEditMode || resizingWidgetId == widget.appWidgetId

                                        PlacedAppWidgetCard(
                                            widget = widget,
                                            cellWidthDp = cellWidth,
                                            cellHeightDp = cellHeight,
                                            gridCols = homeColumns,
                                            gridRows = homeRows,
                                            isEditMode = isGridEditMode,
                                            isResizeActive = isWidgetResizeActive,
                                            onToggleResizeHandles = { active ->
                                                resizingWidgetId = if (active) widget.appWidgetId else null
                                                if (active) resizingFolderId = null
                                            },
                                            onResizeInteractionChanged = { dragging ->
                                                isDraggingResizeHandle = dragging
                                            },
                                            onUpdateBounds = { targetRow, targetCol, newSpanX, newSpanY ->
                                                onUpdateThirdPartyWidgetBounds(
                                                    widget.appWidgetId,
                                                    targetRow,
                                                    targetCol,
                                                    newSpanX,
                                                    newSpanY,
                                                    homeColumns,
                                                    homeRows
                                                )
                                            },
                                            onOpenWidgetOptions = { activeManageWidget = widget },
                                            onRemove = {
                                                if (resizingWidgetId == widget.appWidgetId) {
                                                    resizingWidgetId = null
                                                }
                                                onRemoveThirdPartyWidget(widget.appWidgetId)
                                            },
                                            modifier = Modifier
                                                .zIndex(if (isWidgetResizeActive) 3f else 0f)
                                                .offset(x = cellWidth * clampedCol, y = cellHeight * clampedRow)
                                        )
                                    }

                                    // Layer 2b: Placed Resizable App Folders on Home Grid
                                    pageHomeFolders.forEach { placedFolder ->
                                        val folderObj = folders.firstOrNull { it.id == placedFolder.folderId }
                                        if (folderObj != null) {
                                            val clampedRow = placedFolder.row.coerceIn(0, (homeRows - 1).coerceAtLeast(0))
                                            val clampedCol = placedFolder.col.coerceIn(0, (homeColumns - 1).coerceAtLeast(0))
                                            val isFolderResizeActive = isGridEditMode || resizingFolderId == placedFolder.folderId
                                            val folderApps = remember(folderObj.packageNames, visibleAppsByPkg) {
                                                folderObj.packageNames.mapNotNull { visibleAppsByPkg[it] }
                                            }

                                            PlacedHomeFolderCard(
                                                placedFolder = placedFolder,
                                                folder = folderObj,
                                                appsInFolder = folderApps,
                                                iconPackStyle = preferences.iconPackStyle,
                                                iconSize = preferences.iconSize,
                                                iconShape = preferences.iconShape,
                                                tabletExpandedFolderIconShape = preferences.tabletExpandedFolderIconShape,
                                                tabletFolderBackdrop = preferences.tabletFolderBackdrop,
                                                showLabels = preferences.showAppLabels,
                                                cellWidthDp = cellWidth,
                                                cellHeightDp = cellHeight,
                                                gridCols = homeColumns,
                                                gridRows = homeRows,
                                                isTabletLayout = isTabletLayout,
                                                isEditMode = isGridEditMode,
                                                isResizeActive = isFolderResizeActive,
                                                onOpenFolder = { activeExpandedHomeFolder = folderObj },
                                                onAppClick = onAppClick,
                                                onToggleResizeHandles = { active ->
                                                    resizingFolderId = if (active) placedFolder.folderId else null
                                                    if (active) resizingWidgetId = null
                                                },
                                                onResizeInteractionChanged = { dragging ->
                                                    isDraggingResizeHandle = dragging
                                                },
                                                onUpdateBounds = { targetRow, targetCol, newSpanX, newSpanY ->
                                                    onUpdateHomeFolderBounds(
                                                        placedFolder.folderId,
                                                        targetRow,
                                                        targetCol,
                                                        newSpanX,
                                                        newSpanY,
                                                        homeColumns,
                                                        homeRows
                                                    )
                                                },
                                                onRemoveFromHome = {
                                                    if (resizingFolderId == placedFolder.folderId) {
                                                        resizingFolderId = null
                                                    }
                                                    onRemoveHomeFolder(placedFolder.folderId)
                                                },
                                                modifier = Modifier
                                                    .zIndex(if (isFolderResizeActive) 3f else 1f)
                                                    .offset(x = cellWidth * clampedCol, y = cellHeight * clampedRow)
                                            )
                                        }
                                    }

                                    // Layer 3: Freely Positioned App Icons on Grid Cells
                                    pagePlacements.forEach { (placement, app) ->
                                        val clampedRow = placement.row.coerceIn(0, (homeRows - 1).coerceAtLeast(0))
                                        val clampedCol = placement.col.coerceIn(0, (homeColumns - 1).coerceAtLeast(0))
                                        val slotBoundsState = remember(page, app.packageName, clampedRow, clampedCol) {
                                            mutableStateOf(Rect.Zero)
                                        }
                                        val slotIndex = clampedRow * homeColumns + clampedCol

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .offset(x = cellWidth * clampedCol, y = cellHeight * clampedRow)
                                                .size(width = cellWidth, height = cellHeight)
                                                .onGloballyPositioned { coords ->
                                                    slotBoundsState.value = coords.boundsInRoot()
                                                }
                                        ) {
                                            AppIconView(
                                                app = app,
                                                iconPackStyle = preferences.iconPackStyle,
                                                iconSize = preferences.iconSize,
                                                iconShape = preferences.iconShape,
                                                showLabel = preferences.showAppLabels,
                                                isBeingDragged = dragSession?.app?.packageName == app.packageName,
                                                isDropTarget = isOverHomeGrid &&
                                                    currentHomePage == page &&
                                                    hoveredHomeCell == (clampedRow to clampedCol) &&
                                                    dragSession?.app?.packageName != app.packageName,
                                                onWallpaper = useSystemWallpaper,
                                                wallpaperDarkText = wallpaperSupportsDarkText,
                                                onClick = { onAppClick(app) },
                                                onLongClick = { onAppLongClick(app) },
                                                onTouchActiveChanged = { active -> isTouchingAppIcon = active },
                                                onDragStartAfterLongPress = { localOffset ->
                                                    startDrag(
                                                        app,
                                                        DragSource.HOME,
                                                        page,
                                                        slotIndex,
                                                        localOffset,
                                                        slotBoundsState.value
                                                    )
                                                },
                                                onDragDelta = onDragDelta,
                                                onDragEnd = onDragEnd,
                                                onDragCancel = resetDrag
                                            )
                                        }
                                    }
                                }

                                // Swipe up hint pill
                                val hintColor = if (useSystemWallpaper) {
                                    if (wallpaperSupportsDarkText) Color(0xFF14161D).copy(alpha = 0.88f)
                                    else Color.White.copy(alpha = 0.92f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                }
                                val hintShadow = if (useSystemWallpaper) {
                                    Shadow(
                                        color = if (wallpaperSupportsDarkText) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.75f),
                                        offset = Offset(0f, 1.5f),
                                        blurRadius = 6f
                                    )
                                } else null

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenDrawer(AppDrawerSide.FULL) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Swipe up for all apps",
                                        tint = hintColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isTabletLayout) {
                                            "Swipe ↑ Left · Center · Right for apps"
                                        } else {
                                            "Swipe up for apps"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(shadow = hintShadow),
                                        color = hintColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Dock
                    DockBar(
                        dockedApps = dockedApps,
                        iconPackStyle = preferences.iconPackStyle,
                        iconSize = preferences.iconSize,
                        iconShape = preferences.iconShape,
                        showSearchButton = preferences.enableSmartSearch,
                        draggedAppPackage = dragSession?.app?.packageName,
                        isDraggingAnyApp = dragSession != null,
                        isDockHovered = isOverDock,
                        hoveredDockSlotIndex = hoveredDockSlotIndex,
                        onDockBoundsChanged = { dockBounds = it },
                        onDockSlotBoundsChanged = { idx, rect -> dockSlotBounds[idx] = rect },
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        onAppTouchActiveChanged = { active -> isTouchingAppIcon = active },
                        onAppDragStart = { app, index, localOffset, slotBounds ->
                            startDrag(app, DragSource.DOCK, -1, index, localOffset, slotBounds)
                        },
                        onAppDragDelta = onDragDelta,
                        onAppDragEnd = onDragEnd,
                        onAppDragCancel = resetDrag,
                        onSearchClick = onOpenSearch
                    )
                }
            }
        }

        // Top "× Remove" Drop Zone while dragging an app
        AnimatedVisibility(
            visible = dragSession != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            val zoneScale by animateFloatAsState(
                targetValue = if (isOverRemoveZone) 1.1f else 1.0f,
                label = "remove_zone_scale"
            )
            val zoneBg by animateColorAsState(
                targetValue = if (isOverRemoveZone) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                },
                label = "remove_zone_bg"
            )
            val zoneContentColor by animateColorAsState(
                targetValue = if (isOverRemoveZone) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.error
                },
                label = "remove_zone_fg"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        removeZoneBounds = coords.boundsInRoot()
                    }
                    .graphicsLayer {
                        scaleX = zoneScale
                        scaleY = zoneScale
                    }
                    .shadow(elevation = 10.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(zoneBg)
                    .border(1.5.dp, MaterialTheme.colorScheme.error, CircleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("remove_drop_zone")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from Home Screen",
                    tint = zoneContentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isOverRemoveZone) "Release to Remove" else "Remove",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = zoneContentColor
                )
            }
        }

        // Floating Dragged Icon Overlay (uses graphicsLayer translation to avoid re-layout during drag)
        dragSession?.let { session ->
            val iconSizeDp = preferences.iconSize.dpValue.dp
            val iconSizePx = with(density) { iconSizeDp.toPx() }
            val liftPx = with(density) { 28.dp.toPx() }
            val offsetX = dragPointerInRoot.x - rootOrigin.x - iconSizePx / 2f
            val offsetY = dragPointerInRoot.y - rootOrigin.y - iconSizePx / 2f - liftPx

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offsetX
                        translationY = offsetY
                        scaleX = 1.15f
                        scaleY = 1.15f
                        alpha = if (isOverRemoveZone) 0.72f else 0.96f
                    }
            ) {
                AppIconGraphic(
                    app = session.app,
                    style = preferences.iconPackStyle,
                    size = iconSizeDp,
                    shape = getIconShape(preferences.iconPackStyle, preferences.iconShape),
                    context = context
                )
            }
        }

        // Home Screen Long-Press Menu Sheet (Widget Options & Add Page)
        if (showHomeScreenMenu) {
            val activePage = (pagerState.currentPage - homeStartPagerIndex)
                .coerceIn(0, (preferences.homePageCount - 1).coerceAtLeast(0))
            val (cols, rows) = pageGridDimensions[activePage] ?: (4 to 4)
            HomeScreenMenuSheet(
                preferences = preferences,
                currentPageIndex = activePage,
                isGridEditMode = isGridEditMode,
                onDismiss = { showHomeScreenMenu = false },
                onAddNewPage = {
                    onAddNewPage()
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(homeStartPagerIndex + preferences.homePageCount)
                    }
                },
                onRemoveCurrentPage = {
                    onRemoveCurrentPage()
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage - 1).coerceAtLeast(homeStartPagerIndex)
                        )
                    }
                },
                onOpenSettings = onOpenSettings,
                onCycleWallpaper = onCycleWallpaper,
                onToggleWidget = onToggleWidget,
                onOpenThirdPartyWidgetPicker = {
                    onOpenThirdPartyWidgetPicker(activePage, null, null, cols, rows)
                },
                onToggleGridGuides = onToggleGridGuides,
                onToggleGridEditMode = { isGridEditMode = !isGridEditMode }
            )
        }

        // Empty Grid Cell Quick-Place Sheet (Place App, Folder, or 3rd-Party / System Widget at exact cell)
        selectedEmptyCell?.let { (cellPage, cellRow, cellCol) ->
            val (cols, rows) = pageGridDimensions[cellPage] ?: (4 to 4)
            val defaultFolderSpan = if (isTabletLayout) 2 else 1
            EmptyGridCellActionSheet(
                page = cellPage,
                row = cellRow,
                col = cellCol,
                apps = allApps,
                folders = folders,
                isTabletLayout = isTabletLayout,
                iconPackStyle = preferences.iconPackStyle,
                iconShape = preferences.iconShape,
                onPlaceAppHere = { app ->
                    onPlaceAppOnHomeGrid(cellPage, app, cellRow, cellCol, cols, rows)
                },
                onPlaceFolderHere = { folder ->
                    onPlaceFolderOnHomeGrid(
                        folder.id,
                        cellPage,
                        cellRow,
                        cellCol,
                        defaultFolderSpan.coerceAtMost(cols),
                        defaultFolderSpan.coerceAtMost(rows),
                        cols,
                        rows
                    )
                },
                onCreateAndPlaceFolderHere = { name ->
                    onCreateAndPlaceFolderOnHomeGrid(
                        name,
                        cellPage,
                        cellRow,
                        cellCol,
                        defaultFolderSpan.coerceAtMost(cols),
                        defaultFolderSpan.coerceAtMost(rows),
                        cols,
                        rows
                    )
                },
                onAddWidgetHere = {
                    onOpenThirdPartyWidgetPicker(cellPage, cellRow, cellCol, cols, rows)
                },
                onOpenHomeMenu = { showHomeScreenMenu = true },
                onDismiss = { selectedEmptyCell = null }
            )
        }

        // Expanded Home Folder Dialog (available in both Phone and Tablet modes)
        activeExpandedHomeFolder?.let { targetFolder ->
            val liveFolder = folders.firstOrNull { it.id == targetFolder.id }
            if (liveFolder == null) {
                activeExpandedHomeFolder = null
            } else {
                val folderApps = remember(liveFolder.packageNames, visibleAppsByPkg) {
                    liveFolder.packageNames.mapNotNull { visibleAppsByPkg[it] }
                }
                val isPinned = placedHomeFolders.any { it.folderId == liveFolder.id }
                FolderExpandedDialog(
                    folder = liveFolder,
                    appsInFolder = folderApps,
                    iconPackStyle = preferences.iconPackStyle,
                    iconSize = preferences.iconSize,
                    iconShape = preferences.iconShape,
                    isTabletLayout = isTabletLayout,
                    tabletFolderSize = preferences.tabletFolderSize,
                    tabletExpandedFolderIconShape = preferences.tabletExpandedFolderIconShape,
                    tabletFolderBackdrop = preferences.tabletFolderBackdrop,
                    isPinnedToHome = isPinned,
                    onChangeTabletFolderSize = onChangeTabletFolderSize,
                    onTogglePinToHome = {
                        if (isPinned) {
                            onRemoveHomeFolder(liveFolder.id)
                            activeExpandedHomeFolder = null
                        }
                    },
                    onAppClick = onAppClick,
                    onRemoveFromFolder = { app -> onRemoveAppFromFolder(liveFolder, app) },
                    onRenameFolder = { newName -> onRenameFolder(liveFolder, newName) },
                    onDeleteFolder = {
                        onDeleteFolder(liveFolder)
                        activeExpandedHomeFolder = null
                    },
                    onDismiss = { activeExpandedHomeFolder = null }
                )
            }
        }

        // Placed Third-Party / System Widget Resize, Move, Configure & Remove Dialog
        activeManageWidget?.let { targetWidget ->
            val liveWidget = placedWidgets.firstOrNull { it.appWidgetId == targetWidget.appWidgetId }
            if (liveWidget == null) {
                activeManageWidget = null
            } else {
                val (cols, rows) = pageGridDimensions[liveWidget.page] ?: (4 to 4)
                val info = SystemAppWidgetHelper.getAppWidgetInfo(context, liveWidget.appWidgetId)
                    ?: SystemAppWidgetHelper.findProviderInfo(
                        context,
                        liveWidget.providerPackage,
                        liveWidget.providerClass
                    )
                WidgetManageDialog(
                    widget = liveWidget,
                    gridCols = cols,
                    gridRows = rows,
                    hasConfigureActivity = info?.configure != null,
                    onMove = { newRow, newCol ->
                        onMoveThirdPartyWidget(liveWidget.appWidgetId, liveWidget.page, newRow, newCol, cols, rows)
                    },
                    onResize = { newSpanX, newSpanY ->
                        onResizeThirdPartyWidget(liveWidget.appWidgetId, newSpanX, newSpanY, cols, rows)
                    },
                    onActivateDragHandles = {
                        resizingWidgetId = liveWidget.appWidgetId
                    },
                    onConfigure = { onConfigureThirdPartyWidget(liveWidget.appWidgetId) },
                    onRemove = { onRemoveThirdPartyWidget(liveWidget.appWidgetId) },
                    onDismiss = { activeManageWidget = null }
                )
            }
        }

        if (showFailsafeDialog && renderFaults.isNotEmpty()) {
            PurpleFailsafeDetailsDialog(
                faults = renderFaults,
                softwareFallbackActive = softwareFallbackActive,
                onClearAll = {
                    wallpaperFault = false
                    RenderFailsafeMonitor.clearActiveFaults(context)
                    showFailsafeDialog = false
                },
                onDismiss = { showFailsafeDialog = false }
            )
        }
    }
    }
}
