package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppContextMenu
import com.example.ui.components.AppVaultAuthDialog
import com.example.ui.components.AppVaultSheet
import com.example.ui.screens.AppDrawerSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LauncherSettingsDialog
import com.example.ui.screens.SmartSearchOverlay
import com.example.ui.theme.AuraLauncherTheme
import com.example.util.LauncherManager
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val allApps by viewModel.allApps.collectAsStateWithLifecycle()
            val folders by viewModel.folders.collectAsStateWithLifecycle()
            val notes by viewModel.notes.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            val habits by viewModel.habits.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                paletteSeed = preferences.colorPaletteSeed
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base Home Screen
                    HomeScreen(
                        preferences = preferences,
                        allApps = allApps,
                        notes = notes,
                        tasks = tasks,
                        habits = habits,
                        isDefaultLauncher = uiState.isDefaultLauncher,
                        onSetDefaultLauncher = onSetDefaultLauncher,
                        onAppClick = { viewModel.launchApp(it) },
                        onAppLongClick = { viewModel.openAppContextMenu(it) },
                        onOpenDrawer = { viewModel.openDrawer() },
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
                            val next = (preferences.wallpaperThemeIndex + 1) % 4
                            viewModel.updatePreferences { it.copy(wallpaperThemeIndex = next) }
                        }
                    )

                    // App Drawer Overlay with spring physics
                    AnimatedVisibility(
                        visible = uiState.isDrawerOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 450f)
                        ) + fadeOut()
                    ) {
                        AppDrawerSheet(
                            apps = allApps,
                            folders = folders,
                            hiddenAppsCount = allApps.count { it.isHidden },
                            drawerLayoutMode = preferences.drawerLayoutMode,
                            iconPackStyle = preferences.iconPackStyle,
                            iconSize = preferences.iconSize,
                            showAppLabels = preferences.showAppLabels,
                            onAppClick = { viewModel.launchApp(it) },
                            onAppLongClick = { viewModel.openAppContextMenu(it) },
                            onSearchClick = { viewModel.openSearch() },
                            onOpenAppVault = { viewModel.openVaultAuth() },
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
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 450f)
                        ) + fadeOut()
                    ) {
                        SmartSearchOverlay(
                            apps = allApps,
                            iconPackStyle = preferences.iconPackStyle,
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
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 450f)
                        ) + fadeOut()
                    ) {
                        val hiddenApps = allApps.filter { it.isHidden }
                        LauncherSettingsDialog(
                            preferences = preferences,
                            hiddenApps = hiddenApps,
                            isDefaultLauncher = uiState.isDefaultLauncher,
                            onSetDefaultLauncher = onSetDefaultLauncher,
                            onUpdatePreferences = { update -> viewModel.updatePreferences(update) },
                            onUnhideApp = { viewModel.unhideApp(it) },
                            onDismiss = { viewModel.closeSettings() }
                        )
                    }

                    // Context Menu Bottom Sheet
                    uiState.contextMenuApp?.let { targetApp ->
                        AppContextMenu(
                            app = targetApp,
                            iconPackStyle = preferences.iconPackStyle,
                            folders = folders,
                            onDismiss = { viewModel.closeAppContextMenu() },
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
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDefaultLauncherStatus()
    }
}
