/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.content.ActivityNotFoundException
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import android.app.SearchManager

class AndroidPlatformActions(
    private val context: Context,
) : PlatformActions {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun toast(message: String, long: Boolean) {
        mainHandler.post {
            Toast.makeText(context, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareText(text: String, subject: String?, chooserTitle: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun searchWeb(query: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_WEB_SEARCH)
                    .putExtra(SearchManager.QUERY, query)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun shareFile(fileName: String, mimeType: String, content: String, chooserTitle: String?): Result<Unit> =
        runCatching {
            val file = createExportFile(context, fileName).apply { writeText(content) }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, getExportFileUri(context, file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

    override fun saveToDocuments(fileName: String, mimeType: String, content: String): Result<Unit> =
        runCatching { createExportFile(context, fileName).apply { writeText(content) } }
            .mapCatching { file -> saveToPublicDocuments(context, file, mimeType).getOrThrow(); Unit }

    override val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override val supportsDynamicIcon = true

    override fun setDynamicIcon(enabled: Boolean) = IconUtils.setIcon(context, enabled)

    override val hasSystemAppLanguageSettings = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun openSystemAppLanguageSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override val hasDefaultLinkSettings = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun openDefaultLinkSettings(): Boolean =
        try {
            context.startActivity(
                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }

    override val isAndroidAutoInstalled: Boolean
        get() = runCatching { context.packageManager.getPackageInfo("com.google.android.projection.gearhead", 0) }.isSuccess
}
