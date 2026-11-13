package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LauncherRepository
import com.example.model.AppCustomization
import com.example.model.AppFolder
import com.example.model.AppItem
import com.example.model.DrawerLayoutMode
import com.example.model.HabitItem
import com.example.model.IconPackStyle
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.TaskItem
import com.example.util.IconPackManager
import com.example.util.ImportedIconPackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherUiState(
    val isDrawerOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val isVaultAuthOpen: Boolean = false,
    val isVaultSheetOpen: Boolean = false,
    val contextMenuApp: AppItem? = null,
    val launchToastMessage: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = LauncherRepository(application, database.launcherDao())
    private val iconPackManager = IconPackManager(application)

    private val _uiState = MutableStateFlow(
        LauncherUiState(
            isDefaultLauncher = com.example.util.LauncherManager.isDefaultLauncher(application)
        )
    )
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    val importedIconPack: StateFlow<ImportedIconPackState> = iconPackManager.state

    private val _isImportingIconPack = MutableStateFlow(false)
    val isImportingIconPack: StateFlow<Boolean> = _isImportingIconPack.asStateFlow()

    private val _rawApps = MutableStateFlow<List<AppItem>>(emptyList())

    val preferences: StateFlow<LauncherPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherPreferences()
    )

    val folders: StateFlow<List<AppFolder>> = repository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notes: StateFlow<List<NoteItem>> = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasks: StateFlow<List<TaskItem>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitItem>> = repository.allHabits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customizations: StateFlow<List<AppCustomization>> = repository.allCustomizations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Merged apps with customizations
    val allApps: StateFlow<List<AppItem>> = combine(_rawApps, customizations, repository.dockPackages) { raw, customList, dockPkgs ->
        val customMap = customList.associateBy { it.packageName }
        val dockSet = dockPkgs.toSet()
        raw.map { app ->
            val custom = customMap[app.packageName]
            if (custom != null) {
                app.copy(
                    customLabel = custom.customLabel,
                    isHidden = custom.isHidden,
                    isFavorite = custom.isFavorite,
                    isDocked = app.packageName in dockSet,
                    launchCount = custom.launchCount,
                    lastLaunchedTime = custom.lastLaunchTime
                )
            } else {
                app.copy(isDocked = app.packageName in dockSet)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dockedApps: StateFlow<List<AppItem>> = combine(allApps, repository.dockPackages) { apps, dockPkgs ->
        val appMap = apps.filter { !it.isHidden }.associateBy { it.packageName }
        dockPkgs.mapNotNull { appMap[it] }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val homePageAppsMap: StateFlow<Map<Int, List<AppItem>>> = combine(allApps, repository.homePages) { apps, pages ->
        val appMap = apps.filter { !it.isHidden }.associateBy { it.packageName }
        pages.mapValues { (_, pkgs) -> pkgs.mapNotNull { appMap[it] } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            val apps = repository.loadInstalledApps()
            _rawApps.value = apps
            iconPackManager.refreshMatchesIfNeeded(apps)
        }
        checkDefaultLauncherStatus()
    }

    fun checkDefaultLauncherStatus() {
        val isDefault = com.example.util.LauncherManager.isDefaultLauncher(getApplication())
        _uiState.value = _uiState.value.copy(isDefaultLauncher = isDefault)
    }

    fun openDrawer() {
        _uiState.value = _uiState.value.copy(isDrawerOpen = true, isSearchOpen = false)
    }

    fun closeDrawer() {
        _uiState.value = _uiState.value.copy(isDrawerOpen = false)
    }

    fun openSearch() {
        _uiState.value = _uiState.value.copy(isSearchOpen = true)
    }

    fun closeSearch() {
        _uiState.value = _uiState.value.copy(isSearchOpen = false)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    fun openAppContextMenu(app: AppItem) {
        _uiState.value = _uiState.value.copy(contextMenuApp = app)
    }

    fun closeAppContextMenu() {
        _uiState.value = _uiState.value.copy(contextMenuApp = null)
    }

    fun addHomePage() {
        val current = preferences.value.homePageCount
        if (current < 5) {
            updatePreferences { it.copy(homePageCount = current + 1) }
        }
    }

    fun removeHomePage() {
        val current = preferences.value.homePageCount
        if (current > 1) {
            updatePreferences { it.copy(homePageCount = current - 1) }
        }
    }

    fun launchApp(app: AppItem) {
        if (app.packageName == "com.aurora.launcher.settings") {
            closeDrawer()
            closeSearch()
            openSettings()
            return
        }
        viewModelScope.launch {
            repository.recordAppLaunch(app.packageName)
            closeDrawer()
            closeSearch()

            val context = getApplication<Application>()
            var launched = false

            // Try real launch intent first
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching {
                    context.startActivity(launchIntent)
                    launched = true
                }
            }

            if (!launched) {
                // If package isn't installed on device/emulator, provide helpful feedback
                Toast.makeText(
                    context,
                    "Opening ${app.displayLabel}...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun openAppInfo(app: AppItem) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "${app.displayLabel}: ${app.packageName}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePreferences(update: (LauncherPreferences) -> LauncherPreferences) {
        repository.updatePreferences(update)
    }

    fun setDrawerLayoutMode(mode: DrawerLayoutMode) {
        repository.updatePreferences { it.copy(drawerLayoutMode = mode) }
    }

    // Custom Icon Pack Import actions
    fun importIconPackFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImportingIconPack.value = true
            val result = iconPackManager.importFromFiles(uris, allApps.value)
            _isImportingIconPack.value = false
            val context = getApplication<Application>()
            result.onSuccess { pack ->
                updatePreferences { it.copy(iconPackStyle = IconPackStyle.IMPORTED_CUSTOM) }
                val msg = if (pack.matchedCount > 0) {
                    "Imported '${pack.packName}' (${pack.totalIcons} icons, ${pack.matchedCount} apps matched)"
                } else {
                    "Imported ${pack.totalIcons} icon(s). Tap 'Styles & map' to assign to apps."
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(
                    context,
                    err.message ?: "Could not import icon pack from selected file(s).",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun importIconPackFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isImportingIconPack.value = true
            val result = iconPackManager.importFromFolder(treeUri, allApps.value)
            _isImportingIconPack.value = false
            val context = getApplication<Application>()
            result.onSuccess { pack ->
                updatePreferences { it.copy(iconPackStyle = IconPackStyle.IMPORTED_CUSTOM) }
                val msg = if (pack.matchedCount > 0) {
                    "Imported folder '${pack.packName}' (${pack.totalIcons} icons, ${pack.matchedCount} apps matched)"
                } else {
                    "Imported ${pack.totalIcons} icons from folder. Tap 'Styles & map' to assign."
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(
                    context,
                    err.message ?: "Could not import icon pack folder.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun assignAppIconFromPack(packageName: String, iconFilePath: String?) {
        viewModelScope.launch {
            iconPackManager.assignManualIcon(packageName, iconFilePath, allApps.value)
            if (iconFilePath != null && preferences.value.iconPackStyle != IconPackStyle.IMPORTED_CUSTOM) {
                updatePreferences { it.copy(iconPackStyle = IconPackStyle.IMPORTED_CUSTOM) }
            }
        }
    }

    fun clearImportedIconPack() {
        viewModelScope.launch {
            iconPackManager.clearImportedPack()
            if (preferences.value.iconPackStyle == IconPackStyle.IMPORTED_CUSTOM) {
                updatePreferences { it.copy(iconPackStyle = IconPackStyle.MATERIAL_YOU_THEMED) }
            }
            Toast.makeText(getApplication(), "Imported icon pack removed", Toast.LENGTH_SHORT).show()
        }
    }

    // App Customization actions
    fun toggleDock(app: AppItem, isDocked: Boolean) {
        viewModelScope.launch {
            repository.setAppDocked(app.packageName, isDocked)
        }
    }

    fun toggleFavorite(app: AppItem, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setAppFavorite(app.packageName, isFavorite)
        }
    }

    fun renameApp(app: AppItem, newLabel: String?) {
        viewModelScope.launch {
            repository.setAppCustomLabel(app.packageName, newLabel)
        }
    }

    fun hideApp(app: AppItem) {
        viewModelScope.launch {
            repository.setAppHidden(app.packageName, true)
            Toast.makeText(getApplication(), "${app.displayLabel} hidden. Restore from Settings.", Toast.LENGTH_SHORT).show()
        }
    }

    fun unhideApp(app: AppItem) {
        viewModelScope.launch {
            repository.setAppHidden(app.packageName, false)
        }
    }

    // Widgets actions
    fun addNote(content: String) {
        viewModelScope.launch { repository.addNote(content) }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun addTask(title: String) {
        viewModelScope.launch { repository.addTask(title) }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun toggleHabit(habit: HabitItem) {
        viewModelScope.launch { repository.toggleHabit(habit) }
    }

    // App Vault actions
    fun openVaultAuth() {
        _uiState.value = _uiState.value.copy(isVaultAuthOpen = true)
    }

    fun closeVaultAuth() {
        _uiState.value = _uiState.value.copy(isVaultAuthOpen = false)
    }

    fun openVaultSheet() {
        _uiState.value = _uiState.value.copy(isVaultAuthOpen = false, isVaultSheetOpen = true)
    }

    fun closeVaultSheet() {
        _uiState.value = _uiState.value.copy(isVaultSheetOpen = false)
    }

    fun hasVaultPassword(): Boolean = repository.hasVaultPassword()

    fun verifyVaultPassword(password: String): Boolean = repository.verifyVaultPassword(password)

    fun setVaultPassword(password: String) = repository.setVaultPassword(password)

    // Folder actions
    fun createFolder(name: String, app: AppItem? = null) {
        val pkgs = if (app != null) listOf(app.packageName) else emptyList()
        repository.createFolder(name, pkgs)
    }

    fun addAppToFolder(folder: AppFolder, app: AppItem) {
        repository.addAppToFolder(folder.id, app.packageName)
    }

    fun removeAppFromFolder(folder: AppFolder, app: AppItem) {
        repository.removeAppFromFolder(folder.id, app.packageName)
    }

    fun renameFolder(folder: AppFolder, newName: String) {
        repository.renameFolder(folder.id, newName)
    }

    fun deleteFolder(folder: AppFolder) {
        repository.deleteFolder(folder.id)
    }

    // Home Screen & Dock Drag-and-Drop / Layout Actions
    fun addAppToHomePage(page: Int, app: AppItem, index: Int? = null) {
        repository.addAppToHomePage(page, app.packageName, index)
    }

    fun removeAppFromHomePage(page: Int, app: AppItem) {
        repository.removeAppFromHomePage(page, app.packageName)
    }

    fun removeAppFromDock(app: AppItem) {
        repository.removeAppFromDock(app.packageName)
    }

    fun moveAppWithinHomePage(page: Int, app: AppItem, toIndex: Int) {
        repository.moveAppWithinHomePage(page, app.packageName, toIndex)
    }

    fun moveAppWithinDock(app: AppItem, toIndex: Int) {
        repository.moveAppWithinDock(app.packageName, toIndex)
    }

    fun moveAppHomeToDock(page: Int, app: AppItem, dockIndex: Int) {
        repository.moveAppHomeToDock(page, app.packageName, dockIndex)
    }

    fun moveAppDockToHome(app: AppItem, page: Int, homeIndex: Int?) {
        repository.moveAppDockToHome(app.packageName, page, homeIndex)
    }
}
