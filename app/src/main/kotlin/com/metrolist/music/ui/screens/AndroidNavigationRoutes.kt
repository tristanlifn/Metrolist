/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.metrolist.music.ui.screens.equalizer.EqScreen
import com.metrolist.music.ui.screens.equalizer.wizard.WizardScreen
import com.metrolist.music.ui.screens.recognition.RecognitionHistoryScreen
import com.metrolist.music.ui.screens.recognition.RecognitionScreen
import com.metrolist.music.ui.screens.settings.AndroidAutoSettings
import com.metrolist.music.ui.screens.settings.BackupAndRestore
import com.metrolist.music.ui.screens.settings.integrations.DiscordSettings
import com.metrolist.music.ui.screens.settings.integrations.ListenTogetherSettings
import com.metrolist.music.ui.screens.wrapped.WrappedScreen

/** Screens that only exist on Android: login, Listen Together, Wrapped, equalizer, recognition and device settings. */
fun NavGraphBuilder.androidNavigationRoutes(navController: NavHostController) {
    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable(
        route = "listen_together_from_topbar",
    ) {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    composable("settings/integrations/discord") {
        DiscordSettings(navController)
    }

    composable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("switch_channel") {
        LoginScreen(
            navController = navController,
            isSwitchingChannel = true,
        )
    }

    composable("wrapped") {
        WrappedScreen()
    }

    composable("equalizer") {
        EqScreen()
    }

    composable("eq_wizard") {
        WizardScreen(onNavigateBack = {
            navController.popBackStack()
        })
    }

    composable(
        route = "recognition?autoStart={autoStart}",
        arguments =
            listOf(
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }

    composable("settings/android_auto") {
        AndroidAutoSettings(navController)
    }
}
