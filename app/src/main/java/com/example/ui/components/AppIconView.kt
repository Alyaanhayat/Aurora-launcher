package com.example.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shadow
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.model.AppIconType
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.IconSize
import com.example.util.IconPackManager

val LocalImportedIconMap = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

fun getGlyphForName(name: String): ImageVector {
    return when (name) {
        "phone" -> Icons.Default.Phone
        "message" -> Icons.Default.Sms
        "browser" -> Icons.Default.Language
        "camera" -> Icons.Default.PhotoCamera
        "photo" -> Icons.Default.Photo
        "music" -> Icons.Default.MusicNote
        "calendar" -> Icons.Default.CalendarMonth
        "note" -> Icons.Default.Notes
        "clock" -> Icons.Default.Schedule
        "calculator" -> Icons.Default.Calculate
        "mail" -> Icons.Default.Email
        "map" -> Icons.Default.Map
        "settings" -> Icons.Default.Settings
        "video" -> Icons.Default.PlayArrow
        "chat" -> Icons.Default.Chat
        "folder" -> Icons.Default.Folder
        "fitness" -> Icons.Default.FitnessCenter
        "games" -> Icons.Default.SportsEsports
        "book" -> Icons.Default.Book
        else -> Icons.Default.Apps
    }
}

val FlowerIconShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(cx, cy)
    val steps = 96
    for (i in 0..steps) {
        val theta = (i.toDouble() / steps) * (2.0 * PI)
        val r = radius * (0.83 - 0.15 * cos(4.0 * theta))
        val x = (cx + r * cos(theta)).toFloat()
        val y = (cy + r * sin(theta)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

val StarIconShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outerR = min(cx, cy) * 0.98f
    val innerR = min(cx, cy) * 0.77f
    val points = 8
    for (i in 0 until points * 2) {
        val angle = -PI / 2.0 + i * (PI / points)
        val r = if (i % 2 == 0) outerR else innerR
        val x = (cx + r * cos(angle)).toFloat()
        val y = (cy + r * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun getIconShape(style: IconPackStyle, iconShape: IconShape = IconShape.SQUIRCLE): Shape {
    return when (iconShape) {
        IconShape.SQUIRCLE -> when (style) {
            IconPackStyle.CLASSIC_ROUND -> CircleShape
            IconPackStyle.PEBBLE -> RoundedCornerShape(topStart = 45f, topEnd = 25f, bottomEnd = 45f, bottomStart = 25f)
            else -> RoundedCornerShape(26)
        }
        IconShape.CIRCLE -> CircleShape
        IconShape.FLOWER -> FlowerIconShape
        IconShape.STAR -> StarIconShape
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppIconView(
    app: AppItem,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    iconShape: IconShape = IconShape.SQUIRCLE,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier,
    isBeingDragged: Boolean = false,
    isDropTarget: Boolean = false,
    onWallpaper: Boolean = false,
    wallpaperDarkText: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onTouchActiveChanged: ((Boolean) -> Unit)? = null,
    onDragStartAfterLongPress: ((Offset) -> Unit)? = null,
    onDragDelta: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null
) {
    val sizeDp = iconSize.dpValue.dp
    val shape = getIconShape(iconPackStyle, iconShape)
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnTouchActiveChanged by rememberUpdatedState(onTouchActiveChanged)
    val currentOnDragStart by rememberUpdatedState(onDragStartAfterLongPress)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val gestureModifier = if (onDragStartAfterLongPress != null) {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(app.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    currentOnTouchActiveChanged?.invoke(true)
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)

                    try {
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val touchSlop = viewConfiguration.touchSlop
                        var cancelledBeforeLongPress = false

                        val timedOut = withTimeoutOrNull(longPressTimeout) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    cancelledBeforeLongPress = true
                                    return@withTimeoutOrNull false
                                }
                                if (change.isConsumed) {
                                    cancelledBeforeLongPress = true
                                    return@withTimeoutOrNull false
                                }
                                if (change.changedToUpIgnoreConsumed()) {
                                    interactionSource.tryEmit(PressInteraction.Release(press))
                                    currentOnClick()
                                    return@withTimeoutOrNull true
                                }
                                if ((change.position - down.position).getDistance() > touchSlop) {
                                    cancelledBeforeLongPress = true
                                    return@withTimeoutOrNull false
                                }
                            }
                            @Suppress("UNREACHABLE_CODE")
                            false
                        }

                        if (timedOut == true || cancelledBeforeLongPress) {
                            if (cancelledBeforeLongPress) {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                            }
                            return@awaitEachGesture
                        }

                        // Long-press timeout elapsed without moving or lifting! Start drag mode.
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                        currentOnDragStart?.invoke(down.position)

                        var finishedCleanly = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || change.changedToUpIgnoreConsumed()) {
                                    finishedCleanly = true
                                    currentOnDragEnd?.invoke()
                                    break
                                }
                                val delta = change.positionChange()
                                change.consume()
                                if (delta != Offset.Zero) {
                                    currentOnDragDelta?.invoke(delta)
                                }
                            }
                        } finally {
                            if (!finishedCleanly) {
                                currentOnDragCancel?.invoke()
                            }
                        }
                    } finally {
                        currentOnTouchActiveChanged?.invoke(false)
                    }
                }
            }
    } else {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(sizeDp + 24.dp)
            .then(gestureModifier)
            .alpha(if (isBeingDragged) 0.2f else 1f)
            .testTag("app_item_${app.packageName.replace('.', '_')}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (isDropTarget) {
                Modifier
                    .border(2.5.dp, MaterialTheme.colorScheme.primary, shape)
                    .clip(shape)
            } else {
                Modifier
            }
        ) {
            AppIconGraphic(
                app = app,
                style = iconPackStyle,
                size = sizeDp,
                shape = shape,
                context = context
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            val labelColor = if (onWallpaper) {
                if (wallpaperDarkText) Color(0xFF14161D) else Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val labelShadow = if (onWallpaper) {
                Shadow(
                    color = if (wallpaperDarkText) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.78f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 6f
                )
            } else null

            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = labelShadow
                ),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppIconGraphic(
    app: AppItem,
    style: IconPackStyle,
    size: androidx.compose.ui.unit.Dp,
    shape: Shape,
    context: Context
) {
    val glyph = getGlyphForName(app.iconGlyphName)
    val fallbackColor = Color(app.iconColor)

    val systemDrawable: Drawable? = remember(app.packageName) {
        if (app.iconType == AppIconType.SYSTEM_PACKAGE) {
            runCatching {
                context.packageManager.getApplicationIcon(app.packageName)
            }.getOrNull()
        } else null
    }

    val importedMap = LocalImportedIconMap.current
    val importedPath = importedMap[app.packageName]
    val importedBitmap = remember(importedPath, style) {
        if (style == IconPackStyle.IMPORTED_CUSTOM && importedPath != null) {
            IconPackManager.decodeSampledBitmap(importedPath, reqSizePx = 192)?.asImageBitmap()
        } else null
    }

    if (style == IconPackStyle.IMPORTED_CUSTOM && importedBitmap != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(shape)
        ) {
            Image(
                bitmap = importedBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    when (style) {
        IconPackStyle.MATERIAL_YOU_THEMED -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .shadow(elevation = 2.dp, shape = shape)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = glyph,
                    contentDescription = app.label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }

        IconPackStyle.MINIMAL_OUTLINE -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), shape)
            ) {
                Icon(
                    imageVector = glyph,
                    contentDescription = app.label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size * 0.50f)
                )
            }
        }

        IconPackStyle.VIBRANT_MATERIAL, IconPackStyle.IMPORTED_CUSTOM -> {
            if (app.packageName == "com.aurora.launcher.settings") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .shadow(elevation = 3.dp, shape = shape)
                        .clip(shape)
                        .background(Color(0xFFEAD3FA))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_aurora_icon),
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(size * 0.90f)
                    )
                }
            } else {
                val gradient = Brush.linearGradient(
                    listOf(fallbackColor, fallbackColor.copy(alpha = 0.78f))
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .shadow(elevation = 3.dp, shape = shape)
                        .clip(shape)
                        .background(gradient)
                ) {
                    if (systemDrawable != null) {
                        val bitmap = remember(systemDrawable) {
                            systemDrawable.toBitmap(size.value.toInt() * 2, size.value.toInt() * 2).asImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(size * 0.68f)
                        )
                    } else {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.label,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                }
            }
        }

        IconPackStyle.CLASSIC_ROUND, IconPackStyle.SQUIRCLE, IconPackStyle.PEBBLE -> {
            if (app.packageName == "com.aurora.launcher.settings") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .shadow(elevation = 3.dp, shape = shape)
                        .clip(shape)
                        .background(Color(0xFFEAD3FA))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_aurora_icon),
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(size * 0.90f)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .shadow(elevation = 3.dp, shape = shape)
                        .clip(shape)
                        .background(fallbackColor)
                ) {
                    if (systemDrawable != null) {
                        val bitmap = remember(systemDrawable) {
                            systemDrawable.toBitmap(size.value.toInt() * 2, size.value.toInt() * 2).asImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(size * 0.65f)
                        )
                    } else {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.label,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                }
            }
        }
    }
}
