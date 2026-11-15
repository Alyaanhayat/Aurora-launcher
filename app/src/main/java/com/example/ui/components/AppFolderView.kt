package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.IconSize
import com.example.model.TabletFolderBackdrop
import com.example.model.TabletFolderSize

@Immutable
data class TabletFolderBackdropStyle(
    val backgroundBrush: Brush,
    val borderColor: Color,
    val borderWidth: Dp,
    val contentColor: Color,
    val secondaryContentColor: Color,
    val accentColor: Color,
    val chipFillColor: Color
)

fun resolveTabletFolderBackdropStyle(
    backdrop: TabletFolderBackdrop,
    colorScheme: ColorScheme
): TabletFolderBackdropStyle {
    return when (backdrop) {
        TabletFolderBackdrop.TONAL_SURFACE -> TabletFolderBackdropStyle(
            backgroundBrush = Brush.verticalGradient(
                listOf(
                    colorScheme.surfaceVariant.copy(alpha = 0.90f),
                    colorScheme.surface.copy(alpha = 0.96f)
                )
            ),
            borderColor = colorScheme.outline.copy(alpha = 0.26f),
            borderWidth = 1.dp,
            contentColor = colorScheme.onSurface,
            secondaryContentColor = colorScheme.onSurfaceVariant,
            accentColor = colorScheme.primary,
            chipFillColor = colorScheme.primary.copy(alpha = 0.14f)
        )

        TabletFolderBackdrop.FROSTED_GLASS -> TabletFolderBackdropStyle(
            backgroundBrush = Brush.verticalGradient(
                listOf(
                    colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    colorScheme.primaryContainer.copy(alpha = 0.44f)
                )
            ),
            borderColor = colorScheme.primary.copy(alpha = 0.36f),
            borderWidth = 1.25.dp,
            contentColor = colorScheme.onSurface,
            secondaryContentColor = colorScheme.onSurface.copy(alpha = 0.78f),
            accentColor = colorScheme.primary,
            chipFillColor = colorScheme.surface.copy(alpha = 0.56f)
        )

        TabletFolderBackdrop.MONET_GRADIENT -> TabletFolderBackdropStyle(
            backgroundBrush = Brush.linearGradient(
                listOf(
                    colorScheme.primaryContainer.copy(alpha = 0.94f),
                    colorScheme.secondaryContainer.copy(alpha = 0.90f),
                    colorScheme.tertiaryContainer.copy(alpha = 0.92f)
                )
            ),
            borderColor = colorScheme.primary.copy(alpha = 0.42f),
            borderWidth = 1.5.dp,
            contentColor = colorScheme.onPrimaryContainer,
            secondaryContentColor = colorScheme.onPrimaryContainer.copy(alpha = 0.80f),
            accentColor = colorScheme.primary,
            chipFillColor = colorScheme.surface.copy(alpha = 0.45f)
        )

        TabletFolderBackdrop.VIBRANT_MONET -> TabletFolderBackdropStyle(
            backgroundBrush = Brush.verticalGradient(
                listOf(
                    colorScheme.primaryContainer,
                    colorScheme.primaryContainer.copy(alpha = 0.88f)
                )
            ),
            borderColor = colorScheme.primary.copy(alpha = 0.58f),
            borderWidth = 1.5.dp,
            contentColor = colorScheme.onPrimaryContainer,
            secondaryContentColor = colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            accentColor = colorScheme.onPrimaryContainer,
            chipFillColor = colorScheme.primary.copy(alpha = 0.18f)
        )

        TabletFolderBackdrop.OUTLINED_ACCENT -> TabletFolderBackdropStyle(
            backgroundBrush = Brush.verticalGradient(
                listOf(
                    colorScheme.surface.copy(alpha = 0.96f),
                    colorScheme.primaryContainer.copy(alpha = 0.34f)
                )
            ),
            borderColor = colorScheme.primary,
            borderWidth = 2.dp,
            contentColor = colorScheme.onSurface,
            secondaryContentColor = colorScheme.onSurfaceVariant,
            accentColor = colorScheme.primary,
            chipFillColor = colorScheme.primaryContainer.copy(alpha = 0.65f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppFolderView(
    folder: AppFolder,
    appsInFolder: List<AppItem>,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    iconShape: IconShape = IconShape.SQUIRCLE,
    folderTileSizeDp: Dp = iconSize.dpValue.dp,
    previewColumns: Int = 2,
    showLabel: Boolean = true,
    showAppCountBadge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticsEnabled = LocalLauncherHapticEnabled.current
    val cols = previewColumns.coerceIn(2, 3)
    val maxPreview = cols * cols
    val shape = RoundedCornerShape(24)
    val miniIconSize = (folderTileSizeDp * (if (cols >= 3) 0.23f else 0.34f)).coerceAtLeast(13.dp)
    val miniShape = getIconShape(iconPackStyle, iconShape)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(folderTileSizeDp + 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { cb ->
                    {
                        if (hapticsEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        cb()
                    }
                }
            )
            .padding(vertical = 2.dp)
            .testTag("app_folder_${folder.id}")
    ) {
        Box(modifier = Modifier.size(folderTileSizeDp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(folderTileSizeDp)
                    .shadow(elevation = 3.dp, shape = shape)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f), shape)
                    .padding((folderTileSizeDp * 0.1f).coerceIn(5.dp, 10.dp))
            ) {
                if (appsInFolder.isEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = folder.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(folderTileSizeDp * 0.46f)
                    )
                } else {
                    val previewApps = appsInFolder.take(maxPreview)
                    val rows = previewApps.chunked(cols)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(if (cols >= 3) 3.dp else 4.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.size(folderTileSizeDp * 0.82f)
                    ) {
                        rows.forEach { rowApps ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(if (cols >= 3) 3.dp else 4.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowApps.forEach { app ->
                                    AppIconGraphic(
                                        app = app,
                                        style = iconPackStyle,
                                        size = miniIconSize,
                                        shape = miniShape,
                                        context = context
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAppCountBadge && appsInFolder.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = appsInFolder.size.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (folderTileSizeDp >= 78.dp) 13.sp else 11.5.sp,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderExpandedDialog(
    folder: AppFolder,
    appsInFolder: List<AppItem>,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    iconShape: IconShape = IconShape.SQUIRCLE,
    isTabletLayout: Boolean = false,
    tabletFolderSize: TabletFolderSize = TabletFolderSize.LARGE,
    tabletExpandedFolderIconShape: IconShape? = null,
    tabletFolderBackdrop: TabletFolderBackdrop = TabletFolderBackdrop.TONAL_SURFACE,
    isPinnedToHome: Boolean = false,
    onChangeTabletFolderSize: ((TabletFolderSize) -> Unit)? = null,
    onTogglePinToHome: (() -> Unit)? = null,
    onAppClick: (AppItem) -> Unit,
    onRemoveFromFolder: (AppItem) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var folderNameText by remember(folder.name) { mutableStateOf(folder.name) }

    val effectiveIconShape = if (isTabletLayout) {
        tabletExpandedFolderIconShape ?: iconShape
    } else {
        iconShape
    }
    val tabletBackdropStyle = if (isTabletLayout) {
        resolveTabletFolderBackdropStyle(tabletFolderBackdrop, MaterialTheme.colorScheme)
    } else {
        null
    }
    val headerTextColor = tabletBackdropStyle?.contentColor ?: MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = tabletBackdropStyle?.secondaryContentColor ?: MaterialTheme.colorScheme.onSurfaceVariant

    val dialogCols = if (isTabletLayout) tabletFolderSize.dialogCols else 4
    val dialogMaxWidth = if (isTabletLayout) tabletFolderSize.dialogMaxWidthDp.dp else 440.dp
    val gridHeight = if (isTabletLayout) {
        when (tabletFolderSize) {
            TabletFolderSize.STANDARD -> 280.dp
            TabletFolderSize.LARGE -> 360.dp
            TabletFolderSize.EXTRA_LARGE -> 430.dp
        }
    } else {
        260.dp
    }
    val itemIconSize = if (isTabletLayout && tabletFolderSize != TabletFolderSize.STANDARD) {
        IconSize.LARGE
    } else {
        iconSize
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !isTabletLayout)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTabletLayout) 28.dp else 8.dp)
                .widthIn(max = dialogMaxWidth)
                .testTag("expanded_folder_dialog"),
            shape = RoundedCornerShape(28.dp),
            border = tabletBackdropStyle?.let { BorderStroke(it.borderWidth, it.borderColor) },
            colors = CardDefaults.cardColors(
                containerColor = if (tabletBackdropStyle != null) Color.Transparent else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (tabletBackdropStyle != null) 0.dp else 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (tabletBackdropStyle != null) {
                            Modifier.background(tabletBackdropStyle.backgroundBrush)
                        } else {
                            Modifier
                        }
                    )
                    .padding(if (isTabletLayout) 24.dp else 20.dp)
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
                                style = if (isTabletLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = headerTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Folder",
                                    tint = secondaryTextColor,
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
                                tint = secondaryTextColor
                            )
                        }
                    }
                }

                // Tablet Size & Pin Controls inside Expanded Folder
                if ((isTabletLayout && onChangeTabletFolderSize != null) || onTogglePinToHome != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTabletLayout && onChangeTabletFolderSize != null) {
                            TabletFolderSize.entries.forEach { sizeOption ->
                                FilterChip(
                                    selected = tabletFolderSize == sizeOption,
                                    onClick = { onChangeTabletFolderSize(sizeOption) },
                                    label = { Text(sizeOption.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("dialog_folder_size_${sizeOption.name}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (onTogglePinToHome != null) {
                            FilterChip(
                                selected = isPinnedToHome,
                                onClick = onTogglePinToHome,
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isPinnedToHome) Icons.Default.Home else Icons.Default.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (isPinnedToHome) "On Home" else "Add to Home",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                modifier = Modifier.testTag("dialog_folder_pin_home_btn")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            color = secondaryTextColor
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(dialogCols),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = gridHeight)
                    ) {
                        items(appsInFolder, key = { it.id }) { app ->
                            AppIconView(
                                app = app,
                                iconPackStyle = iconPackStyle,
                                iconSize = itemIconSize,
                                iconShape = effectiveIconShape,
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
                        color = secondaryTextColor.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
