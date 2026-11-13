package com.example.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.model.ColorPaletteSeed
import com.example.model.DrawerLayoutMode
import com.example.model.IconPackStyle
import com.example.model.IconSize
import com.example.model.LauncherPreferences
import com.example.model.ThemeMode
import com.example.ui.theme.AuroraBodyFontFamily
import com.example.ui.theme.AuroraDisplayFontFamily
import com.example.ui.theme.isLauncherDarkTheme
import com.example.util.IconPackManager
import com.example.util.ImportedIconPackState

@Immutable
internal data class AuroraSettingsPalette(
    val bgDeep: Color,
    val headerBg: Color,
    val headerShadow: Color,
    val onHeader: Color,
    val cardSurface: Color,
    val cardLip: Color,
    val innerTray: Color,
    val pillFace: Color,
    val pillLip: Color,
    val onPill: Color,
    val onBg: Color,
    val onCard: Color,
    val onTray: Color,
    val highlightAccent: Color,
    val selectionRing: Color
)

private val DefaultTwilightPalette = AuroraSettingsPalette(
    bgDeep = Color(0xFF280654),
    headerBg = Color(0xFF5615C9),
    headerShadow = Color(0xFF1D0440),
    onHeader = Color.White,
    cardSurface = Color(0xFF3E0B87),
    cardLip = Color(0xFF22054B),
    innerTray = Color(0xFF2E0768),
    pillFace = Color(0xFF631DEB),
    pillLip = Color(0xFF350988),
    onPill = Color.White,
    onBg = Color.White,
    onCard = Color.White,
    onTray = Color.White,
    highlightAccent = Color(0xFFFFDF59),
    selectionRing = Color.White
)

internal val LocalAuroraSettingsPalette = staticCompositionLocalOf { DefaultTwilightPalette }

internal val SettingsAllowFace = Color(0xFF00BA54)
internal val SettingsAllowLip = Color(0xFF007D37)
internal val SettingsDenyFace = Color(0xFFFF3830)
internal val SettingsDenyLip = Color(0xFFB51A14)

internal val CustomisationTitleBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFF72A8),
        Color(0xFFFF9868),
        Color(0xFFFFDF59)
    )
)

@Composable
internal fun rememberAuroraSettingsPalette(preferences: LauncherPreferences): AuroraSettingsPalette {
    val isDark = isLauncherDarkTheme(preferences.themeMode)
    val isAmoled = preferences.themeMode == ThemeMode.AMOLED_BLACK
    val dynamicPrimary = MaterialTheme.colorScheme.primary

    val seedAccent = if (preferences.themeMode == ThemeMode.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dynamicPrimary.luminance() > 0.55f) lerp(dynamicPrimary, Color.Black, 0.35f) else dynamicPrimary
    } else {
        when (preferences.colorPaletteSeed) {
            ColorPaletteSeed.LAVENDER -> Color(0xFF631DEB)
            ColorPaletteSeed.OCEAN_BLUE -> Color(0xFF1976D2)
            ColorPaletteSeed.BOTANICAL_SAGE -> Color(0xFF2E7D32)
            ColorPaletteSeed.SUNSET_AMBER -> Color(0xFFD84315)
            ColorPaletteSeed.ROSE_BERRY -> Color(0xFFC2185B)
            ColorPaletteSeed.MINIMAL_MONO -> Color(0xFF455A64)
        }
    }

    val target = when {
        !isDark -> {
            // White / Clean Light (or System Light)
            AuroraSettingsPalette(
                bgDeep = lerp(Color(0xFFF3F5FC), seedAccent, 0.08f),
                headerBg = lerp(Color.White, seedAccent, 0.14f),
                headerShadow = lerp(Color(0xFFD2D8EA), seedAccent, 0.24f),
                onHeader = Color(0xFF1A1C24),
                cardSurface = Color.White,
                cardLip = lerp(Color(0xFFD6DCF0), seedAccent, 0.22f),
                innerTray = lerp(Color(0xFFECEFF8), seedAccent, 0.12f),
                pillFace = seedAccent,
                pillLip = lerp(seedAccent, Color.Black, 0.38f),
                onPill = Color.White,
                onBg = Color(0xFF1A1C24),
                onCard = Color(0xFF1A1C24),
                onTray = Color(0xFF222634),
                highlightAccent = seedAccent,
                selectionRing = Color(0xFF1A1C24)
            )
        }
        isAmoled -> {
            // Black / AMOLED Pure Black
            AuroraSettingsPalette(
                bgDeep = Color(0xFF000000),
                headerBg = lerp(Color(0xFF0C0C10), seedAccent, 0.18f),
                headerShadow = Color(0xFF000000),
                onHeader = Color.White,
                cardSurface = Color(0xFF121217),
                cardLip = Color(0xFF040406),
                innerTray = Color(0xFF08080C),
                pillFace = lerp(Color(0xFF1B1C24), seedAccent, 0.42f),
                pillLip = Color(0xFF08080C),
                onPill = Color.White,
                onBg = Color.White,
                onCard = Color.White,
                onTray = Color.White,
                highlightAccent = Color(0xFFFFDF59),
                selectionRing = Color.White
            )
        }
        preferences.themeMode == ThemeMode.MINIMAL_DARK && preferences.colorPaletteSeed == ColorPaletteSeed.LAVENDER -> {
            // Signature Twilight Purple palette matching the mockups
            DefaultTwilightPalette
        }
        else -> {
            // Custom Dark Palette Seed (or System Dark)
            AuroraSettingsPalette(
                bgDeep = lerp(Color(0xFF090A0F), seedAccent, 0.24f),
                headerBg = lerp(Color(0xFF101218), seedAccent, 0.72f),
                headerShadow = lerp(Color.Black, seedAccent, 0.14f),
                onHeader = Color.White,
                cardSurface = lerp(Color(0xFF11131A), seedAccent, 0.36f),
                cardLip = lerp(Color.Black, seedAccent, 0.18f),
                innerTray = lerp(Color(0xFF0A0B10), seedAccent, 0.25f),
                pillFace = lerp(Color(0xFF151822), seedAccent, 0.80f),
                pillLip = lerp(Color.Black, seedAccent, 0.44f),
                onPill = Color.White,
                onBg = Color.White,
                onCard = Color.White,
                onTray = Color.White,
                highlightAccent = Color(0xFFFFDF59),
                selectionRing = Color.White
            )
        }
    }

    val animSpec = tween<Color>(durationMillis = 220)
    val bgDeep by animateColorAsState(target.bgDeep, animSpec, label = "settings_bg")
    val headerBg by animateColorAsState(target.headerBg, animSpec, label = "settings_header_bg")
    val headerShadow by animateColorAsState(target.headerShadow, animSpec, label = "settings_header_shadow")
    val onHeader by animateColorAsState(target.onHeader, animSpec, label = "settings_on_header")
    val cardSurface by animateColorAsState(target.cardSurface, animSpec, label = "settings_card")
    val cardLip by animateColorAsState(target.cardLip, animSpec, label = "settings_card_lip")
    val innerTray by animateColorAsState(target.innerTray, animSpec, label = "settings_tray")
    val pillFace by animateColorAsState(target.pillFace, animSpec, label = "settings_pill")
    val pillLip by animateColorAsState(target.pillLip, animSpec, label = "settings_pill_lip")
    val onPill by animateColorAsState(target.onPill, animSpec, label = "settings_on_pill")
    val onBg by animateColorAsState(target.onBg, animSpec, label = "settings_on_bg")
    val onCard by animateColorAsState(target.onCard, animSpec, label = "settings_on_card")
    val onTray by animateColorAsState(target.onTray, animSpec, label = "settings_on_tray")
    val highlightAccent by animateColorAsState(target.highlightAccent, animSpec, label = "settings_highlight")
    val selectionRing by animateColorAsState(target.selectionRing, animSpec, label = "settings_ring")

    return AuroraSettingsPalette(
        bgDeep = bgDeep,
        headerBg = headerBg,
        headerShadow = headerShadow,
        onHeader = onHeader,
        cardSurface = cardSurface,
        cardLip = cardLip,
        innerTray = innerTray,
        pillFace = pillFace,
        pillLip = pillLip,
        onPill = onPill,
        onBg = onBg,
        onCard = onCard,
        onTray = onTray,
        highlightAccent = highlightAccent,
        selectionRing = selectionRing
    )
}

@Composable
internal fun AuroraSettingsHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.headerBg)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 14.dp)
        ) {
            if (showBack) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .minimumInteractiveComponentSize()
                        .testTag("settings_back_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(palette.pillLip)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(palette.pillFace)
                                .clickable(onClick = onBack)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = palette.onPill,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = title,
                style = TextStyle(
                    fontFamily = AuroraDisplayFontFamily,
                    fontSize = if (title.length > 18) 21.sp else 25.sp,
                    color = palette.onHeader
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = if (showBack) 64.dp else 44.dp)
            )

            if (!showBack) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = palette.onHeader.copy(alpha = 0.85f)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(palette.headerShadow)
        )
    }
}

@Composable
internal fun Aurora3DPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    cornerRadius: Dp = 26.dp,
    fontSize: TextUnit = 26.sp,
    textBrush: Brush? = null,
    faceColor: Color? = null,
    lipColor: Color? = null,
    testTagName: String? = null
) {
    val palette = LocalAuroraSettingsPalette.current
    val resolvedFace = faceColor ?: palette.pillFace
    val resolvedLip = lipColor ?: palette.pillLip

    val baseMod = modifier
        .fillMaxWidth()
        .height(height + 5.dp)
        .minimumInteractiveComponentSize()
        .let { if (testTagName != null) it.testTag(testTagName) else it }

    Box(modifier = baseMod) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(resolvedLip)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(resolvedFace)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = text,
                style = if (textBrush != null) {
                    TextStyle(
                        brush = textBrush,
                        fontFamily = AuroraDisplayFontFamily,
                        fontSize = fontSize
                    )
                } else {
                    TextStyle(
                        color = palette.onPill,
                        fontFamily = AuroraDisplayFontFamily,
                        fontSize = fontSize
                    )
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun Aurora3DPillHeader(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    fontSize: TextUnit = 22.sp
) {
    val palette = LocalAuroraSettingsPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.pillLip)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.pillFace)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = AuroraDisplayFontFamily,
                    fontSize = fontSize,
                    color = palette.onPill
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun AuroraCardPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.cardLip)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.cardSurface)
                .padding(padding),
            content = content
        )
    }
}

@Composable
internal fun AuroraAllowDenyButtons(
    isAllowed: Boolean,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    compact: Boolean = false,
    allowTag: String = "allow_btn",
    denyTag: String = "deny_btn"
) {
    val btnHeight = if (compact) 42.dp else 86.dp
    val btnRadius = if (compact) 21.dp else 18.dp
    val fontSize = if (compact) 13.sp else 22.sp

    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (compact) Modifier else Modifier.fillMaxWidth()
    ) {
        val allowModifier = if (compact) Modifier.width(72.dp) else Modifier.weight(1f)
        val denyModifier = if (compact) Modifier.width(72.dp) else Modifier.weight(1f)

        AuroraActionToggleButton(
            label = "Allow",
            selected = isAllowed,
            faceColor = SettingsAllowFace,
            lipColor = SettingsAllowLip,
            height = btnHeight,
            cornerRadius = btnRadius,
            fontSize = fontSize,
            onClick = onAllow,
            modifier = allowModifier.testTag(allowTag)
        )

        AuroraActionToggleButton(
            label = "Deny",
            selected = !isAllowed,
            faceColor = SettingsDenyFace,
            lipColor = SettingsDenyLip,
            height = btnHeight,
            cornerRadius = btnRadius,
            fontSize = fontSize,
            onClick = onDeny,
            modifier = denyModifier.testTag(denyTag)
        )
    }
}

@Composable
private fun AuroraActionToggleButton(
    label: String,
    selected: Boolean,
    faceColor: Color,
    lipColor: Color,
    height: Dp,
    cornerRadius: Dp,
    fontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAuroraSettingsPalette.current

    Box(
        modifier = modifier
            .height(height + 4.dp)
            .minimumInteractiveComponentSize()
            .alpha(if (selected) 1f else 0.56f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(lipColor)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(faceColor)
                .then(
                    if (selected) Modifier.border(2.5.dp, palette.selectionRing, RoundedCornerShape(cornerRadius))
                    else Modifier
                )
                .clickable(onClick = onClick)
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = AuroraDisplayFontFamily,
                    fontSize = fontSize,
                    color = Color.White
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun DashedUploadBox(
    subtitle: String,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAuroraSettingsPalette.current
    val strokeColor = palette.onCard

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(144.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(
                    width = 5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)
                )
                drawRoundRect(
                    color = strokeColor,
                    style = stroke,
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    color = strokeColor,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Canvas(modifier = Modifier.size(84.dp)) {
                    val w = size.width
                    val h = size.height
                    val strokePx = 10.dp.toPx()
                    drawLine(
                        color = strokeColor,
                        start = Offset(w / 2f, h * 0.08f),
                        end = Offset(w / 2f, h * 0.58f),
                        strokeWidth = strokePx,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(w * 0.30f, h * 0.42f),
                        end = Offset(w / 2f, h * 0.62f),
                        strokeWidth = strokePx,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(w * 0.70f, h * 0.42f),
                        end = Offset(w / 2f, h * 0.62f),
                        strokeWidth = strokePx,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    val trayPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.12f, h * 0.60f)
                        lineTo(w * 0.12f, h * 0.84f)
                        quadraticTo(w * 0.12f, h * 0.92f, w * 0.22f, h * 0.92f)
                        lineTo(w * 0.78f, h * 0.92f)
                        quadraticTo(w * 0.88f, h * 0.92f, w * 0.88f, h * 0.84f)
                        lineTo(w * 0.88f, h * 0.60f)
                    }
                    drawPath(
                        path = trayPath,
                        color = strokeColor,
                        style = Stroke(
                            width = strokePx,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                fontFamily = AuroraBodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = palette.onCard.copy(alpha = 0.88f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AuroraCustomThemeModal(
    preferences: LauncherPreferences,
    onUpdatePreferences: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
        titleContentColor = palette.onCard,
        textContentColor = palette.onCard,
        title = {
            Text(
                text = "Custom Theme & Colors",
                fontFamily = AuroraDisplayFontFamily,
                fontSize = 21.sp,
                color = palette.onCard
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Theme Mode",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = preferences.themeMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdatePreferences { it.copy(themeMode = mode) } },
                            label = { Text(mode.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.pillFace,
                                selectedLabelColor = palette.onPill,
                                containerColor = palette.innerTray,
                                labelColor = palette.onTray.copy(alpha = 0.88f)
                            )
                        )
                    }
                }

                Text(
                    text = "Accent Palette Seed",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorPaletteSeed.entries.forEach { seed ->
                        val selected = preferences.colorPaletteSeed == seed
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(seed.primaryColor))
                                .then(
                                    if (selected) Modifier.border(3.dp, palette.selectionRing, CircleShape)
                                    else Modifier
                                )
                                .clickable {
                                    onUpdatePreferences { it.copy(colorPaletteSeed = seed) }
                                }
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = seed.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Wallpaper",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Android System", "Cosmic Sky", "Velvet Lavender", "Emerald Mist", "Minimal Pure").forEachIndexed { idx, name ->
                        val selected = preferences.wallpaperThemeIndex == idx
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdatePreferences { it.copy(wallpaperThemeIndex = idx) } },
                            label = { Text(name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.pillFace,
                                selectedLabelColor = palette.onPill,
                                containerColor = palette.innerTray,
                                labelColor = palette.onTray.copy(alpha = 0.88f)
                            )
                        )
                    }
                }
                val ctx = LocalContext.current
                OutlinedButton(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SET_WALLPAPER),
                                    "Choose Android Wallpaper"
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = palette.onCard,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Change Android Wallpaper",
                        color = palette.onCard,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = palette.highlightAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AuroraIconPackModal(
    preferences: LauncherPreferences,
    hiddenApps: List<AppItem>,
    allApps: List<AppItem>,
    importedIconPack: ImportedIconPackState,
    onImportFilesClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onAssignAppIcon: (String, String?) -> Unit,
    onClearImportedPack: () -> Unit,
    onUpdatePreferences: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onUnhideApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current
    var showMapperDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
        titleContentColor = palette.onCard,
        textContentColor = palette.onCard,
        title = {
            Text(
                text = "Icon Pack & Layout",
                fontFamily = AuroraDisplayFontFamily,
                fontSize = 21.sp,
                color = palette.onCard
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Import Icon Pack from Files",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.innerTray)
                        .padding(12.dp)
                ) {
                    if (importedIconPack.totalIcons > 0) {
                        Text(
                            text = "${importedIconPack.packName ?: "Custom Pack"}",
                            fontFamily = AuroraBodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = palette.onTray,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${importedIconPack.totalIcons} icons loaded • ${importedIconPack.matchedCount} apps matched",
                            fontFamily = AuroraBodyFontFamily,
                            color = palette.onTray.copy(alpha = 0.78f),
                            fontSize = 11.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { showMapperDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Assign Icons",
                                    color = palette.highlightAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(
                                onClick = onClearImportedPack
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove imported icon pack",
                                    tint = SettingsDenyFace,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remove",
                                    color = SettingsDenyFace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Select a .zip, .apk, icon image(s), or an extracted icon folder.",
                            fontFamily = AuroraBodyFontFamily,
                            color = palette.onTray.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onImportFilesClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = palette.onTray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick File", color = palette.onTray, fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onImportFolderClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = palette.onTray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick Folder", color = palette.onTray, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Text(
                    text = "Icon Pack Style",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconPackStyle.entries.forEach { style ->
                        val selected = preferences.iconPackStyle == style
                        val rowTextColor = if (selected) palette.onPill else palette.onTray
                        val subtitleText = if (style == IconPackStyle.IMPORTED_CUSTOM && importedIconPack.totalIcons == 0) {
                            "Tap to import .zip, .apk, or image icons from your files"
                        } else {
                            style.description
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) palette.pillFace else palette.innerTray)
                                .clickable {
                                    if (style == IconPackStyle.IMPORTED_CUSTOM && importedIconPack.totalIcons == 0) {
                                        onImportFilesClick()
                                    } else {
                                        onUpdatePreferences { it.copy(iconPackStyle = style) }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.label,
                                    fontFamily = AuroraBodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = rowTextColor,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = subtitleText,
                                    fontFamily = AuroraBodyFontFamily,
                                    color = rowTextColor.copy(alpha = 0.76f),
                                    fontSize = 11.sp
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = palette.onPill,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Icon Size",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconSize.entries.forEach { size ->
                        FilterChip(
                            selected = preferences.iconSize == size,
                            onClick = { onUpdatePreferences { it.copy(iconSize = size) } },
                            label = { Text(size.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.pillFace,
                                selectedLabelColor = palette.onPill,
                                containerColor = palette.innerTray,
                                labelColor = palette.onTray.copy(alpha = 0.88f)
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.innerTray)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Show App Labels",
                        fontFamily = AuroraBodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onTray,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = preferences.showAppLabels,
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(showAppLabels = checked) }
                        }
                    )
                }

                Text(
                    text = "Drawer Layout",
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DrawerLayoutMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.drawerLayoutMode == mode,
                            onClick = { onUpdatePreferences { it.copy(drawerLayoutMode = mode) } },
                            label = { Text(mode.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.pillFace,
                                selectedLabelColor = palette.onPill,
                                containerColor = palette.innerTray,
                                labelColor = palette.onTray.copy(alpha = 0.88f)
                            )
                        )
                    }
                }

                if (hiddenApps.isNotEmpty()) {
                    Text(
                        text = "Hidden Apps (${hiddenApps.size})",
                        fontFamily = AuroraBodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = palette.onCard.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                    hiddenApps.forEach { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.innerTray)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = app.displayLabel,
                                fontFamily = AuroraBodyFontFamily,
                                color = palette.onTray,
                                fontSize = 13.sp
                            )
                            IconButton(onClick = { onUnhideApp(app) }) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Unhide",
                                    tint = palette.highlightAccent
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = palette.highlightAccent, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showMapperDialog && importedIconPack.allIcons.isNotEmpty()) {
        AuroraManualIconAssignDialog(
            allApps = allApps,
            importedIconPack = importedIconPack,
            onAssignAppIcon = onAssignAppIcon,
            onDismiss = { showMapperDialog = false }
        )
    }
}

@Composable
private fun AuroraManualIconAssignDialog(
    allApps: List<AppItem>,
    importedIconPack: ImportedIconPackState,
    onAssignAppIcon: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
        titleContentColor = palette.onCard,
        textContentColor = palette.onCard,
        title = {
            Text(
                text = if (selectedApp == null) "Tap an App to Customize" else "Choose Icon for ${selectedApp!!.displayLabel}",
                fontFamily = AuroraDisplayFontFamily,
                fontSize = 18.sp,
                color = palette.onCard
            )
        },
        text = {
            if (selectedApp == null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allApps.forEach { app ->
                        val mappedPath = importedIconPack.packageToPath[app.packageName]
                        val mappedBmp = remember(mappedPath) {
                            mappedPath?.let { IconPackManager.decodeSampledBitmap(it, 96)?.asImageBitmap() }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.innerTray)
                                .clickable { selectedApp = app }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.displayLabel,
                                    fontFamily = AuroraBodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.onTray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (mappedPath != null) "Custom icon active" else "Default icon • Tap to assign",
                                    fontFamily = AuroraBodyFontFamily,
                                    color = palette.onTray.copy(alpha = 0.72f),
                                    fontSize = 11.sp
                                )
                            }
                            if (mappedBmp != null) {
                                Image(
                                    bitmap = mappedBmp,
                                    contentDescription = app.displayLabel,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            } else {
                val targetApp = selectedApp!!
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onAssignAppIcon(targetApp.packageName, null)
                            selectedApp = null
                        }
                    ) {
                        Text("Reset to Auto / Default", color = palette.highlightAccent, fontSize = 12.sp)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(importedIconPack.allIcons.take(200), key = { it.filePath }) { iconEntry ->
                            val bmp = remember(iconEntry.filePath) {
                                IconPackManager.decodeSampledBitmap(iconEntry.filePath, 128)?.asImageBitmap()
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.innerTray)
                                    .clickable {
                                        onAssignAppIcon(targetApp.packageName, iconEntry.filePath)
                                        selectedApp = null
                                    }
                                    .padding(6.dp)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = iconEntry.displayName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedApp != null) {
                TextButton(onClick = { selectedApp = null }) {
                    Text("Back", color = palette.highlightAccent, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = palette.highlightAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
