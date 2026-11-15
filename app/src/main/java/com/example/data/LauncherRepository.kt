package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.example.model.AppCategory
import com.example.model.AppCustomization
import com.example.model.AppIconType
import com.example.model.AppItem
import com.example.model.ColorPaletteSeed
import com.example.model.DeviceMode
import com.example.model.DrawerLayoutMode
import com.example.model.HabitItem
import com.example.model.HomeGridAppPlacement
import com.example.model.IconPackStyle
import com.example.model.IconShape
import com.example.model.IconSize
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
import com.example.model.PlacedAppWidget
import com.example.model.PlacedHomeFolder
import com.example.model.TabletFolderBackdrop
import com.example.model.TabletFolderSize
import com.example.model.TaskItem
import com.example.model.AppFolder
import com.example.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LauncherRepository(
    private val context: Context,
    private val dao: LauncherDao
) {
    private val prefs = context.getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: Flow<LauncherPreferences> = _preferences.asStateFlow()

    private val _folders = MutableStateFlow(loadFolders())
    val allFolders: Flow<List<AppFolder>> = _folders.asStateFlow()

    private val _dockPackages = MutableStateFlow(loadDockPackages())
    val dockPackages: Flow<List<String>> = _dockPackages.asStateFlow()

    private val _homePages = MutableStateFlow(loadHomePages())
    val homePages: Flow<Map<Int, List<String>>> = _homePages.asStateFlow()

    private val _homeGridApps = MutableStateFlow(loadHomeGridApps())
    val homeGridApps: Flow<Map<Int, List<HomeGridAppPlacement>>> = _homeGridApps.asStateFlow()

    private val _placedWidgets = MutableStateFlow(loadPlacedWidgets())
    val placedWidgets: Flow<List<PlacedAppWidget>> = _placedWidgets.asStateFlow()

    private val _placedHomeFolders = MutableStateFlow(loadPlacedHomeFolders())
    val placedHomeFolders: Flow<List<PlacedHomeFolder>> = _placedHomeFolders.asStateFlow()

    val allNotes: Flow<List<NoteItem>> = dao.getAllNotes()
    val allTasks: Flow<List<TaskItem>> = dao.getAllTasks()
    val allHabits: Flow<List<HabitItem>> = dao.getAllHabits()
    val allCustomizations: Flow<List<AppCustomization>> = dao.getAllCustomizations()

    private fun loadPreferences(): LauncherPreferences {
        val themeStr = prefs.getString("theme_mode", ThemeMode.MINIMAL_DARK.name)
        val paletteStr = prefs.getString("color_palette", ColorPaletteSeed.LAVENDER.name)
        val deviceModeStr = prefs.getString("device_mode", DeviceMode.AUTO.name)
        val iconShapeStr = prefs.getString("icon_shape", IconShape.SQUIRCLE.name)
        val iconPackStr = prefs.getString("icon_pack", IconPackStyle.MATERIAL_YOU_THEMED.name)
        val iconSizeStr = prefs.getString("icon_size", IconSize.STANDARD.name)
        val tabletFolderSizeStr = prefs.getString("tablet_folder_size", TabletFolderSize.LARGE.name)
        val tabletFolderShapeStr = prefs.getString("tablet_expanded_folder_icon_shape", null)
        val tabletFolderBackdropStr = prefs.getString("tablet_folder_backdrop", TabletFolderBackdrop.TONAL_SURFACE.name)
        val drawerModeStr = prefs.getString("drawer_mode", DrawerLayoutMode.GRID_4.name)
        val showLabels = prefs.getBoolean("show_labels", true)
        val autoLaunch = prefs.getBoolean("auto_launch_single", false)
        val enableSearch = prefs.getBoolean("enable_smart_search", true)
        val enableShelf = prefs.getBoolean("enable_productivity_shelf", true)
        val allowThirdPartyWidgets = prefs.getBoolean("allow_third_party_widgets", true)
        val dynamicWallpaperColors = prefs.getBoolean("dynamic_wallpaper_colors", true)
        val hapticFeedbackEnabled = prefs.getBoolean("haptic_feedback_enabled", true)
        val showHomeGridGuides = prefs.getBoolean("show_home_grid_guides", false)
        val dockCount = prefs.getInt("dock_count", 5)
        val wallpaperIdx = prefs.getInt("wallpaper_idx", 0)
        val pageCount = prefs.getInt("home_page_count", 1)
        val enabledWidgets = prefs.getStringSet("enabled_widgets", null) ?: setOf(
            "at_a_glance", "quick_notes", "tasks", "habits", "quick_toggles", "screen_time"
        )

        return LauncherPreferences(
            themeMode = runCatching { ThemeMode.valueOf(themeStr ?: "") }.getOrDefault(ThemeMode.MINIMAL_DARK),
            colorPaletteSeed = runCatching { ColorPaletteSeed.valueOf(paletteStr ?: "") }.getOrDefault(ColorPaletteSeed.LAVENDER),
            deviceMode = runCatching { DeviceMode.valueOf(deviceModeStr ?: "") }.getOrDefault(DeviceMode.AUTO),
            iconShape = runCatching { IconShape.valueOf(iconShapeStr ?: "") }.getOrDefault(IconShape.SQUIRCLE),
            iconPackStyle = runCatching { IconPackStyle.valueOf(iconPackStr ?: "") }.getOrDefault(IconPackStyle.MATERIAL_YOU_THEMED),
            iconSize = runCatching { IconSize.valueOf(iconSizeStr ?: "") }.getOrDefault(IconSize.STANDARD),
            tabletFolderSize = runCatching { TabletFolderSize.valueOf(tabletFolderSizeStr ?: "") }.getOrDefault(TabletFolderSize.LARGE),
            tabletExpandedFolderIconShape = tabletFolderShapeStr?.takeIf { it.isNotBlank() }?.let {
                runCatching { IconShape.valueOf(it) }.getOrNull()
            },
            tabletFolderBackdrop = runCatching { TabletFolderBackdrop.valueOf(tabletFolderBackdropStr ?: "") }
                .getOrDefault(TabletFolderBackdrop.TONAL_SURFACE),
            drawerLayoutMode = runCatching { DrawerLayoutMode.valueOf(drawerModeStr ?: "") }.getOrDefault(DrawerLayoutMode.GRID_4),
            showAppLabels = showLabels,
            autoLaunchSingleMatch = autoLaunch,
            enableSmartSearch = enableSearch,
            enableProductivityShelf = enableShelf,
            allowThirdPartyWidgets = allowThirdPartyWidgets,
            dynamicWallpaperColors = dynamicWallpaperColors,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            showHomeGridGuides = showHomeGridGuides,
            dockAppCount = dockCount,
            wallpaperThemeIndex = wallpaperIdx,
            homePageCount = pageCount.coerceIn(1, 5),
            enabledWidgets = enabledWidgets
        )
    }

    fun updatePreferences(update: (LauncherPreferences) -> LauncherPreferences) {
        val newPrefs = update(_preferences.value)
        _preferences.value = newPrefs
        prefs.edit().apply {
            putString("theme_mode", newPrefs.themeMode.name)
            putString("color_palette", newPrefs.colorPaletteSeed.name)
            putString("device_mode", newPrefs.deviceMode.name)
            putString("icon_shape", newPrefs.iconShape.name)
            putString("icon_pack", newPrefs.iconPackStyle.name)
            putString("icon_size", newPrefs.iconSize.name)
            putString("tablet_folder_size", newPrefs.tabletFolderSize.name)
            putString("tablet_expanded_folder_icon_shape", newPrefs.tabletExpandedFolderIconShape?.name ?: "")
            putString("tablet_folder_backdrop", newPrefs.tabletFolderBackdrop.name)
            putString("drawer_mode", newPrefs.drawerLayoutMode.name)
            putBoolean("show_labels", newPrefs.showAppLabels)
            putBoolean("auto_launch_single", newPrefs.autoLaunchSingleMatch)
            putBoolean("enable_smart_search", newPrefs.enableSmartSearch)
            putBoolean("enable_productivity_shelf", newPrefs.enableProductivityShelf)
            putBoolean("allow_third_party_widgets", newPrefs.allowThirdPartyWidgets)
            putBoolean("dynamic_wallpaper_colors", newPrefs.dynamicWallpaperColors)
            putBoolean("haptic_feedback_enabled", newPrefs.hapticFeedbackEnabled)
            putBoolean("show_home_grid_guides", newPrefs.showHomeGridGuides)
            putInt("dock_count", newPrefs.dockAppCount)
            putInt("wallpaper_idx", newPrefs.wallpaperThemeIndex)
            putInt("home_page_count", newPrefs.homePageCount)
            putStringSet("enabled_widgets", newPrefs.enabledWidgets)
            apply()
        }
    }

    suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val myPackageName = context.packageName
        val scannedApps = resolveInfos
            .filter { it.activityInfo.packageName != myPackageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val cls = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager).toString()
                AppItem(
                    id = "$pkg/$cls",
                    packageName = pkg,
                    className = cls,
                    label = label,
                    category = guessCategory(label, pkg),
                    isSystemApp = false,
                    iconType = AppIconType.SYSTEM_PACKAGE,
                    iconGlyphName = guessIconGlyph(label)
                )
            }

        val auroraSettingsApp = AppItem(
            id = "com.aurora.launcher.settings",
            packageName = "com.aurora.launcher.settings",
            label = "Aurora Settings",
            category = AppCategory.TOOLS,
            iconColor = 0xFF7C4DFF,
            iconType = AppIconType.BUILTIN_CODE,
            iconGlyphName = "settings",
            isFavorite = true
        )

        val crashAssistantApp = AppItem(
            id = "com.aurora.launcher.crashassistant",
            packageName = "com.aurora.launcher.crashassistant",
            label = "Crash Assistant",
            category = AppCategory.TOOLS,
            iconColor = 0xFF9D00FF,
            iconType = AppIconType.BUILTIN_CODE,
            iconGlyphName = "bug_report",
            isFavorite = false
        )

        // Strictly show only real installed apps on the device + built-in launcher apps
        val builtInApps = listOf(auroraSettingsApp, crashAssistantApp)
        val existingPkgs = scannedApps.map { it.packageName }.toSet()
        val finalApps = scannedApps + builtInApps.filter { it.packageName !in existingPkgs }

        val sortedApps = finalApps.sortedBy { it.label.lowercase() }
        initializeHomeAndDockIfNeeded(sortedApps)
        sortedApps
    }

    private fun guessCategory(label: String, pkg: String): AppCategory {
        val lower = "$label $pkg".lowercase()
        return when {
            lower.contains("message") || lower.contains("chat") || lower.contains("phone") ||
                    lower.contains("contact") || lower.contains("mail") || lower.contains("social") ||
                    lower.contains("telegram") || lower.contains("whatsapp") -> AppCategory.COMMUNICATION

            lower.contains("music") || lower.contains("audio") || lower.contains("video") ||
                    lower.contains("player") || lower.contains("camera") || lower.contains("photo") ||
                    lower.contains("gallery") || lower.contains("youtube") || lower.contains("spotify") -> AppCategory.MEDIA

            lower.contains("note") || lower.contains("task") || lower.contains("todo") ||
                    lower.contains("calendar") || lower.contains("doc") || lower.contains("sheet") ||
                    lower.contains("office") || lower.contains("drive") || lower.contains("slack") -> AppCategory.PRODUCTIVITY

            lower.contains("game") || lower.contains("play") || lower.contains("arcade") -> AppCategory.GAMES

            else -> AppCategory.TOOLS
        }
    }

    private fun guessIconGlyph(label: String): String {
        val lower = label.lowercase()
        return when {
            lower.contains("camera") -> "camera"
            lower.contains("photo") || lower.contains("gallery") -> "photo"
            lower.contains("chrome") || lower.contains("browser") || lower.contains("web") -> "browser"
            lower.contains("music") || lower.contains("spotify") -> "music"
            lower.contains("message") || lower.contains("chat") || lower.contains("sms") -> "message"
            lower.contains("phone") || lower.contains("dial") -> "phone"
            lower.contains("mail") || lower.contains("gmail") -> "mail"
            lower.contains("map") -> "map"
            lower.contains("setting") -> "settings"
            lower.contains("clock") || lower.contains("alarm") -> "clock"
            lower.contains("calc") -> "calculator"
            lower.contains("calendar") -> "calendar"
            lower.contains("note") -> "note"
            lower.contains("file") -> "folder"
            else -> "apps"
        }
    }

    private fun getCuratedPresets(): List<AppItem> {
        return listOf(
            AppItem(
                id = "com.aurora.launcher.settings",
                packageName = "com.aurora.launcher.settings",
                label = "Aurora Settings",
                category = AppCategory.TOOLS,
                iconColor = 0xFF7C4DFF,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "settings",
                isFavorite = true
            ),
            AppItem(
                id = "com.google.android.dialer",
                packageName = "com.google.android.dialer",
                label = "Phone",
                category = AppCategory.COMMUNICATION,
                iconColor = 0xFF1976D2,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "phone",
                isDocked = true
            ),
            AppItem(
                id = "com.google.android.apps.messaging",
                packageName = "com.google.android.apps.messaging",
                label = "Messages",
                category = AppCategory.COMMUNICATION,
                iconColor = 0xFF0288D1,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "message",
                isDocked = true
            ),
            AppItem(
                id = "com.android.chrome",
                packageName = "com.android.chrome",
                label = "Chrome",
                category = AppCategory.PRODUCTIVITY,
                iconColor = 0xFFFBBC04,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "browser",
                isDocked = true
            ),
            AppItem(
                id = "com.google.android.GoogleCamera",
                packageName = "com.google.android.GoogleCamera",
                label = "Camera",
                category = AppCategory.MEDIA,
                iconColor = 0xFFEA4335,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "camera",
                isDocked = true
            ),
            AppItem(
                id = "com.google.android.apps.photos",
                packageName = "com.google.android.apps.photos",
                label = "Photos",
                category = AppCategory.MEDIA,
                iconColor = 0xFF34A853,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "photo",
                isDocked = true
            ),
            AppItem(
                id = "com.spotify.music",
                packageName = "com.spotify.music",
                label = "Spotify",
                category = AppCategory.MEDIA,
                iconColor = 0xFF1DB954,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "music",
                isFavorite = true
            ),
            AppItem(
                id = "com.google.android.calendar",
                packageName = "com.google.android.calendar",
                label = "Calendar",
                category = AppCategory.PRODUCTIVITY,
                iconColor = 0xFF4285F4,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "calendar",
                isFavorite = true
            ),
            AppItem(
                id = "com.google.android.keep",
                packageName = "com.google.android.keep",
                label = "Notes",
                category = AppCategory.PRODUCTIVITY,
                iconColor = 0xFFFB8C00,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "note",
                isFavorite = true
            ),
            AppItem(
                id = "com.google.android.deskclock",
                packageName = "com.google.android.deskclock",
                label = "Clock",
                category = AppCategory.TOOLS,
                iconColor = 0xFF5C6BC0,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "clock"
            ),
            AppItem(
                id = "com.google.android.calculator",
                packageName = "com.google.android.calculator",
                label = "Calculator",
                category = AppCategory.TOOLS,
                iconColor = 0xFF00ACC1,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "calculator"
            ),
            AppItem(
                id = "com.google.android.gm",
                packageName = "com.google.android.gm",
                label = "Gmail",
                category = AppCategory.COMMUNICATION,
                iconColor = 0xFFD93025,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "mail"
            ),
            AppItem(
                id = "com.google.android.apps.maps",
                packageName = "com.google.android.apps.maps",
                label = "Maps",
                category = AppCategory.TOOLS,
                iconColor = 0xFF34A853,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "map"
            ),
            AppItem(
                id = "com.android.settings",
                packageName = "com.android.settings",
                label = "Settings",
                category = AppCategory.TOOLS,
                iconColor = 0xFF78909C,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "settings"
            ),
            AppItem(
                id = "com.google.android.youtube",
                packageName = "com.google.android.youtube",
                label = "YouTube",
                category = AppCategory.MEDIA,
                iconColor = 0xFFFF0000,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "video"
            ),
            AppItem(
                id = "com.slack",
                packageName = "com.slack",
                label = "Slack",
                category = AppCategory.PRODUCTIVITY,
                iconColor = 0xFF4A154B,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "chat"
            ),
            AppItem(
                id = "com.google.android.apps.docs",
                packageName = "com.google.android.apps.docs",
                label = "Drive",
                category = AppCategory.PRODUCTIVITY,
                iconColor = 0xFF0F9D58,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "folder"
            ),
            AppItem(
                id = "com.google.android.apps.nbu.files",
                packageName = "com.google.android.apps.nbu.files",
                label = "Files",
                category = AppCategory.TOOLS,
                iconColor = 0xFF4285F4,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "folder"
            ),
            AppItem(
                id = "com.instagram.android",
                packageName = "com.instagram.android",
                label = "Instagram",
                category = AppCategory.COMMUNICATION,
                iconColor = 0xFFE1306C,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "photo"
            ),
            AppItem(
                id = "com.twitter.android",
                packageName = "com.twitter.android",
                label = "X / Twitter",
                category = AppCategory.COMMUNICATION,
                iconColor = 0xFF14171A,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "chat"
            ),
            AppItem(
                id = "org.telegram.messenger",
                packageName = "org.telegram.messenger",
                label = "Telegram",
                category = AppCategory.COMMUNICATION,
                iconColor = 0xFF2AABEE,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "message"
            ),
            AppItem(
                id = "com.duolingo",
                packageName = "com.duolingo",
                label = "Duolingo",
                category = AppCategory.PRODUCTIVITY,
                iconColor = 0xFF58CC02,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "book"
            ),
            AppItem(
                id = "com.fitbit.FitbitMobile",
                packageName = "com.fitbit.FitbitMobile",
                label = "Fitness & Health",
                category = AppCategory.TOOLS,
                iconColor = 0xFF00B0B9,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "fitness"
            ),
            AppItem(
                id = "com.valvesoftware.android.steam.community",
                packageName = "com.valvesoftware.android.steam.community",
                label = "Steam Gaming",
                category = AppCategory.GAMES,
                iconColor = 0xFF171A21,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "games"
            ),
            AppItem(
                id = "com.nianticlabs.pokemongo",
                packageName = "com.nianticlabs.pokemongo",
                label = "Pokemon GO",
                category = AppCategory.GAMES,
                iconColor = 0xFFFF5722,
                iconType = AppIconType.BUILTIN_CODE,
                iconGlyphName = "games"
            )
        )
    }

    // App customization helpers
    suspend fun setAppCustomLabel(packageName: String, label: String?) {
        val current = dao.getCustomization(packageName) ?: AppCustomization(packageName)
        dao.saveCustomization(current.copy(customLabel = label))
    }

    suspend fun setAppHidden(packageName: String, isHidden: Boolean) {
        val current = dao.getCustomization(packageName) ?: AppCustomization(packageName)
        dao.saveCustomization(current.copy(isHidden = isHidden))
        if (isHidden) {
            removeAppFromDock(packageName)
            removeAppFromAllHomePages(packageName)
        }
    }

    suspend fun setAppFavorite(packageName: String, isFavorite: Boolean) {
        val current = dao.getCustomization(packageName) ?: AppCustomization(packageName)
        dao.saveCustomization(current.copy(isFavorite = isFavorite))
    }

    suspend fun setAppDocked(packageName: String, isDocked: Boolean) {
        val current = dao.getCustomization(packageName) ?: AppCustomization(packageName)
        dao.saveCustomization(current.copy(isDocked = isDocked))
        if (isDocked) {
            addAppToDock(packageName)
        } else {
            removeAppFromDock(packageName)
        }
    }

    suspend fun recordAppLaunch(packageName: String) {
        dao.recordLaunch(packageName)
    }

    // Notes
    suspend fun addNote(content: String) {
        dao.insertNote(NoteItem(content = content))
    }

    suspend fun deleteNote(note: NoteItem) {
        dao.deleteNote(note)
    }

    // Tasks
    suspend fun addTask(title: String, category: String = "General") {
        dao.insertTask(TaskItem(title = title, category = category))
    }

    suspend fun toggleTask(task: TaskItem) {
        dao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: TaskItem) {
        dao.deleteTask(task)
    }

    // Habits
    suspend fun addHabit(title: String, iconName: String) {
        dao.insertHabit(HabitItem(title = title, iconName = iconName))
    }

    suspend fun toggleHabit(habit: HabitItem) {
        val newStreak = if (!habit.isCompletedToday) habit.currentStreak + 1 else maxOf(0, habit.currentStreak - 1)
        dao.updateHabit(habit.copy(isCompletedToday = !habit.isCompletedToday, currentStreak = newStreak))
    }

    suspend fun deleteHabit(habit: HabitItem) {
        dao.deleteHabit(habit)
    }

    // App Folders
    private fun loadFolders(): List<AppFolder> {
        val jsonStr = prefs.getString("launcher_app_folders", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AppFolder>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Folder")
                val color = obj.optLong("color", 0xFF7C4DFF)
                val pkgsArray = obj.optJSONArray("packages")
                val pkgs = mutableListOf<String>()
                if (pkgsArray != null) {
                    for (j in 0 until pkgsArray.length()) {
                        pkgs.add(pkgsArray.getString(j))
                    }
                }
                list.add(AppFolder(id = id, name = name, packageNames = pkgs, colorSeed = color))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveFolders(folders: List<AppFolder>) {
        _folders.value = folders
        try {
            val jsonArray = JSONArray()
            folders.forEach { folder ->
                val obj = JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("color", folder.colorSeed)
                    val pkgs = JSONArray()
                    folder.packageNames.forEach { pkgs.put(it) }
                    put("packages", pkgs)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("launcher_app_folders", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun createFolder(name: String, initialPackages: List<String> = emptyList()): AppFolder {
        val current = _folders.value.toMutableList()
        val newFolder = AppFolder(name = name, packageNames = initialPackages)
        current.add(newFolder)
        saveFolders(current)
        return newFolder
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        val current = _folders.value.map { folder ->
            if (folder.id == folderId) {
                if (packageName !in folder.packageNames) {
                    folder.copy(packageNames = folder.packageNames + packageName)
                } else folder
            } else folder
        }
        saveFolders(current)
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val current = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(packageNames = folder.packageNames - packageName)
            } else folder
        }
        saveFolders(current)
    }

    fun deleteFolder(folderId: String) {
        val current = _folders.value.filter { it.id != folderId }
        saveFolders(current)
        removeFolderFromHome(folderId)
    }

    fun renameFolder(folderId: String, newName: String) {
        val current = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(name = newName)
            } else folder
        }
        saveFolders(current)
    }

    // App Vault Password Management
    fun hasVaultPassword(): Boolean {
        return !prefs.getString("vault_password_hash", null).isNullOrBlank()
    }

    fun setVaultPassword(password: String) {
        prefs.edit().putString("vault_password_hash", password.trim()).apply()
    }

    fun verifyVaultPassword(password: String): Boolean {
        val saved = prefs.getString("vault_password_hash", null) ?: return false
        return saved == password.trim()
    }

    // Home Screen & Dock Persisted Layout Management
    private fun loadDockPackages(): List<String> {
        val jsonStr = prefs.getString("dock_packages_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            List(arr.length()) { i -> arr.getString(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDockPackages(packages: List<String>) {
        val clean = packages.distinct().take(5)
        _dockPackages.value = clean
        val arr = JSONArray()
        clean.forEach { arr.put(it) }
        prefs.edit().putString("dock_packages_json", arr.toString()).apply()
    }

    private fun loadHomePages(): Map<Int, List<String>> {
        val jsonStr = prefs.getString("home_pages_json", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(jsonStr)
            val result = mutableMapOf<Int, List<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val pageIdx = k.toIntOrNull() ?: continue
                val arr = obj.getJSONArray(k)
                result[pageIdx] = List(arr.length()) { i -> arr.getString(i) }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveHomePages(pages: Map<Int, List<String>>) {
        _homePages.value = pages
        val obj = JSONObject()
        pages.forEach { (page, pkgs) ->
            val arr = JSONArray()
            pkgs.distinct().forEach { arr.put(it) }
            obj.put(page.toString(), arr)
        }
        prefs.edit().putString("home_pages_json", obj.toString()).apply()
    }

    private fun loadHomeGridApps(): Map<Int, List<HomeGridAppPlacement>> {
        val jsonStr = prefs.getString("home_grid_apps_json", null)
        if (jsonStr != null) {
            return try {
                val obj = JSONObject(jsonStr)
                val result = mutableMapOf<Int, List<HomeGridAppPlacement>>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val pageIdx = k.toIntOrNull() ?: continue
                    val arr = obj.getJSONArray(k)
                    val list = mutableListOf<HomeGridAppPlacement>()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val pkg = item.optString("packageName", "")
                        if (pkg.isNotBlank()) {
                            list.add(
                                HomeGridAppPlacement(
                                    packageName = pkg,
                                    page = pageIdx,
                                    row = item.optInt("row", i / 4).coerceAtLeast(0),
                                    col = item.optInt("col", i % 4).coerceAtLeast(0)
                                )
                            )
                        }
                    }
                    result[pageIdx] = list.distinctBy { it.packageName }
                }
                result
            } catch (e: Exception) {
                emptyMap()
            }
        }
        // Migrate legacy sequential homePages into grid coordinates
        val legacy = loadHomePages()
        if (legacy.isNotEmpty()) {
            val migrated = legacy.mapValues { (pageIdx, pkgs) ->
                pkgs.distinct().mapIndexed { idx, pkg ->
                    HomeGridAppPlacement(
                        packageName = pkg,
                        page = pageIdx,
                        row = idx / 4,
                        col = idx % 4
                    )
                }
            }
            saveHomeGridApps(migrated)
            return migrated
        }
        return emptyMap()
    }

    private fun saveHomeGridApps(gridMap: Map<Int, List<HomeGridAppPlacement>>) {
        val cleanMap = gridMap.mapValues { (_, list) -> list.distinctBy { it.packageName } }
        _homeGridApps.value = cleanMap

        val obj = JSONObject()
        val legacyPages = mutableMapOf<Int, List<String>>()
        cleanMap.forEach { (page, placements) ->
            val arr = JSONArray()
            val sorted = placements.sortedWith(compareBy({ it.row }, { it.col }))
            sorted.forEach { p ->
                arr.put(
                    JSONObject().apply {
                        put("packageName", p.packageName)
                        put("row", p.row)
                        put("col", p.col)
                    }
                )
            }
            obj.put(page.toString(), arr)
            legacyPages[page] = sorted.map { it.packageName }
        }
        prefs.edit().putString("home_grid_apps_json", obj.toString()).apply()
        saveHomePages(legacyPages)
    }

    private fun loadPlacedWidgets(): List<PlacedAppWidget> {
        val jsonStr = prefs.getString("placed_app_widgets_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<PlacedAppWidget>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optInt("appWidgetId", -1)
                val pkg = obj.optString("providerPackage", "")
                val cls = obj.optString("providerClass", "")
                if (id >= 0 && pkg.isNotBlank() && cls.isNotBlank()) {
                    list.add(
                        PlacedAppWidget(
                            appWidgetId = id,
                            providerPackage = pkg,
                            providerClass = cls,
                            label = obj.optString("label", "Widget"),
                            page = obj.optInt("page", 0).coerceAtLeast(0),
                            row = obj.optInt("row", 0).coerceAtLeast(0),
                            col = obj.optInt("col", 0).coerceAtLeast(0),
                            spanX = obj.optInt("spanX", 2).coerceIn(1, 6),
                            spanY = obj.optInt("spanY", 2).coerceIn(1, 6)
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlacedWidgets(widgets: List<PlacedAppWidget>) {
        _placedWidgets.value = widgets
        val arr = JSONArray()
        widgets.forEach { w ->
            arr.put(
                JSONObject().apply {
                    put("appWidgetId", w.appWidgetId)
                    put("providerPackage", w.providerPackage)
                    put("providerClass", w.providerClass)
                    put("label", w.label)
                    put("page", w.page)
                    put("row", w.row)
                    put("col", w.col)
                    put("spanX", w.spanX)
                    put("spanY", w.spanY)
                }
            )
        }
        prefs.edit().putString("placed_app_widgets_json", arr.toString()).apply()
    }

    private fun initializeHomeAndDockIfNeeded(installedApps: List<AppItem>) {
        if (!prefs.contains("dock_packages_json")) {
            val preferredDock = installedApps
                .filter { it.packageName != "com.aurora.launcher.settings" }
                .sortedByDescending { app ->
                    val name = "${app.label} ${app.packageName}".lowercase()
                    when {
                        "dialer" in name || "phone" in name -> 5
                        "messaging" in name || "sms" in name || "messages" in name -> 4
                        "chrome" in name || "browser" in name -> 3
                        "camera" in name -> 2
                        "photos" in name || "gallery" in name -> 1
                        else -> 0
                    }
                }
                .take(4)
                .map { it.packageName }
            saveDockPackages(preferredDock)
        }

        if (!prefs.contains("home_grid_apps_json") && !prefs.contains("home_pages_json")) {
            val dockSet = _dockPackages.value.toSet()
            val settingsPkg = installedApps.firstOrNull { it.packageName == "com.aurora.launcher.settings" }
            val others = installedApps
                .filter { it.packageName !in dockSet && it.packageName != "com.aurora.launcher.settings" }
                .take(7)
            val initialList = (listOfNotNull(settingsPkg) + others).mapIndexed { idx, app ->
                HomeGridAppPlacement(
                    packageName = app.packageName,
                    page = 0,
                    row = idx / 4,
                    col = idx % 4
                )
            }
            saveHomeGridApps(mapOf(0 to initialList))
        }
    }

    private fun loadPlacedHomeFolders(): List<PlacedHomeFolder> {
        val jsonStr = prefs.getString("placed_home_folders_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<PlacedHomeFolder>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val folderId = obj.optString("folderId", "")
                if (folderId.isNotBlank()) {
                    list.add(
                        PlacedHomeFolder(
                            folderId = folderId,
                            page = obj.optInt("page", 0).coerceAtLeast(0),
                            row = obj.optInt("row", 0).coerceAtLeast(0),
                            col = obj.optInt("col", 0).coerceAtLeast(0),
                            spanX = obj.optInt("spanX", 1).coerceIn(1, 6),
                            spanY = obj.optInt("spanY", 1).coerceIn(1, 6)
                        )
                    )
                }
            }
            list.distinctBy { it.folderId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlacedHomeFolders(folders: List<PlacedHomeFolder>) {
        val clean = folders.distinctBy { it.folderId }
        _placedHomeFolders.value = clean
        val arr = JSONArray()
        clean.forEach { f ->
            arr.put(
                JSONObject().apply {
                    put("folderId", f.folderId)
                    put("page", f.page)
                    put("row", f.row)
                    put("col", f.col)
                    put("spanX", f.spanX)
                    put("spanY", f.spanY)
                }
            )
        }
        prefs.edit().putString("placed_home_folders_json", arr.toString()).apply()
    }

    private fun isCellCoveredByWidget(
        page: Int,
        row: Int,
        col: Int,
        excludeWidgetId: Int? = null,
        excludeFolderId: String? = null
    ): Boolean {
        val hitWidget = _placedWidgets.value.any { w ->
            w.page == page &&
                w.appWidgetId != excludeWidgetId &&
                row in w.row until (w.row + w.spanY) &&
                col in w.col until (w.col + w.spanX)
        }
        if (hitWidget) return true
        return _placedHomeFolders.value.any { f ->
            f.page == page &&
                f.folderId != excludeFolderId &&
                row in f.row until (f.row + f.spanY) &&
                col in f.col until (f.col + f.spanX)
        }
    }

    private fun findFreeCell(
        page: Int,
        cols: Int,
        rows: Int,
        placements: List<HomeGridAppPlacement>,
        excludeWidgetId: Int? = null
    ): Pair<Int, Int>? {
        val occupiedByApps = placements.map { it.row to it.col }.toSet()
        for (r in 0 until rows.coerceAtLeast(1)) {
            for (c in 0 until cols.coerceAtLeast(1)) {
                if ((r to c) !in occupiedByApps && !isCellCoveredByWidget(page, r, c, excludeWidgetId)) {
                    return r to c
                }
            }
        }
        return null
    }

    private fun findFreeWidgetOrigin(
        page: Int,
        spanX: Int,
        spanY: Int,
        cols: Int,
        rows: Int,
        preferredRow: Int? = null,
        preferredCol: Int? = null,
        excludeWidgetId: Int? = null,
        excludeFolderId: String? = null
    ): Pair<Int, Int>? {
        val maxStartRow = (rows - spanY).coerceAtLeast(0)
        val maxStartCol = (cols - spanX).coerceAtLeast(0)

        fun fitsAt(r: Int, c: Int): Boolean {
            if (r < 0 || c < 0 || r + spanY > rows || c + spanX > cols) return false
            for (rr in r until r + spanY) {
                for (cc in c until c + spanX) {
                    if (isCellCoveredByWidget(page, rr, cc, excludeWidgetId, excludeFolderId)) return false
                }
            }
            return true
        }

        if (preferredRow != null && preferredCol != null) {
            val clampedR = preferredRow.coerceIn(0, maxStartRow)
            val clampedC = preferredCol.coerceIn(0, maxStartCol)
            if (fitsAt(clampedR, clampedC)) return clampedR to clampedC
        }

        for (r in 0..maxStartRow) {
            for (c in 0..maxStartCol) {
                if (fitsAt(r, c)) return r to c
            }
        }
        return null
    }

    private fun evacuateAppsCoveredByWidget(
        page: Int,
        widget: PlacedAppWidget,
        cols: Int,
        rows: Int
    ) {
        val gridMap = _homeGridApps.value.toMutableMap()
        val list = (gridMap[page] ?: return).toMutableList()
        val covered = list.filter { p ->
            p.row in widget.row until (widget.row + widget.spanY) &&
                p.col in widget.col until (widget.col + widget.spanX)
        }
        if (covered.isEmpty()) return

        list.removeAll(covered)
        covered.forEach { displaced ->
            val free = findFreeCell(page, cols, rows, list)
                ?: findFreeCell(page, cols, rows + 2, list)
            if (free != null) {
                list.add(displaced.copy(row = free.first, col = free.second))
            }
        }
        gridMap[page] = list
        saveHomeGridApps(gridMap)
    }

    fun addAppToDock(packageName: String, index: Int? = null) {
        val current = _dockPackages.value.toMutableList()
        current.remove(packageName)
        if (current.size >= 5) {
            current.removeAt(current.lastIndex)
        }
        val insertIdx = (index ?: current.size).coerceIn(0, current.size)
        current.add(insertIdx, packageName)
        saveDockPackages(current)
    }

    fun removeAppFromDock(packageName: String) {
        val current = _dockPackages.value.toMutableList()
        if (current.remove(packageName)) {
            saveDockPackages(current)
        }
    }

    fun moveAppWithinDock(packageName: String, toIndex: Int) {
        val current = _dockPackages.value.toMutableList()
        val oldIdx = current.indexOf(packageName)
        if (oldIdx < 0) return
        current.removeAt(oldIdx)
        val clamped = toIndex.coerceIn(0, current.size)
        current.add(clamped, packageName)
        saveDockPackages(current)
    }

    fun addAppToHomePage(
        page: Int,
        packageName: String,
        targetRow: Int? = null,
        targetCol: Int? = null,
        cols: Int = 4,
        rows: Int = 5
    ) {
        val gridMap = _homeGridApps.value.toMutableMap()
        val list = (gridMap[page] ?: emptyList()).toMutableList()
        val existingSelf = list.firstOrNull { it.packageName == packageName }
        if (targetRow != null && targetCol != null) {
            placeAppOnHomeGrid(page, packageName, targetRow, targetCol, cols, rows)
            return
        }
        if (existingSelf != null) return
        val free = findFreeCell(page, cols, rows, list)
            ?: findFreeCell(page, cols, rows + 3, list)
            ?: (0 to 0)
        list.add(HomeGridAppPlacement(packageName = packageName, page = page, row = free.first, col = free.second))
        gridMap[page] = list
        saveHomeGridApps(gridMap)
    }

    fun placeAppOnHomeGrid(
        page: Int,
        packageName: String,
        targetRow: Int,
        targetCol: Int,
        cols: Int = 4,
        rows: Int = 5
    ) {
        val r = targetRow.coerceIn(0, (rows - 1).coerceAtLeast(0))
        val c = targetCol.coerceIn(0, (cols - 1).coerceAtLeast(0))
        val gridMap = _homeGridApps.value.toMutableMap()
        val list = (gridMap[page] ?: emptyList()).toMutableList()

        // Never drop an app directly on top of a placed widget cell
        if (isCellCoveredByWidget(page, r, c)) {
            val fallback = findFreeCell(page, cols, rows, list) ?: return
            return placeAppOnHomeGrid(page, packageName, fallback.first, fallback.second, cols, rows)
        }

        val oldPlacement = list.firstOrNull { it.packageName == packageName }
        val occupant = list.firstOrNull { it.row == r && it.col == c && it.packageName != packageName }

        list.removeAll { it.packageName == packageName }
        if (occupant != null) {
            list.removeAll { it.packageName == occupant.packageName }
            if (oldPlacement != null) {
                // Swap positions cleanly!
                list.add(occupant.copy(row = oldPlacement.row, col = oldPlacement.col))
            } else {
                // Push occupant to nearest available free cell
                val free = findFreeCell(page, cols, rows, list + HomeGridAppPlacement(packageName, page, r, c))
                if (free != null) {
                    list.add(occupant.copy(row = free.first, col = free.second))
                }
            }
        }

        list.add(HomeGridAppPlacement(packageName = packageName, page = page, row = r, col = c))
        gridMap[page] = list
        saveHomeGridApps(gridMap)
    }

    fun removeAppFromHomePage(page: Int, packageName: String) {
        val gridMap = _homeGridApps.value.toMutableMap()
        val list = (gridMap[page] ?: return).toMutableList()
        if (list.removeAll { it.packageName == packageName }) {
            gridMap[page] = list
            saveHomeGridApps(gridMap)
        }
    }

    private fun removeAppFromAllHomePages(packageName: String) {
        val gridMap = _homeGridApps.value.mapValues { (_, list) ->
            list.filter { it.packageName != packageName }
        }
        saveHomeGridApps(gridMap)
    }

    fun moveAppWithinHomePage(page: Int, packageName: String, toIndex: Int) {
        val cols = 4
        placeAppOnHomeGrid(page, packageName, toIndex / cols, toIndex % cols, cols, 6)
    }

    fun moveAppHomeToDock(page: Int, packageName: String, dockIndex: Int) {
        val dock = _dockPackages.value.toMutableList()
        if (packageName in dock) {
            moveAppWithinDock(packageName, dockIndex)
            return
        }

        val gridMap = _homeGridApps.value.toMutableMap()
        val pageList = (gridMap[page] ?: emptyList()).toMutableList()
        val oldPlacement = pageList.firstOrNull { it.packageName == packageName }
        pageList.removeAll { it.packageName == packageName }

        if (dock.size >= 5) {
            val swapIdx = dockIndex.coerceIn(0, dock.lastIndex)
            val displaced = dock.removeAt(swapIdx)
            dock.add(swapIdx, packageName)
            val row = oldPlacement?.row ?: 0
            val col = oldPlacement?.col ?: 0
            pageList.add(HomeGridAppPlacement(packageName = displaced, page = page, row = row, col = col))
        } else {
            val insertIdx = dockIndex.coerceIn(0, dock.size)
            dock.add(insertIdx, packageName)
        }

        gridMap[page] = pageList
        saveDockPackages(dock)
        saveHomeGridApps(gridMap)
    }

    fun moveAppDockToHome(packageName: String, page: Int, homeIndex: Int?) {
        val dock = _dockPackages.value.toMutableList()
        dock.remove(packageName)
        saveDockPackages(dock)
        val r = homeIndex?.div(4)
        val c = homeIndex?.rem(4)
        addAppToHomePage(page, packageName, r, c)
    }

    fun moveAppDockToHomeGrid(
        packageName: String,
        page: Int,
        targetRow: Int?,
        targetCol: Int?,
        cols: Int = 4,
        rows: Int = 5
    ) {
        val dock = _dockPackages.value.toMutableList()
        dock.remove(packageName)
        saveDockPackages(dock)
        addAppToHomePage(page, packageName, targetRow, targetCol, cols, rows)
    }

    // Third-Party & System Widget Grid Actions
    fun addPlacedWidget(
        widget: PlacedAppWidget,
        cols: Int,
        rows: Int
    ): PlacedAppWidget {
        val clampedSpanX = widget.spanX.coerceIn(1, cols.coerceAtLeast(1))
        val clampedSpanY = widget.spanY.coerceIn(1, rows.coerceAtLeast(1))
        val freeOrigin = findFreeWidgetOrigin(
            page = widget.page,
            spanX = clampedSpanX,
            spanY = clampedSpanY,
            cols = cols,
            rows = rows,
            preferredRow = widget.row,
            preferredCol = widget.col
        )
        val finalRow = freeOrigin?.first ?: widget.row.coerceIn(0, (rows - clampedSpanY).coerceAtLeast(0))
        val finalCol = freeOrigin?.second ?: widget.col.coerceIn(0, (cols - clampedSpanX).coerceAtLeast(0))

        val finalWidget = widget.copy(
            row = finalRow,
            col = finalCol,
            spanX = clampedSpanX,
            spanY = clampedSpanY
        )

        val updated = _placedWidgets.value.filterNot { it.appWidgetId == finalWidget.appWidgetId } + finalWidget
        savePlacedWidgets(updated)
        evacuateAppsCoveredByWidget(finalWidget.page, finalWidget, cols, rows)
        return finalWidget
    }

    fun movePlacedWidget(
        appWidgetId: Int,
        targetPage: Int,
        targetRow: Int,
        targetCol: Int,
        cols: Int,
        rows: Int
    ): Boolean {
        val existing = _placedWidgets.value.firstOrNull { it.appWidgetId == appWidgetId } ?: return false
        val spanX = existing.spanX.coerceIn(1, cols.coerceAtLeast(1))
        val spanY = existing.spanY.coerceIn(1, rows.coerceAtLeast(1))
        val maxRow = (rows - spanY).coerceAtLeast(0)
        val maxCol = (cols - spanX).coerceAtLeast(0)
        val clampedRow = targetRow.coerceIn(0, maxRow)
        val clampedCol = targetCol.coerceIn(0, maxCol)

        // Ensure no overlap with another widget
        for (rr in clampedRow until clampedRow + spanY) {
            for (cc in clampedCol until clampedCol + spanX) {
                if (isCellCoveredByWidget(targetPage, rr, cc, excludeWidgetId = appWidgetId)) {
                    return false
                }
            }
        }

        val moved = existing.copy(
            page = targetPage,
            row = clampedRow,
            col = clampedCol,
            spanX = spanX,
            spanY = spanY
        )
        savePlacedWidgets(_placedWidgets.value.map { if (it.appWidgetId == appWidgetId) moved else it })
        evacuateAppsCoveredByWidget(targetPage, moved, cols, rows)
        return true
    }

    fun resizePlacedWidget(
        appWidgetId: Int,
        newSpanX: Int,
        newSpanY: Int,
        cols: Int,
        rows: Int
    ): Boolean {
        val existing = _placedWidgets.value.firstOrNull { it.appWidgetId == appWidgetId } ?: return false
        val spanX = newSpanX.coerceIn(1, cols.coerceAtLeast(1))
        val spanY = newSpanY.coerceIn(1, rows.coerceAtLeast(1))
        val adjustedRow = existing.row.coerceIn(0, (rows - spanY).coerceAtLeast(0))
        val adjustedCol = existing.col.coerceIn(0, (cols - spanX).coerceAtLeast(0))

        for (rr in adjustedRow until adjustedRow + spanY) {
            for (cc in adjustedCol until adjustedCol + spanX) {
                if (isCellCoveredByWidget(existing.page, rr, cc, excludeWidgetId = appWidgetId)) {
                    return false
                }
            }
        }

        val resized = existing.copy(
            row = adjustedRow,
            col = adjustedCol,
            spanX = spanX,
            spanY = spanY
        )
        savePlacedWidgets(_placedWidgets.value.map { if (it.appWidgetId == appWidgetId) resized else it })
        evacuateAppsCoveredByWidget(existing.page, resized, cols, rows)
        return true
    }

    fun updatePlacedWidgetBounds(
        appWidgetId: Int,
        targetRow: Int,
        targetCol: Int,
        newSpanX: Int,
        newSpanY: Int,
        cols: Int,
        rows: Int
    ): Boolean {
        val existing = _placedWidgets.value.firstOrNull { it.appWidgetId == appWidgetId } ?: return false
        val safeCols = cols.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        val spanX = newSpanX.coerceIn(1, safeCols)
        val spanY = newSpanY.coerceIn(1, safeRows)
        val adjustedRow = targetRow.coerceIn(0, (safeRows - spanY).coerceAtLeast(0))
        val adjustedCol = targetCol.coerceIn(0, (safeCols - spanX).coerceAtLeast(0))

        for (rr in adjustedRow until adjustedRow + spanY) {
            for (cc in adjustedCol until adjustedCol + spanX) {
                if (isCellCoveredByWidget(existing.page, rr, cc, excludeWidgetId = appWidgetId)) {
                    return false
                }
            }
        }

        val updated = existing.copy(
            row = adjustedRow,
            col = adjustedCol,
            spanX = spanX,
            spanY = spanY
        )
        savePlacedWidgets(_placedWidgets.value.map { if (it.appWidgetId == appWidgetId) updated else it })
        evacuateAppsCoveredByWidget(existing.page, updated, safeCols, safeRows)
        return true
    }

    fun removePlacedWidget(appWidgetId: Int) {
        val updated = _placedWidgets.value.filterNot { it.appWidgetId == appWidgetId }
        savePlacedWidgets(updated)
    }

    private fun evacuateAppsCoveredByRect(
        page: Int,
        row: Int,
        col: Int,
        spanX: Int,
        spanY: Int,
        cols: Int,
        rows: Int
    ) {
        val gridMap = _homeGridApps.value.toMutableMap()
        val list = (gridMap[page] ?: return).toMutableList()
        val covered = list.filter { p ->
            p.row in row until (row + spanY) &&
                p.col in col until (col + spanX)
        }
        if (covered.isEmpty()) return

        list.removeAll(covered)
        covered.forEach { displaced ->
            val free = findFreeCell(page, cols, rows, list)
                ?: findFreeCell(page, cols, rows + 2, list)
            if (free != null) {
                list.add(displaced.copy(row = free.first, col = free.second))
            }
        }
        gridMap[page] = list
        saveHomeGridApps(gridMap)
    }

    // Resizable Home Screen Folders
    fun placeFolderOnHome(
        folderId: String,
        page: Int,
        preferredRow: Int?,
        preferredCol: Int?,
        spanX: Int,
        spanY: Int,
        cols: Int,
        rows: Int
    ): PlacedHomeFolder {
        val safeCols = cols.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        var targetSpanX = spanX.coerceIn(1, safeCols)
        var targetSpanY = spanY.coerceIn(1, safeRows)

        var origin = findFreeWidgetOrigin(
            page = page,
            spanX = targetSpanX,
            spanY = targetSpanY,
            cols = safeCols,
            rows = safeRows,
            preferredRow = preferredRow,
            preferredCol = preferredCol,
            excludeFolderId = folderId
        )
        if (origin == null && (targetSpanX > 1 || targetSpanY > 1)) {
            targetSpanX = 1
            targetSpanY = 1
            origin = findFreeWidgetOrigin(
                page = page,
                spanX = 1,
                spanY = 1,
                cols = safeCols,
                rows = safeRows,
                preferredRow = preferredRow,
                preferredCol = preferredCol,
                excludeFolderId = folderId
            )
        }

        val finalRow = origin?.first ?: (preferredRow ?: 0).coerceIn(0, (safeRows - targetSpanY).coerceAtLeast(0))
        val finalCol = origin?.second ?: (preferredCol ?: 0).coerceIn(0, (safeCols - targetSpanX).coerceAtLeast(0))

        val placed = PlacedHomeFolder(
            folderId = folderId,
            page = page,
            row = finalRow,
            col = finalCol,
            spanX = targetSpanX,
            spanY = targetSpanY
        )

        val updated = _placedHomeFolders.value.filterNot { it.folderId == folderId } + placed
        savePlacedHomeFolders(updated)
        evacuateAppsCoveredByRect(page, finalRow, finalCol, targetSpanX, targetSpanY, safeCols, safeRows)
        return placed
    }

    fun updateHomeFolderBounds(
        folderId: String,
        targetRow: Int,
        targetCol: Int,
        newSpanX: Int,
        newSpanY: Int,
        cols: Int,
        rows: Int
    ): Boolean {
        val existing = _placedHomeFolders.value.firstOrNull { it.folderId == folderId } ?: return false
        val safeCols = cols.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        val spanX = newSpanX.coerceIn(1, safeCols)
        val spanY = newSpanY.coerceIn(1, safeRows)
        val adjustedRow = targetRow.coerceIn(0, (safeRows - spanY).coerceAtLeast(0))
        val adjustedCol = targetCol.coerceIn(0, (safeCols - spanX).coerceAtLeast(0))

        for (rr in adjustedRow until adjustedRow + spanY) {
            for (cc in adjustedCol until adjustedCol + spanX) {
                if (isCellCoveredByWidget(existing.page, rr, cc, excludeFolderId = folderId)) {
                    return false
                }
            }
        }

        val updatedFolder = existing.copy(
            row = adjustedRow,
            col = adjustedCol,
            spanX = spanX,
            spanY = spanY
        )
        savePlacedHomeFolders(_placedHomeFolders.value.map { if (it.folderId == folderId) updatedFolder else it })
        evacuateAppsCoveredByRect(existing.page, adjustedRow, adjustedCol, spanX, spanY, safeCols, safeRows)
        return true
    }

    fun removeFolderFromHome(folderId: String) {
        val updated = _placedHomeFolders.value.filterNot { it.folderId == folderId }
        if (updated.size != _placedHomeFolders.value.size) {
            savePlacedHomeFolders(updated)
        }
    }
}
