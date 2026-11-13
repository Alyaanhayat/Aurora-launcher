package com.example.model

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    MINIMAL_DARK("Minimal Dark"),
    AMOLED_BLACK("AMOLED Pure Black"),
    CLEAN_LIGHT("Clean Light")
}

enum class ColorPaletteSeed(
    val label: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val tertiaryColor: Long
) {
    LAVENDER("Monet Lavender", 0xFF6750A4, 0xFF625B71, 0xFF7D5260),
    OCEAN_BLUE("Ocean Slate", 0xFF1976D2, 0xFF0288D1, 0xFF0097A7),
    BOTANICAL_SAGE("Botanical Sage", 0xFF2E7D32, 0xFF558B2F, 0xFF689F38),
    SUNSET_AMBER("Warm Sunset", 0xFFE65100, 0xFFF57C00, 0xFFFFB74D),
    ROSE_BERRY("Rose Berry", 0xFFC2185B, 0xFFD81B60, 0xFFE91E63),
    MINIMAL_MONO("Monochrome Minimal", 0xFF374151, 0xFF4B5563, 0xFF6B7280)
}

enum class IconShape(val label: String) {
    SQUIRCLE("Squircle"),
    CIRCLE("Circle"),
    FLOWER("Flower"),
    STAR("Star")
}

enum class IconPackStyle(val label: String, val description: String) {
    MATERIAL_YOU_THEMED("Material You Themed", "Monochrome glyphs tinted with active dynamic palette"),
    MINIMAL_OUTLINE("Minimalist Outline", "Clean thin outlines, distraction-free"),
    VIBRANT_MATERIAL("Vibrant Material 3", "Vivid modern shapes with rich elevation"),
    CLASSIC_ROUND("Classic Round", "Smooth circular icons"),
    SQUIRCLE("Modern Squircle", "Gentle continuous curved squircle"),
    PEBBLE("Organic Pebble", "Asymmetric modern pebble shape"),
    IMPORTED_CUSTOM("Imported Icon Pack", "Custom icon pack imported from your files")
}

enum class IconSize(val label: String, val dpValue: Int) {
    COMPACT("Compact (46dp)", 46),
    STANDARD("Standard (54dp)", 54),
    LARGE("Comfortable (62dp)", 62)
}

enum class DrawerLayoutMode(val label: String, val description: String) {
    GRID_4("4-Column Grid", "Classic standard Android grid with icons and labels"),
    GRID_5("5-Column Compact", "Dense high-capacity grid for larger screens"),
    ALPHABETICAL_LIST("Alphabetical List", "Clean vertical list with A-Z section headers"),
    CATEGORIZED_TABS("Categorized Tabs", "Grouped by Productivity, Social, Media, Tools"),
    MINIMALIST_TEXT("Minimalist Text Only", "Distraction-free pure typography for digital wellbeing")
}

enum class HomeScreenWidget(val id: String, val title: String, val subtitle: String) {
    AT_A_GLANCE("at_a_glance", "At-a-Glance", "Clock, date, weather & battery level"),
    QUICK_NOTES("quick_notes", "Quick Scratchpad", "Instant bullet notes on your home screen"),
    TASKS("tasks", "Priority Tasks", "Interactive checklist with completion marks"),
    HABITS("habits", "Daily Habit Streaks", "Track daily routine streaks with quick check-offs"),
    SCREEN_TIME("screen_time", "Digital Wellbeing", "Usage stats and quick focus session timer"),
    QUICK_TOGGLES("quick_toggles", "System Quick Toggles", "Wi-Fi, Bluetooth, Torch, Sound & Battery modes"),
    MEDIA_PLAYER("media_player", "Now Playing Media", "Audio playback controls and track progress")
}

data class LauncherPreferences(
    val themeMode: ThemeMode = ThemeMode.MINIMAL_DARK,
    val colorPaletteSeed: ColorPaletteSeed = ColorPaletteSeed.LAVENDER,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconPackStyle: IconPackStyle = IconPackStyle.MATERIAL_YOU_THEMED,
    val iconSize: IconSize = IconSize.STANDARD,
    val showAppLabels: Boolean = true,
    val drawerLayoutMode: DrawerLayoutMode = DrawerLayoutMode.GRID_4,
    val dockAppCount: Int = 5,
    val autoLaunchSingleMatch: Boolean = false,
    val enableSmartSearch: Boolean = true,
    val enableProductivityShelf: Boolean = true,
    val allowThirdPartyWidgets: Boolean = true,
    val wallpaperThemeIndex: Int = 0,
    val homePageCount: Int = 1,
    val enabledWidgets: Set<String> = setOf(
        HomeScreenWidget.AT_A_GLANCE.id,
        HomeScreenWidget.QUICK_NOTES.id,
        HomeScreenWidget.TASKS.id,
        HomeScreenWidget.HABITS.id,
        HomeScreenWidget.QUICK_TOGGLES.id,
        HomeScreenWidget.SCREEN_TIME.id
    )
)
