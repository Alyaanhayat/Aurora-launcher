package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.model.HabitItem
import com.example.model.IconPackStyle
import com.example.model.IconSize
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.TaskItem
import com.example.model.ThemeMode
import com.example.ui.components.AppIconView
import com.example.ui.components.DockBar
import com.example.ui.components.HomeScreenMenuSheet
import com.example.ui.widgets.AtAGlanceWidget
import com.example.ui.widgets.NotesWidget
import com.example.ui.widgets.TasksWidget
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    preferences: LauncherPreferences,
    allApps: List<AppItem>,
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
    onCycleWallpaper: () -> Unit = {}
) {
    // Total pages: Home pages (0 until homePageCount) + Productivity Shelf on the right!
    val totalPages = (preferences.homePageCount + 1).coerceAtLeast(2)
    val shelfPageIndex = preferences.homePageCount
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()
    var showHomeScreenMenu by remember { mutableStateOf(false) }

    val dockedApps = remember(allApps) {
        val docked = allApps.filter { it.isDocked && !it.isHidden }
        if (docked.isNotEmpty()) docked else allApps.take(5)
    }

    val homeGridApps = remember(allApps) {
        allApps.filter { (it.isFavorite || !it.isDocked) && !it.isHidden }.take(8)
    }

    // Wallpaper background gradient based on settings & dark mode
    val wallpaperBrush = remember(preferences.wallpaperThemeIndex, preferences.themeMode) {
        when {
            preferences.themeMode == ThemeMode.AMOLED_BLACK -> Brush.verticalGradient(
                listOf(Color(0xFF000000), Color(0xFF000000))
            )
            preferences.wallpaperThemeIndex == 0 -> Brush.verticalGradient(
                listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF0B0E14))
            )
            preferences.wallpaperThemeIndex == 1 -> Brush.verticalGradient(
                listOf(Color(0xFF1E102F), Color(0xFF130924), Color(0xFF0A0512))
            )
            preferences.wallpaperThemeIndex == 2 -> Brush.verticalGradient(
                listOf(Color(0xFF0A2218), Color(0xFF081912), Color(0xFF05100B))
            )
            else -> Brush.verticalGradient(
                listOf(Color(0xFF0A0A0A), Color(0xFF141414), Color(0xFF000000))
            )
        }
    }

    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaperBrush)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        // Long press on wallpaper opens options for adding a new page and widget options!
                        showHomeScreenMenu = true
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (totalDragY < -60f) {
                            // Swiped UP -> Open App Drawer
                            onOpenDrawer()
                        } else if (totalDragY > 60f) {
                            // Swiped DOWN -> Open Smart Search
                            onOpenSearch()
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = pagerState.currentPage == i
                        val isShelf = i == shelfPageIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 18.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isShelf) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                .clickable { coroutineScope.launch { pagerState.animateScrollToPage(i) } }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                // Toggle between home and shelf on the right
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
            AnimatedVisibility(visible = !isDefaultLauncher && showDefaultBanner) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == shelfPageIndex) {
                    // Rightmost Page: Productivity Shelf Dashboard!
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
                } else if (page == 0) {
                    // Page 0: Main Home Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
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

                        // Pinned apps grid on Home
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(homeGridApps, key = { it.id }) { app ->
                                AppIconView(
                                    app = app,
                                    iconPackStyle = preferences.iconPackStyle,
                                    iconSize = preferences.iconSize,
                                    showLabel = preferences.showAppLabels,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }

                        // Swipe up hint pill
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Swipe up for apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    // Additional Home Pages (Page 1, 2, etc.)
                    val pageOffset = 8 + (page - 1) * 12
                    val pageApps = allApps.drop(pageOffset).take(12)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Quick Action Card on additional pages
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showHomeScreenMenu = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                                            text = "Long press or tap to manage widgets & pages",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(pageApps, key = { it.id }) { app ->
                                AppIconView(
                                    app = app,
                                    iconPackStyle = preferences.iconPackStyle,
                                    iconSize = preferences.iconSize,
                                    showLabel = preferences.showAppLabels,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }

                        // Swipe up hint pill
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Swipe up for apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                onSearchClick = onOpenSearch
            )
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
