package com.example.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppIconType
import com.example.model.AppItem
import com.example.model.IconPackStyle
import com.example.model.IconSize

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

fun getIconShape(style: IconPackStyle): Shape {
    return when (style) {
        IconPackStyle.CLASSIC_ROUND -> CircleShape
        IconPackStyle.SQUIRCLE -> RoundedCornerShape(26)
        IconPackStyle.PEBBLE -> RoundedCornerShape(topStart = 45f, topEnd = 25f, bottomEnd = 45f, bottomStart = 25f)
        IconPackStyle.MINIMAL_OUTLINE -> RoundedCornerShape(22)
        IconPackStyle.MATERIAL_YOU_THEMED -> RoundedCornerShape(24)
        IconPackStyle.VIBRANT_MATERIAL -> RoundedCornerShape(22)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppIconView(
    app: AppItem,
    iconPackStyle: IconPackStyle,
    iconSize: IconSize,
    showLabel: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val sizeDp = iconSize.dpValue.dp
    val shape = getIconShape(iconPackStyle)
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(sizeDp + 24.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("app_item_${app.packageName.replace('.', '_')}")
    ) {
        AppIconGraphic(
            app = app,
            style = iconPackStyle,
            size = sizeDp,
            shape = shape,
            context = context
        )

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
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
fun AppIconGraphic(
    app: AppItem,
    style: IconPackStyle,
    size: androidx.compose.ui.unit.Dp,
    shape: Shape,
    context: Context
) {
    val glyph = getGlyphForName(app.iconGlyphName)
    val fallbackColor = Color(app.iconColor)

    // Try loading system icon if it is a system package
    val systemDrawable: Drawable? = remember(app.packageName) {
        if (app.iconType == AppIconType.SYSTEM_PACKAGE) {
            runCatching {
                context.packageManager.getApplicationIcon(app.packageName)
            }.getOrNull()
        } else null
    }

    when (style) {
        IconPackStyle.MATERIAL_YOU_THEMED -> {
            // Material You Themed: Dynamic monochrome surface and tinted glyph
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
            // Pure minimalist outline
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Color.Transparent)
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

        IconPackStyle.VIBRANT_MATERIAL -> {
            // Vibrant with colorful gradient
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

        IconPackStyle.CLASSIC_ROUND, IconPackStyle.SQUIRCLE, IconPackStyle.PEBBLE -> {
            // Distinct shapes with subtle tinted background or vibrant color
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
