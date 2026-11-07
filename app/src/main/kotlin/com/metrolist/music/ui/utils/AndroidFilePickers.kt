/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.extensions.fileNameAndSize
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.utils.rememberEnumPreference
import com.yalantis.ucrop.UCrop
import timber.log.Timber
import java.io.File

private val UploadMimeTypes =
    arrayOf("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/flac", "audio/ogg", "audio/x-ms-wma")

@Composable
fun rememberAndroidAudioFilesPicker(onPicked: (List<PickedFile>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: SecurityException) {
                    Timber.w(e, "Could not take persistable permission")
                }
            }
            onPicked(
                uris.map { uri ->
                    PickedFile(
                        nameAndSize = { context.contentResolver.fileNameAndSize(uri) },
                        openStream = { checkNotNull(context.contentResolver.openInputStream(uri)) },
                    )
                },
            )
        }
    return { launcher.launch(UploadMimeTypes) }
}

@Composable
fun rememberAndroidPlaylistCoverPicker(
    title: String,
    onPicked: (PickedImage) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var pendingCropDestUri by remember { mutableStateOf<Uri?>(null) }
    val cropLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val output = res.data?.let { UCrop.getOutput(it) } ?: pendingCropDestUri
                if (output != null) {
                    onPicked(
                        PickedImage(
                            location = output.toString(),
                            readBytes = {
                                try {
                                    context.contentResolver.openInputStream(output)?.use { it.readBytes() }
                                } catch (_: SecurityException) {
                                    null
                                }
                            },
                        ),
                    )
                }
            }
        }
    val (darkMode, _) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val cropColor = MaterialTheme.colorScheme
    val darkTheme = darkMode == DarkMode.ON || (darkMode == DarkMode.AUTO && isSystemInDarkTheme())
    val pickLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { sourceUri ->
                val destFile = File(context.cacheDir, "playlist_cover_crop_${System.currentTimeMillis()}.jpg")
                val destUri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", destFile)
                pendingCropDestUri = destUri

                val options =
                    UCrop.Options().apply {
                        setCompressionFormat(Bitmap.CompressFormat.JPEG)
                        setCompressionQuality(90)
                        setHideBottomControls(true)
                        setToolbarTitle(title)
                        setStatusBarLight(!darkTheme)
                        setToolbarColor(cropColor.surface.toArgb())
                        setToolbarWidgetColor(cropColor.inverseSurface.toArgb())
                        setRootViewBackgroundColor(cropColor.surface.toArgb())
                        setLogoColor(cropColor.surface.toArgb())
                    }

                val intent =
                    UCrop
                        .of(sourceUri, destUri)
                        .withAspectRatio(1f, 1f)
                        .withOptions(options)
                        .getIntent(context)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cropLauncher.launch(intent)
            }
        }
    return { pickLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
