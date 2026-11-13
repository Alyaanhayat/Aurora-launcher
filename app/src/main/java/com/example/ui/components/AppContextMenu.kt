package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenu(
    app: AppItem,
    iconPackStyle: IconPackStyle,
    iconShape: IconShape = IconShape.SQUIRCLE,
    folders: List<AppFolder> = emptyList(),
    isOnHomeScreen: Boolean = false,
    onDismiss: () -> Unit,
    onToggleHomeScreen: () -> Unit = {},
    onToggleDock: (Boolean) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onRename: (String?) -> Unit,
    onHide: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onAddToFolder: (AppFolder, AppItem) -> Unit = { _, _ -> },
    onCreateFolderWithApp: (String, AppItem) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(app.displayLabel) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header with App Icon and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIconGraphic(
                    app = app,
                    style = iconPackStyle,
                    size = 48.dp,
                    shape = getIconShape(iconPackStyle, iconShape),
                    context = LocalContext.current
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Action: Add/Remove from Home Screen
            ContextMenuActionRow(
                icon = Icons.Default.Home,
                title = if (isOnHomeScreen) "Remove from Home Screen" else "Add to Home Screen",
                subtitle = if (isOnHomeScreen) "Remove shortcut from your home grid" else "Place app shortcut on your home screen",
                onClick = {
                    onToggleHomeScreen()
                    onDismiss()
                }
            )

            // Action: Add to Folder
            ContextMenuActionRow(
                icon = Icons.Default.Folder,
                title = "Add to Folder",
                subtitle = "Organize into an app group or folder",
                onClick = {
                    showFolderDialog = true
                }
            )

            // Action: Dock
            ContextMenuActionRow(
                icon = if (app.isDocked) Icons.Default.PushPin else Icons.Outlined.PushPin,
                title = if (app.isDocked) "Remove from Dock" else "Pin to Dock",
                subtitle = "Keep in bottom favorite dock bar",
                onClick = {
                    onToggleDock(!app.isDocked)
                    onDismiss()
                }
            )

            // Action: Favorite
            ContextMenuActionRow(
                icon = if (app.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                title = if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                subtitle = "Prioritize on home screen and suggestions",
                onClick = {
                    onToggleFavorite(!app.isFavorite)
                    onDismiss()
                }
            )

            // Action: Rename / Custom Label
            ContextMenuActionRow(
                icon = Icons.Default.Edit,
                title = "Edit Label",
                subtitle = "Customize how this app is named",
                onClick = {
                    showRenameDialog = true
                }
            )

            // Action: Hide
            ContextMenuActionRow(
                icon = Icons.Default.VisibilityOff,
                title = "Hide App",
                subtitle = "Conceal from main drawer into password-protected App Vault",
                onClick = {
                    onHide()
                    onDismiss()
                }
            )

            // Action: App Info
            ContextMenuActionRow(
                icon = Icons.Default.Info,
                title = "App Info",
                subtitle = "Permissions, storage, and notifications",
                onClick = {
                    onOpenAppInfo()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit App Label") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_app_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRename(renameText.trim())
                        showRenameDialog = false
                        onDismiss()
                    },
                    modifier = Modifier.testTag("save_app_name_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onRename(null)
                        showRenameDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Reset to Default")
                }
            }
        )
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showFolderDialog = false
                isCreatingFolder = false
            },
            title = { Text(if (isCreatingFolder) "Create New Folder" else "Choose Folder") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isCreatingFolder) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Folder Name") },
                            placeholder = { Text("e.g. Work, Social, Games") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_folder_name_input")
                        )
                    } else {
                        if (folders.isEmpty()) {
                            Text(
                                text = "You haven't created any folders yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Add ${app.displayLabel} to:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            folders.forEach { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onAddToFolder(folder, app)
                                            showFolderDialog = false
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isCreatingFolder) {
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onCreateFolderWithApp(newFolderName.trim(), app)
                                showFolderDialog = false
                                isCreatingFolder = false
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_folder_btn")
                    ) {
                        Text("Create")
                    }
                } else {
                    Button(onClick = { isCreatingFolder = true }) {
                        Text("+ New Folder")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFolderDialog = false
                    isCreatingFolder = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContextMenuActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
