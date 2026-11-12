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
import com.example.model.DrawerLayoutMode
import com.example.model.HabitItem
import com.example.model.IconPackStyle
import com.example.model.IconSize
import com.example.model.LauncherPreferences
import com.example.model.NoteItem
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

    val allNotes: Flow<List<NoteItem>> = dao.getAllNotes()
    val allTasks: Flow<List<TaskItem>> = dao.getAllTasks()
    val allHabits: Flow<List<HabitItem>> = dao.getAllHabits()
    val allCustomizations: Flow<List<AppCustomization>> = dao.getAllCustomizations()

    private fun loadPreferences(): LauncherPreferences {
        val themeStr = prefs.getString("theme_mode", ThemeMode.MINIMAL_DARK.name)
        val paletteStr = prefs.getString("color_palette", ColorPaletteSeed.LAVENDER.name)
        val iconPackStr = prefs.getString("icon_pack", IconPackStyle.MATERIAL_YOU_THEMED.name)
        val iconSizeStr = prefs.getString("icon_size", IconSize.STANDARD.name)
        val drawerModeStr = prefs.getString("drawer_mode", DrawerLayoutMode.GRID_4.name)
        val showLabels = prefs.getBoolean("show_labels", true)
        val autoLaunch = prefs.getBoolean("auto_launch_single", false)
        val dockCount = prefs.getInt("dock_count", 5)
        val wallpaperIdx = prefs.getInt("wallpaper_idx", 0)
        val pageCount = prefs.getInt("home_page_count", 1)
        val enabledWidgets = prefs.getStringSet("enabled_widgets", null) ?: setOf(
            "at_a_glance", "quick_notes", "tasks", "habits", "quick_toggles", "screen_time"
        )

        return LauncherPreferences(
            themeMode = runCatching { ThemeMode.valueOf(themeStr ?: "") }.getOrDefault(ThemeMode.MINIMAL_DARK),
            colorPaletteSeed = runCatching { ColorPaletteSeed.valueOf(paletteStr ?: "") }.getOrDefault(ColorPaletteSeed.LAVENDER),
            iconPackStyle = runCatching { IconPackStyle.valueOf(iconPackStr ?: "") }.getOrDefault(IconPackStyle.MATERIAL_YOU_THEMED),
            iconSize = runCatching { IconSize.valueOf(iconSizeStr ?: "") }.getOrDefault(IconSize.STANDARD),
            drawerLayoutMode = runCatching { DrawerLayoutMode.valueOf(drawerModeStr ?: "") }.getOrDefault(DrawerLayoutMode.GRID_4),
            showAppLabels = showLabels,
            autoLaunchSingleMatch = autoLaunch,
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
            putString("icon_pack", newPrefs.iconPackStyle.name)
            putString("icon_size", newPrefs.iconSize.name)
            putString("drawer_mode", newPrefs.drawerLayoutMode.name)
            putBoolean("show_labels", newPrefs.showAppLabels)
            putBoolean("auto_launch_single", newPrefs.autoLaunchSingleMatch)
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

        // Strictly show only real installed apps on the device + Aurora Settings
        val finalApps = if (scannedApps.any { it.packageName == "com.aurora.launcher.settings" }) {
            scannedApps
        } else {
            scannedApps + auroraSettingsApp
        }

        finalApps.sortedBy { it.label.lowercase() }
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
    }

    suspend fun setAppFavorite(packageName: String, isFavorite: Boolean) {
        val current = dao.getCustomization(packageName) ?: AppCustomization(packageName)
        dao.saveCustomization(current.copy(isFavorite = isFavorite))
    }

    suspend fun setAppDocked(packageName: String, isDocked: Boolean) {
        val current = dao.getCustomization(packageName) ?: AppCustomization(packageName)
        dao.saveCustomization(current.copy(isDocked = isDocked))
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

    fun createFolder(name: String, initialPackages: List<String> = emptyList()) {
        val current = _folders.value.toMutableList()
        val newFolder = AppFolder(name = name, packageNames = initialPackages)
        current.add(newFolder)
        saveFolders(current)
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
}
