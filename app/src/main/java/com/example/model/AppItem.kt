package com.example.model

data class AppItem(
    val id: String,
    val packageName: String,
    val className: String = "",
    val label: String,
    val customLabel: String? = null,
    val category: AppCategory = AppCategory.PRODUCTIVITY,
    val isSystemApp: Boolean = false,
    val isFavorite: Boolean = false,
    val isDocked: Boolean = false,
    val isHidden: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchedTime: Long = 0L,
    val iconColor: Long = 0xFF6750A4,
    val iconType: AppIconType = AppIconType.BUILTIN_CODE,
    val iconGlyphName: String = "apps"
) {
    val displayLabel: String get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

enum class AppCategory(val title: String) {
    ALL("All"),
    PRODUCTIVITY("Productivity"),
    COMMUNICATION("Social & Chat"),
    MEDIA("Media & Audio"),
    TOOLS("Tools & System"),
    GAMES("Games")
}

enum class AppIconType {
    SYSTEM_PACKAGE,
    BUILTIN_CODE
}
