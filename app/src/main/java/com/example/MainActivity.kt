package com.example

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppDrawerSide
import com.example.model.DeviceMode
import com.example.ui.components.AppContextMenu
import com.example.ui.components.AppVaultAuthDialog
import com.example.ui.components.AppVaultSheet
import com.example.ui.components.LocalImportedIconMap
import com.example.ui.components.LocalLauncherHapticEnabled
import com.example.ui.components.PurpleFailsafeDetailsDialog
import com.example.ui.components.RenderFailsafeMonitor
import com.example.ui.components.ThirdPartyWidgetPickerSheet
import com.example.ui.components.failsafeDrawBoundary
import com.example.ui.screens.AppDrawerSheet
import com.example.ui.screens.CrashAssistantScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LauncherSettingsDialog
import com.example.ui.screens.SmartSearchOverlay
import com.example.ui.theme.AuraLauncherTheme
import com.example.util.LauncherManager
import com.example.util.SystemAppWidgetHelper
import com.example.viewmodel.LauncherViewModel
import java.util.concurrent.atomic.AtomicBoolean

private data class WidgetPickerTarget(
    val page: Int,
    val preferredRow: Int?,
    val preferredCol: Int?,
    val cols: Int,
    val rows: Int
)

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    private var pendingConfigureWidgetId: Int = -1
    private var pendingConfigureInfo: AppWidgetProviderInfo? = null
    private var pendingConfigureTarget: WidgetPickerTarget? = null
    private var pendingIsReconfigure: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RenderFailsafeMonitor.init(applicationContext)
        if (RenderFailsafeMonitor.softwareFallbackActive.value) {
            window.decorView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        installRenderFailsafeHandler()

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()

        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val allApps by viewModel.allApps.collectAsStateWithLifecycle()
            val dockedApps by viewModel.dockedApps.collectAsStateWithLifecycle()
            val homeGridAppsMap by viewModel.homeGridAppsMap.collectAsStateWithLifecycle()
            val placedWidgets by viewModel.placedWidgets.collectAsStateWithLifecycle()
            val systemWidgetProviders by viewModel.systemWidgetProviders.collectAsStateWithLifecycle()
            val isLoadingWidgetProviders by viewModel.isLoadingWidgetProviders.collectAsStateWithLifecycle()
            val folders by viewModel.folders.collectAsStateWithLifecycle()
            val placedHomeFolders by viewModel.placedHomeFolders.collectAsStateWithLifecycle()
            val notes by viewModel.notes.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            val habits by viewModel.habits.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val importedIconPack by viewModel.importedIconPack.collectAsStateWithLifecycle()
            val isImportingIconPack by viewModel.isImportingIconPack.collectAsStateWithLifecycle()
            val renderFaults by RenderFailsafeMonitor.faults.collectAsStateWithLifecycle()
            val softwareFallbackActive by RenderFailsafeMonitor.softwareFallbackActive.collectAsStateWithLifecycle()
            val autoReportPromptVisible by RenderFailsafeMonitor.autoReportPromptVisible.collectAsStateWithLifecycle()
            var activeHomePage by remember { mutableIntStateOf(0) }
            var widgetPickerTarget by remember { mutableStateOf<WidgetPickerTarget?>(null) }

            var pendingBindId by remember { mutableIntStateOf(-1) }
            var pendingBindInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
            var pendingBindTarget by remember { mutableStateOf<WidgetPickerTarget?>(null) }
            var pendingPickId by remember { mutableIntStateOf(-1) }
            var pendingPickTarget by remember { mutableStateOf<WidgetPickerTarget?>(null) }

            val finalizeOrConfigureWidget: (Int, AppWidgetProviderInfo, WidgetPickerTarget) -> Unit =
                { appWidgetId, providerInfo, target ->
                    if (providerInfo.configure != null) {
                        pendingConfigureWidgetId = appWidgetId
                        pendingConfigureInfo = providerInfo
                        pendingConfigureTarget = target
                        pendingIsReconfigure = false
                        val launched = SystemAppWidgetHelper.launchConfigureActivity(this, appWidgetId)
                        if (!launched) {
                            pendingConfigureWidgetId = -1
                            pendingConfigureInfo = null
                            pendingConfigureTarget = null
                            viewModel.addThirdPartyWidget(
                                appWidgetId = appWidgetId,
                                providerInfo = providerInfo,
                                page = target.page,
                                preferredRow = target.preferredRow,
                                preferredCol = target.preferredCol,
                                cols = target.cols,
                                rows = target.rows
                            )
                        }
                    } else {
                        viewModel.addThirdPartyWidget(
                            appWidgetId = appWidgetId,
                            providerInfo = providerInfo,
                            page = target.page,
                            preferredRow = target.preferredRow,
                            preferredCol = target.preferredCol,
                            cols = target.cols,
                            rows = target.rows
                        )
                    }
                }

            val bindWidgetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val id = pendingBindId
                val info = pendingBindInfo
                val target = pendingBindTarget
                pendingBindId = -1
                pendingBindInfo = null
                pendingBindTarget = null
                if (result.resultCode == RESULT_OK && id >= 0 && info != null && target != null) {
                    finalizeOrConfigureWidget(id, info, target)
                } else if (id >= 0) {
                    SystemAppWidgetHelper.deleteAppWidgetId(applicationContext, id)
                }
            }

            val systemPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val idFromData = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                val id = if (idFromData >= 0) idFromData else pendingPickId
                val target = pendingPickTarget
                pendingPickId = -1
                pendingPickTarget = null
                if (result.resultCode == RESULT_OK && id >= 0 && target != null) {
                    val info = SystemAppWidgetHelper.getAppWidgetInfo(applicationContext, id)
                    if (info != null) {
                        finalizeOrConfigureWidget(id, info, target)
                    } else {
                        SystemAppWidgetHelper.deleteAppWidgetId(applicationContext, id)
                    }
                } else if (id >= 0) {
                    SystemAppWidgetHelper.deleteAppWidgetId(applicationContext, id)
                }
            }

            val roleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.checkDefaultLauncherStatus()
            }

            val onSetDefaultLauncher = {
                LauncherManager.requestSetDefaultLauncher(this, roleLauncher)
            }

            AuraLauncherTheme(
                themeMode = preferences.themeMode,
                paletteSeed = preferences.colorPaletteSeed,
                wallpaperThemeIndex = preferences.wallpaperThemeIndex,
                dynamicWallpaperColors = preferences.dynamicWallpaperColors
            ) {
                CompositionLocalProvider(
                    LocalImportedIconMap provides importedIconPack.packageToPath,
                    LocalLauncherHapticEnabled provides preferences.hapticFeedbackEnabled
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .failsafeDrawBoundary("Launcher Root Surface")
                    ) {
                    val isTabletLayout = when (preferences.deviceMode) {
                        DeviceMode.TABLET -> true
                        DeviceMode.PHONE -> false
                        DeviceMode.AUTO -> maxWidth >= 600.dp
                    }

                    // Base Home Screen
                    HomeScreen(
                        preferences = preferences,
                        allApps = allApps,
                        dockedApps = dockedApps,
                        homeGridAppsMap = homeGridAppsMap,
                        placedWidgets = placedWidgets,
                        folders = folders,
                        placedHomeFolders = placedHomeFolders,
                        notes = notes,
                        tasks = tasks,
                        habits = habits,
                        isDefaultLauncher = uiState.isDefaultLauncher,
                        isTabletLayout = isTabletLayout,
                        onSetDefaultLauncher = onSetDefaultLauncher,
                        onAppClick = { viewModel.launchApp(it) },
                        onAppLongClick = { viewModel.openAppContextMenu(it) },
                        onOpenDrawer = { side -> viewModel.openDrawer(side) },
                        onOpenSearch = { viewModel.openSearch() },
                        onOpenSettings = { viewModel.openSettings() },
                        onAddNote = { viewModel.addNote(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onAddTask = { viewModel.addTask(it) },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onToggleHabit = { viewModel.toggleHabit(it) },
                        onAddNewPage = { viewModel.addHomePage() },
                        onRemoveCurrentPage = { viewModel.removeHomePage() },
                        onToggleWidget = { widgetId, enabled ->
                            val current = preferences.enabledWidgets
                            val updated = if (enabled) current + widgetId else current - widgetId
                            viewModel.updatePreferences { it.copy(enabledWidgets = updated) }
                        },
                        onCycleWallpaper = {
                            val next = (preferences.wallpaperThemeIndex + 1) % 5
                            viewModel.updatePreferences { it.copy(wallpaperThemeIndex = next) }
                        },
                        onPlaceAppOnHomeGrid = { page, app, row, col, cols, rows ->
                            viewModel.placeAppOnHomeGrid(page, app, row, col, cols, rows)
                        },
                        onMoveAppWithinDock = { app, toIdx ->
                            viewModel.moveAppWithinDock(app, toIdx)
                        },
                        onMoveAppHomeToDock = { page, app, dockIdx ->
                            viewModel.moveAppHomeToDock(page, app, dockIdx)
                        },
                        onMoveAppDockToHomeGrid = { app, page, row, col, cols, rows ->
                            viewModel.moveAppDockToHomeGrid(app, page, row, col, cols, rows)
                        },
                        onRemoveAppFromHomePage = { page, app ->
                            viewModel.removeAppFromHomePage(page, app)
                        },
                        onRemoveAppFromDock = { app ->
                            viewModel.removeAppFromDock(app)
                        },
                        onOpenThirdPartyWidgetPicker = { page, prefRow, prefCol, cols, rows ->
                            val target = WidgetPickerTarget(page, prefRow, prefCol, cols, rows)
                            widgetPickerTarget = target
                            viewModel.loadSystemWidgetProviders(cols, rows)
                        },
                        onMoveThirdPartyWidget = { widgetId, targetPage, targetRow, targetCol, cols, rows ->
                            val moved = viewModel.moveThirdPartyWidget(widgetId, targetPage, targetRow, targetCol, cols, rows)
                            if (!moved) {
                                Toast.makeText(this@MainActivity, "Cell is occupied by another widget", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onResizeThirdPartyWidget = { widgetId, spanX, spanY, cols, rows ->
                            val resized = viewModel.resizeThirdPartyWidget(widgetId, spanX, spanY, cols, rows)
                            if (!resized) {
                                Toast.makeText(this@MainActivity, "Not enough free grid space for that size", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUpdateThirdPartyWidgetBounds = { widgetId, targetRow, targetCol, spanX, spanY, cols, rows ->
                            val updated = viewModel.updateThirdPartyWidgetBounds(
                                appWidgetId = widgetId,
                                targetRow = targetRow,
                                targetCol = targetCol,
                                newSpanX = spanX,
                                newSpanY = spanY,
                                cols = cols,
                                rows = rows
                            )
                            if (!updated) {
                                Toast.makeText(this@MainActivity, "Overlaps another widget on grid", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onConfigureThirdPartyWidget = { widgetId ->
                            pendingConfigureWidgetId = widgetId
                            pendingConfigureInfo = SystemAppWidgetHelper.getAppWidgetInfo(applicationContext, widgetId)
                            pendingConfigureTarget = null
                            pendingIsReconfigure = true
                            SystemAppWidgetHelper.launchConfigureActivity(this@MainActivity, widgetId)
                        },
                        onRemoveThirdPartyWidget = { widgetId ->
                            viewModel.removeThirdPartyWidget(widgetId)
                        },
                        onPlaceFolderOnHomeGrid = { folderId, page, prefRow, prefCol, spanX, spanY, cols, rows ->
                            viewModel.placeFolderOnHome(folderId, page, prefRow, prefCol, spanX, spanY, cols, rows)
                        },
                        onCreateAndPlaceFolderOnHomeGrid = { name, page, prefRow, prefCol, spanX, spanY, cols, rows ->
                            viewModel.createAndPlaceFolderOnHome(name, page, prefRow, prefCol, spanX, spanY, cols, rows)
                        },
                        onUpdateHomeFolderBounds = { folderId, targetRow, targetCol, newSpanX, newSpanY, cols, rows ->
                            val updated = viewModel.updateHomeFolderBounds(
                                folderId = folderId,
                                targetRow = targetRow,
                                targetCol = targetCol,
                                newSpanX = if (isTabletLayout) newSpanX else 1,
                                newSpanY = if (isTabletLayout) newSpanY else 1,
                                cols = cols,
                                rows = rows
                            )
                            if (!updated) {
                                Toast.makeText(this@MainActivity, "Overlaps another item on grid", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRemoveHomeFolder = { folderId ->
                            viewModel.removeFolderFromHome(folderId)
                        },
                        onRemoveAppFromFolder = { folder, app -> viewModel.removeAppFromFolder(folder, app) },
                        onRenameFolder = { folder, newName -> viewModel.renameFolder(folder, newName) },
                        onDeleteFolder = { folder -> viewModel.deleteFolder(folder) },
                        onChangeTabletFolderSize = { size ->
                            viewModel.updatePreferences { it.copy(tabletFolderSize = size) }
                        },
                        onToggleGridGuides = { enabled ->
                            viewModel.updatePreferences { it.copy(showHomeGridGuides = enabled) }
                        },
                        onCurrentHomePageChanged = { activeHomePage = it }
                    )

                    // App Drawer Overlay with tablet-aware slide-in physics
                    AnimatedVisibility(
                        visible = uiState.isDrawerOpen,
                        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 140))
                    ) {
                        AppDrawerSheet(
                            apps = allApps,
                            folders = folders,
                            hiddenAppsCount = allApps.count { it.isHidden },
                            drawerLayoutMode = preferences.drawerLayoutMode,
                            iconPackStyle = preferences.iconPackStyle,
                            iconSize = preferences.iconSize,
                            iconShape = preferences.iconShape,
                            showAppLabels = preferences.showAppLabels,
                            drawerSide = if (isTabletLayout) uiState.drawerSide else AppDrawerSide.FULL,
                            isTabletLayout = isTabletLayout,
                            tabletFolderSize = preferences.tabletFolderSize,
                            tabletExpandedFolderIconShape = preferences.tabletExpandedFolderIconShape,
                            tabletFolderBackdrop = preferences.tabletFolderBackdrop,
                            placedHomeFolderIds = placedHomeFolders.map { it.folderId }.toSet(),
                            animatedVisibilityScope = this,
                            onAppClick = { viewModel.launchApp(it) },
                            onAppLongClick = { viewModel.openAppContextMenu(it) },
                            onSearchClick = { viewModel.openSearch() },
                            onOpenAppVault = { viewModel.openVaultAuth() },
                            onChangeTabletFolderSize = { size ->
                                viewModel.updatePreferences { it.copy(tabletFolderSize = size) }
                            },
                            onTogglePinFolderToHome = { folder ->
                                val isPinned = placedHomeFolders.any { it.folderId == folder.id }
                                if (isPinned) {
                                    viewModel.removeFolderFromHome(folder.id)
                                } else {
                                    val span = if (isTabletLayout) 2 else 1
                                    viewModel.placeFolderOnHome(
                                        folderId = folder.id,
                                        page = activeHomePage,
                                        preferredRow = null,
                                        preferredCol = null,
                                        spanX = span,
                                        spanY = span,
                                        cols = if (isTabletLayout) 5 else 4,
                                        rows = 4
                                    )
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Pinned '${folder.name}' to Home Screen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onRemoveAppFromFolder = { folder, app -> viewModel.removeAppFromFolder(folder, app) },
                            onRenameFolder = { folder, newName -> viewModel.renameFolder(folder, newName) },
                            onDeleteFolder = { folder -> viewModel.deleteFolder(folder) },
                            onLayoutChange = { viewModel.setDrawerLayoutMode(it) },
                            onDismiss = { viewModel.closeDrawer() }
                        )
                    }

                    // Smart Search Overlay
                    AnimatedVisibility(
                        visible = uiState.isSearchOpen,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f)
                        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = spring(dampingRatio = 0.92f, stiffness = 520f)
                        ) + fadeOut(animationSpec = tween(durationMillis = 140))
                    ) {
                        SmartSearchOverlay(
                            apps = allApps,
                            iconPackStyle = preferences.iconPackStyle,
                            iconShape = preferences.iconShape,
                            onAppClick = { viewModel.launchApp(it) },
                            onAppLongClick = { viewModel.openAppContextMenu(it) },
                            onDismiss = { viewModel.closeSearch() }
                        )
                    }

                    // Launcher Settings & Customization Hub
                    AnimatedVisibility(
                        visible = uiState.isSettingsOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f)
                        ) + fadeIn(animationSpec = tween(durationMillis = 160)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.92f, stiffness = 520f)
                        ) + fadeOut(animationSpec = tween(durationMillis = 140))
                    ) {
                        val hiddenApps = remember(allApps) { allApps.filter { it.isHidden } }
                        val visibleApps = remember(allApps) { allApps.filter { !it.isHidden } }
                        val previewApps = remember(visibleApps) { visibleApps.take(4) }
                        LauncherSettingsDialog(
                            preferences = preferences,
                            hiddenApps = hiddenApps,
                            allApps = visibleApps,
                            previewApps = previewApps,
                            importedIconPack = importedIconPack,
                            isImportingIconPack = isImportingIconPack,
                            isDefaultLauncher = uiState.isDefaultLauncher,
                            isTabletLayout = isTabletLayout,
                            onSetDefaultLauncher = onSetDefaultLauncher,
                            onImportIconPackFiles = { viewModel.importIconPackFiles(it) },
                            onImportIconPackFolder = { viewModel.importIconPackFolder(it) },
                            onAssignAppIcon = { pkg, path -> viewModel.assignAppIconFromPack(pkg, path) },
                            onClearImportedIconPack = { viewModel.clearImportedIconPack() },
                            onUpdatePreferences = { update -> viewModel.updatePreferences(update) },
                            onUnhideApp = { viewModel.unhideApp(it) },
                            onOpenCrashAssistant = {
                                viewModel.closeSettings()
                                viewModel.openCrashAssistant()
                            },
                            onDismiss = { viewModel.closeSettings() }
                        )
                    }

                    // Built-in Crash Assistant App
                    AnimatedVisibility(
                        visible = uiState.isCrashAssistantOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f)
                        ) + fadeIn(animationSpec = tween(durationMillis = 160)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.92f, stiffness = 520f)
                        ) + fadeOut(animationSpec = tween(durationMillis = 140))
                    ) {
                        CrashAssistantScreen(
                            isTabletLayout = isTabletLayout,
                            onDismiss = { viewModel.closeCrashAssistant() }
                        )
                    }

                    // Context Menu Bottom Sheet
                    uiState.contextMenuApp?.let { targetApp ->
                        val isOnCurrentHome = (homeGridAppsMap[activeHomePage] ?: emptyList())
                            .any { it.second.packageName == targetApp.packageName }
                        AppContextMenu(
                            app = targetApp,
                            iconPackStyle = preferences.iconPackStyle,
                            iconShape = preferences.iconShape,
                            folders = folders,
                            isOnHomeScreen = isOnCurrentHome,
                            onDismiss = { viewModel.closeAppContextMenu() },
                            onToggleHomeScreen = {
                                if (isOnCurrentHome) {
                                    viewModel.removeAppFromHomePage(activeHomePage, targetApp)
                                } else {
                                    viewModel.addAppToHomePage(activeHomePage, targetApp)
                                }
                            },
                            onToggleDock = { viewModel.toggleDock(targetApp, it) },
                            onToggleFavorite = { viewModel.toggleFavorite(targetApp, it) },
                            onRename = { viewModel.renameApp(targetApp, it) },
                            onHide = { viewModel.hideApp(targetApp) },
                            onOpenAppInfo = { viewModel.openAppInfo(targetApp) },
                            onAddToFolder = { folder, app -> viewModel.addAppToFolder(folder, app) },
                            onCreateFolderWithApp = { name, app -> viewModel.createFolder(name, app) }
                        )
                    }

                    // App Vault Password Auth Dialog
                    if (uiState.isVaultAuthOpen) {
                        AppVaultAuthDialog(
                            hasPasswordSet = viewModel.hasVaultPassword(),
                            onVerifyPassword = { viewModel.verifyVaultPassword(it) },
                            onSetNewPassword = { viewModel.setVaultPassword(it) },
                            onSuccess = { viewModel.openVaultSheet() },
                            onDismiss = { viewModel.closeVaultAuth() }
                        )
                    }

                    // App Vault Sheet (Protected Hidden Apps Drawer)
                    if (uiState.isVaultSheetOpen) {
                        val hiddenApps = allApps.filter { it.isHidden }
                        AppVaultSheet(
                            hiddenApps = hiddenApps,
                            iconPackStyle = preferences.iconPackStyle,
                            iconSize = preferences.iconSize,
                            onAppClick = { viewModel.launchApp(it) },
                            onUnhideApp = { viewModel.unhideApp(it) },
                            onChangePassword = {
                                viewModel.closeVaultSheet()
                                viewModel.openVaultAuth()
                            },
                            onDismiss = { viewModel.closeVaultSheet() }
                        )
                    }

                    // Third-Party & Android System AppWidget Picker Sheet
                    widgetPickerTarget?.let { target ->
                        ThirdPartyWidgetPickerSheet(
                            providers = systemWidgetProviders,
                            isLoading = isLoadingWidgetProviders,
                            allowThirdPartyWidgets = preferences.allowThirdPartyWidgets,
                            targetCellLabel = if (target.preferredRow != null && target.preferredCol != null) {
                                "Placing on Page ${target.page + 1} (Row ${target.preferredRow + 1}, Col ${target.preferredCol + 1})"
                            } else {
                                "Widgets from installed apps & Android OS"
                            },
                            onSelectProvider = { entry ->
                                widgetPickerTarget = null
                                val id = SystemAppWidgetHelper.allocateAppWidgetId(applicationContext)
                                if (id >= 0) {
                                    val bound = SystemAppWidgetHelper.bindAppWidgetIdIfAllowed(
                                        applicationContext,
                                        id,
                                        entry.providerInfo
                                    )
                                    if (bound) {
                                        finalizeOrConfigureWidget(id, entry.providerInfo, target)
                                    } else {
                                        pendingBindId = id
                                        pendingBindInfo = entry.providerInfo
                                        pendingBindTarget = target
                                        runCatching {
                                            bindWidgetLauncher.launch(
                                                SystemAppWidgetHelper.createBindIntent(id, entry.providerInfo)
                                            )
                                        }.onFailure {
                                            SystemAppWidgetHelper.deleteAppWidgetId(applicationContext, id)
                                            pendingBindId = -1
                                            pendingBindInfo = null
                                            pendingBindTarget = null
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Unable to request widget permission on this device",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onOpenSystemPickerFallback = {
                                widgetPickerTarget = null
                                val id = SystemAppWidgetHelper.allocateAppWidgetId(applicationContext)
                                if (id >= 0) {
                                    pendingPickId = id
                                    pendingPickTarget = target
                                    runCatching {
                                        systemPickerLauncher.launch(
                                            SystemAppWidgetHelper.createSystemPickerIntent(id)
                                        )
                                    }.onFailure {
                                        SystemAppWidgetHelper.deleteAppWidgetId(applicationContext, id)
                                        pendingPickId = -1
                                        pendingPickTarget = null
                                        Toast.makeText(
                                            this@MainActivity,
                                            "System widget picker unavailable",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onOpenSettings = { viewModel.openSettings() },
                            onDismiss = { widgetPickerTarget = null }
                        )
                    }

                    // Global Report Dialog triggered automatically when Purple Failsafe activates
                    if (autoReportPromptVisible && renderFaults.isNotEmpty() && !uiState.isCrashAssistantOpen) {
                        PurpleFailsafeDetailsDialog(
                            faults = renderFaults,
                            softwareFallbackActive = softwareFallbackActive,
                            onClearAll = { RenderFailsafeMonitor.clearActiveFaults(applicationContext) },
                            onOpenCrashAssistant = {
                                RenderFailsafeMonitor.dismissAutoReportPrompt()
                                viewModel.openCrashAssistant()
                            },
                            onDismiss = { RenderFailsafeMonitor.dismissAutoReportPrompt() }
                        )
                    }
                }
                }
            }
        }
    }

    @Deprecated("Required for AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SystemAppWidgetHelper.REQUEST_CONFIGURE_APPWIDGET) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingConfigureWidgetId)
                ?: pendingConfigureWidgetId
            val info = pendingConfigureInfo
            val target = pendingConfigureTarget
            val isReconfigure = pendingIsReconfigure

            pendingConfigureWidgetId = -1
            pendingConfigureInfo = null
            pendingConfigureTarget = null
            pendingIsReconfigure = false

            if (!isReconfigure && widgetId >= 0) {
                if (resultCode == RESULT_OK && info != null && target != null) {
                    viewModel.addThirdPartyWidget(
                        appWidgetId = widgetId,
                        providerInfo = info,
                        page = target.page,
                        preferredRow = target.preferredRow,
                        preferredCol = target.preferredCol,
                        cols = target.cols,
                        rows = target.rows
                    )
                } else {
                    SystemAppWidgetHelper.deleteAppWidgetId(applicationContext, widgetId)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SystemAppWidgetHelper.startListening(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        SystemAppWidgetHelper.stopListening(applicationContext)
    }

    private fun installRenderFailsafeHandler() {
        if (!failsafeHandlerInstalled.compareAndSet(false, true)) return
        val appCtx = applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val component = when {
                thread.name.contains("RenderThread", ignoreCase = true) -> "GPU / RenderThread"
                RenderFailsafeMonitor.isGraphicsOrDriverIssue(throwable) -> "Display / Graphics Driver"
                else -> "Launcher Renderer"
            }
            RenderFailsafeMonitor.persistCrashFault(appCtx, component, throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDefaultLauncherStatus()
    }

    companion object {
        private val failsafeHandlerInstalled = AtomicBoolean(false)
    }
}
