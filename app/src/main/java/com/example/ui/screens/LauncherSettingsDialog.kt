package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppIconType
import com.example.model.AppItem
import com.example.model.HomeScreenWidget
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.LauncherPreferences
import com.example.model.ThemeMode
import com.example.ui.components.AppIconGraphic
import com.example.ui.components.getIconShape
import com.example.ui.theme.AuroraBodyFontFamily
import com.example.ui.theme.AuroraDisplayFontFamily
import com.example.ui.theme.isLauncherDarkTheme
import com.example.util.ImportedIconPackState

private enum class SettingsPage(val title: String) {
    MAIN("Aurora Settings"),
    CUSTOMISATION("Customisation"),
    ADVANCED_CUSTOMISATION("Advanced customisation"),
    FEATURES("Features"),
    GENERAL("General"),
    ABOUT("About")
}

@Composable
fun LauncherSettingsDialog(
    preferences: LauncherPreferences,
    hiddenApps: List<AppItem>,
    allApps: List<AppItem> = emptyList(),
    previewApps: List<AppItem> = emptyList(),
    importedIconPack: ImportedIconPackState = ImportedIconPackState(),
    isImportingIconPack: Boolean = false,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit,
    onImportIconPackFiles: (List<Uri>) -> Unit = {},
    onImportIconPackFolder: (Uri) -> Unit = {},
    onAssignAppIcon: (String, String?) -> Unit = { _, _ -> },
    onClearImportedIconPack: () -> Unit = {},
    onUpdatePreferences: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onUnhideApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    var showCustomThemeModal by remember { mutableStateOf(false) }
    var showIconPackModal by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportIconPackFiles(uris)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onImportIconPackFolder(treeUri)
        }
    }

    val launchFileImport: () -> Unit = {
        filePickerLauncher.launch(arrayOf("*/*"))
    }
    val launchFolderImport: () -> Unit = {
        folderPickerLauncher.launch(null)
    }

    val palette = rememberAuroraSettingsPalette(preferences)

    val handleBack: () -> Unit = {
        when (currentPage) {
            SettingsPage.MAIN -> onDismiss()
            SettingsPage.ADVANCED_CUSTOMISATION -> currentPage = SettingsPage.CUSTOMISATION
            else -> currentPage = SettingsPage.MAIN
        }
    }

    BackHandler(onBack = handleBack)

    CompositionLocalProvider(LocalAuroraSettingsPalette provides palette) {
        Surface(
            color = palette.bgDeep,
            modifier = Modifier
                .fillMaxSize()
                .testTag("launcher_settings_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                AuroraSettingsHeader(
                    title = currentPage.title,
                    showBack = currentPage != SettingsPage.MAIN,
                    onBack = handleBack,
                    onClose = onDismiss
                )

                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        if (forward) {
                            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 3 } + fadeOut())
                        }
                    },
                    label = "aurora_settings_page",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        SettingsPage.MAIN -> MainSettingsPage(
                            onOpenCustomisation = { currentPage = SettingsPage.CUSTOMISATION },
                            onOpenGeneral = { currentPage = SettingsPage.GENERAL },
                            onOpenFeatures = { currentPage = SettingsPage.FEATURES },
                            onOpenAbout = { currentPage = SettingsPage.ABOUT }
                        )

                        SettingsPage.CUSTOMISATION -> CustomisationPage(
                            preferences = preferences,
                            previewApps = previewApps,
                            onSelectIconShape = { shape ->
                                onUpdatePreferences { it.copy(iconShape = shape) }
                            },
                            onSelectThemeMode = { mode ->
                                onUpdatePreferences { it.copy(themeMode = mode) }
                            },
                            onOpenCustomTheme = { showCustomThemeModal = true },
                            onOpenAdvanced = { currentPage = SettingsPage.ADVANCED_CUSTOMISATION }
                        )

                        SettingsPage.ADVANCED_CUSTOMISATION -> AdvancedCustomisationPage(
                            preferences = preferences,
                            importedIconPack = importedIconPack,
                            isImportingIconPack = isImportingIconPack,
                            onImportFromFiles = launchFileImport,
                            onImportFromFolder = launchFolderImport,
                            onOpenIconPack = { showIconPackModal = true },
                            onAllowThirdPartyWidgets = { allowed ->
                                onUpdatePreferences { it.copy(allowThirdPartyWidgets = allowed) }
                            }
                        )

                        SettingsPage.FEATURES -> FeaturesPage(
                            preferences = preferences,
                            onToggleSearch = { enabled ->
                                onUpdatePreferences { it.copy(enableSmartSearch = enabled) }
                            },
                            onToggleProductivityShelf = { enabled ->
                                onUpdatePreferences { it.copy(enableProductivityShelf = enabled) }
                            },
                            onToggleAtAGlance = { enabled ->
                                val updated = if (enabled) {
                                    preferences.enabledWidgets + HomeScreenWidget.AT_A_GLANCE.id
                                } else {
                                    preferences.enabledWidgets - HomeScreenWidget.AT_A_GLANCE.id
                                }
                                onUpdatePreferences { it.copy(enabledWidgets = updated) }
                            }
                        )

                        SettingsPage.GENERAL -> GeneralPage(
                            isDefaultLauncher = isDefaultLauncher,
                            onSetDefaultLauncher = onSetDefaultLauncher
                        )

                        SettingsPage.ABOUT -> AboutPage()
                    }
                }
            }
        }

        if (showCustomThemeModal) {
            AuroraCustomThemeModal(
                preferences = preferences,
                onUpdatePreferences = onUpdatePreferences,
                onDismiss = { showCustomThemeModal = false }
            )
        }

        if (showIconPackModal) {
            AuroraIconPackModal(
                preferences = preferences,
                hiddenApps = hiddenApps,
                allApps = allApps,
                importedIconPack = importedIconPack,
                onImportFilesClick = launchFileImport,
                onImportFolderClick = launchFolderImport,
                onAssignAppIcon = onAssignAppIcon,
                onClearImportedPack = onClearImportedIconPack,
                onUpdatePreferences = onUpdatePreferences,
                onUnhideApp = onUnhideApp,
                onDismiss = { showIconPackModal = false }
            )
        }
    }
}

@Composable
private fun MainSettingsPage(
    onOpenCustomisation: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Aurora3DPillButton(
                text = "Customisation",
                textBrush = CustomisationTitleBrush,
                onClick = onOpenCustomisation,
                height = 68.dp,
                fontSize = 27.sp,
                testTagName = "settings_menu_customisation"
            )

            Aurora3DPillButton(
                text = "General",
                onClick = onOpenGeneral,
                height = 68.dp,
                fontSize = 27.sp,
                testTagName = "settings_menu_general"
            )

            Aurora3DPillButton(
                text = "Features",
                onClick = onOpenFeatures,
                height = 68.dp,
                fontSize = 27.sp,
                testTagName = "settings_menu_features"
            )

            Aurora3DPillButton(
                text = "About",
                onClick = onOpenAbout,
                height = 68.dp,
                fontSize = 27.sp,
                testTagName = "settings_menu_about"
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Aurora launcher by Twilight",
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = palette.onBg
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "© 2026 Twilight. All rights reserved.",
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = palette.onBg.copy(alpha = 0.85f)
                )
            )
        }
    }
}

@Composable
private fun CustomisationPage(
    preferences: LauncherPreferences,
    previewApps: List<AppItem>,
    onSelectIconShape: (IconShape) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onOpenCustomTheme: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current
    val context = LocalContext.current
    val isDarkPreview = isLauncherDarkTheme(preferences.themeMode)
    val fallbackPreviewApps = remember {
        listOf(
            AppItem("preview_settings", "com.aurora.launcher.settings", label = "Aurora", category = AppCategory.TOOLS, iconColor = 0xFF7C4DFF, iconType = AppIconType.BUILTIN_CODE, iconGlyphName = "settings"),
            AppItem("preview_phone", "preview.phone", label = "Phone", category = AppCategory.COMMUNICATION, iconColor = 0xFF1976D2, iconType = AppIconType.BUILTIN_CODE, iconGlyphName = "phone"),
            AppItem("preview_msg", "preview.msg", label = "Messages", category = AppCategory.COMMUNICATION, iconColor = 0xFF0288D1, iconType = AppIconType.BUILTIN_CODE, iconGlyphName = "message"),
            AppItem("preview_cam", "preview.cam", label = "Camera", category = AppCategory.MEDIA, iconColor = 0xFFEA4335, iconType = AppIconType.BUILTIN_CODE, iconGlyphName = "camera")
        )
    }
    val displayedPreviewApps = remember(previewApps) {
        if (previewApps.size >= 4) previewApps.take(4)
        else (previewApps + fallbackPreviewApps).distinctBy { it.packageName }.take(4)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // App Preview Card
        AuroraCardPanel(padding = 14.dp) {
            Aurora3DPillHeader(
                text = "App Preview",
                height = 50.dp,
                fontSize = 23.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            val previewBg = when {
                preferences.themeMode == ThemeMode.AMOLED_BLACK -> Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF0D0D12)))
                isDarkPreview -> Brush.verticalGradient(listOf(palette.innerTray, palette.bgDeep))
                else -> Brush.verticalGradient(listOf(Color(0xFFEAF0FF), Color(0xFFF6F8FF)))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(previewBg)
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    displayedPreviewApps.forEach { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(64.dp)
                        ) {
                            AppIconGraphic(
                                app = app,
                                style = preferences.iconPackStyle,
                                size = 54.dp,
                                shape = getIconShape(preferences.iconPackStyle, preferences.iconShape),
                                context = context
                            )
                            if (preferences.showAppLabels) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = app.displayLabel,
                                    style = TextStyle(
                                        fontFamily = AuroraBodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = if (isDarkPreview) Color.White else Color(0xFF1A1C22)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4 Icon Shape Buttons Row (Squircle, Circle, Flower, Star)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconShape.entries.forEach { shape ->
                val isSelected = preferences.iconShape == shape
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("icon_shape_${shape.name}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.pillLip)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.pillFace)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, palette.highlightAccent, RoundedCornerShape(18.dp))
                                    else Modifier
                                )
                                .clickable { onSelectIconShape(shape) }
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(getIconShape(preferences.iconPackStyle, shape))
                                    .background(palette.onPill)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shape.label,
                        style = TextStyle(
                            fontFamily = AuroraDisplayFontFamily,
                            fontSize = 14.sp,
                            color = if (isSelected) palette.highlightAccent else palette.onBg
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        // Launcher Theme Card (White, Black, System, Custom)
        AuroraCardPanel(padding = 14.dp) {
            Aurora3DPillHeader(
                text = "Launcher theme",
                height = 48.dp,
                fontSize = 23.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeSwatchCard(
                    label = "White",
                    isSelected = preferences.themeMode == ThemeMode.CLEAN_LIGHT,
                    onClick = { onSelectThemeMode(ThemeMode.CLEAN_LIGHT) },
                    modifier = Modifier.weight(1f).testTag("theme_mode_${ThemeMode.CLEAN_LIGHT.name}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                ThemeSwatchCard(
                    label = "Black",
                    isSelected = preferences.themeMode == ThemeMode.AMOLED_BLACK,
                    onClick = { onSelectThemeMode(ThemeMode.AMOLED_BLACK) },
                    modifier = Modifier.weight(1f).testTag("theme_mode_${ThemeMode.AMOLED_BLACK.name}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                ThemeSwatchCard(
                    label = "System",
                    isSelected = preferences.themeMode == ThemeMode.SYSTEM,
                    onClick = { onSelectThemeMode(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f).testTag("theme_mode_${ThemeMode.SYSTEM.name}")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color.White)
                        val blackTriangle = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path = blackTriangle, color = Color.Black)
                    }
                }

                ThemeSwatchCard(
                    label = "Custom",
                    isSelected = preferences.themeMode == ThemeMode.MINIMAL_DARK,
                    onClick = onOpenCustomTheme,
                    modifier = Modifier.weight(1f).testTag("theme_mode_CUSTOM")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFFFF3D57),
                                            Color(0xFFFFCA28),
                                            Color(0xFF66BB6A),
                                            Color(0xFF29B6F6),
                                            Color(0xFFAB47BC),
                                            Color(0xFFFF3D57)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        Aurora3DPillButton(
            text = "Advanced customisation",
            onClick = onOpenAdvanced,
            height = 54.dp,
            fontSize = 20.sp,
            testTagName = "settings_advanced_customisation_btn"
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ThemeSwatchCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchContent: @Composable () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (isSelected) Modifier.border(3.dp, palette.highlightAccent, RoundedCornerShape(18.dp))
                    else Modifier.border(1.5.dp, palette.onCard.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                )
                .clickable(onClick = onClick)
        ) {
            swatchContent()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = TextStyle(
                fontFamily = AuroraDisplayFontFamily,
                fontSize = 14.sp,
                color = if (isSelected) palette.highlightAccent else palette.onCard
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun AdvancedCustomisationPage(
    preferences: LauncherPreferences,
    importedIconPack: ImportedIconPackState,
    isImportingIconPack: Boolean,
    onImportFromFiles: () -> Unit,
    onImportFromFolder: () -> Unit,
    onOpenIconPack: () -> Unit,
    onAllowThirdPartyWidgets: (Boolean) -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current
    val importSubtitle = when {
        isImportingIconPack -> "Importing icon pack from files..."
        importedIconPack.totalIcons > 0 && preferences.iconPackStyle == IconPackStyle.IMPORTED_CUSTOM ->
            "Active: ${importedIconPack.packName ?: "Custom Pack"} (${importedIconPack.matchedCount}/${importedIconPack.totalIcons} matched) • Tap to import"
        importedIconPack.totalIcons > 0 ->
            "Saved: ${importedIconPack.packName ?: "Custom Pack"} • Active style: ${preferences.iconPackStyle.label}"
        else ->
            "Tap to import .zip, .apk, or icon images from your files"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        AuroraCardPanel(padding = 18.dp) {
            Aurora3DPillHeader(
                text = "Icon pack",
                height = 50.dp,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            DashedUploadBox(
                subtitle = importSubtitle,
                isLoading = isImportingIconPack,
                onClick = onImportFromFiles,
                modifier = Modifier.testTag("settings_icon_pack_box")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Aurora3DPillButton(
                    text = "Pick folder",
                    onClick = onImportFromFolder,
                    height = 42.dp,
                    fontSize = 14.sp,
                    testTagName = "settings_icon_pack_folder_btn",
                    modifier = Modifier.weight(1f)
                )
                Aurora3DPillButton(
                    text = "Styles & map",
                    onClick = onOpenIconPack,
                    height = 42.dp,
                    fontSize = 14.sp,
                    testTagName = "settings_icon_pack_styles_btn",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AuroraCardPanel(padding = 18.dp) {
            Aurora3DPillHeader(
                text = "Allow third party widgets",
                height = 50.dp,
                fontSize = 19.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.innerTray)
                    .padding(16.dp)
            ) {
                AuroraAllowDenyButtons(
                    isAllowed = preferences.allowThirdPartyWidgets,
                    onAllow = { onAllowThirdPartyWidgets(true) },
                    onDeny = { onAllowThirdPartyWidgets(false) },
                    compact = false,
                    allowTag = "third_party_widgets_allow",
                    denyTag = "third_party_widgets_deny"
                )
            }
        }
    }
}

@Composable
private fun FeaturesPage(
    preferences: LauncherPreferences,
    onToggleSearch: (Boolean) -> Unit,
    onToggleProductivityShelf: (Boolean) -> Unit,
    onToggleAtAGlance: (Boolean) -> Unit
) {
    val isAtAGlanceEnabled = HomeScreenWidget.AT_A_GLANCE.id in preferences.enabledWidgets

    Column(
        verticalArrangement = Arrangement.spacedBy(36.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        FeatureTogglePillRow(
            title = "Search",
            titleSize = 26.sp,
            isAllowed = preferences.enableSmartSearch,
            onAllow = { onToggleSearch(true) },
            onDeny = { onToggleSearch(false) },
            allowTag = "feature_search_allow",
            denyTag = "feature_search_deny"
        )

        FeatureTogglePillRow(
            title = "Productivity\nshelf",
            titleSize = 20.sp,
            isAllowed = preferences.enableProductivityShelf,
            onAllow = { onToggleProductivityShelf(true) },
            onDeny = { onToggleProductivityShelf(false) },
            allowTag = "feature_shelf_allow",
            denyTag = "feature_shelf_deny"
        )

        FeatureTogglePillRow(
            title = "At a glance\nwidget",
            titleSize = 20.sp,
            isAllowed = isAtAGlanceEnabled,
            onAllow = { onToggleAtAGlance(true) },
            onDeny = { onToggleAtAGlance(false) },
            allowTag = "feature_at_a_glance_allow",
            denyTag = "feature_at_a_glance_deny"
        )
    }
}

@Composable
private fun FeatureTogglePillRow(
    title: String,
    titleSize: TextUnit,
    isAllowed: Boolean,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    allowTag: String,
    denyTag: String
) {
    val palette = LocalAuroraSettingsPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.pillLip)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(69.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.pillFace)
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = AuroraDisplayFontFamily,
                    fontSize = titleSize,
                    lineHeight = (titleSize.value + 3).sp,
                    color = palette.onPill
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )

            AuroraAllowDenyButtons(
                isAllowed = isAllowed,
                onAllow = onAllow,
                onDeny = onDeny,
                compact = true,
                allowTag = allowTag,
                denyTag = denyTag
            )
        }
    }
}

@Composable
private fun GeneralPage(
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        AuroraCardPanel(padding = 20.dp) {
            Aurora3DPillHeader(
                text = "Default launcher",
                height = 56.dp,
                fontSize = 25.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.innerTray)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_aurora_icon),
                    contentDescription = "Aurora Launcher Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Aurora3DPillButton(
                text = if (isDefaultLauncher) "Default active ✓" else "Set as default",
                onClick = onSetDefaultLauncher,
                height = 58.dp,
                fontSize = 24.sp,
                testTagName = "set_default_launcher_button"
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AboutPage() {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        AuroraCardPanel(padding = 20.dp) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.innerTray)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_aurora_icon),
                    contentDescription = "Aurora Launcher Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Aurora3DPillHeader(
                text = "Aurora launcher 1.0",
                height = 56.dp,
                fontSize = 23.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Aurora Launcher is a customizable native Android launcher built with Kotlin, featuring gesture controls, custom icon packs, a password-protected App Vault, and a personalized home screen experience.",
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = palette.onCard
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "© 2026 Twilight. All rights reserved.",
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = palette.onCard.copy(alpha = 0.85f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
