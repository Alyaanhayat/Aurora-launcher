package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HomeScreenWidget
import com.example.model.LauncherPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenMenuSheet(
    preferences: LauncherPreferences,
    currentPageIndex: Int,
    isGridEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddNewPage: () -> Unit,
    onRemoveCurrentPage: () -> Unit,
    onOpenSettings: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onToggleWidget: (String, Boolean) -> Unit,
    onOpenThirdPartyWidgetPicker: () -> Unit = {},
    onToggleGridGuides: (Boolean) -> Unit = {},
    onToggleGridEditMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showWidgetPicker by remember { mutableStateOf(false) }
    val wallpaperLabel = when (preferences.wallpaperThemeIndex) {
        0 -> "Android System"
        1 -> "Cosmic Sky"
        2 -> "Velvet Lavender"
        3 -> "Emerald Mist"
        else -> "Minimal Pure"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("home_screen_long_press_menu")
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Home Screen Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize layout, pages & productivity widgets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Quick Actions Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Action: App & System Widgets
                MenuActionCard(
                    icon = Icons.Default.Add,
                    title = "Add App Widget",
                    subtitle = "3rd-party & Android OS",
                    isHighlight = true,
                    onClick = {
                        onDismiss()
                        onOpenThirdPartyWidgetPicker()
                    },
                    modifier = Modifier.weight(1f),
                    tag = "menu_third_party_widgets_btn"
                )

                // Action: Add Page
                MenuActionCard(
                    icon = Icons.Default.NoteAdd,
                    title = "Add New Page",
                    subtitle = "${preferences.homePageCount} of 5 pages",
                    isHighlight = false,
                    onClick = {
                        onAddNewPage()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    tag = "menu_add_page_btn"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Action: Launcher Settings
                MenuActionCard(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Icon pack & themes",
                    isHighlight = false,
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.weight(1f),
                    tag = "menu_settings_btn"
                )

                // Action: Edit Grid & Widgets
                MenuActionCard(
                    icon = Icons.Default.Edit,
                    title = if (isGridEditMode) "Done Editing" else "Edit Grid",
                    subtitle = if (isGridEditMode) "Exit grid edit mode" else "Resize & move widgets",
                    isHighlight = isGridEditMode,
                    onClick = {
                        onToggleGridEditMode()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    tag = "menu_edit_grid_btn"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Action: Built-in Widget Options
                MenuActionCard(
                    icon = Icons.Default.Widgets,
                    title = "Shelf Widgets",
                    subtitle = if (showWidgetPicker) "Hide options" else "Built-in widgets",
                    isHighlight = showWidgetPicker,
                    onClick = { showWidgetPicker = !showWidgetPicker },
                    modifier = Modifier.weight(1f),
                    tag = "menu_widget_options_btn"
                )

                // Action: Wallpaper Style
                MenuActionCard(
                    icon = Icons.Default.ColorLens,
                    title = "Wallpaper",
                    subtitle = wallpaperLabel,
                    isHighlight = preferences.wallpaperThemeIndex == 0,
                    onClick = {
                        onCycleWallpaper()
                    },
                    modifier = Modifier.weight(1f),
                    tag = "menu_wallpaper_btn"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Show Home Grid Guides",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Always outline cells on the home screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = preferences.showHomeGridGuides,
                    onCheckedChange = onToggleGridGuides,
                    modifier = Modifier.testTag("switch_show_home_grid_guides")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    runCatching {
                        val intent = Intent.createChooser(
                            Intent(Intent.ACTION_SET_WALLPAPER),
                            "Choose Android Wallpaper"
                        )
                        context.startActivity(intent)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("menu_system_wallpaper_picker_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Android System Wallpaper")
            }

            // Remove current page option if extra page exists
            if (preferences.homePageCount > 1 && currentPageIndex > 0 && currentPageIndex < preferences.homePageCount) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onRemoveCurrentPage()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("menu_remove_page_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remove Current Page",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Widget Options Panel (Shown when user taps Widget Options)
            AnimatedVisibility(visible = showWidgetPicker) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Active Productivity Widgets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HomeScreenWidget.entries.forEach { widget ->
                        val isEnabled = widget.id in preferences.enabledWidgets
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = widget.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = widget.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    onToggleWidget(widget.id, checked)
                                },
                                modifier = Modifier.testTag("switch_widget_${widget.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MenuActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isHighlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
