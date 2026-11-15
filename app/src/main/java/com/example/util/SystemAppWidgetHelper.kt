package com.example.util

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.SizeF
import com.example.model.SystemWidgetProviderEntry
import kotlin.math.ceil

object SystemAppWidgetHelper {
    const val APPWIDGET_HOST_ID = 1024
    const val REQUEST_CONFIGURE_APPWIDGET = 4091

    @Volatile
    private var host: AppWidgetHost? = null

    fun getHost(context: Context): AppWidgetHost {
        return host ?: synchronized(this) {
            host ?: AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID).also { host = it }
        }
    }

    fun startListening(context: Context) {
        runCatching { getHost(context).startListening() }
    }

    fun stopListening(context: Context) {
        runCatching { host?.stopListening() }
    }

    fun allocateAppWidgetId(context: Context): Int {
        return runCatching { getHost(context).allocateAppWidgetId() }.getOrDefault(-1)
    }

    fun deleteAppWidgetId(context: Context, appWidgetId: Int) {
        if (appWidgetId < 0) return
        runCatching { getHost(context).deleteAppWidgetId(appWidgetId) }
    }

    fun bindAppWidgetIdIfAllowed(
        context: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo
    ): Boolean {
        if (appWidgetId < 0) return false
        val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return false
        return runCatching {
            manager.bindAppWidgetIdIfAllowed(
                appWidgetId,
                providerInfo.profile,
                providerInfo.provider,
                null
            )
        }.getOrDefault(false)
    }

    fun createBindIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, providerInfo.profile)
        }
    }

    fun createSystemPickerIntent(appWidgetId: Int): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    fun launchConfigureActivity(
        activity: Activity,
        appWidgetId: Int,
        requestCode: Int = REQUEST_CONFIGURE_APPWIDGET
    ): Boolean {
        return runCatching {
            getHost(activity).startAppWidgetConfigureActivityForResult(
                activity,
                appWidgetId,
                0,
                requestCode,
                null
            )
            true
        }.getOrDefault(false)
    }

    fun getAppWidgetInfo(context: Context, appWidgetId: Int): AppWidgetProviderInfo? {
        if (appWidgetId < 0) return null
        return runCatching {
            AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
        }.getOrNull()
    }

    fun findProviderInfo(
        context: Context,
        providerPackage: String,
        providerClass: String
    ): AppWidgetProviderInfo? {
        return getAllRawProviders(context).firstOrNull { info ->
            info.provider.packageName == providerPackage && info.provider.className == providerClass
        }
    }

    fun createHostView(
        context: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo
    ): AppWidgetHostView? {
        return runCatching {
            val view = getHost(context).createView(context.applicationContext, appWidgetId, providerInfo)
            view.setAppWidget(appWidgetId, providerInfo)
            view
        }.getOrNull()
    }

    fun updateWidgetSize(hostView: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        val safeW = widthDp.coerceAtLeast(56)
        val safeH = heightDp.coerceAtLeast(56)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                hostView.updateAppWidgetSize(
                    Bundle(),
                    listOf(SizeF(safeW.toFloat(), safeH.toFloat()))
                )
            } else {
                @Suppress("DEPRECATION")
                hostView.updateAppWidgetSize(null, safeW, safeH, safeW, safeH)
            }
        }
    }

    fun computeDefaultSpans(
        info: AppWidgetProviderInfo,
        maxCols: Int,
        maxRows: Int
    ): Pair<Int, Int> {
        val minW = info.minWidth.coerceAtLeast(40)
        val minH = info.minHeight.coerceAtLeast(40)
        val rawSpanX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
            info.targetCellWidth
        } else {
            ceil((minW + 30) / 70.0).toInt()
        }
        val rawSpanY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
            info.targetCellHeight
        } else {
            ceil((minH + 30) / 70.0).toInt()
        }
        return rawSpanX.coerceIn(1, maxCols.coerceAtLeast(1)) to
            rawSpanY.coerceIn(1, maxRows.coerceAtLeast(1))
    }

    fun loadInstalledWidgetProviders(
        context: Context,
        maxCols: Int = 4,
        maxRows: Int = 4
    ): List<SystemWidgetProviderEntry> {
        val pm = context.packageManager
        val rawProviders = getAllRawProviders(context)
        val densityDpi = context.resources.displayMetrics.densityDpi

        return rawProviders.mapNotNull { info ->
            runCatching {
                val pkg = info.provider.packageName
                if (pkg == context.packageName) return@runCatching null

                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                    }.getOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                }

                val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
                val widgetTitle = runCatching { info.loadLabel(pm) }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: appName
                val isSystem = appInfo != null &&
                    ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
                        pkg.startsWith("com.android.") ||
                        pkg.startsWith("com.google.android."))

                val (spanX, spanY) = computeDefaultSpans(info, maxCols, maxRows)
                val preview = runCatching { info.loadPreviewImage(context, densityDpi) }.getOrNull()
                val icon = runCatching { info.loadIcon(context, densityDpi) }.getOrNull()
                    ?: appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }

                SystemWidgetProviderEntry(
                    providerInfo = info,
                    appName = appName,
                    widgetTitle = widgetTitle,
                    packageName = pkg,
                    className = info.provider.className,
                    defaultSpanX = spanX,
                    defaultSpanY = spanY,
                    isSystemApp = isSystem,
                    hasConfigureActivity = info.configure != null,
                    previewDrawable = preview,
                    appIconDrawable = icon
                )
            }.getOrNull()
        }.sortedWith(
            compareBy<SystemWidgetProviderEntry> { !it.isSystemApp }
                .thenBy { it.appName.lowercase() }
                .thenBy { it.widgetTitle.lowercase() }
        )
    }

    private fun getAllRawProviders(context: Context): List<AppWidgetProviderInfo> {
        val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull()
            ?: return emptyList()
        val combined = mutableListOf<AppWidgetProviderInfo>()
        val seen = HashSet<String>()

        runCatching {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val profiles = userManager?.userProfiles
            if (!profiles.isNullOrEmpty()) {
                profiles.forEach { profile ->
                    runCatching {
                        manager.getInstalledProvidersForProfile(profile).forEach { info ->
                            val key = "${info.provider.flattenToString()}::${info.profile}"
                            if (seen.add(key)) combined.add(info)
                        }
                    }
                }
            }
        }

        if (combined.isEmpty()) {
            runCatching {
                manager.installedProviders.forEach { info ->
                    val key = info.provider.flattenToString()
                    if (seen.add(key)) combined.add(info)
                }
            }
        }

        return combined
    }
}
