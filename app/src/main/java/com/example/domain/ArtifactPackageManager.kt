package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.remote.GitHubNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class DownloadedPackageInfo(
    val fileName: String,
    val filePath: String,
    val packageType: String, // "APK", "IPA", or "ARCHIVE"
    val sizeBytes: Long,
    val sha256: String,
)

class ArtifactPackageManager(
    private val context: Context,
    private val networkClient: GitHubNetworkClient,
) {
    private val artifactsDir: File
        get() = File(context.filesDir, "artifacts").apply { mkdirs() }

    private val exportsDir: File
        get() = File(context.cacheDir, "exports").apply { mkdirs() }

    suspend fun downloadAndExtractArtifact(
        downloadUrl: String,
        suggestedName: String,
        repoFullName: String,
        isZipArchive: Boolean,
    ): Result<List<DownloadedPackageInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val safePrefix = repoFullName.replace("/", "_")
            val rawFile = File(artifactsDir, "${safePrefix}_${sanitizeName(suggestedName)}")
            val response = networkClient.executeAuthorizedDownload(downloadUrl)
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val hint = if (resp.code == 401 || resp.code == 403) {
                        "Authentication required: add a GitHub Personal Access Token with 'repo' scope in the Token tab to download GitHub Actions artifacts."
                    } else {
                        "HTTP ${resp.code}: ${resp.message}"
                    }
                    error(hint)
                }
                val body = resp.body ?: error("Empty download response")
                FileOutputStream(rawFile).use { out ->
                    body.byteStream().copyTo(out)
                }
            }

            if (isZipArchive || rawFile.extension.equals("zip", ignoreCase = true)) {
                val extracted = extractApkAndIpaFromZip(rawFile, safePrefix)
                if (extracted.isNotEmpty()) {
                    rawFile.delete()
                    extracted
                } else {
                    listOf(buildInfo(rawFile, detectType(rawFile.name)))
                }
            } else {
                listOf(buildInfo(rawFile, detectType(rawFile.name)))
            }
        }
    }

    private fun extractApkAndIpaFromZip(zipFile: File, safePrefix: String): List<DownloadedPackageInfo> {
        val results = mutableListOf<DownloadedPackageInfo>()
        val seenNames = mutableSetOf<String>()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val baseName = File(entry.name).name
                    val isAppBinary = baseName.endsWith(".apk", ignoreCase = true) ||
                        baseName.endsWith(".ipa", ignoreCase = true)
                    if (isAppBinary && seenNames.add(baseName)) {
                        val outFile = File(artifactsDir, "${safePrefix}_${sanitizeName(baseName)}")
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        results += buildInfo(outFile, detectType(baseName))
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return results
    }

    fun copyFileToUri(sourcePath: String, destinationUri: Uri): Result<Unit> = runCatching {
        val source = File(sourcePath)
        require(source.exists()) { "Package file no longer exists on device." }
        val output: OutputStream = context.contentResolver.openOutputStream(destinationUri)
            ?: error("Unable to open destination output stream.")
        output.use { out ->
            FileInputStream(source).use { input ->
                input.copyTo(out)
            }
        }
    }

    fun createShareIntent(filePath: String, fileName: String): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val mimeType = when {
            fileName.endsWith(".apk", ignoreCase = true) -> "application/vnd.android.package-archive"
            fileName.endsWith(".ipa", ignoreCase = true) -> "application/octet-stream"
            fileName.endsWith(".yml", ignoreCase = true) -> "text/yaml"
            else -> "application/zip"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createShareYamlIntent(yamlContent: String, fileName: String): Intent {
        val file = File(exportsDir, sanitizeName(fileName))
        file.writeText(yamlContent)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, yamlContent)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun deleteLocalFile(filePath: String) {
        runCatching { File(filePath).delete() }
    }

    private fun buildInfo(file: File, type: String): DownloadedPackageInfo {
        return DownloadedPackageInfo(
            fileName = file.name,
            filePath = file.absolutePath,
            packageType = type,
            sizeBytes = file.length(),
            sha256 = computeSha256(file),
        )
    }

    private fun detectType(name: String): String = when {
        name.endsWith(".apk", ignoreCase = true) -> "APK"
        name.endsWith(".ipa", ignoreCase = true) -> "IPA"
        else -> "ARCHIVE"
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read = fis.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.take(16) + "..."
    }

    private fun sanitizeName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
