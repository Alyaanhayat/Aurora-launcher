package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppCategory
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.DrawerLayoutMode
import com.example.model.IconPackStyle
import com.example.model.IconSize
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
    showAppLabels: Boolean,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onSearchClick: () -> Unit,
    onOpenAppVault: () -> Unit,
    onLayoutChange: (DrawerLayoutMode) -> Unit,
    onRemoveAppFromFolder: (AppFolder, AppItem) -> Unit = { _, _ -> },
    onRenameFolder: (AppFolder, String) -> Unit = { _, _ -> },
    onDeleteFolder: (AppFolder) -> Unit = {},
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val coroutineScope = rememberCoroutineScope()
    val visibleApps = remember(apps) { apps.filter { !it.isHidden } }

    val availableLetters = remember(visibleApps) {
        visibleApps.map { it.displayLabel.firstOrNull()?.uppercaseChar() ?: '#' }
            .distinct()
            .sorted()
    }

    var selectedTabCategory by remember { mutableStateOf(AppCategory.ALL) }
    var activeExpandedFolder by remember { mutableStateOf<AppFolder?>(null) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .draggable(
                state = rememberDraggableState { delta ->
                    dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                    if (dragOffsetY > 180f) {
                        onDismiss()
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (dragOffsetY > 120f) {
                        onDismiss()
                    }
                    dragOffsetY = 0f
                }
            )
            .testTag("app_drawer_sheet")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search ${visibleApps.size} apps...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(
                        text = "App Folders",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(folders, key = { it.id }) { folder ->
                            val appsInFolder = apps.filter { it.packageName in folder.packageNames }
                            AppFolderView(
                                folder = folder,
                                appsInFolder = appsInFolder,
                                iconPackStyle = iconPackStyle,
                                iconSize = IconSize.COMPACT,
                                showLabel = showAppLabels,
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
                        val cols = if (drawerLayoutMode == DrawerLayoutMode.GRID_5) 5 else 4
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            state = gridState,
                            contentPadding = PaddingValues(start = 12.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibleApps, key = { it.id }) { app ->
                                AppIconView(
                                    app = app,
                                    iconPackStyle = iconPackStyle,
                                    iconSize = if (cols == 5) IconSize.COMPACT else iconSize,
                                    showLabel = showAppLabels,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }
                    }

                    DrawerLayoutMode.ALPHABETICAL_LIST -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 36.dp, top = 8.dp, bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val grouped = visibleApps.groupBy { it.displayLabel.firstOrNull()?.uppercaseChar() ?: '#' }
                            grouped.forEach { (letter, appsInGroup) ->
                                item(key = "header_$letter") {
                                    Text(
                                        text = letter.toString(),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(appsInGroup, key = { it.id }) { app ->
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
                                            shape = getIconShape(iconPackStyle),
                                            context = LocalContext.current
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
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredByTab, key = { it.id }) { app ->
                                AppIconView(
                                    app = app,
                                    iconPackStyle = iconPackStyle,
                                    iconSize = iconSize,
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
                            items(visibleApps, key = { it.id }) { app ->
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

        // Folder Expanded Dialog if user taps an app folder
        activeExpandedFolder?.let { folder ->
            val appsInFolder = apps.filter { it.packageName in folder.packageNames }
            FolderExpandedDialog(
                folder = folder,
                appsInFolder = appsInFolder,
                iconPackStyle = iconPackStyle,
                iconSize = iconSize,
                onAppClick = onAppClick,
                onRemoveFromFolder = { app ->
                    onRemoveAppFromFolder(folder, app)
                },
                onRenameFolder = { newName ->
                    onRenameFolder(folder, newName)
                },
                onDeleteFolder = {
                    onDeleteFolder(folder)
                    activeExpandedFolder = null
                },
                onDismiss = { activeExpandedFolder = null }
            )
        }
    }
}
