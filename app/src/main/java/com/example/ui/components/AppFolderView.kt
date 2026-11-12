package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconSize

@Composable
fun AppFolderView(
    folder: AppFolder,
    appsInFolder: List<AppItem>,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    val sizeDp = iconSize.dpValue.dp
    val shape = RoundedCornerShape(24)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(sizeDp + 24.dp)
            .clickable(onClick = onClick)
            .testTag("app_folder_${folder.id}")
    ) {
        // Folder Icon Box (Showing mini 2x2 preview of apps inside)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(sizeDp)
                .shadow(elevation = 3.dp, shape = shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape)
                .padding(6.dp)
        ) {
            if (appsInFolder.isEmpty()) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = folder.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(sizeDp * 0.5f)
                )
            } else {
                // 2x2 miniature icon grid
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.size(sizeDp * 0.78f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        appsInFolder.take(2).forEach { app ->
                            MiniIconGlyph(app = app, size = (sizeDp * 0.32f))
                        }
                    }
                    if (appsInFolder.size > 2) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            appsInFolder.drop(2).take(2).forEach { app ->
                                MiniIconGlyph(app = app, size = (sizeDp * 0.32f))
                            }
                        }
                    }
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MiniIconGlyph(app: AppItem, size: androidx.compose.ui.unit.Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(app.iconColor).copy(alpha = 0.9f))
    ) {
        Icon(
            imageVector = getGlyphForName(app.iconGlyphName),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderExpandedDialog(
    folder: AppFolder,
    appsInFolder: List<AppItem>,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    onAppClick: (AppItem) -> Unit,
    onRemoveFromFolder: (AppItem) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var folderNameText by remember { mutableStateOf(folder.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("expanded_folder_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Folder Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = folderNameText,
                            onValueChange = { folderNameText = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = {
                            if (folderNameText.isNotBlank()) {
                                onRenameFolder(folderNameText.trim())
                            }
                            isEditingName = false
                        }) {
                            Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDeleteFolder) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Folder",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (appsInFolder.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Text(
                            text = "This folder is empty.\nLong press any app in your drawer and select 'Add to Folder'.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(appsInFolder, key = { it.id }) { app ->
                            AppIconView(
                                app = app,
                                iconPackStyle = iconPackStyle,
                                iconSize = iconSize,
                                showLabel = true,
                                onClick = {
                                    onAppClick(app)
                                    onDismiss()
                                },
                                onLongClick = {
                                    onRemoveFromFolder(app)
                                }
                            )
                        }
                    }
                    Text(
                        text = "Tip: Long press any app inside to remove it from this folder",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
