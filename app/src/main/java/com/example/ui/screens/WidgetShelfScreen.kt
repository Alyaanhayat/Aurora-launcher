package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.HabitItem
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.TaskItem
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("productivity_widget_shelf"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Shelf",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Productivity Shelf",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("shelf_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Widgets
        if ("at_a_glance" in preferences.enabledWidgets) {
            item {
                AtAGlanceWidget()
            }
        }

        if ("quick_toggles" in preferences.enabledWidgets) {
            item {
                QuickTogglesWidget()
            }
        }

        if ("tasks" in preferences.enabledWidgets) {
            item {
                TasksWidget(
                    tasks = tasks,
                    onToggleTask = onToggleTask,
                    onAddTask = onAddTask,
                    onDeleteTask = onDeleteTask
                )
            }
        }

        if ("habits" in preferences.enabledWidgets) {
            item {
                HabitTrackerWidget(
                    habits = habits,
                    onToggleHabit = onToggleHabit
                )
            }
        }

        if ("quick_notes" in preferences.enabledWidgets) {
            item {
                NotesWidget(
                    notes = notes,
                    onAddNote = onAddNote,
                    onDeleteNote = onDeleteNote
                )
            }
        }

        if ("screen_time" in preferences.enabledWidgets) {
            item {
                ScreenTimeWidget()
            }
        }

        if ("media_player" in preferences.enabledWidgets) {
            item {
                MediaPlayerWidget()
            }
        }
    }
}
