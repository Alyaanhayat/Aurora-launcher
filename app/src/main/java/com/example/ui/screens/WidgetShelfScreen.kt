package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.HabitItem
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.TaskItem
import com.example.model.ThemeMode
import com.example.ui.components.PurpleFailsafeCard
import com.example.ui.components.PurpleFailsafeDetailsDialog
import com.example.ui.components.RenderErrorBoundary
import com.example.ui.components.RenderFailsafeMonitor
import com.example.ui.theme.isLauncherDarkTheme
import com.example.ui.widgets.AtAGlanceWidget
import com.example.ui.widgets.HabitTrackerWidget
import com.example.ui.widgets.MediaPlayerWidget
import com.example.ui.widgets.NotesWidget
import com.example.ui.widgets.QuickTogglesWidget
import com.example.ui.widgets.ScreenTimeWidget
import com.example.ui.widgets.TasksWidget

@Composable
fun WidgetShelfScreen(
    preferences: LauncherPreferences,
    notes: List<NoteItem>,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleHabit: (HabitItem) -> Unit,
    onOpenSettings: () -> Unit,
    isTabletSidePane: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isDark = isLauncherDarkTheme(preferences.themeMode)
    val isAmoled = preferences.themeMode == ThemeMode.AMOLED_BLACK
    val renderFaults by RenderFailsafeMonitor.faults.collectAsState()
    val softwareFallbackActive by RenderFailsafeMonitor.softwareFallbackActive.collectAsState()
    val simulatedTestMode by RenderFailsafeMonitor.simulatedTestMode.collectAsState()
    var showFailsafeDialog by remember { mutableStateOf(false) }

    val shelfShape: Shape = if (isTabletSidePane) {
        RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    } else {
        RectangleShape
    }

    val scrimAlpha = when {
        isTabletSidePane && isAmoled -> 0.86f
        isTabletSidePane -> 0.78f
        isDark -> 0.36f
        else -> 0.62f
    }

    val topTint = if (isAmoled) {
        colorScheme.background
    } else {
        lerp(colorScheme.background, colorScheme.primary, 0.05f)
    }
    val bottomTint = colorScheme.background

    val shelfBrush = Brush.verticalGradient(
        colors = listOf(
            topTint.copy(alpha = scrimAlpha),
            bottomTint.copy(alpha = (scrimAlpha + 0.08f).coerceAtMost(0.95f))
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shelfShape)
            .background(shelfBrush)
            .then(
                if (isTabletSidePane) {
                    Modifier.border(
                        width = 1.dp,
                        color = colorScheme.outline.copy(alpha = 0.28f),
                        shape = shelfShape
                    )
                } else {
                    Modifier
                }
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("productivity_widget_shelf"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (isTabletSidePane) 28.dp else 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Shelf",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Productivity Shelf",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shelf_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (simulatedTestMode || softwareFallbackActive) {
                renderFaults.firstOrNull()?.let { activeFault ->
                    item {
                        PurpleFailsafeCard(
                            fault = activeFault,
                            onTapDetails = { showFailsafeDialog = true }
                        )
                    }
                }
            }

            // Widgets
            if ("at_a_glance" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "At-a-Glance Widget") {
                        AtAGlanceWidget()
                    }
                }
            }

            if ("quick_toggles" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "Quick Controls Widget") {
                        QuickTogglesWidget()
                    }
                }
            }

            if ("tasks" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "Focus Tasks Widget") {
                        TasksWidget(
                            tasks = tasks,
                            onToggleTask = onToggleTask,
                            onAddTask = onAddTask,
                            onDeleteTask = onDeleteTask
                        )
                    }
                }
            }

            if ("habits" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "Habit Tracker Widget") {
                        HabitTrackerWidget(
                            habits = habits,
                            onToggleHabit = onToggleHabit
                        )
                    }
                }
            }

            if ("quick_notes" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "Quick Notes Widget") {
                        NotesWidget(
                            notes = notes,
                            onAddNote = onAddNote,
                            onDeleteNote = onDeleteNote
                        )
                    }
                }
            }

            if ("screen_time" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "Digital Wellbeing Widget") {
                        ScreenTimeWidget()
                    }
                }
            }

            if ("media_player" in preferences.enabledWidgets) {
                item {
                    RenderErrorBoundary(componentName = "Media Player Widget") {
                        MediaPlayerWidget()
                    }
                }
            }
        }

        if (showFailsafeDialog && renderFaults.isNotEmpty()) {
            PurpleFailsafeDetailsDialog(
                faults = renderFaults,
                softwareFallbackActive = softwareFallbackActive,
                onClearAll = {
                    RenderFailsafeMonitor.clearActiveFaults(context)
                    showFailsafeDialog = false
                },
                onDismiss = { showFailsafeDialog = false }
            )
        }
    }
}
