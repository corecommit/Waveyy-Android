package com.waveyy.app.ui.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads an update APK and hands it to the system PackageInstaller.
 * The system then shows its own install prompt — a full silent install is
 * not possible for a sideloaded app without device-owner privileges.
 */
@Singleton
class UpdateInstaller @Inject constructor(
    private val okHttp: OkHttpClient
) {

    /**
     * Downloads [downloadUrl] to the cache directory and writes it into a
     * PackageInstaller session, then commits. Returns [Result.success]
     * when the session commits (the system install prompt then appears);
     * [Result.failure] otherwise.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure the system will let us install unknown packages.
            if (!context.packageManager.canRequestPackageInstalls()) {
                launchUnknownSourcesSettings(context)
                throw IllegalStateException(
                    "Grant \"Install unknown apps\" for Waveyy, then retry."
                )
            }
            val apkFile = File(context.cacheDir, fileName)
            downloadApk(downloadUrl, apkFile, onProgress)
            installApk(context, apkFile)
        }
    }

    private fun downloadApk(url: String, dest: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(url).build()
        okHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var written = 0L
                    while (input.read(buf).also { read = it } > 0) {
                        out.write(buf, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(context.packageName)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("waveyy-update.apk", 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { input ->
                    input.copyTo(out)
                }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = UpdateInstallReceiver.ACTION_INSTALL_STATUS
            }
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pending.intentSender)
        }
        Timber.tag("UpdateInstaller").d("committed session $sessionId")
    }

    private fun launchUnknownSourcesSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
