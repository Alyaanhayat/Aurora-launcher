package com.example.ui.components

import android.appwidget.AppWidgetHostView
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.PlacedAppWidget
import com.example.util.SystemAppWidgetHelper
import kotlin.math.roundToInt

private enum class ResizeEdge {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    BOTTOM_RIGHT
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlacedAppWidgetCard(
    widget: PlacedAppWidget,
    cellWidthDp: Dp,
    cellHeightDp: Dp,
    gridCols: Int,
    gridRows: Int,
    isEditMode: Boolean,
    isResizeActive: Boolean,
    onToggleResizeHandles: (Boolean) -> Unit,
    onResizeInteractionChanged: (Boolean) -> Unit,
    onUpdateBounds: (targetRow: Int, targetCol: Int, newSpanX: Int, newSpanY: Int) -> Unit,
    onOpenWidgetOptions: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current
    val hapticsEnabled by rememberUpdatedState(LocalLauncherHapticEnabled.current)

    val providerInfo = remember(widget.appWidgetId, widget.providerPackage, widget.providerClass) {
        SystemAppWidgetHelper.getAppWidgetInfo(context, widget.appWidgetId)
            ?: SystemAppWidgetHelper.findProviderInfo(context, widget.providerPackage, widget.providerClass)
    }

    val clampedRow = widget.row.coerceIn(0, (gridRows - 1).coerceAtLeast(0))
    val clampedCol = widget.col.coerceIn(0, (gridCols - 1).coerceAtLeast(0))
    val clampedSpanX = widget.spanX.coerceIn(1, (gridCols - clampedCol).coerceAtLeast(1))
    val clampedSpanY = widget.spanY.coerceIn(1, (gridRows - clampedRow).coerceAtLeast(1))

    val cellWidthPx = with(density) { cellWidthDp.toPx().coerceAtLeast(1f) }
    val cellHeightPx = with(density) { cellHeightDp.toPx().coerceAtLeast(1f) }

    val baseLeftPx = clampedCol * cellWidthPx
    val baseTopPx = clampedRow * cellHeightPx
    val baseRightPx = (clampedCol + clampedSpanX) * cellWidthPx
    val baseBottomPx = (clampedRow + clampedSpanY) * cellHeightPx
    val maxGridRightPx = gridCols * cellWidthPx
    val maxGridBottomPx = gridRows * cellHeightPx

    var activeEdge by remember(widget.appWidgetId) { mutableStateOf<ResizeEdge?>(null) }
    var dragTotalDx by remember(widget.appWidgetId) { mutableFloatStateOf(0f) }
    var dragTotalDy by remember(widget.appWidgetId) { mutableFloatStateOf(0f) }

    val isDragging = activeEdge != null

    val liveLeftPx = when (activeEdge) {
        ResizeEdge.LEFT -> (baseLeftPx + dragTotalDx).coerceIn(0f, baseRightPx - cellWidthPx)
        else -> baseLeftPx
    }
    val liveTopPx = when (activeEdge) {
        ResizeEdge.TOP -> (baseTopPx + dragTotalDy).coerceIn(0f, baseBottomPx - cellHeightPx)
        else -> baseTopPx
    }
    val liveRightPx = when (activeEdge) {
        ResizeEdge.RIGHT, ResizeEdge.BOTTOM_RIGHT ->
            (baseRightPx + dragTotalDx).coerceIn(baseLeftPx + cellWidthPx, maxGridRightPx)
        else -> baseRightPx
    }
    val liveBottomPx = when (activeEdge) {
        ResizeEdge.BOTTOM, ResizeEdge.BOTTOM_RIGHT ->
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
        label = "widget_width_anim"
    )
    val animatedHeightDp by animateDpAsState(
        targetValue = liveHeightDp,
        animationSpec = if (isDragging) spring(stiffness = 2200f) else spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "widget_height_anim"
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

    // Outer unclipped host container so snap-preview & circular resize handles can sit right on the frame border
    Box(
        modifier = modifier
            .size(width = cellWidthDp * clampedSpanX, height = cellHeightDp * clampedSpanY)
    ) {
        // Snapped target footprint preview while dragging a resize handle
        if (isDragging) {
            val snapOffsetX = ((candCol * cellWidthPx) - baseLeftPx).roundToInt()
            val snapOffsetY = ((candRow * cellHeightPx) - baseTopPx).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(snapOffsetX, snapOffsetY) }
                    .size(width = cellWidthDp * candSpanX, height = cellHeightDp * candSpanY)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
        }

        // Fluid live resizing widget container
        Box(
            modifier = Modifier
                .offset { IntOffset(liveOffsetXPx.roundToInt(), liveOffsetYPx.roundToInt()) }
                .size(width = animatedWidthDp, height = animatedHeightDp)
        ) {
            RenderErrorBoundary(
                componentName = "Widget: ${widget.label}",
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (showHandles) {
                            Modifier.border(
                                width = if (isDragging) 3.dp else 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(22.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .testTag("placed_widget_${widget.appWidgetId}")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (providerInfo != null) {
                        val widthInt = liveWidthDp.value.toInt().coerceAtLeast(64)
                        val heightInt = liveHeightDp.value.toInt().coerceAtLeast(64)

                        AndroidView(
                            factory = { ctx ->
                                val container = FrameLayout(ctx)
                                val hostView = SystemAppWidgetHelper.createHostView(ctx, widget.appWidgetId, providerInfo)
                                if (hostView != null) {
                                    (hostView.parent as? ViewGroup)?.removeView(hostView)
                                    SystemAppWidgetHelper.updateWidgetSize(hostView, widthInt, heightInt)
                                    container.addView(
                                        hostView,
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                                container
                            },
                            update = { container ->
                                val child = container.getChildAt(0) as? AppWidgetHostView
                                if (child != null) {
                                    SystemAppWidgetHelper.updateWidgetSize(child, widthInt, heightInt)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                .combinedClickable(
                                    onClick = { onOpenWidgetOptions() },
                                    onLongClick = {
                                        if (hapticsEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        }
                                        onToggleResizeHandles(true)
                                    }
                                )
                                .padding(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = widget.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tap to manage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Active resize / edit overlay controls
                    if (showHandles) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.scrim.copy(
                                        alpha = if (isDragging) 0.14f else 0.22f
                                    )
                                )
                        ) {
                            // Top-Right Quick Action Pills (Options, Remove, Done)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                                        .clickable { onOpenWidgetOptions() }
                                        .testTag("widget_tune_${widget.appWidgetId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Widget Options",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .clickable { onRemove() }
                                        .testTag("widget_remove_${widget.appWidgetId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Widget",
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (!isEditMode) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .clickable {
                                                if (hapticsEnabled) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                                }
                                                onToggleResizeHandles(false)
                                            }
                                            .testTag("widget_done_resize_${widget.appWidgetId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Done Resizing",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Live Dimension Badge (updates dynamically as handles are dragged)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDragging) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isDragging) "${candSpanX} × ${candSpanY} cells" else "${widget.spanX} × ${widget.spanY} • Drag edges",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDragging) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Subtle corner resize/edit trigger without blocking widget interaction
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                                .clickable {
                                    if (hapticsEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    }
                                    onToggleResizeHandles(true)
                                }
                                .testTag("widget_options_handle_${widget.appWidgetId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Resize or Edit Widget",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Edge & Corner Resize Handles rendered above border so they are easy to grab
            if (showHandles) {
                val startResize: (ResizeEdge) -> Unit = { edge ->
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

                // Right Edge Pill Handle
                EdgeResizeHandle(
                    isHorizontal = false,
                    isActive = activeEdge == ResizeEdge.RIGHT,
                    contentDescription = "Resize Right Edge",
                    onDragStart = { startResize(ResizeEdge.RIGHT) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 8.dp)
                        .testTag("widget_handle_right_${widget.appWidgetId}")
                )

                // Left Edge Pill Handle
                EdgeResizeHandle(
                    isHorizontal = false,
                    isActive = activeEdge == ResizeEdge.LEFT,
                    contentDescription = "Resize Left Edge",
                    onDragStart = { startResize(ResizeEdge.LEFT) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-8).dp)
                        .testTag("widget_handle_left_${widget.appWidgetId}")
                )

                // Bottom Edge Pill Handle
                EdgeResizeHandle(
                    isHorizontal = true,
                    isActive = activeEdge == ResizeEdge.BOTTOM,
                    contentDescription = "Resize Bottom Edge",
                    onDragStart = { startResize(ResizeEdge.BOTTOM) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .testTag("widget_handle_bottom_${widget.appWidgetId}")
                )

                // Top Edge Pill Handle
                EdgeResizeHandle(
                    isHorizontal = true,
                    isActive = activeEdge == ResizeEdge.TOP,
                    contentDescription = "Resize Top Edge",
                    onDragStart = { startResize(ResizeEdge.TOP) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                        .testTag("widget_handle_top_${widget.appWidgetId}")
                )

                // Bottom-Right Corner Diagonal Handle
                CornerResizeHandle(
                    isActive = activeEdge == ResizeEdge.BOTTOM_RIGHT,
                    onDragStart = { startResize(ResizeEdge.BOTTOM_RIGHT) },
                    onDragDeltaRoot = updateResize,
                    onDragEnd = finishResize,
                    onDragCancel = cancelResize,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                        .testTag("widget_handle_corner_${widget.appWidgetId}")
                )
            }
        }
    }
}

@Composable
private fun EdgeResizeHandle(
    isHorizontal: Boolean,
    isActive: Boolean,
    contentDescription: String,
    onDragStart: () -> Unit,
    onDragDeltaRoot: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coordsRef by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragStartRoot by remember { mutableStateOf(Offset.Zero) }

    // 48dp hit target for comfortable touch gripping, with sleek pill knob inside
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (isHorizontal) 56.dp else 44.dp, if (isHorizontal) 44.dp else 56.dp)
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
                    width = if (isHorizontal) (if (isActive) 40.dp else 32.dp) else (if (isActive) 12.dp else 10.dp),
                    height = if (isHorizontal) (if (isActive) 12.dp else 10.dp) else (if (isActive) 40.dp else 32.dp)
                )
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
        )
    }
}

@Composable
private fun CornerResizeHandle(
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
                .size(if (isActive) 28.dp else 24.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Drag corner to resize width and height",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun WidgetManageDialog(
    widget: PlacedAppWidget,
    gridCols: Int,
    gridRows: Int,
    hasConfigureActivity: Boolean,
    onMove: (targetRow: Int, targetCol: Int) -> Unit,
    onResize: (newSpanX: Int, newSpanY: Int) -> Unit,
    onActivateDragHandles: () -> Unit = {},
    onConfigure: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val hapticsEnabled = LocalLauncherHapticEnabled.current
    val tick: () -> Unit = {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = widget.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (hapticsEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                        onDismiss()
                        onActivateDragHandles()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_dialog_drag_handles_btn")
                ) {
                    Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resize with Drag Handles")
                }

                // Size controls
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Widget Size (${widget.spanX} × ${widget.spanY} cells)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Width", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    tick()
                                    onResize((widget.spanX - 1).coerceAtLeast(1), widget.spanY)
                                },
                                enabled = widget.spanX > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Narrower")
                            }
                            Text(
                                text = "${widget.spanX}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = {
                                    tick()
                                    onResize((widget.spanX + 1).coerceAtMost(gridCols), widget.spanY)
                                },
                                enabled = widget.spanX < gridCols
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Wider")
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Height", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    tick()
                                    onResize(widget.spanX, (widget.spanY - 1).coerceAtLeast(1))
                                },
                                enabled = widget.spanY > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Shorter")
                            }
                            Text(
                                text = "${widget.spanY}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = {
                                    tick()
                                    onResize(widget.spanX, (widget.spanY + 1).coerceAtMost(gridRows))
                                },
                                enabled = widget.spanY < gridRows
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Taller")
                            }
                        }
                    }
                }

                // Grid position nudges
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Position on Grid (Row ${widget.row + 1}, Col ${widget.col + 1})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            tick()
                            onMove(widget.row - 1, widget.col)
                        },
                        enabled = widget.row > 0
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IconButton(
                            onClick = {
                                tick()
                                onMove(widget.row, widget.col - 1)
                            },
                            enabled = widget.col > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left")
                        }
                        IconButton(
                            onClick = {
                                tick()
                                onMove(widget.row, widget.col + 1)
                            },
                            enabled = widget.col + widget.spanX < gridCols
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right")
                        }
                    }
                    IconButton(
                        onClick = {
                            tick()
                            onMove(widget.row + 1, widget.col)
                        },
                        enabled = widget.row + widget.spanY < gridRows
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                    }
                }

                if (hasConfigureActivity) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onConfigure()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Widget")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onRemove()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove")
            }
        }
    )
}
