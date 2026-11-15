package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.IconSize
import com.example.model.PlacedHomeFolder
import com.example.model.TabletFolderBackdrop
import kotlin.math.roundToInt

private enum class FolderResizeEdge {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    BOTTOM_RIGHT
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlacedHomeFolderCard(
    placedFolder: PlacedHomeFolder,
    folder: AppFolder,
    appsInFolder: List<AppItem>,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    iconShape: IconShape,
    tabletExpandedFolderIconShape: IconShape? = null,
    tabletFolderBackdrop: TabletFolderBackdrop = TabletFolderBackdrop.TONAL_SURFACE,
    showLabels: Boolean,
    cellWidthDp: Dp,
    cellHeightDp: Dp,
    gridCols: Int,
    gridRows: Int,
    isTabletLayout: Boolean,
    isEditMode: Boolean,
    isResizeActive: Boolean,
    onOpenFolder: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onToggleResizeHandles: (Boolean) -> Unit,
    onResizeInteractionChanged: (Boolean) -> Unit,
    onUpdateBounds: (targetRow: Int, targetCol: Int, newSpanX: Int, newSpanY: Int) -> Unit,
    onRemoveFromHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current
    val hapticsEnabled by rememberUpdatedState(LocalLauncherHapticEnabled.current)

    val clampedRow = placedFolder.row.coerceIn(0, (gridRows - 1).coerceAtLeast(0))
    val clampedCol = placedFolder.col.coerceIn(0, (gridCols - 1).coerceAtLeast(0))
    val clampedSpanX = if (isTabletLayout) placedFolder.spanX.coerceIn(1, (gridCols - clampedCol).coerceAtLeast(1)) else 1
    val clampedSpanY = if (isTabletLayout) placedFolder.spanY.coerceIn(1, (gridRows - clampedRow).coerceAtLeast(1)) else 1

    val cellWidthPx = with(density) { cellWidthDp.toPx().coerceAtLeast(1f) }
    val cellHeightPx = with(density) { cellHeightDp.toPx().coerceAtLeast(1f) }

    val baseLeftPx = clampedCol * cellWidthPx
    val baseTopPx = clampedRow * cellHeightPx
    val baseRightPx = (clampedCol + clampedSpanX) * cellWidthPx
    val baseBottomPx = (clampedRow + clampedSpanY) * cellHeightPx
    val maxGridRightPx = gridCols * cellWidthPx
    val maxGridBottomPx = gridRows * cellHeightPx

    var activeEdge by remember(placedFolder.folderId) { mutableStateOf<FolderResizeEdge?>(null) }
    var dragTotalDx by remember(placedFolder.folderId) { mutableFloatStateOf(0f) }
    var dragTotalDy by remember(placedFolder.folderId) { mutableFloatStateOf(0f) }
    val isDragging = activeEdge != null

    val liveLeftPx = when (activeEdge) {
        FolderResizeEdge.LEFT -> (baseLeftPx + dragTotalDx).coerceIn(0f, baseRightPx - cellWidthPx)
        else -> baseLeftPx
    }
    val liveTopPx = when (activeEdge) {
        FolderResizeEdge.TOP -> (baseTopPx + dragTotalDy).coerceIn(0f, baseBottomPx - cellHeightPx)
        else -> baseTopPx
    }
    val liveRightPx = when (activeEdge) {
        FolderResizeEdge.RIGHT, FolderResizeEdge.BOTTOM_RIGHT ->
            (baseRightPx + dragTotalDx).coerceIn(baseLeftPx + cellWidthPx, maxGridRightPx)
        else -> baseRightPx
    }
    val liveBottomPx = when (activeEdge) {
        FolderResizeEdge.BOTTOM, FolderResizeEdge.BOTTOM_RIGHT ->
            (baseBottomPx + dragTotalDy).coerceIn(baseTopPx + cellHeightPx, maxGridBottomPx)
        else -> baseBottomPx
    }

    val liveWidthDp = with(density) { (liveRightPx - liveLeftPx).toDp() }
    val liveHeightDp = with(density) { (liveBottomPx - liveTopPx).toDp() }
    val liveOffsetXPx = liveLeftPx - baseLeftPx
    val liveOffsetYPx = liveTopPx - baseTopPx

    val animatedWidthDp by animateDpAsState(
        targetValue = liveWidthDp,
        animationSpec = if (isDragging) spring(stiffness = 2200f) else spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "folder_width_anim"
    )
    val animatedHeightDp by animateDpAsState(
        targetValue = liveHeightDp,
        animationSpec = if (isDragging) spring(stiffness = 2200f) else spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "folder_height_anim"
    )

    val candCol = (liveLeftPx / cellWidthPx).roundToInt().coerceIn(0, (gridCols - 1).coerceAtLeast(0))
    val candRightCol = (liveRightPx / cellWidthPx).roundToInt().coerceIn(candCol + 1, gridCols.coerceAtLeast(1))
    val candRow = (liveTopPx / cellHeightPx).roundToInt().coerceIn(0, (gridRows - 1).coerceAtLeast(0))
    val candBottomRow = (liveBottomPx / cellHeightPx).roundToInt().coerceIn(candRow + 1, gridRows.coerceAtLeast(1))
    val candSpanX = (candRightCol - candCol).coerceIn(1, (gridCols - candCol).coerceAtLeast(1))
    val candSpanY = (candBottomRow - candRow).coerceIn(1, (gridRows - candRow).coerceAtLeast(1))

    LaunchedEffect(isDragging, candRow, candCol, candSpanX, candSpanY, hapticsEnabled) {
        if (hapticsEnabled && isDragging) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val showHandles = isResizeActive || isEditMode
    val isExpandedCard = isTabletLayout && (candSpanX > 1 || candSpanY > 1 || clampedSpanX > 1 || clampedSpanY > 1)
    val tabletBackdropStyle = if (isTabletLayout && isExpandedCard) {
        resolveTabletFolderBackdropStyle(tabletFolderBackdrop, MaterialTheme.colorScheme)
    } else {
        null
    }
    val expandedHeaderColor = tabletBackdropStyle?.contentColor ?: MaterialTheme.colorScheme.onSurface
    val expandedSecondaryColor = tabletBackdropStyle?.secondaryContentColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    val expandedAccentColor = tabletBackdropStyle?.accentColor ?: MaterialTheme.colorScheme.primary
    val expandedBadgeFill = tabletBackdropStyle?.chipFillColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .size(width = cellWidthDp * clampedSpanX, height = cellHeightDp * clampedSpanY)
    ) {
        if (isDragging) {
            val snapOffsetX = ((candCol * cellWidthPx) - baseLeftPx).roundToInt()
            val snapOffsetY = ((candRow * cellHeightPx) - baseTopPx).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(snapOffsetX, snapOffsetY) }
                    .size(width = cellWidthDp * candSpanX, height = cellHeightDp * candSpanY)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(liveOffsetXPx.roundToInt(), liveOffsetYPx.roundToInt()) }
                .size(width = animatedWidthDp, height = animatedHeightDp)
        ) {
            val cardShape = RoundedCornerShape(22.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isExpandedCard) 4.dp else 2.dp)
                    .clip(cardShape)
                    .then(
                        if (isExpandedCard) {
                            val bgModifier = if (tabletBackdropStyle != null) {
                                Modifier.background(tabletBackdropStyle.backgroundBrush)
                            } else {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f))
                            }
                            bgModifier.border(
                                width = if (showHandles) 2.dp else (tabletBackdropStyle?.borderWidth ?: 1.dp),
                                color = if (showHandles) MaterialTheme.colorScheme.primary
                                else (tabletBackdropStyle?.borderColor ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shape = cardShape
                            )
                        } else if (showHandles) {
                            Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(2.dp, MaterialTheme.colorScheme.primary, cardShape)
                        } else {
                            Modifier
                        }
                    )
                    .combinedClickable(
                        onClick = {
                            if (showHandles && !isEditMode) {
                                onToggleResizeHandles(false)
                            } else {
                                onOpenFolder()
                            }
                        },
                        onLongClick = {
                            if (hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            onToggleResizeHandles(true)
                        }
                    )
                    .testTag("placed_home_folder_${folder.id}")
            ) {
                if (!isExpandedCard) {
                    val compactTileSize = (iconSize.dpValue.dp).coerceAtMost((cellWidthDp - 20.dp).coerceAtLeast(42.dp))
                    AppFolderView(
                        folder = folder,
                        appsInFolder = appsInFolder,
                        iconPackStyle = iconPackStyle,
                        iconSize = iconSize,
                        iconShape = iconShape,
                        folderTileSizeDp = compactTileSize,
                        previewColumns = 2,
                        showLabel = showLabels,
                        showAppCountBadge = false,
                        onLongClick = { onToggleResizeHandles(true) },
                        onClick = {
                            if (showHandles && !isEditMode) {
                                onToggleResizeHandles(false)
                            } else {
                                onOpenFolder()
                            }
                        }
                    )
                } else {
                    // Enlarged Interactive Folder Card (directly tap apps or tap header to open full dialog)
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        val innerCols = when {
                            maxWidth >= 320.dp -> 4
                            maxWidth >= 210.dp -> 3
                            else -> 2
                        }
                        val headerHeight = 26.dp
                        val gridAreaHeight = (maxHeight - headerHeight - 6.dp).coerceAtLeast(48.dp)
                        val innerRows = (gridAreaHeight / 64.dp).toInt().coerceIn(1, 3)
                        val maxDirectSlots = innerCols * innerRows
                        val hasOverflow = appsInFolder.size > maxDirectSlots
                        val visibleDirectApps = if (hasOverflow) {
                            appsInFolder.take((maxDirectSlots - 1).coerceAtLeast(1))
                        } else {
                            appsInFolder.take(maxDirectSlots)
                        }
                        val appIconSize = when {
                            maxWidth >= 240.dp && maxHeight >= 180.dp -> 46.dp
                            maxWidth >= 180.dp -> 40.dp
                            else -> 36.dp
                        }
                        val effectiveExpandedShape = if (isTabletLayout) {
                            tabletExpandedFolderIconShape ?: iconShape
                        } else {
                            iconShape
                        }
                        val appShape = getIconShape(iconPackStyle, effectiveExpandedShape)

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(headerHeight)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenFolder() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = expandedAccentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = expandedHeaderColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(expandedBadgeFill)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = appsInFolder.size.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = expandedAccentColor
                                        )
                                    }
                                }

                                if (!showHandles) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                                            .clickable { onOpenFolder() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInFull,
                                            contentDescription = "Expand Folder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (appsInFolder.isEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clickable { onOpenFolder() }
                                ) {
                                    Text(
                                        text = "Empty folder • Tap to manage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = expandedSecondaryColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                val rows = (visibleDirectApps.map<AppItem, AppItem?> { it } +
                                    if (hasOverflow) listOf(null) else emptyList()).chunked(innerCols)
                                Column(
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    rows.forEach { rowEntries ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            for (i in 0 until innerCols) {
                                                val item = rowEntries.getOrNull(i)
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (item != null) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .combinedClickable(
                                                                    onClick = { onAppClick(item) },
                                                                    onLongClick = { onToggleResizeHandles(true) }
                                                                )
                                                                .padding(2.dp)
                                                        ) {
                                                            AppIconGraphic(
                                                                app = item,
                                                                style = iconPackStyle,
                                                                size = appIconSize,
                                                                shape = appShape,
                                                                context = context
                                                            )
                                                            if (showLabels && gridAreaHeight >= 110.dp) {
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = item.displayLabel,
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                    color = expandedHeaderColor,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    } else if (hasOverflow && rowEntries === rows.last() && i == rowEntries.lastIndex) {
                                                        val remaining = appsInFolder.size - visibleDirectApps.size
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier
                                                                .size(appIconSize)
                                                                .clip(appShape)
                                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                                                                .clickable { onOpenFolder() }
                                                        ) {
                                                            Text(
                                                                text = "+$remaining",
                                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active resize overlay controls (quick size buttons + remove + done + live dimensions)
                if (showHandles) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = if (isDragging) 0.14f else 0.22f))
                    ) {
                        // Top-right quick controls (Grow, Remove, Done)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            if (isTabletLayout && (clampedSpanX < 2 || clampedSpanY < 2)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            if (hapticsEnabled) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                            }
                                            onUpdateBounds(clampedRow, clampedCol, 2.coerceAtMost(gridCols), 2.coerceAtMost(gridRows))
                                        }
                                        .padding(horizontal = 8.dp)
                                        .testTag("folder_make_bigger_${folder.id}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Make Folder Bigger",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "2×2",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable { onRemoveFromHome() }
                                    .testTag("folder_remove_home_${folder.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Folder from Home",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (!isEditMode) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            if (hapticsEnabled) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                            }
                                            onToggleResizeHandles(false)
                                        }
                                        .testTag("folder_done_resize_${folder.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done Resizing Folder",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // Live dimension badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDragging) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when {
                                    !isTabletLayout -> "1×1 • Phone"
                                    isDragging -> "${candSpanX}×${candSpanY}"
                                    else -> "${clampedSpanX}×${clampedSpanY} • Drag"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDragging) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Interactive Edge & Corner Resize Handles (Tablet mode only)
            if (showHandles && isTabletLayout) {
                val startResize: (FolderResizeEdge) -> Unit = { edge ->
                    if (hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    activeEdge = edge
                    dragTotalDx = 0f
                    dragTotalDy = 0f
                    onResizeInteractionChanged(true)
                }
                val updateResize: (Float, Float) -> Unit = { dx, dy ->
                    dragTotalDx = dx
                    dragTotalDy = dy
                }
                val finishResize: () -> Unit = {
                    val targetRow = candRow
                    val targetCol = candCol
                    val targetSpanX = candSpanX
                    val targetSpanY = candSpanY
                    activeEdge = null
                    dragTotalDx = 0f
                    dragTotalDy = 0f
                    onResizeInteractionChanged(false)
                    onUpdateBounds(targetRow, targetCol, targetSpanX, targetSpanY)
                    if (hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
                val cancelResize: () -> Unit = {
                    activeEdge = null
                    dragTotalDx = 0f
                    dragTotalDy = 0f
                    onResizeInteractionChanged(false)
                }

                FolderEdgeHandle(
                    isHorizontal = false,
                    isActive = activeEdge == FolderResizeEdge.RIGHT,
                    onDragStart = { startResize(FolderResizeEdge.RIGHT) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 8.dp)
                        .testTag("folder_handle_right_${folder.id}")
                )
                FolderEdgeHandle(
                    isHorizontal = false,
                    isActive = activeEdge == FolderResizeEdge.LEFT,
                    onDragStart = { startResize(FolderResizeEdge.LEFT) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-8).dp)
                        .testTag("folder_handle_left_${folder.id}")
                )
                FolderEdgeHandle(
                    isHorizontal = true,
                    isActive = activeEdge == FolderResizeEdge.BOTTOM,
                    onDragStart = { startResize(FolderResizeEdge.BOTTOM) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .testTag("folder_handle_bottom_${folder.id}")
                )
                FolderEdgeHandle(
                    isHorizontal = true,
                    isActive = activeEdge == FolderResizeEdge.TOP,
                    onDragStart = { startResize(FolderResizeEdge.TOP) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                        .testTag("folder_handle_top_${folder.id}")
                )
                FolderCornerHandle(
                    isActive = activeEdge == FolderResizeEdge.BOTTOM_RIGHT,
                    onDragStart = { startResize(FolderResizeEdge.BOTTOM_RIGHT) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                        .testTag("folder_handle_corner_${folder.id}")
                )
            }
        }
    }
}

@Composable
private fun FolderEdgeHandle(
    isHorizontal: Boolean,
    isActive: Boolean,
    onDragStart: () -> Unit,
    onDragDeltaRoot: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coordsRef by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragStartRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (isHorizontal) 52.dp else 40.dp, if (isHorizontal) 40.dp else 52.dp)
            .onGloballyPositioned { coordsRef = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragStartRoot = coordsRef?.localToRoot(startOffset) ?: Offset.Zero
                        onDragStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val curRoot = coordsRef?.localToRoot(change.position) ?: dragStartRoot
                        onDragDeltaRoot(
                            curRoot.x - dragStartRoot.x,
                            curRoot.y - dragStartRoot.y
                        )
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (isHorizontal) (if (isActive) 38.dp else 28.dp) else (if (isActive) 12.dp else 9.dp),
                    height = if (isHorizontal) (if (isActive) 12.dp else 9.dp) else (if (isActive) 38.dp else 28.dp)
                )
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
        )
    }
}

@Composable
private fun FolderCornerHandle(
    isActive: Boolean,
    onDragStart: () -> Unit,
    onDragDeltaRoot: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coordsRef by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragStartRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .onGloballyPositioned { coordsRef = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragStartRoot = coordsRef?.localToRoot(startOffset) ?: Offset.Zero
                        onDragStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val curRoot = coordsRef?.localToRoot(change.position) ?: dragStartRoot
                        onDragDeltaRoot(
                            curRoot.x - dragStartRoot.x,
                            curRoot.y - dragStartRoot.y
                        )
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (isActive) 26.dp else 22.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Drag corner to resize folder",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
