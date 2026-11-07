/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.LocalNavController
import com.metrolist.music.ui.screens.settings.AccountSettings

@Composable
fun AccountSettingsDialog(
    onDismiss: () -> Unit,
    latestVersionName: String,
) {
    val navController = LocalNavController.current
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onDismiss() }
                        )
                    },
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(28.dp)),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                AccountSettings(
                    navController = navController,
                    onClose = onDismiss,
                    latestVersionName = latestVersionName,
                )
            }
        }
    }
}
