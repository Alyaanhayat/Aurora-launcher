package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppCategory
import com.example.model.AppDrawerSide
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.DrawerLayoutMode
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.IconSize
import com.example.model.TabletFolderBackdrop
import com.example.model.TabletFolderSize
import com.example.ui.components.AlphabetScrubber
import com.example.ui.components.AppFolderView
import com.example.ui.components.AppIconGraphic
import com.example.ui.components.AppIconView
import com.example.ui.components.FolderExpandedDialog
import com.example.ui.components.getIconShape
import kotlinx.coroutines.launch

@Composable
fun AppDrawerSheet(
    apps: List<AppItem>,
    folders: List<AppFolder> = emptyList(),
    hiddenAppsCount: Int = 0,
    drawerLayoutMode: DrawerLayoutMode,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    iconShape: IconShape = IconShape.SQUIRCLE,
    showAppLabels: Boolean,
    drawerSide: AppDrawerSide = AppDrawerSide.FULL,
    isTabletLayout: Boolean = false,
    tabletFolderSize: TabletFolderSize = TabletFolderSize.LARGE,
    tabletExpandedFolderIconShape: IconShape? = null,
    tabletFolderBackdrop: TabletFolderBackdrop = TabletFolderBackdrop.TONAL_SURFACE,
    placedHomeFolderIds: Set<String> = emptySet(),
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onSearchClick: () -> Unit,
    onOpenAppVault: () -> Unit,
    onLayoutChange: (DrawerLayoutMode) -> Unit,
    onChangeTabletFolderSize: (TabletFolderSize) -> Unit = {},
    onTogglePinFolderToHome: (AppFolder) -> Unit = {},
    onRemoveAppFromFolder: (AppFolder, AppItem) -> Unit = { _, _ -> },
    onRenameFolder: (AppFolder, String) -> Unit = { _, _ -> },
    onDeleteFolder: (AppFolder) -> Unit = {},
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val coroutineScope = rememberCoroutineScope()
    val visibleApps = remember(apps) { apps.filter { !it.isHidden } }
    val visibleAppsByPkg = remember(visibleApps) { visibleApps.associateBy { it.packageName } }
    val allAppsByPkg = remember(apps) { apps.associateBy { it.packageName } }

    val availableLetters = remember(visibleApps) {
        visibleApps.map { it.displayLabel.firstOrNull()?.uppercaseChar() ?: '#' }
            .distinct()
            .sorted()
    }

    var selectedTabCategory by remember { mutableStateOf(AppCategory.ALL) }
    var activeExpandedFolder by remember { mutableStateOf<AppFolder?>(null) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    val dragOffsetY = remember { Animatable(0f) }
    val dragOffsetX = remember { Animatable(0f) }
    val effectiveDrawerSide = if (isTabletLayout) drawerSide else AppDrawerSide.FULL
    val isSideDrawer = effectiveDrawerSide != AppDrawerSide.FULL

    // Smooth slide-in when switching between LEFT / RIGHT / FULL while already open in tablet mode
    var previousSide by remember { mutableStateOf(effectiveDrawerSide) }
    val sideSwitchOffsetFraction = remember { Animatable(0f) }
    LaunchedEffect(effectiveDrawerSide) {
        if (previousSide != effectiveDrawerSide) {
            previousSide = effectiveDrawerSide
            val startFraction = when (effectiveDrawerSide) {
                AppDrawerSide.LEFT -> -1f
                AppDrawerSide.RIGHT -> 1f
                AppDrawerSide.FULL -> 0f
            }
            if (startFraction != 0f) {
                sideSwitchOffsetFraction.snapTo(startFraction)
                sideSwitchOffsetFraction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f)
                )
            } else {
                sideSwitchOffsetFraction.snapTo(0f)
            }
        }
    }

    val drawerEnterTransition = remember(isTabletLayout, effectiveDrawerSide) {
        if (isTabletLayout) {
            when (effectiveDrawerSide) {
                AppDrawerSide.LEFT -> slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f)
                ) + fadeIn(animationSpec = tween(durationMillis = 160))
                AppDrawerSide.RIGHT -> slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f)
                ) + fadeIn(animationSpec = tween(durationMillis = 160))
                AppDrawerSide.FULL -> slideInVertically(
                    initialOffsetY = { (it * 0.35f).toInt() },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f)
                ) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 460f)
                ) + fadeIn(animationSpec = tween(durationMillis = 160))
            }
        } else {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f)
            ) + fadeIn(animationSpec = tween(durationMillis = 160))
        }
    }

    val drawerExitTransition = remember(isTabletLayout, effectiveDrawerSide) {
        if (isTabletLayout) {
            when (effectiveDrawerSide) {
                AppDrawerSide.LEFT -> slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f)
                ) + fadeOut(animationSpec = tween(durationMillis = 140))
                AppDrawerSide.RIGHT -> slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f)
                ) + fadeOut(animationSpec = tween(durationMillis = 140))
                AppDrawerSide.FULL -> slideOutVertically(
                    targetOffsetY = { (it * 0.35f).toInt() },
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f)
                ) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(durationMillis = 140)
                ) + fadeOut(animationSpec = tween(durationMillis = 140))
            }
        } else {
            slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 520f)
            ) + fadeOut(animationSpec = tween(durationMillis = 140))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val sideFraction = if (totalWidth >= 600.dp) 0.48f else 0.54f
        val effectiveDrawerWidth = if (isSideDrawer) totalWidth * sideFraction else totalWidth
        val sideShape = when (effectiveDrawerSide) {
            AppDrawerSide.LEFT -> RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            AppDrawerSide.RIGHT -> RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            AppDrawerSide.FULL -> RectangleShape
        }
        val sheetAlignment = when (effectiveDrawerSide) {
            AppDrawerSide.LEFT -> Alignment.CenterStart
            AppDrawerSide.RIGHT -> Alignment.CenterEnd
            AppDrawerSide.FULL -> Alignment.Center
        }

        val animatedModifier = if (animatedVisibilityScope != null) {
            with(animatedVisibilityScope) {
                Modifier.animateEnterExit(
                    enter = drawerEnterTransition,
                    exit = drawerExitTransition
                )
            }
        } else {
            Modifier
        }

        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = if (isSideDrawer) 0.96f else 1f),
            shape = sideShape,
            shadowElevation = if (isSideDrawer) 16.dp else 0.dp,
            tonalElevation = if (isSideDrawer) 8.dp else 0.dp,
            modifier = Modifier
                .align(sheetAlignment)
                .then(animatedModifier)
                .then(
                    if (isSideDrawer) {
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(sideFraction)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = sideShape
                            )
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .graphicsLayer {
                    translationX = (sideSwitchOffsetFraction.value * size.width) + dragOffsetX.value
                    translationY = dragOffsetY.value
                }
                .draggable(
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            dragOffsetY.snapTo((dragOffsetY.value + delta).coerceAtLeast(0f))
                            if (dragOffsetY.value > 220f) {
                                onDismiss()
                            }
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (dragOffsetY.value > 130f) {
                            onDismiss()
                        } else {
                            dragOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)
                            )
                        }
                    }
                )
                .then(
                    if (isSideDrawer) {
                        Modifier.draggable(
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    val next = when (effectiveDrawerSide) {
                                        AppDrawerSide.LEFT -> (dragOffsetX.value + delta).coerceAtMost(0f)
                                        AppDrawerSide.RIGHT -> (dragOffsetX.value + delta).coerceAtLeast(0f)
                                        AppDrawerSide.FULL -> 0f
                                    }
                                    dragOffsetX.snapTo(next)
                                    if (kotlin.math.abs(next) > 220f) {
                                        onDismiss()
                                    }
                                }
                            },
                            orientation = Orientation.Horizontal,
                            onDragStopped = {
                                if (kotlin.math.abs(dragOffsetX.value) > 120f) {
                                    onDismiss()
                                } else {
                                    dragOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)
                                    )
                                }
                            }
                        )
                    } else {
                        Modifier
                    }
                )
                .testTag("app_drawer_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
            // Drag handle pill at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }

            // Search Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Search Trigger Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onSearchClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("drawer_search_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isSideDrawer) 8.dp else 12.dp))
                    Text(
                        text = if (isSideDrawer) "Search..." else "Search ${visibleApps.size} apps...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Layout Switcher Menu Button
                IconButton(
                    onClick = {
                        val nextMode = when (drawerLayoutMode) {
                            DrawerLayoutMode.GRID_4 -> DrawerLayoutMode.GRID_5
                            DrawerLayoutMode.GRID_5 -> DrawerLayoutMode.ALPHABETICAL_LIST
                            DrawerLayoutMode.ALPHABETICAL_LIST -> DrawerLayoutMode.CATEGORIZED_TABS
                            DrawerLayoutMode.CATEGORIZED_TABS -> DrawerLayoutMode.MINIMALIST_TEXT
                            DrawerLayoutMode.MINIMALIST_TEXT -> DrawerLayoutMode.GRID_4
                        }
                        onLayoutChange(nextMode)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .testTag("drawer_layout_switch_button")
                ) {
                    val icon = when (drawerLayoutMode) {
                        DrawerLayoutMode.GRID_4 -> Icons.Default.GridView
                        DrawerLayoutMode.GRID_5 -> Icons.Default.GridView
                        DrawerLayoutMode.ALPHABETICAL_LIST -> Icons.Default.List
                        DrawerLayoutMode.CATEGORIZED_TABS -> Icons.Default.Tab
                        DrawerLayoutMode.MINIMALIST_TEXT -> Icons.Default.TextFields
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Change Layout",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .testTag("drawer_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Drawer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Categorized tabs if in tabbed mode
            if (drawerLayoutMode == DrawerLayoutMode.CATEGORIZED_TABS) {
                ScrollableTabRow(
                    selectedTabIndex = AppCategory.entries.indexOf(selectedTabCategory),
                    edgePadding = 16.dp,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    divider = {}
                ) {
                    AppCategory.entries.forEach { category ->
                        val isSelected = selectedTabCategory == category
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabCategory = category },
                            text = {
                                Text(
                                    text = category.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            // App Folders Row (if any folders exist)
            if (folders.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "App Folders",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isTabletLayout) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TabletFolderSize.entries.forEach { sizeOption ->
                                    val selected = tabletFolderSize == sizeOption
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            )
                                            .clickable { onChangeTabletFolderSize(sizeOption) }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("tablet_folder_size_${sizeOption.name}")
                                    ) {
                                        Text(
                                            text = sizeOption.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(if (isTabletLayout) 16.dp else 12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(folders, key = { it.id }, contentType = { "folder" }) { folder ->
                            val appsInFolder = remember(folder.packageNames, allAppsByPkg) {
                                folder.packageNames.mapNotNull { allAppsByPkg[it] }
                            }
                            AppFolderView(
                                folder = folder,
                                appsInFolder = appsInFolder,
                                iconPackStyle = iconPackStyle,
                                iconSize = iconSize,
                                iconShape = iconShape,
                                folderTileSizeDp = if (isTabletLayout) tabletFolderSize.tileSizeDp.dp else IconSize.COMPACT.dpValue.dp,
                                previewColumns = if (isTabletLayout) tabletFolderSize.previewColumns else 2,
                                showLabel = showAppLabels,
                                showAppCountBadge = isTabletLayout && tabletFolderSize != TabletFolderSize.STANDARD,
                                onLongClick = { activeExpandedFolder = folder },
                                onClick = { activeExpandedFolder = folder }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area with Scrubber
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (drawerLayoutMode) {
                    DrawerLayoutMode.GRID_4, DrawerLayoutMode.GRID_5 -> {
                        val cols = when {
                            isSideDrawer -> when {
                                effectiveDrawerWidth >= 340.dp -> if (drawerLayoutMode == DrawerLayoutMode.GRID_5) 4 else 3
                                effectiveDrawerWidth >= 230.dp -> 3
                                else -> 2
                            }
                            isTabletLayout && totalWidth >= 600.dp -> if (drawerLayoutMode == DrawerLayoutMode.GRID_5) 7 else 6
                            isTabletLayout -> if (drawerLayoutMode == DrawerLayoutMode.GRID_5) 6 else 5
                            else -> if (drawerLayoutMode == DrawerLayoutMode.GRID_5) 5 else 4
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            state = gridState,
                            contentPadding = PaddingValues(start = 12.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibleApps, key = { it.id }, contentType = { "app_icon" }) { app ->
                                AppIconView(
                                    app = app,
                                    iconPackStyle = iconPackStyle,
                                    iconSize = if (cols >= 5 || isSideDrawer) IconSize.COMPACT else iconSize,
                                    iconShape = iconShape,
                                    showLabel = showAppLabels,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }
                    }

                    DrawerLayoutMode.ALPHABETICAL_LIST -> {
                        val grouped = remember(visibleApps) {
                            visibleApps.groupBy { it.displayLabel.firstOrNull()?.uppercaseChar() ?: '#' }
                        }
                        val rowContext = LocalContext.current
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 36.dp, top = 8.dp, bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            grouped.forEach { (letter, appsInGroup) ->
                                item(key = "header_$letter", contentType = "header") {
                                    Text(
                                        text = letter.toString(),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(appsInGroup, key = { it.id }, contentType = { "app_row" }) { app ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onAppClick(app) }
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    ) {
                                        AppIconGraphic(
                                            app = app,
                                            style = iconPackStyle,
                                            size = 40.dp,
                                            shape = getIconShape(iconPackStyle, iconShape),
                                            context = rowContext
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = app.displayLabel,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = app.category.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DrawerLayoutMode.CATEGORIZED_TABS -> {
                        val filteredByTab = remember(selectedTabCategory, visibleApps) {
                            if (selectedTabCategory == AppCategory.ALL) visibleApps
                            else visibleApps.filter { it.category == selectedTabCategory }
                        }
                        val tabCols = when {
                            isSideDrawer -> if (effectiveDrawerWidth >= 320.dp) 3 else 2
                            isTabletLayout && totalWidth >= 600.dp -> 6
                            isTabletLayout -> 5
                            else -> 4
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(tabCols),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredByTab, key = { it.id }, contentType = { "app_icon" }) { app ->
                                AppIconView(
                                    app = app,
                                    iconPackStyle = iconPackStyle,
                                    iconSize = if (isSideDrawer) IconSize.COMPACT else iconSize,
                                    iconShape = iconShape,
                                    showLabel = showAppLabels,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }
                    }

                    DrawerLayoutMode.MINIMALIST_TEXT -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 24.dp, end = 36.dp, top = 12.dp, bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibleApps, key = { it.id }, contentType = { "minimal_row" }) { app ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAppClick(app) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            letterSpacing = 0.5.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Vertical Alphabet Scrubber on right side
                if (drawerLayoutMode in listOf(DrawerLayoutMode.GRID_4, DrawerLayoutMode.GRID_5, DrawerLayoutMode.ALPHABETICAL_LIST, DrawerLayoutMode.MINIMALIST_TEXT)) {
                    AlphabetScrubber(
                        availableLetters = availableLetters,
                        activeLetter = null,
                        onLetterSelected = { letter ->
                            val index = visibleApps.indexOfFirst {
                                (it.displayLabel.firstOrNull()?.uppercaseChar() ?: '#') >= letter
                            }
                            if (index >= 0) {
                                coroutineScope.launch {
                                    if (drawerLayoutMode in listOf(DrawerLayoutMode.GRID_4, DrawerLayoutMode.GRID_5)) {
                                        gridState.animateScrollToItem(index)
                                    } else {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            // App Vault Drawer Button at bottom of App Drawer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenAppVault() }
                    .testTag("drawer_app_vault_button"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hidden apps • Password protected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hiddenAppsCount > 0) "$hiddenAppsCount hidden" else "Locked",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
        }

        // Folder Expanded Dialog if user taps an app folder (available in both Phone and Tablet modes)
        activeExpandedFolder?.let { targetFolder ->
            val liveFolder = folders.firstOrNull { it.id == targetFolder.id }
            if (liveFolder == null) {
                activeExpandedFolder = null
            } else {
                val appsInFolder = remember(liveFolder.packageNames, visibleAppsByPkg) {
                    liveFolder.packageNames.mapNotNull { visibleAppsByPkg[it] }
                }
                FolderExpandedDialog(
                    folder = liveFolder,
                    appsInFolder = appsInFolder,
                    iconPackStyle = iconPackStyle,
                    iconSize = iconSize,
                    iconShape = iconShape,
                    isTabletLayout = isTabletLayout,
                    tabletFolderSize = tabletFolderSize,
                    tabletExpandedFolderIconShape = tabletExpandedFolderIconShape,
                    tabletFolderBackdrop = tabletFolderBackdrop,
                    isPinnedToHome = liveFolder.id in placedHomeFolderIds,
                    onChangeTabletFolderSize = onChangeTabletFolderSize,
                    onTogglePinToHome = { onTogglePinFolderToHome(liveFolder) },
                    onAppClick = onAppClick,
                    onRemoveFromFolder = { app ->
                        onRemoveAppFromFolder(liveFolder, app)
                    },
                    onRenameFolder = { newName ->
                        onRenameFolder(liveFolder, newName)
                    },
                    onDeleteFolder = {
                        onDeleteFolder(liveFolder)
                        activeExpandedFolder = null
                    },
                    onDismiss = { activeExpandedFolder = null }
                )
            }
        }
    }
}
