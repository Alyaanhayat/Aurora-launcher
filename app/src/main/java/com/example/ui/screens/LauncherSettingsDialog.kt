package com.example.ui.screens

import android.net.Uri
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppIconType
import com.example.model.AppItem
import com.example.model.DeviceMode
import com.example.model.HomeScreenWidget
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.LauncherPreferences
import com.example.model.TabletFolderBackdrop
import com.example.model.TabletFolderSize
import com.example.model.ThemeMode
import com.example.ui.components.AppIconGraphic
import com.example.ui.components.PurpleFailsafeCard
import com.example.ui.components.PurpleFailsafeDetailsDialog
import com.example.ui.components.RenderFailsafeMonitor
import com.example.ui.components.getIconShape
import com.example.ui.components.resolveTabletFolderBackdropStyle
import com.example.ui.theme.AuroraBodyFontFamily
import com.example.ui.theme.AuroraDisplayFontFamily
import com.example.ui.theme.isLauncherDarkTheme
import com.example.util.ImportedIconPackState

private enum class SettingsPage(val title: String) {
    MAIN("Aurora Settings"),
    CUSTOMISATION("Customisation"),
    TABLET_FOLDERS("Tablet expanded folders"),
    ADVANCED_CUSTOMISATION("Advanced customisation"),
    DEVICE_MODE("Device mode"),
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
    isTabletLayout: Boolean = false,
    onSetDefaultLauncher: () -> Unit,
    onImportIconPackFiles: (List<Uri>) -> Unit = {},
    onImportIconPackFolder: (Uri) -> Unit = {},
    onAssignAppIcon: (String, String?) -> Unit = { _, _ -> },
    onClearImportedIconPack: () -> Unit = {},
    onUpdatePreferences: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onUnhideApp: (AppItem) -> Unit,
    onOpenCrashAssistant: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    var tabletFoldersReturnPage by remember { mutableStateOf(SettingsPage.CUSTOMISATION) }
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
            SettingsPage.TABLET_FOLDERS -> currentPage = tabletFoldersReturnPage
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
                            isTabletLayout = isTabletLayout,
                            onOpenCustomisation = { currentPage = SettingsPage.CUSTOMISATION },
                            onOpenGeneral = { currentPage = SettingsPage.GENERAL },
                            onOpenDeviceMode = { currentPage = SettingsPage.DEVICE_MODE },
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
                            onOpenTabletFolders = {
                                tabletFoldersReturnPage = SettingsPage.CUSTOMISATION
                                currentPage = SettingsPage.TABLET_FOLDERS
                            },
                            onOpenAdvanced = { currentPage = SettingsPage.ADVANCED_CUSTOMISATION }
                        )

                        SettingsPage.TABLET_FOLDERS -> TabletExpandedFoldersPage(
                            preferences = preferences,
                            previewApps = previewApps,
                            onSelectFolderIconShape = { shape ->
                                onUpdatePreferences { it.copy(tabletExpandedFolderIconShape = shape) }
                            },
                            onSelectFolderBackdrop = { backdrop ->
                                onUpdatePreferences { it.copy(tabletFolderBackdrop = backdrop) }
                            },
                            onSelectTabletFolderSize = { size ->
                                onUpdatePreferences { it.copy(tabletFolderSize = size) }
                            }
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
                            },
                            onToggleDynamicWallpaperColors = { enabled ->
                                onUpdatePreferences { it.copy(dynamicWallpaperColors = enabled) }
                            }
                        )

                        SettingsPage.DEVICE_MODE -> DeviceModePage(
                            preferences = preferences,
                            isTabletLayout = isTabletLayout,
                            onSelectDeviceMode = { mode ->
                                onUpdatePreferences { it.copy(deviceMode = mode) }
                            },
                            onSelectTabletFolderSize = { size ->
                                onUpdatePreferences { it.copy(tabletFolderSize = size) }
                            },
                            onOpenTabletFolderCustomisation = {
                                tabletFoldersReturnPage = SettingsPage.DEVICE_MODE
                                currentPage = SettingsPage.TABLET_FOLDERS
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
                            },
                            onToggleHapticFeedback = { enabled ->
                                onUpdatePreferences { it.copy(hapticFeedbackEnabled = enabled) }
                            }
                        )

                        SettingsPage.GENERAL -> GeneralPage(
                            isDefaultLauncher = isDefaultLauncher,
                            onSetDefaultLauncher = onSetDefaultLauncher,
                            onOpenCrashAssistant = onOpenCrashAssistant
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
    isTabletLayout: Boolean,
    onOpenCustomisation: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenDeviceMode: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTabletLayout) 620.dp else Dp.Unspecified)
        ) {
            Aurora3DPillButton(
                text = "Customisation",
                textBrush = CustomisationTitleBrush,
                onClick = onOpenCustomisation,
                height = 60.dp,
                fontSize = 25.sp,
                testTagName = "settings_menu_customisation"
            )

            Aurora3DPillButton(
                text = "General",
                onClick = onOpenGeneral,
                height = 60.dp,
                fontSize = 25.sp,
                testTagName = "settings_menu_general"
            )

            Aurora3DPillButton(
                text = "Device mode",
                onClick = onOpenDeviceMode,
                height = 60.dp,
                fontSize = 25.sp,
                testTagName = "settings_menu_device_mode"
            )

            Aurora3DPillButton(
                text = "Features",
                onClick = onOpenFeatures,
                height = 60.dp,
                fontSize = 25.sp,
                testTagName = "settings_menu_features"
            )

            Aurora3DPillButton(
                text = "About",
                onClick = onOpenAbout,
                height = 60.dp,
                fontSize = 25.sp,
                testTagName = "settings_menu_about"
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
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
    onOpenTabletFolders: () -> Unit,
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
            text = "Tablet expanded folders",
            onClick = onOpenTabletFolders,
            height = 54.dp,
            fontSize = 19.sp,
            testTagName = "settings_tablet_folders_btn"
        )

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
private fun TabletExpandedFoldersPage(
    preferences: LauncherPreferences,
    previewApps: List<AppItem>,
    onSelectFolderIconShape: (IconShape?) -> Unit,
    onSelectFolderBackdrop: (TabletFolderBackdrop) -> Unit,
    onSelectTabletFolderSize: (TabletFolderSize) -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current
    val context = LocalContext.current
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
    val effectivePreviewShape = preferences.tabletExpandedFolderIconShape ?: preferences.iconShape
    val backdropStyle = resolveTabletFolderBackdropStyle(
        backdrop = preferences.tabletFolderBackdrop,
        colorScheme = MaterialTheme.colorScheme
    )
    val paletteSourceLabel = if (preferences.dynamicWallpaperColors) {
        "Material You • Synced with wallpaper"
    } else {
        "Material You • ${preferences.colorPaletteSeed.label}"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Live Material You Expanded Folder Preview
        AuroraCardPanel(padding = 14.dp) {
            Aurora3DPillHeader(
                text = "Tablet Folder Preview",
                height = 48.dp,
                fontSize = 21.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = palette.highlightAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = paletteSourceLabel,
                    style = TextStyle(
                        fontFamily = AuroraBodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = palette.onCard.copy(alpha = 0.88f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(backdropStyle.backgroundBrush)
                    .border(backdropStyle.borderWidth, backdropStyle.borderColor, RoundedCornerShape(24.dp))
                    .padding(16.dp)
                    .testTag("tablet_folder_live_preview")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = backdropStyle.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Productivity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = backdropStyle.contentColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(backdropStyle.chipFillColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${preferences.tabletFolderBackdrop.label} • ${preferences.tabletExpandedFolderIconShape?.label ?: "Home (${preferences.iconShape.label})"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = backdropStyle.accentColor
                            )
                        }
                    }

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
                                    size = 50.dp,
                                    shape = getIconShape(preferences.iconPackStyle, effectivePreviewShape),
                                    context = context
                                )
                                if (preferences.showAppLabels) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = app.displayLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = backdropStyle.contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded Folder Icon Shape Selector (Match Home or Override)
        AuroraCardPanel(padding = 16.dp) {
            Aurora3DPillHeader(
                text = "Expanded folder icon shape",
                height = 48.dp,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Choose a custom icon mask used inside expanded tablet folders, or match your Home screen icons.",
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = palette.onCard.copy(alpha = 0.86f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            val matchHomeSelected = preferences.tabletExpandedFolderIconShape == null
            Aurora3DPillButton(
                text = "Match Home icons (${preferences.iconShape.label})",
                onClick = { onSelectFolderIconShape(null) },
                height = 44.dp,
                fontSize = 15.sp,
                faceColor = if (matchHomeSelected) palette.pillFace else palette.innerTray,
                lipColor = if (matchHomeSelected) palette.pillLip else palette.cardLip,
                textColor = if (matchHomeSelected) palette.onPill else palette.onTray,
                testTagName = "tablet_folder_icon_shape_MATCH_HOME",
                modifier = if (matchHomeSelected) {
                    Modifier.border(2.5.dp, palette.highlightAccent, RoundedCornerShape(26.dp))
                } else {
                    Modifier
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconShape.entries.forEach { shape ->
                    val isSelected = preferences.tabletExpandedFolderIconShape == shape
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tablet_folder_icon_shape_${shape.name}")
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
                                    .background(if (isSelected) palette.pillLip else palette.cardLip)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) palette.pillFace else palette.innerTray)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, palette.highlightAccent, RoundedCornerShape(18.dp))
                                        else Modifier
                                    )
                                    .clickable { onSelectFolderIconShape(shape) }
                                    .padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(getIconShape(preferences.iconPackStyle, shape))
                                        .background(if (isSelected) palette.onPill else palette.onTray)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = shape.label,
                            style = TextStyle(
                                fontFamily = AuroraDisplayFontFamily,
                                fontSize = 13.sp,
                                color = if (isSelected) palette.highlightAccent else palette.onCard
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Material You Backdrop Styles Selector
        AuroraCardPanel(padding = 16.dp) {
            Aurora3DPillHeader(
                text = "Material You backdrop",
                height = 48.dp,
                fontSize = 19.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Each backdrop dynamically adapts to your active Material You palette.",
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = palette.onCard.copy(alpha = 0.86f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabletFolderBackdrop.entries.forEach { backdropOption ->
                    val isSelected = preferences.tabletFolderBackdrop == backdropOption
                    val optionStyle = resolveTabletFolderBackdropStyle(backdropOption, MaterialTheme.colorScheme)
                    val rowBg = if (isSelected) palette.pillFace else palette.innerTray
                    val rowFg = if (isSelected) palette.onPill else palette.onTray

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tablet_folder_backdrop_${backdropOption.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) palette.pillLip else palette.cardLip)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(rowBg)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, palette.highlightAccent, RoundedCornerShape(20.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectFolderBackdrop(backdropOption) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            // Live Material You mini swatch
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(optionStyle.backgroundBrush)
                                    .border(optionStyle.borderWidth, optionStyle.borderColor, RoundedCornerShape(14.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = optionStyle.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = backdropOption.label,
                                    style = TextStyle(
                                        fontFamily = AuroraDisplayFontFamily,
                                        fontSize = 18.sp,
                                        color = rowFg
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = backdropOption.description,
                                    style = TextStyle(
                                        fontFamily = AuroraBodyFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = rowFg.copy(alpha = 0.85f)
                                    )
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = palette.highlightAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tablet Folder Size Quick Selector
        AuroraCardPanel(padding = 16.dp) {
            Aurora3DPillHeader(
                text = "Tablet folder size",
                height = 46.dp,
                fontSize = 19.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabletFolderSize.entries.forEach { sizeOption ->
                    val selected = preferences.tabletFolderSize == sizeOption
                    Aurora3DPillButton(
                        text = sizeOption.label,
                        onClick = { onSelectTabletFolderSize(sizeOption) },
                        height = 44.dp,
                        fontSize = 14.sp,
                        faceColor = if (selected) palette.pillFace else palette.innerTray,
                        lipColor = if (selected) palette.pillLip else palette.cardLip,
                        textColor = if (selected) palette.onPill else palette.onTray,
                        testTagName = "tablet_folders_page_size_${sizeOption.name}",
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, palette.highlightAccent, RoundedCornerShape(26.dp))
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
    onAllowThirdPartyWidgets: (Boolean) -> Unit,
    onToggleDynamicWallpaperColors: (Boolean) -> Unit
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

    val wallpaperName = when (preferences.wallpaperThemeIndex) {
        0 -> "Android System"
        1 -> "Cosmic Sky"
        2 -> "Velvet Lavender"
        3 -> "Emerald Mist"
        else -> "Minimal Pure"
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
                text = "Wallpaper colours",
                height = 50.dp,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (preferences.dynamicWallpaperColors) {
                    "Enabled • Matching launcher & settings colours to \"$wallpaperName\" wallpaper."
                } else {
                    "Disabled • Using manual \"${preferences.colorPaletteSeed.label}\" accent palette."
                },
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.onCard.copy(alpha = 0.85f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.innerTray)
                    .padding(16.dp)
            ) {
                AuroraAllowDenyButtons(
                    isAllowed = preferences.dynamicWallpaperColors,
                    onAllow = { onToggleDynamicWallpaperColors(true) },
                    onDeny = { onToggleDynamicWallpaperColors(false) },
                    compact = false,
                    allowTag = "wallpaper_colors_allow",
                    denyTag = "wallpaper_colors_deny"
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
private fun DeviceModePage(
    preferences: LauncherPreferences,
    isTabletLayout: Boolean,
    onSelectDeviceMode: (DeviceMode) -> Unit,
    onSelectTabletFolderSize: (TabletFolderSize) -> Unit,
    onOpenTabletFolderCustomisation: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTabletLayout) 680.dp else Dp.Unspecified)
        ) {
            AuroraCardPanel(padding = 18.dp) {
                Aurora3DPillHeader(
                    text = if (isTabletLayout) "Active: Tablet layout" else "Active: Phone layout",
                    height = 50.dp,
                    fontSize = 21.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Switch between Phone and Tablet launcher modes, or keep Auto to adapt to your screen size. In Tablet mode, swipe up from the left or right edge to open the App Drawer on that side only.",
                    style = TextStyle(
                        fontFamily = AuroraBodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = palette.onCard.copy(alpha = 0.88f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AuroraCardPanel(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeviceMode.entries.forEach { mode ->
                        val isSelected = preferences.deviceMode == mode
                        val iconVector = when (mode) {
                            DeviceMode.AUTO -> Icons.Default.Devices
                            DeviceMode.PHONE -> Icons.Default.PhoneAndroid
                            DeviceMode.TABLET -> Icons.Default.TabletAndroid
                        }
                        val rowBg = if (isSelected) palette.pillFace else palette.innerTray
                        val rowFg = if (isSelected) palette.onPill else palette.onTray

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("device_mode_${mode.name}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (isSelected) palette.pillLip else palette.cardLip)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(rowBg)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 2.5.dp,
                                                color = palette.highlightAccent,
                                                shape = RoundedCornerShape(22.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelectDeviceMode(mode) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = mode.label,
                                    tint = rowFg,
                                    modifier = Modifier.size(30.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        style = TextStyle(
                                            fontFamily = AuroraDisplayFontFamily,
                                            fontSize = 20.sp,
                                            color = rowFg
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.description,
                                        style = TextStyle(
                                            fontFamily = AuroraBodyFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = rowFg.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = palette.highlightAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AuroraCardPanel(padding = 18.dp) {
                Aurora3DPillHeader(
                    text = "Tablet folder size",
                    height = 48.dp,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Scale folder tiles, 3×3 previews, and expanded folder popups in Tablet mode. You can also drag-resize pinned folders on the Home Screen.",
                    style = TextStyle(
                        fontFamily = AuroraBodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = palette.onCard.copy(alpha = 0.86f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabletFolderSize.entries.forEach { sizeOption ->
                        val selected = preferences.tabletFolderSize == sizeOption
                        Aurora3DPillButton(
                            text = "${sizeOption.label} (${sizeOption.tileSizeDp}dp)",
                            onClick = { onSelectTabletFolderSize(sizeOption) },
                            height = 46.dp,
                            fontSize = 13.sp,
                            faceColor = if (selected) palette.pillFace else palette.innerTray,
                            lipColor = if (selected) palette.pillLip else palette.cardLip,
                            textColor = if (selected) palette.onPill else palette.onTray,
                            testTagName = "settings_tablet_folder_size_${sizeOption.name}",
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = palette.highlightAccent,
                                            shape = RoundedCornerShape(26.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Aurora3DPillButton(
                    text = "Customise shape & backdrop",
                    onClick = onOpenTabletFolderCustomisation,
                    height = 48.dp,
                    fontSize = 15.sp,
                    testTagName = "device_mode_tablet_folder_customise_btn"
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
    onToggleAtAGlance: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit
) {
    val isAtAGlanceEnabled = HomeScreenWidget.AT_A_GLANCE.id in preferences.enabledWidgets
    val view = LocalView.current

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
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

        FeatureTogglePillRow(
            title = "Haptic\nfeedback",
            titleSize = 20.sp,
            isAllowed = preferences.hapticFeedbackEnabled,
            onAllow = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onToggleHapticFeedback(true)
            },
            onDeny = { onToggleHapticFeedback(false) },
            allowTag = "feature_haptic_allow",
            denyTag = "feature_haptic_deny"
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
    onSetDefaultLauncher: () -> Unit,
    onOpenCrashAssistant: () -> Unit
) {
    val palette = LocalAuroraSettingsPalette.current
    val context = LocalContext.current
    val faults by RenderFailsafeMonitor.faults.collectAsState()
    val simulatedTest by RenderFailsafeMonitor.simulatedTestMode.collectAsState()
    val softwareFallback by RenderFailsafeMonitor.softwareFallbackActive.collectAsState()
    var showFaultDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
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

        AuroraCardPanel(padding = 18.dp) {
            Aurora3DPillHeader(
                text = "Purple render failsafe",
                height = 48.dp,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (faults.isEmpty()) {
                    "Renderer healthy (${if (softwareFallback) "Software mode" else "Hardware GPU"}). If a component or driver fails to render, it displays in purple and logs to Crash Assistant."
                } else {
                    "${faults.size} render/driver issue(s) captured in Purple Failsafe mode."
                },
                style = TextStyle(
                    fontFamily = AuroraBodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = palette.onCard.copy(alpha = 0.88f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            faults.firstOrNull()?.let { latest ->
                Spacer(modifier = Modifier.height(14.dp))
                PurpleFailsafeCard(
                    fault = latest,
                    onTapDetails = { showFaultDialog = true },
                    onOpenCrashAssistant = onOpenCrashAssistant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Aurora3DPillButton(
                text = if (simulatedTest) "Stop failsafe test" else "Test purple failsafe",
                onClick = {
                    RenderFailsafeMonitor.setSimulatedTestMode(!simulatedTest)
                },
                height = 48.dp,
                fontSize = 17.sp,
                testTagName = "test_purple_failsafe_button"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Aurora3DPillButton(
                text = "Open Crash Assistant",
                onClick = onOpenCrashAssistant,
                height = 46.dp,
                fontSize = 16.sp,
                testTagName = "open_crash_assistant_button"
            )
        }
    }

    if (showFaultDialog && faults.isNotEmpty()) {
        PurpleFailsafeDetailsDialog(
            faults = faults,
            softwareFallbackActive = softwareFallback,
            onClearAll = {
                RenderFailsafeMonitor.clearActiveFaults(context)
                showFaultDialog = false
            },
            onOpenCrashAssistant = onOpenCrashAssistant,
            onDismiss = { showFaultDialog = false }
        )
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
                text = "Aurora launcher beta 3",
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
