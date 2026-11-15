package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppIconType
import com.example.model.AppItem
import com.example.ui.components.RenderFaultRecord
import com.example.ui.components.RenderFailsafeMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared thread-safe LRU cache for decoded system & custom icon ImageBitmaps.
 * Eliminates redundant PackageManager IPC and Bitmap allocations on the main thread
 * during App Drawer scrolling, Smart Search filtering, and Folder popups.
 */
object AppIconBitmapCache {
    private val systemBitmapCache = object : LruCache<String, ImageBitmap>(220) {}
    private val importedBitmapCache = object : LruCache<String, ImageBitmap>(180) {}
    private val systemFaultCache = ConcurrentHashMap<String, RenderFaultRecord>()
    private val importedFaultCache = ConcurrentHashMap<String, RenderFaultRecord>()

    fun getSystemIcon(packageName: String): ImageBitmap? = systemBitmapCache.get(packageName)

    fun getSystemFault(packageName: String): RenderFaultRecord? = systemFaultCache[packageName]

    fun getImportedIcon(filePath: String): ImageBitmap? = importedBitmapCache.get(filePath)

    fun getImportedFault(filePath: String): RenderFaultRecord? = importedFaultCache[filePath]

    suspend fun loadSystemIcon(
        context: Context,
        packageName: String,
        targetPx: Int,
        componentName: String
    ): Pair<ImageBitmap?, RenderFaultRecord?> {
        systemBitmapCache.get(packageName)?.let { return it to null }
        systemFaultCache[packageName]?.let { return null to it }

        return withContext(Dispatchers.IO) {
            systemBitmapCache.get(packageName)?.let { return@withContext it to null }
            val safePx = targetPx.coerceIn(64, 192)
            val result = runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bmp = drawable.toBitmap(safePx, safePx, Bitmap.Config.ARGB_8888)
                if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) {
                    error("Empty or recycled bitmap returned by driver")
                }
                bmp.asImageBitmap()
            }
            val bitmap = result.getOrNull()
            if (bitmap != null) {
                systemBitmapCache.put(packageName, bitmap)
                systemFaultCache.remove(packageName)
                bitmap to null
            } else {
                val fault = RenderFailsafeMonitor.reportFault(
                    component = componentName,
                    reason = "System app icon failed to load",
                    throwable = result.exceptionOrNull()
                )
                systemFaultCache[packageName] = fault
                null to fault
            }
        }
    }

    suspend fun loadImportedIcon(
        filePath: String,
        componentName: String
    ): Pair<ImageBitmap?, RenderFaultRecord?> {
        importedBitmapCache.get(filePath)?.let { return it to null }
        importedFaultCache[filePath]?.let { return null to it }

        return withContext(Dispatchers.IO) {
            importedBitmapCache.get(filePath)?.let { return@withContext it to null }
            val result = runCatching {
                val bmp = IconPackManager.decodeSampledBitmap(filePath, reqSizePx = 160)
                    ?: error("Image decode failed for custom icon file")
                if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) {
                    error("Invalid decoded custom icon dimensions")
                }
                bmp.asImageBitmap()
            }
            val bitmap = result.getOrNull()
            if (bitmap != null) {
                importedBitmapCache.put(filePath, bitmap)
                importedFaultCache.remove(filePath)
                bitmap to null
            } else {
                val fault = RenderFailsafeMonitor.reportFault(
                    component = componentName,
                    reason = "Custom icon file failed to render",
                    throwable = result.exceptionOrNull()
                )
                importedFaultCache[filePath] = fault
                null to fault
            }
        }
    }

    suspend fun prewarmSystemIcons(context: Context, apps: List<AppItem>) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        for (app in apps) {
            if (app.iconType != AppIconType.SYSTEM_PACKAGE) continue
            if (systemBitmapCache.get(app.packageName) != null || systemFaultCache.containsKey(app.packageName)) {
                continue
            }
            runCatching {
                val drawable = pm.getApplicationIcon(app.packageName)
                val bmp = drawable.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
                if (!bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                    systemBitmapCache.put(app.packageName, bmp.asImageBitmap())
                }
            }
        }
    }

    suspend fun prewarmImportedIcons(paths: Collection<String>) = withContext(Dispatchers.IO) {
        for (path in paths) {
            if (path.isBlank() || importedBitmapCache.get(path) != null) continue
            runCatching {
                val bmp = IconPackManager.decodeSampledBitmap(path, reqSizePx = 160)
                if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                    importedBitmapCache.put(path, bmp.asImageBitmap())
                }
            }
        }
    }
}
