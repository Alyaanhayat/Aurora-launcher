package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.Locale
import java.util.zip.ZipInputStream

data class ImportedIconEntry(
    val keyName: String,
    val displayName: String,
    val filePath: String
)

data class ImportedIconPackState(
    val packName: String? = null,
    val totalIcons: Int = 0,
    val matchedCount: Int = 0,
    val packageToPath: Map<String, String> = emptyMap(),
    val allIcons: List<ImportedIconEntry> = emptyList()
)

class IconPackManager(private val context: Context) {

    private val packRootDir: File
        get() = File(context.filesDir, "imported_icon_pack").apply { mkdirs() }

    private val iconsDir: File
        get() = File(packRootDir, "icons").apply { mkdirs() }

    private val metaFile: File
        get() = File(packRootDir, "meta.json")

    private val _state = MutableStateFlow(loadSavedState())
    val state: StateFlow<ImportedIconPackState> = _state.asStateFlow()

    // Persisted appfilter package -> normalized icon key, plus user manual overrides
    private var appFilterMap: MutableMap<String, String> = mutableMapOf()
    private var manualOverrides: MutableMap<String, String> = mutableMapOf()

    init {
        loadInternalMaps()
    }

    private fun loadInternalMaps() {
        if (!metaFile.exists()) return
        runCatching {
            val obj = JSONObject(metaFile.readText())
            appFilterMap.clear()
            obj.optJSONObject("appfilter")?.let { af ->
                val keys = af.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    appFilterMap[k] = af.getString(k)
                }
            }
            manualOverrides.clear()
            obj.optJSONObject("manual")?.let { man ->
                val keys = man.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val path = man.getString(k)
                    if (File(path).exists()) {
                        manualOverrides[k] = path
                    }
                }
            }
        }
    }

    private fun loadSavedState(): ImportedIconPackState {
        if (!metaFile.exists()) return ImportedIconPackState()
        return runCatching {
            val obj = JSONObject(metaFile.readText())
            val packName = obj.optString("packName").takeIf { it.isNotBlank() } ?: return ImportedIconPackState()
            val iconsArr = obj.optJSONArray("icons") ?: JSONArray()
            val allIcons = mutableListOf<ImportedIconEntry>()
            for (i in 0 until iconsArr.length()) {
                val item = iconsArr.getJSONObject(i)
                val path = item.optString("path")
                if (File(path).exists()) {
                    allIcons.add(
                        ImportedIconEntry(
                            keyName = item.optString("key"),
                            displayName = item.optString("display"),
                            filePath = path
                        )
                    )
                }
            }
            val pkgMap = mutableMapOf<String, String>()
            obj.optJSONObject("packageMap")?.let { mapObj ->
                val keys = mapObj.keys()
                while (keys.hasNext()) {
                    val pkg = keys.next()
                    val path = mapObj.getString(pkg)
                    if (File(path).exists()) {
                        pkgMap[pkg] = path
                    }
                }
            }
            ImportedIconPackState(
                packName = packName,
                totalIcons = allIcons.size,
                matchedCount = pkgMap.size,
                packageToPath = pkgMap,
                allIcons = allIcons
            )
        }.getOrDefault(ImportedIconPackState())
    }

    private fun persistState(state: ImportedIconPackState) {
        _state.value = state
        runCatching {
            val root = JSONObject().apply {
                put("packName", state.packName ?: "")
                val iconsArr = JSONArray()
                state.allIcons.forEach { entry ->
                    iconsArr.put(JSONObject().apply {
                        put("key", entry.keyName)
                        put("display", entry.displayName)
                        put("path", entry.filePath)
                    })
                }
                put("icons", iconsArr)
                val pkgObj = JSONObject()
                state.packageToPath.forEach { (k, v) -> pkgObj.put(k, v) }
                put("packageMap", pkgObj)
                val afObj = JSONObject()
                appFilterMap.forEach { (k, v) -> afObj.put(k, v) }
                put("appfilter", afObj)
                val manObj = JSONObject()
                manualOverrides.forEach { (k, v) -> manObj.put(k, v) }
                put("manual", manObj)
            }
            metaFile.writeText(root.toString())
        }
    }

    suspend fun importFromFiles(
        uris: List<Uri>,
        installedApps: List<AppItem>
    ): Result<ImportedIconPackState> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No files selected"))
        }
        runCatching {
            resetWorkingDirectory()
            val resolver = context.contentResolver
            val extractedEntries = mutableMapOf<String, Pair<Int, ImportedIconEntry>>()
            val parsedAppFilter = mutableMapOf<String, String>()
            val pickedNames = mutableListOf<String>()

            for (uri in uris) {
                val fileName = queryDisplayName(resolver, uri) ?: "icon_pack"
                pickedNames.add(fileName)
                val lowerName = fileName.lowercase(Locale.ROOT)
                val mime = resolver.getType(uri)?.lowercase(Locale.ROOT) ?: ""

                val isArchive = lowerName.endsWith(".zip") ||
                    lowerName.endsWith(".apk") ||
                    mime.contains("zip") ||
                    mime.contains("package-archive") ||
                    hasZipMagicBytes(resolver, uri)

                if (isArchive) {
                    extractArchive(
                        resolver = resolver,
                        uri = uri,
                        extractedEntries = extractedEntries,
                        parsedAppFilter = parsedAppFilter
                    )
                } else if (isSupportedImageFile(lowerName) || mime.startsWith("image/")) {
                    val base = stripExtension(fileName)
                    val destFile = saveImageUriToFile(resolver, uri, base)
                    if (destFile != null) {
                        val normKey = normalizeKey(base)
                        extractedEntries[normKey] = 10 to ImportedIconEntry(
                            keyName = normKey,
                            displayName = base,
                            filePath = destFile.absolutePath
                        )
                    }
                } else if (lowerName == "appfilter.xml") {
                    resolver.openInputStream(uri)?.use { stream ->
                        parseAppFilterXml(stream.bufferedReader().readText(), parsedAppFilter)
                    }
                }
            }

            if (extractedEntries.isEmpty()) {
                error("No .png, .webp, or .jpg icons were found in the selected file(s).")
            }

            appFilterMap.clear()
            appFilterMap.putAll(parsedAppFilter)
            manualOverrides.clear()

            val allIcons = extractedEntries.values
                .map { it.second }
                .sortedBy { it.displayName.lowercase(Locale.ROOT) }

            val packLabel = when {
                pickedNames.size == 1 -> pickedNames.first()
                else -> "${pickedNames.size} imported icon files"
            }

            val packageMap = buildPackageToPathMap(installedApps, allIcons)
            val finalState = ImportedIconPackState(
                packName = packLabel,
                totalIcons = allIcons.size,
                matchedCount = packageMap.size,
                packageToPath = packageMap,
                allIcons = allIcons
            )
            persistState(finalState)
            finalState
        }
    }

    suspend fun importFromFolder(
        treeUri: Uri,
        installedApps: List<AppItem>
    ): Result<ImportedIconPackState> = withContext(Dispatchers.IO) {
        runCatching {
            resetWorkingDirectory()
            val resolver = context.contentResolver
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val folderName = queryTreeDisplayName(resolver, treeUri, docId) ?: "Icon Pack Folder"

            val extractedEntries = mutableMapOf<String, Pair<Int, ImportedIconEntry>>()
            val parsedAppFilter = mutableMapOf<String, String>()

            scanDocumentTree(
                resolver = resolver,
                treeUri = treeUri,
                parentDocumentId = docId,
                depth = 0,
                extractedEntries = extractedEntries,
                parsedAppFilter = parsedAppFilter
            )

            if (extractedEntries.isEmpty()) {
                error("No icon images (.png, .webp, .jpg) were found inside '$folderName'.")
            }

            appFilterMap.clear()
            appFilterMap.putAll(parsedAppFilter)
            manualOverrides.clear()

            val allIcons = extractedEntries.values
                .map { it.second }
                .sortedBy { it.displayName.lowercase(Locale.ROOT) }

            val packageMap = buildPackageToPathMap(installedApps, allIcons)
            val finalState = ImportedIconPackState(
                packName = folderName,
                totalIcons = allIcons.size,
                matchedCount = packageMap.size,
                packageToPath = packageMap,
                allIcons = allIcons
            )
            persistState(finalState)
            finalState
        }
    }

    suspend fun refreshMatchesIfNeeded(installedApps: List<AppItem>) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current.allIcons.isEmpty()) return@withContext
        val updatedMap = buildPackageToPathMap(installedApps, current.allIcons)
        if (updatedMap != current.packageToPath) {
            persistState(
                current.copy(
                    matchedCount = updatedMap.size,
                    packageToPath = updatedMap
                )
            )
        }
    }

    suspend fun assignManualIcon(
        packageName: String,
        iconFilePath: String?,
        installedApps: List<AppItem>
    ) = withContext(Dispatchers.IO) {
        if (iconFilePath == null) {
            manualOverrides.remove(packageName)
        } else {
            manualOverrides[packageName] = iconFilePath
        }
        val current = _state.value
        val updatedMap = buildPackageToPathMap(installedApps, current.allIcons)
        persistState(
            current.copy(
                matchedCount = updatedMap.size,
                packageToPath = updatedMap
            )
        )
    }

    suspend fun clearImportedPack() = withContext(Dispatchers.IO) {
        appFilterMap.clear()
        manualOverrides.clear()
        iconsDir.deleteRecursively()
        iconsDir.mkdirs()
        if (metaFile.exists()) metaFile.delete()
        _state.value = ImportedIconPackState()
    }

    private fun resetWorkingDirectory() {
        iconsDir.deleteRecursively()
        iconsDir.mkdirs()
    }

    private fun extractArchive(
        resolver: ContentResolver,
        uri: Uri,
        extractedEntries: MutableMap<String, Pair<Int, ImportedIconEntry>>,
        parsedAppFilter: MutableMap<String, String>
    ) {
        resolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zis ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (entry.isDirectory) {
                        zis.closeEntry()
                        continue
                    }
                    val entryName = entry.name
                    val lowerEntry = entryName.lowercase(Locale.ROOT)
                    val simpleName = entryName.substringAfterLast('/')

                    if (simpleName.equals("appfilter.xml", ignoreCase = true)) {
                        val bytes = zis.readBytesLimited(2 * 1024 * 1024)
                        if (bytes != null) {
                            val text = bytes.toString(Charsets.UTF_8)
                            if (text.contains("<item")) {
                                parseAppFilterXml(text, parsedAppFilter)
                            }
                        }
                        zis.closeEntry()
                        continue
                    }

                    if (shouldSkipArchiveEntry(lowerEntry, simpleName)) {
                        zis.closeEntry()
                        continue
                    }

                    val densityScore = scoreEntryDensity(lowerEntry)
                    val base = stripExtension(simpleName)
                    val normKey = normalizeKey(base)
                    if (normKey.isBlank()) {
                        zis.closeEntry()
                        continue
                    }

                    val existingScore = extractedEntries[normKey]?.first ?: -1
                    if (densityScore < existingScore) {
                        zis.closeEntry()
                        continue
                    }

                    val safeFileName = "${normKey}_${extractedEntries.size}.${extensionOf(simpleName)}"
                    val outFile = File(iconsDir, safeFileName)
                    var totalBytes = 0
                    var valid = true
                    FileOutputStream(outFile).use { fos ->
                        while (true) {
                            val read = zis.read(buffer)
                            if (read <= 0) break
                            totalBytes += read
                            if (totalBytes > 2 * 1024 * 1024) {
                                valid = false
                                break
                            }
                            fos.write(buffer, 0, read)
                        }
                    }

                    if (valid && totalBytes > 0 && isValidBitmapFile(outFile)) {
                        // Delete superseded lower-density file if replacing
                        extractedEntries[normKey]?.second?.filePath?.let { oldPath ->
                            File(oldPath).delete()
                        }
                        extractedEntries[normKey] = densityScore to ImportedIconEntry(
                            keyName = normKey,
                            displayName = base,
                            filePath = outFile.absolutePath
                        )
                    } else {
                        outFile.delete()
                    }
                    zis.closeEntry()
                }
            }
        }
    }

    private fun scanDocumentTree(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocumentId: String,
        depth: Int,
        extractedEntries: MutableMap<String, Pair<Int, ImportedIconEntry>>,
        parsedAppFilter: MutableMap<String, String>
    ) {
        if (depth > 3) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val childId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                val childName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                val childMime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                val lowerName = childName.lowercase(Locale.ROOT)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)

                if (childMime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanDocumentTree(
                        resolver = resolver,
                        treeUri = treeUri,
                        parentDocumentId = childId,
                        depth = depth + 1,
                        extractedEntries = extractedEntries,
                        parsedAppFilter = parsedAppFilter
                    )
                } else if (lowerName == "appfilter.xml") {
                    resolver.openInputStream(childUri)?.use { stream ->
                        parseAppFilterXml(stream.bufferedReader().readText(), parsedAppFilter)
                    }
                } else if (lowerName.endsWith(".zip") || lowerName.endsWith(".apk")) {
                    extractArchive(resolver, childUri, extractedEntries, parsedAppFilter)
                } else if (isSupportedImageFile(lowerName) || childMime.startsWith("image/")) {
                    val base = stripExtension(childName)
                    val destFile = saveImageUriToFile(resolver, childUri, base)
                    if (destFile != null) {
                        val normKey = normalizeKey(base)
                        extractedEntries[normKey] = 8 to ImportedIconEntry(
                            keyName = normKey,
                            displayName = base,
                            filePath = destFile.absolutePath
                        )
                    }
                }
            }
        }
    }

    private fun saveImageUriToFile(resolver: ContentResolver, uri: Uri, baseName: String): File? {
        val normKey = normalizeKey(baseName).ifBlank { "icon_${System.currentTimeMillis()}" }
        val target = File(iconsDir, "${normKey}_${System.nanoTime() % 100000}.png")
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            if (isValidBitmapFile(target)) target else {
                target.delete()
                null
            }
        }.getOrNull()
    }

    private fun buildPackageToPathMap(
        installedApps: List<AppItem>,
        allIcons: List<ImportedIconEntry>
    ): Map<String, String> {
        if (allIcons.isEmpty()) return emptyMap()
        val keyToPath = allIcons.associate { it.keyName to it.filePath }
        val result = mutableMapOf<String, String>()

        for (app in installedApps) {
            // 1. User manual assignment
            val manual = manualOverrides[app.packageName]
            if (manual != null && File(manual).exists()) {
                result[app.packageName] = manual
                continue
            }

            // 2. appfilter.xml match
            val afDrawableKey = appFilterMap[app.packageName]
            if (afDrawableKey != null) {
                keyToPath[afDrawableKey]?.let { path ->
                    result[app.packageName] = path
                    continue
                }
            }

            // 3. Exact package or label normalized match
            val normPkg = normalizeKey(app.packageName)
            val pkgTail = normalizeKey(app.packageName.substringAfterLast('.'))
            val normLabel = normalizeKey(app.label)
            val normDisplay = normalizeKey(app.displayLabel)

            val directHit = keyToPath[normPkg]
                ?: keyToPath[normDisplay]
                ?: keyToPath[normLabel]
                ?: keyToPath[pkgTail]

            if (directHit != null) {
                result[app.packageName] = directHit
                continue
            }

            // 4. Smart token/keyword match across common app aliases
            val matchedAlias = findAliasMatch(app, keyToPath, allIcons)
            if (matchedAlias != null) {
                result[app.packageName] = matchedAlias
            }
        }
        return result
    }

    private fun findAliasMatch(
        app: AppItem,
        keyToPath: Map<String, String>,
        allIcons: List<ImportedIconEntry>
    ): String? {
        val rawTokens = "${app.label} ${app.displayLabel} ${app.packageName}"
            .lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }

        val candidateAliases = mutableListOf<String>()
        rawTokens.forEach { token ->
            candidateAliases.add(token)
            when (token) {
                "dialer", "phone" -> { candidateAliases.add("phone"); candidateAliases.add("dialer"); candidateAliases.add("call") }
                "messaging", "messages", "sms", "mms" -> { candidateAliases.add("messages"); candidateAliases.add("messaging"); candidateAliases.add("sms") }
                "camera", "opencamera" -> { candidateAliases.add("camera") }
                "photos", "gallery", "gallery3d" -> { candidateAliases.add("photos"); candidateAliases.add("gallery") }
                "chrome", "browser", "webview" -> { candidateAliases.add("chrome"); candidateAliases.add("browser") }
                "settings" -> { candidateAliases.add("settings"); candidateAliases.add("aurora") }
                "aurora" -> { candidateAliases.add("aurorasettings"); candidateAliases.add("aurora"); candidateAliases.add("settings") }
                "gm", "gmail", "email", "mail" -> { candidateAliases.add("gmail"); candidateAliases.add("mail"); candidateAliases.add("email") }
                "deskclock", "clock", "alarm" -> { candidateAliases.add("clock"); candidateAliases.add("deskclock") }
                "calculator", "calc" -> { candidateAliases.add("calculator"); candidateAliases.add("calc") }
                "calendar" -> { candidateAliases.add("calendar") }
                "youtube" -> { candidateAliases.add("youtube") }
                "maps" -> { candidateAliases.add("maps"); candidateAliases.add("googlemaps") }
                "files", "documentsui" -> { candidateAliases.add("files"); candidateAliases.add("filemanager") }
                "music", "spotify" -> { candidateAliases.add("spotify"); candidateAliases.add("music") }
                "play", "vending" -> { candidateAliases.add("playstore"); candidateAliases.add("googleplay") }
                "contacts" -> { candidateAliases.add("contacts") }
            }
        }

        for (alias in candidateAliases) {
            keyToPath[alias]?.let { return it }
        }

        // Prefix/contains match for icons like "ic_chrome", "app_youtube", "chrome_icon"
        for (alias in candidateAliases) {
            if (alias.length < 4) continue
            val match = allIcons.firstOrNull { entry ->
                entry.keyName == "ic$alias" ||
                    entry.keyName == "${alias}icon" ||
                    entry.keyName == "app$alias" ||
                    entry.keyName.contains(alias)
            }
            if (match != null) return match.filePath
        }
        return null
    }

    private fun parseAppFilterXml(xmlText: String, outMap: MutableMap<String, String>) {
        runCatching {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlText))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component") ?: ""
                    val pkgAttr = parser.getAttributeValue(null, "package") ?: ""
                    val drawable = parser.getAttributeValue(null, "drawable") ?: ""
                    if (drawable.isNotBlank()) {
                        val normDrawable = normalizeKey(drawable)
                        if (pkgAttr.isNotBlank()) {
                            outMap[pkgAttr] = normDrawable
                        } else if (component.startsWith("ComponentInfo{")) {
                            val inside = component.removePrefix("ComponentInfo{").removeSuffix("}")
                            val pkg = inside.substringBefore('/')
                            if (pkg.isNotBlank()) {
                                outMap[pkg] = normDrawable
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
    }

    private fun shouldSkipArchiveEntry(lowerEntry: String, simpleName: String): Boolean {
        if (!isSupportedImageFile(simpleName.lowercase(Locale.ROOT))) return true
        if (lowerEntry.startsWith("meta-inf/")) return true
        if (lowerEntry.contains("mipmap-ldpi") || lowerEntry.contains("drawable-ldpi")) return true
        if (simpleName.startsWith("abc_") || simpleName.startsWith("btn_") ||
            simpleName.startsWith("notification_") || simpleName.startsWith("ic_stat_")
        ) return true
        return false
    }

    private fun scoreEntryDensity(lowerPath: String): Int {
        return when {
            lowerPath.contains("xxxhdpi") -> 7
            lowerPath.contains("xxhdpi") -> 6
            lowerPath.contains("nodpi") || lowerPath.contains("anydpi") -> 5
            lowerPath.contains("xhdpi") -> 4
            lowerPath.contains("hdpi") -> 3
            lowerPath.contains("mdpi") -> 2
            !lowerPath.contains("res/") -> 6
            else -> 1
        }
    }

    private fun isSupportedImageFile(lowerName: String): Boolean {
        return lowerName.endsWith(".png") ||
            lowerName.endsWith(".webp") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg")
    }

    private fun stripExtension(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    private fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0 && dot < name.length - 1) name.substring(dot + 1).lowercase(Locale.ROOT) else "png"
    }

    private fun normalizeKey(raw: String): String {
        return raw.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    }

    private fun hasZipMagicBytes(resolver: ContentResolver, uri: Uri): Boolean {
        return runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            } ?: false
        }.getOrDefault(false)
    }

    private fun isValidBitmapFile(file: File): Boolean {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return opts.outWidth > 0 && opts.outHeight > 0
    }

    private fun ZipInputStream.readBytesLimited(maxBytes: Int): ByteArray? {
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun queryTreeDisplayName(resolver: ContentResolver, treeUri: Uri, docId: String): String? {
        return runCatching {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            queryDisplayName(resolver, docUri)
        }.getOrNull()
    }

    companion object {
        fun decodeSampledBitmap(path: String, reqSizePx: Int = 192): Bitmap? {
            return runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                var sampleSize = 1
                while (bounds.outWidth / (sampleSize * 2) >= reqSizePx &&
                    bounds.outHeight / (sampleSize * 2) >= reqSizePx
                ) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeFile(path, decodeOpts)
            }.getOrNull()
        }
    }
}
