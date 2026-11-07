/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.playback.PlayerConnection

/** Opens the device's audio effects panel for the current playback session, when one is installed. */
@Composable
fun rememberSystemEqualizerLauncher(): () -> Unit {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    // Some equalizer apps read the calling package, which only an activity result launch provides.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    return {
        val audioSessionId = (playerConnection as? PlayerConnection)?.player?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                launcher.launch(intent)
            }
        }
    }
}
