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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.AppItem
import com.example.model.HabitItem
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.TaskItem
import com.example.model.ThemeMode
import com.example.ui.components.AppIconGraphic
import com.example.ui.components.AppIconView
import com.example.ui.components.DockBar
import com.example.ui.components.HomeScreenMenuSheet
import com.example.ui.components.getIconShape
import com.example.ui.theme.isLauncherDarkTheme
import com.example.ui.widgets.AtAGlanceWidget
import com.example.ui.widgets.NotesWidget
import com.example.ui.widgets.TasksWidget
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
    homePageAppsMap: Map<Int, List<AppItem>>,
    notes: List<NoteItem>,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    isDefaultLauncher: Boolean = false,
    onSetDefaultLauncher: () -> Unit = {},
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onOpenDrawer: () -> Unit,
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
    onMoveAppWithinHomePage: (Int, AppItem, Int) -> Unit = { _, _, _ -> },
    onMoveAppWithinDock: (AppItem, Int) -> Unit = { _, _ -> },
    onMoveAppHomeToDock: (Int, AppItem, Int) -> Unit = { _, _, _ -> },
    onMoveAppDockToHome: (AppItem, Int, Int?) -> Unit = { _, _, _ -> },
    onRemoveAppFromHomePage: (Int, AppItem) -> Unit = { _, _ -> },
    onRemoveAppFromDock: (AppItem) -> Unit = {},
    onCurrentHomePageChanged: (Int) -> Unit = {}
) {
    // Total pages: Home pages (0 until homePageCount) + optional Productivity Shelf on the right
    val totalPages = (preferences.homePageCount + if (preferences.enableProductivityShelf) 1 else 0).coerceAtLeast(1)
    val shelfPageIndex = preferences.homePageCount
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()
    var showHomeScreenMenu by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, preferences.homePageCount) {
        if (pagerState.currentPage < preferences.homePageCount) {
            onCurrentHomePageChanged(pagerState.currentPage)
        }
    }

    val isDark = isLauncherDarkTheme(preferences.themeMode)
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current

    // Read Android system wallpaper brightness hints (API 27+, zero permissions required)
    var wallpaperSupportsDarkText by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                runCatching {
                    val colors = WallpaperManager.getInstance(context)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    colors != null && (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                }.getOrDefault(false)
            } else false
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        DisposableEffect(context) {
            val wm = WallpaperManager.getInstance(context)
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0) {
                    wallpaperSupportsDarkText = colors != null &&
                        (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                }
            }
            runCatching {
                wm.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
            }
            onDispose {
                runCatching { wm.removeOnColorsChangedListener(listener) }
            }
        }
    }

    val useSystemWallpaper = preferences.wallpaperThemeIndex == 0

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
    val homeSlotBounds = remember { mutableStateMapOf<Pair<Int, Int>, Rect>() }
    val homeGridBounds = remember { mutableStateMapOf<Int, Rect>() }

    val resetDrag: () -> Unit = {
        dragSession = null
        dragStartPosInRoot = Offset.Zero
        dragPointerInRoot = Offset.Zero
        hasDraggedSignificantly = false
    }

    val startDrag: (AppItem, DragSource, Int, Int, Offset, Rect) -> Unit =
        { app, source, page, index, localOffset, slotBounds ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
    val currentPage = pagerState.currentPage
    val currentHomePageApps = homePageAppsMap[currentPage] ?: emptyList()

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
        currentPage < preferences.homePageCount &&
        homeGridBounds[currentPage]?.inflate(24f)?.contains(dragPointerInRoot) == true

    val hoveredHomeSlotIndex: Int? = if (isOverHomeGrid) {
        val validEntries = currentHomePageApps.indices.mapNotNull { idx ->
            homeSlotBounds[currentPage to idx]?.let { idx to it }
        }
        val exactHit = validEntries.firstOrNull { (_, rect) -> rect.inflate(14f).contains(dragPointerInRoot) }
        if (exactHit != null) {
            exactHit.first
        } else if (validEntries.isNotEmpty()) {
            val lastRect = validEntries.last().second
            if (dragPointerInRoot.y > lastRect.bottom + 12f) {
                currentHomePageApps.size
            } else {
                validEntries.minByOrNull { (_, rect) ->
                    (dragPointerInRoot - rect.center).getDistance()
                }?.first
            }
        } else {
            0
        }
    } else {
        null
    }

    LaunchedEffect(isOverRemoveZone) {
        if (isOverRemoveZone) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val onDragEnd: () -> Unit = {
        val session = dragSession
        if (session != null) {
            if (!hasDraggedSignificantly) {
                resetDrag()
                onAppLongClick(session.app)
            } else if (isOverRemoveZone) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (session.source == DragSource.HOME) {
                    onRemoveAppFromHomePage(session.sourcePage, session.app)
                } else {
                    onRemoveAppFromDock(session.app)
                }
                resetDrag()
            } else if (isOverDock) {
                val targetIdx = hoveredDockSlotIndex ?: activeDockList.size.coerceAtMost(4)
                if (session.source == DragSource.DOCK) {
                    onMoveAppWithinDock(session.app, targetIdx)
                } else {
                    onMoveAppHomeToDock(session.sourcePage, session.app, targetIdx)
                }
                resetDrag()
            } else if (currentPage < preferences.homePageCount) {
                if (session.source == DragSource.HOME && session.sourcePage == currentPage) {
                    val targetIdx = hoveredHomeSlotIndex
                    if (targetIdx != null) {
                        onMoveAppWithinHomePage(currentPage, session.app, targetIdx)
                    }
                } else if (session.source == DragSource.DOCK &&
                    (isOverHomeGrid || dragPointerInRoot.y < (dockBounds.top - 24f))
                ) {
                    onMoveAppDockToHome(session.app, currentPage, hoveredHomeSlotIndex)
                }
                resetDrag()
            } else {
                resetDrag()
            }
        }
    }

    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootOrigin = coords.boundsInRoot().topLeft
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
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDrag = { change, dragAmount ->
                        if (dragSession == null) {
                            change.consume()
                            totalDragY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        if (dragSession == null) {
                            if (totalDragY < -60f) {
                                // Swiped UP -> Open App Drawer
                                onOpenDrawer()
                            } else if (totalDragY > 60f && preferences.enableSmartSearch) {
                                // Swiped DOWN -> Open Smart Search
                                onOpenSearch()
                            }
                        }
                        totalDragY = 0f
                    }
                )
            }
            .testTag("home_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar with Page Indicators & Quick Settings Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Page Indicator Pills (Home pages + Productivity Shelf on the right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.45f else 0.78f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = pagerState.currentPage == i
                        val isShelf = preferences.enableProductivityShelf && i == shelfPageIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 18.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isShelf) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                                .clickable { coroutineScope.launch { pagerState.animateScrollToPage(i) } }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (preferences.enableProductivityShelf) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val target = if (pagerState.currentPage == shelfPageIndex) 0 else shelfPageIndex
                                    pagerState.animateScrollToPage(target)
                                }
                            },
                            modifier = Modifier.size(36.dp).testTag("toggle_shelf_button")
                        ) {
                            Icon(
                                imageVector = if (pagerState.currentPage == shelfPageIndex) Icons.Default.Home else Icons.Default.Dashboard,
                                contentDescription = "Switch Page",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { showHomeScreenMenu = true },
                        modifier = Modifier.size(36.dp).testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
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

            // Horizontal Pager: Home Pages on Left/Center, Productivity Shelf on Far Right!
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = dragSession == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (preferences.enableProductivityShelf && page == shelfPageIndex) {
                    // Rightmost Page: Productivity Shelf Dashboard!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (useSystemWallpaper) {
                                    Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f))
                                } else Modifier
                            )
                    ) {
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
                    }
                } else {
                    val pageApps = homePageAppsMap[page] ?: emptyList()

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
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // Quick focus task or scratchpad preview
                                if ("tasks" in preferences.enabledWidgets && tasks.isNotEmpty()) {
                                    TasksWidget(
                                        tasks = tasks.take(2),
                                        onToggleTask = onToggleTask,
                                        onAddTask = onAddTask,
                                        onDeleteTask = onDeleteTask
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                } else if ("quick_notes" in preferences.enabledWidgets && notes.isNotEmpty()) {
                                    NotesWidget(
                                        notes = notes.take(1),
                                        onAddNote = onAddNote,
                                        onDeleteNote = onDeleteNote
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        } else {
                            // Additional Home Pages (Page 1, 2, etc.) header card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showHomeScreenMenu = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.45f else 0.80f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Home Page ${page + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Long press to manage widgets & pages, or drag apps here",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Pinned/Draggable apps grid on Home
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    homeGridBounds[page] = coords.boundsInRoot()
                                },
                            contentAlignment = if (page == 0) Alignment.BottomCenter else Alignment.TopCenter
                        ) {
                            if (pageApps.isEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 24.dp)
                                ) {
                                    Text(
                                        text = if (dragSession != null) "Drop app here" else "Long press an app in the Drawer to add it here",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(pageApps, key = { _, app -> app.id }) { index, app ->
                                        val slotBoundsState = remember(page, index, app.packageName) {
                                            mutableStateOf(Rect.Zero)
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.onGloballyPositioned { coords ->
                                                val b = coords.boundsInRoot()
                                                slotBoundsState.value = b
                                                homeSlotBounds[page to index] = b
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
                                                    hoveredHomeSlotIndex == index &&
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
                                                        index,
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
                                .clickable { onOpenDrawer() }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Swipe up for all apps",
                                tint = hintColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Swipe up for apps",
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

        // Floating Dragged Icon Overlay
        dragSession?.let { session ->
            val iconSizeDp = preferences.iconSize.dpValue.dp
            val iconSizePx = with(density) { iconSizeDp.toPx() }
            val liftPx = with(density) { 28.dp.toPx() }
            val offsetX = (dragPointerInRoot.x - rootOrigin.x - iconSizePx / 2f).roundToInt()
            val offsetY = (dragPointerInRoot.y - rootOrigin.y - iconSizePx / 2f - liftPx).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .graphicsLayer {
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
            HomeScreenMenuSheet(
                preferences = preferences,
                currentPageIndex = pagerState.currentPage,
                onDismiss = { showHomeScreenMenu = false },
                onAddNewPage = {
                    onAddNewPage()
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(preferences.homePageCount)
                    }
                },
                onRemoveCurrentPage = {
                    onRemoveCurrentPage()
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onOpenSettings = onOpenSettings,
                onCycleWallpaper = onCycleWallpaper,
                onToggleWidget = onToggleWidget
            )
        }
    }
}
