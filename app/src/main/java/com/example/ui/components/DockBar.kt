package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.IconSize

@Composable
fun DockBar(
    dockedApps: List<AppItem>,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    iconShape: IconShape = IconShape.SQUIRCLE,
    showSearchButton: Boolean = true,
    draggedAppPackage: String? = null,
    isDraggingAnyApp: Boolean = false,
    isDockHovered: Boolean = false,
    hoveredDockSlotIndex: Int? = null,
    onDockBoundsChanged: (Rect) -> Unit = {},
    onDockSlotBoundsChanged: (Int, Rect) -> Unit = { _, _ -> },
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onAppTouchActiveChanged: ((Boolean) -> Unit)? = null,
    onAppDragStart: (AppItem, Int, Offset, Rect) -> Unit = { _, _, _, _ -> },
    onAppDragDelta: (Offset) -> Unit = {},
    onAppDragEnd: () -> Unit = {},
    onAppDragCancel: () -> Unit = {},
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isDockHovered) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        },
        label = "dock_border_color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isDockHovered) 2.5.dp else 1.dp,
        label = "dock_border_width"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isDockHovered) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        label = "dock_bg_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    onDockBoundsChanged(coords.boundsInRoot())
                }
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(bgColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("dock_bar_container"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockedApps.take(5).forEachIndexed { index, app ->
                val slotBoundsState = androidx.compose.runtime.remember(index, app.packageName) {
                    androidx.compose.runtime.mutableStateOf(Rect.Zero)
                }
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInRoot()
                            slotBoundsState.value = b
                            onDockSlotBoundsChanged(index, b)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AppIconView(
                        app = app,
                        iconPackStyle = iconPackStyle,
                        iconSize = IconSize.STANDARD,
                        iconShape = iconShape,
                        showLabel = false,
                        isBeingDragged = draggedAppPackage == app.packageName,
                        isDropTarget = isDockHovered && hoveredDockSlotIndex == index,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                        onTouchActiveChanged = onAppTouchActiveChanged,
                        onDragStartAfterLongPress = { localOffset ->
                            onAppDragStart(app, index, localOffset, slotBoundsState.value)
                        },
                        onDragDelta = onAppDragDelta,
                        onDragEnd = onAppDragEnd,
                        onDragCancel = onAppDragCancel
                    )
                }
            }

            // Show subtle drop slot when dragging and dock has room (< 5 apps)
            if (isDraggingAnyApp && dockedApps.size < 5) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (isDockHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDockHovered) 0.45f else 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Drop into Dock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Smart Search quick access pill in Dock
            if (showSearchButton) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onSearchClick() }
                        .testTag("dock_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
