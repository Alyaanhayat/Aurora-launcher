package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.util.IconPackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

val FailsafePurplePrimary = Color(0xFF9D00FF)
val FailsafePurpleDark = Color(0xFF280044)
val FailsafePurpleSurface = Color(0xFF5A0096)
val FailsafePurpleBorder = Color(0xFFE08BFF)

@Immutable
data class RenderFaultRecord(
    val component: String,
    val reason: String,
    val details: String = "",
    val isDriverIssue: Boolean = false,
    val wasFatalCrash: Boolean = false,
    val dateTime: String = RenderFailsafeMonitor.formatDateTime(System.currentTimeMillis()),
    val deviceInfo: String = RenderFailsafeMonitor.buildDeviceSummary(),
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val key: String get() = "$component::$reason"
}

@Immutable
data class CrashLogEntry(
    val id: String,
    val component: String,
    val reason: String,
    val technicalLog: String,
    val dateTime: String,
    val deviceName: String,
    val androidVersion: String,
    val hardwareInfo: String,
    val isDriverIssue: Boolean,
    val wasFatalCrash: Boolean,
    val timestampMillis: Long
) {
    fun formatTechnicalReport(): String = buildString {
        appendLine("=== Aurora Launcher Crash & Render Report ===")
        appendLine("Date & Time: $dateTime")
        appendLine("Device: $deviceName")
        appendLine("Android OS: $androidVersion")
        appendLine("Hardware / ABI: $hardwareInfo")
        appendLine("Severity: ${if (wasFatalCrash) "Fatal Crash" else "UI / Render Failure"}")
        appendLine("Driver / GPU Issue: ${if (isDriverIssue) "Yes" else "No"}")
        appendLine("Component: $component")
        appendLine("Reason: $reason")
        if (technicalLog.isNotBlank()) {
            appendLine("--- Technical Logs ---")
            appendLine(technicalLog)
        }
    }
}

object RenderFailsafeMonitor {
    private const val TAG = "AuroraRenderFailsafe"
    private const val PREF_NAME = "aurora_render_failsafe_prefs"
    private const val KEY_CRASH_COMPONENT = "last_crash_component"
    private const val KEY_CRASH_REASON = "last_crash_reason"
    private const val KEY_CRASH_DETAILS = "last_crash_details"
    private const val KEY_CRASH_DRIVER = "last_crash_driver"
    private const val KEY_CRASH_DATETIME = "last_crash_datetime"
    private const val KEY_CRASH_DEVICE = "last_crash_device"
    private const val KEY_SOFTWARE_FALLBACK = "software_fallback_active"
    private const val KEY_CRASH_HISTORY_JSON = "crash_assistant_history_json"
    private const val MAX_HISTORY_ENTRIES = 60

    @Volatile
    private var appContext: Context? = null

    private val _faults = MutableStateFlow<List<RenderFaultRecord>>(emptyList())
    val faults: StateFlow<List<RenderFaultRecord>> = _faults.asStateFlow()

    private val _crashHistory = MutableStateFlow<List<CrashLogEntry>>(emptyList())
    val crashHistory: StateFlow<List<CrashLogEntry>> = _crashHistory.asStateFlow()

    private val _autoReportPromptVisible = MutableStateFlow(false)
    val autoReportPromptVisible: StateFlow<Boolean> = _autoReportPromptVisible.asStateFlow()

    private val _simulatedTestMode = MutableStateFlow(false)
    val simulatedTestMode: StateFlow<Boolean> = _simulatedTestMode.asStateFlow()

    private val _softwareFallbackActive = MutableStateFlow(false)
    val softwareFallbackActive: StateFlow<Boolean> = _softwareFallbackActive.asStateFlow()

    fun formatDateTime(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(millis))
    }

    fun buildDeviceSummary(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return "$manufacturer $model (Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT})"
    }

    private fun buildDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        return if (Build.MODEL.startsWith(manufacturer, ignoreCase = true)) Build.MODEL
        else "$manufacturer ${Build.MODEL}"
    }

    private fun buildAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    private fun buildHardwareInfo(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        return "$abi • ${Build.BOARD} / ${Build.HARDWARE}"
    }

    fun init(context: Context): Boolean {
        val ctx = context.applicationContext
        appContext = ctx
        loadHistoryFromPrefs(ctx)
        return restorePersistedCrashFaultIfAny(ctx)
    }

    fun isGraphicsOrDriverIssue(throwable: Throwable): Boolean {
        val msg = (throwable.message ?: "").lowercase()
        val cls = throwable.javaClass.name.lowercase()
        return msg.contains("vulkan") ||
            msg.contains("opengl") ||
            msg.contains("egl") ||
            msg.contains("skia") ||
            msg.contains("hwui") ||
            msg.contains("renderthread") ||
            msg.contains("rendernode") ||
            msg.contains("hardware") ||
            msg.contains("surface") ||
            msg.contains("driver") ||
            msg.contains("gpu") ||
            msg.contains("canvas") ||
            cls.contains("outofmemory") ||
            cls.contains("render")
    }

    fun reportFault(
        component: String,
        reason: String,
        throwable: Throwable? = null,
        isDriverIssue: Boolean = throwable?.let { isGraphicsOrDriverIssue(it) } ?: false,
        triggerReportDialog: Boolean = true,
        wasFatalCrash: Boolean = false
    ): RenderFaultRecord {
        val now = System.currentTimeMillis()
        val formattedTime = formatDateTime(now)
        val deviceSummary = buildDeviceSummary()
        val details = throwable?.let {
            val trace = Log.getStackTraceString(it).take(1200)
            if (trace.isNotBlank()) trace else "${it.javaClass.simpleName}: ${it.message ?: "Unknown error"}"
        } ?: reason

        val record = RenderFaultRecord(
            component = component,
            reason = reason,
            details = details,
            isDriverIssue = isDriverIssue,
            wasFatalCrash = wasFatalCrash,
            dateTime = formattedTime,
            deviceInfo = deviceSummary,
            timestampMillis = now
        )
        Log.e(TAG, "[$component] $reason ($formattedTime | $deviceSummary)\n$details", throwable)

        val current = _faults.value
        val alreadyExists = current.any { it.key == record.key }
        val updated = (listOf(record) + current.filterNot { it.key == record.key }).take(20)
        _faults.value = updated

        if (!alreadyExists) {
            appContext?.let { ctx ->
                appendHistoryEntry(
                    context = ctx,
                    entry = CrashLogEntry(
                        id = UUID.randomUUID().toString(),
                        component = component,
                        reason = reason,
                        technicalLog = details,
                        dateTime = formattedTime,
                        deviceName = buildDeviceName(),
                        androidVersion = buildAndroidVersion(),
                        hardwareInfo = buildHardwareInfo(),
                        isDriverIssue = isDriverIssue,
                        wasFatalCrash = wasFatalCrash,
                        timestampMillis = now
                    ),
                    commitSync = false
                )
            }
            if (triggerReportDialog) {
                _autoReportPromptVisible.value = true
            }
        }
        return record
    }

    fun dismissAutoReportPrompt() {
        _autoReportPromptVisible.value = false
    }

    fun openReportPrompt() {
        if (_faults.value.isNotEmpty()) {
            _autoReportPromptVisible.value = true
        }
    }

    fun setSimulatedTestMode(enabled: Boolean) {
        _simulatedTestMode.value = enabled
        if (enabled) {
            reportFault(
                component = "Render Failsafe Test",
                reason = "Simulated UI render / driver failure",
                throwable = IllegalStateException("Simulated UI render fault triggered from Aurora Settings / Crash Assistant"),
                isDriverIssue = true,
                triggerReportDialog = true,
                wasFatalCrash = false
            )
        } else {
            _faults.value = _faults.value.filterNot { it.component == "Render Failsafe Test" }
            _autoReportPromptVisible.value = false
        }
    }

    fun persistCrashFault(context: Context, component: String, throwable: Throwable) {
        val now = System.currentTimeMillis()
        val formattedTime = formatDateTime(now)
        val deviceSummary = buildDeviceSummary()
        val driver = isGraphicsOrDriverIssue(throwable)
        val reason = if (driver) {
            "Graphics / display driver render failure"
        } else {
            "Unhandled UI / runtime crash"
        }
        val stack = Log.getStackTraceString(throwable).take(1600)
        val technicalLog = "${throwable.javaClass.name}: ${throwable.message}\n$stack"

        val entry = CrashLogEntry(
            id = UUID.randomUUID().toString(),
            component = component,
            reason = reason,
            technicalLog = technicalLog,
            dateTime = formattedTime,
            deviceName = buildDeviceName(),
            androidVersion = buildAndroidVersion(),
            hardwareInfo = buildHardwareInfo(),
            isDriverIssue = driver,
            wasFatalCrash = true,
            timestampMillis = now
        )
        appendHistoryEntry(context, entry, commitSync = true)

        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CRASH_COMPONENT, component)
                .putString(KEY_CRASH_REASON, reason)
                .putString(KEY_CRASH_DETAILS, technicalLog)
                .putString(KEY_CRASH_DATETIME, formattedTime)
                .putString(KEY_CRASH_DEVICE, deviceSummary)
                .putBoolean(KEY_CRASH_DRIVER, driver)
                .putBoolean(KEY_SOFTWARE_FALLBACK, driver)
                .commit()
        }
    }

    fun restorePersistedCrashFaultIfAny(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val swFallback = prefs.getBoolean(KEY_SOFTWARE_FALLBACK, false)
        _softwareFallbackActive.value = swFallback
        val component = prefs.getString(KEY_CRASH_COMPONENT, null)
        val reason = prefs.getString(KEY_CRASH_REASON, null)
        val details = prefs.getString(KEY_CRASH_DETAILS, "") ?: ""
        val dateTime = prefs.getString(KEY_CRASH_DATETIME, null) ?: formatDateTime(System.currentTimeMillis())
        val deviceInfo = prefs.getString(KEY_CRASH_DEVICE, null) ?: buildDeviceSummary()
        val driver = prefs.getBoolean(KEY_CRASH_DRIVER, false)
        if (!component.isNullOrBlank() && !reason.isNullOrBlank()) {
            val record = RenderFaultRecord(
                component = component,
                reason = reason,
                details = details,
                isDriverIssue = driver,
                wasFatalCrash = true,
                dateTime = dateTime,
                deviceInfo = deviceInfo
            )
            _faults.value = listOf(record) + _faults.value.filterNot { it.key == record.key }
            _autoReportPromptVisible.value = true
            return true
        }
        return false
    }

    fun clearActiveFaults(context: Context) {
        _simulatedTestMode.value = false
        _faults.value = emptyList()
        _autoReportPromptVisible.value = false
        _softwareFallbackActive.value = false
        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CRASH_COMPONENT)
                .remove(KEY_CRASH_REASON)
                .remove(KEY_CRASH_DETAILS)
                .remove(KEY_CRASH_DATETIME)
                .remove(KEY_CRASH_DEVICE)
                .remove(KEY_CRASH_DRIVER)
                .remove(KEY_SOFTWARE_FALLBACK)
                .apply()
        }
    }

    fun clearCrashHistory(context: Context) {
        _crashHistory.value = emptyList()
        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CRASH_HISTORY_JSON)
                .apply()
        }
    }

    private fun loadHistoryFromPrefs(context: Context) {
        runCatching {
            val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CRASH_HISTORY_JSON, null)
                ?: return
            val arr = JSONArray(raw)
            val parsed = mutableListOf<CrashLogEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsed.add(
                    CrashLogEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        component = obj.optString("component", "Unknown"),
                        reason = obj.optString("reason", "Unknown failure"),
                        technicalLog = obj.optString("technicalLog", ""),
                        dateTime = obj.optString("dateTime", ""),
                        deviceName = obj.optString("deviceName", buildDeviceName()),
                        androidVersion = obj.optString("androidVersion", buildAndroidVersion()),
                        hardwareInfo = obj.optString("hardwareInfo", buildHardwareInfo()),
                        isDriverIssue = obj.optBoolean("isDriverIssue", false),
                        wasFatalCrash = obj.optBoolean("wasFatalCrash", false),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            _crashHistory.value = parsed.sortedByDescending { it.timestampMillis }
        }
    }

    private fun appendHistoryEntry(context: Context, entry: CrashLogEntry, commitSync: Boolean) {
        val current = _crashHistory.value
        val updated = (listOf(entry) + current).take(MAX_HISTORY_ENTRIES)
        _crashHistory.value = updated

        runCatching {
            val arr = JSONArray()
            updated.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("component", item.component)
                        put("reason", item.reason)
                        put("technicalLog", item.technicalLog)
                        put("dateTime", item.dateTime)
                        put("deviceName", item.deviceName)
                        put("androidVersion", item.androidVersion)
                        put("hardwareInfo", item.hardwareInfo)
                        put("isDriverIssue", item.isDriverIssue)
                        put("wasFatalCrash", item.wasFatalCrash)
                        put("timestampMillis", item.timestampMillis)
                    }
                )
            }
            val editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CRASH_HISTORY_JSON, arr.toString())
            if (commitSync) editor.commit() else editor.apply()
        }
    }

    fun formatFaultReport(fault: RenderFaultRecord): String = buildString {
        appendLine("=== Aurora Launcher Purple Failsafe Report ===")
        appendLine("Date & Time: ${fault.dateTime}")
        appendLine("Device: ${fault.deviceInfo}")
        appendLine("Hardware / ABI: ${buildHardwareInfo()}")
        appendLine("Driver / GPU Issue: ${if (fault.isDriverIssue) "Yes" else "No"}")
        appendLine("Fatal Crash: ${if (fault.wasFatalCrash) "Yes" else "No"}")
        appendLine("Component: ${fault.component}")
        appendLine("Reason: ${fault.reason}")
        if (fault.details.isNotBlank()) {
            appendLine("--- Technical Logs ---")
            appendLine(fault.details)
        }
    }

    fun copyFaultReportToClipboard(context: Context, fault: RenderFaultRecord) {
        val summary = formatFaultReport(fault)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Aurora Render Report", summary))
        Toast.makeText(context, "Technical logs copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun copyCrashEntryToClipboard(context: Context, entry: CrashLogEntry) {
        val summary = entry.formatTechnicalReport()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Aurora Crash Log", summary))
        Toast.makeText(context, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareFaultReport(context: Context, fault: RenderFaultRecord) {
        shareText(context, "Aurora Render Report: ${fault.component}", formatFaultReport(fault))
    }

    fun shareCrashEntry(context: Context, entry: CrashLogEntry) {
        shareText(context, "Aurora Crash Assistant: ${entry.component}", entry.formatTechnicalReport())
    }

    private fun shareText(context: Context, subject: String, body: String) {
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Technical Logs").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(subject, body))
            Toast.makeText(context, "No share app found — technical logs copied", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun DrawScope.drawInlinePurpleCheckerboard(tilePx: Float = 28f) {
    val cols = ceil(size.width / tilePx).toInt().coerceAtLeast(1)
    val rows = ceil(size.height / tilePx).toInt().coerceAtLeast(1)
    drawRect(color = FailsafePurpleDark)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if ((r + c) % 2 == 0) {
                drawRect(
                    color = FailsafePurplePrimary,
                    topLeft = Offset(c * tilePx, r * tilePx),
                    size = Size(tilePx + 0.5f, tilePx + 0.5f)
                )
            }
        }
    }
}

fun Modifier.failsafeDrawBoundary(componentName: String): Modifier = this.drawWithContent {
    try {
        drawContent()
    } catch (t: Throwable) {
        RenderFailsafeMonitor.reportFault(
            component = componentName,
            reason = "Canvas / UI draw failure",
            throwable = t,
            isDriverIssue = true,
            triggerReportDialog = true
        )
        drawInlinePurpleCheckerboard(32f)
    }
}

@Composable
fun RenderErrorBoundary(
    componentName: String,
    shape: Shape = RoundedCornerShape(24.dp),
    modifier: Modifier = Modifier,
    onOpenCrashAssistant: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val faults by RenderFailsafeMonitor.faults.collectAsState()
    val matchingFault = faults.firstOrNull { it.component == componentName }

    Box(modifier = modifier.failsafeDrawBoundary(componentName)) {
        if (matchingFault != null) {
            PurpleFailsafeCard(
                fault = matchingFault,
                shape = shape,
                onTapDetails = { RenderFailsafeMonitor.openReportPrompt() },
                onOpenCrashAssistant = onOpenCrashAssistant
            )
        } else {
            content()
        }
    }
}

@Composable
fun rememberSafeDrawableBitmap(
    drawable: Drawable?,
    sizePx: Int,
    componentName: String
): Pair<ImageBitmap?, RenderFaultRecord?> {
    return remember(drawable, sizePx, componentName) {
        if (drawable == null) {
            null to null
        } else {
            val targetPx = sizePx.coerceIn(48, 256)
            val result = runCatching {
                val bmp = drawable.toBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) {
                    error("Empty or recycled bitmap returned by driver")
                }
                bmp.asImageBitmap()
            }
            val bitmap = result.getOrNull()
            val error = result.exceptionOrNull()
            if (bitmap != null) {
                bitmap to null
            } else {
                val fault = RenderFailsafeMonitor.reportFault(
                    component = componentName,
                    reason = "Drawable/GPU bitmap render failed",
                    throwable = error
                )
                null to fault
            }
        }
    }
}

@Composable
fun rememberSafeImportedIconBitmap(
    filePath: String?,
    componentName: String
): Pair<ImageBitmap?, RenderFaultRecord?> {
    return remember(filePath, componentName) {
        if (filePath.isNullOrBlank()) {
            null to null
        } else {
            val result = runCatching {
                val bmp = IconPackManager.decodeSampledBitmap(filePath, reqSizePx = 192)
                    ?: error("Image decode failed for custom icon file")
                if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) {
                    error("Invalid decoded custom icon dimensions")
                }
                bmp.asImageBitmap()
            }
            val bitmap = result.getOrNull()
            val error = result.exceptionOrNull()
            if (bitmap != null) {
                bitmap to null
            } else {
                val fault = RenderFailsafeMonitor.reportFault(
                    component = componentName,
                    reason = "Custom icon file failed to render",
                    throwable = error
                )
                null to fault
            }
        }
    }
}

@Composable
fun PurpleCheckerboardBackdrop(
    tileSize: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val tilePx = tileSize.toPx().coerceAtLeast(8f)
        drawInlinePurpleCheckerboard(tilePx)
    }
}

@Composable
fun PurpleFailsafeIcon(
    size: Dp,
    shape: Shape,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.5.dp, FailsafePurpleBorder, shape)
    ) {
        PurpleCheckerboardBackdrop(
            tileSize = (size.value / 4f).dp.coerceAtLeast(8.dp),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.44f)
                .clip(CircleShape)
                .background(FailsafePurpleDark.copy(alpha = 0.88f))
                .border(1.dp, FailsafePurpleBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "$contentDescription render failsafe",
                tint = FailsafePurpleBorder,
                modifier = Modifier.size(size * 0.25f)
            )
        }
    }
}

@Composable
fun PurpleFailsafeCard(
    fault: RenderFaultRecord,
    shape: Shape = RoundedCornerShape(24.dp),
    onTapDetails: (() -> Unit)? = null,
    onOpenCrashAssistant: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(2.dp, FailsafePurpleBorder, shape)
            .then(
                if (onTapDetails != null) Modifier.clickable { onTapDetails() }
                else Modifier.clickable {
                    RenderFailsafeMonitor.shareFaultReport(context, fault)
                }
            )
            .testTag("purple_render_failsafe_card")
    ) {
        PurpleCheckerboardBackdrop(
            tileSize = 18.dp,
            modifier = Modifier.matchParentSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FailsafePurpleDark.copy(alpha = 0.92f))
                .border(1.dp, FailsafePurpleBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Render Failsafe",
                    tint = FailsafePurpleBorder,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (fault.isDriverIssue) "DRIVER RENDER FAILSAFE" else "RENDER FAILSAFE",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${fault.component}: ${fault.reason}",
                color = FailsafePurpleBorder,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${fault.dateTime} • ${fault.deviceInfo}",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onTapDetails?.invoke() ?: RenderFailsafeMonitor.shareFaultReport(context, fault) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FailsafePurplePrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (onOpenCrashAssistant != null) {
                    OutlinedButton(
                        onClick = onOpenCrashAssistant,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Crash Assistant", color = FailsafePurpleBorder, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PurpleFailsafeBanner(
    faults: List<RenderFaultRecord>,
    softwareFallbackActive: Boolean,
    onOpenDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latest = faults.firstOrNull() ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FailsafePurplePrimary)
            .border(1.5.dp, FailsafePurpleBorder, RoundedCornerShape(16.dp))
            .clickable { onOpenDetails() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("purple_failsafe_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Render Failsafe",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (softwareFallbackActive) {
                        "Purple Failsafe • Software Driver Mode"
                    } else {
                        "Purple Failsafe (${faults.size}) • ${latest.component}"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to open Report dialog & share logs",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear failsafe notice",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PurpleFailsafeDetailsDialog(
    faults: List<RenderFaultRecord>,
    softwareFallbackActive: Boolean,
    onClearAll: () -> Unit,
    onOpenCrashAssistant: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val latest = faults.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FailsafePurpleDark,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = FailsafePurpleBorder,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Report Render Failure",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (softwareFallbackActive) {
                        "A graphics or UI render error was caught and saved to Crash Assistant. Purple Failsafe is active—share these technical logs so it can be fixed:"
                    } else {
                        "A UI component failed to render cleanly and switched to Purple Failsafe mode. This failure has been logged in Crash Assistant:"
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.92f)
                )
                faults.forEach { fault ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FailsafePurpleSurface.copy(alpha = 0.72f))
                            .border(1.dp, FailsafePurpleBorder.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${fault.component}${if (fault.isDriverIssue) " (Driver)" else ""}",
                            color = FailsafePurpleBorder,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = fault.reason,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${fault.dateTime}\n${fault.deviceInfo}",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 10.sp
                        )
                        if (fault.details.isNotBlank() && fault.details != fault.reason) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fault.details.take(450),
                                color = Color.White.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                if (onOpenCrashAssistant != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenCrashAssistant()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Crash Assistant App", color = FailsafePurpleBorder, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (latest != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { RenderFailsafeMonitor.copyFaultReportToClipboard(context, latest) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = FailsafePurpleBorder,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", color = FailsafePurpleBorder)
                    }
                    Button(
                        onClick = { RenderFailsafeMonitor.shareFaultReport(context, latest) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FailsafePurplePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Logs")
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onClearAll) {
                    Text("Clear", color = FailsafePurpleBorder)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White)
                }
            }
        }
    )
}
