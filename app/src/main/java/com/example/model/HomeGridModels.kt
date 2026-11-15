package com.example.model

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class HomeGridAppPlacement(
    val packageName: String,
    val page: Int,
    val row: Int,
    val col: Int
)

@Immutable
data class PlacedAppWidget(
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val label: String,
    val page: Int,
    val row: Int,
    val col: Int,
    val spanX: Int = 2,
    val spanY: Int = 2
)

@Immutable
data class PlacedHomeFolder(
    val folderId: String,
    val page: Int,
    val row: Int,
    val col: Int,
    val spanX: Int = 1,
    val spanY: Int = 1
)

data class SystemWidgetProviderEntry(
    val providerInfo: AppWidgetProviderInfo,
    val appName: String,
    val widgetTitle: String,
    val packageName: String,
    val className: String,
    val defaultSpanX: Int,
    val defaultSpanY: Int,
    val isSystemApp: Boolean,
    val hasConfigureActivity: Boolean,
    val previewDrawable: Drawable?,
    val appIconDrawable: Drawable?
)
