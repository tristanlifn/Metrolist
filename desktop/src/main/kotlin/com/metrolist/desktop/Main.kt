/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.metrolist.desktop.login.DesktopLoginScreen
import com.metrolist.desktop.login.runLoginCheck
import com.metrolist.desktop.mpris.MprisService
import androidx.navigation.compose.composable
import com.metrolist.desktop.login.keepBrowserSessionFresh
import com.metrolist.desktop.playback.DesktopMusicPlayer
import com.metrolist.desktop.playback.LibC
import com.metrolist.music.MetrolistApp
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.di.sharedViewModelModule
import com.metrolist.music.playback.SongDownloads
import com.metrolist.music.utils.BuildInfo
import com.metrolist.music.utils.SyncUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import com.metrolist.music.constants.LastSeenVersionKey
import com.metrolist.music.utils.safeEdit
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.io.File
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--probe") return runPlaybackProbe(args.drop(1))
    if (args.firstOrNull() == "--login-check") return runLoginCheck()

    BuildInfo.init(
        versionName = DESKTOP_VERSION,
        baseVersionName = DESKTOP_VERSION,
        versionCode = 0,
        architecture = System.getProperty("os.arch"),
        isDebug = DESKTOP_VERSION.endsWith("-dev"),
        castAvailable = false,
        updaterAvailable = false,
        platformDetails = listOf("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}"),
    )
    val koin = startKoin { modules(desktopModule, sharedViewModelModule) }.koin
    CoroutineScope(SupervisorJob() + Dispatchers.IO).syncInnerTubeSettings(koin.get<DataStore<Preferences>>())

    if (args.firstOrNull() == "--player-check") return runPlayerCheck(DesktopMusicPlayer(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), sessionsFile = null), args.drop(1))

    if (args.firstOrNull() == "--snapshot") {
        // Snapshots show the app itself, not the first-run changelog sheet.
        runBlocking { koin.get<DataStore<Preferences>>().safeEdit { it[LastSeenVersionKey] = DESKTOP_VERSION } }
        renderSnapshot(File(args.getOrElse(1) { "metrolist-snapshot.png" }), seconds = args.getOrNull(2)?.toIntOrNull() ?: 20) { DesktopApp() }
        exitProcess(0)
    }

    CoroutineScope(SupervisorJob() + Dispatchers.IO).keepBrowserSessionFresh(koin.get<DataStore<Preferences>>())
    var raiseWindow: () -> Unit = {}
    val mpris = MprisService.start(koin.get<DesktopMusicPlayer>()) { raiseWindow() }
    val cleanedUp = AtomicBoolean(false)
    val cleanUp = {
        if (cleanedUp.compareAndSet(false, true)) {
            mpris?.close()
            koin.get<DesktopMusicPlayer>().release()
        }
    }
    // Logging out or `kill` sends SIGTERM/SIGHUP; save the same way as closing the window.
    Runtime.getRuntime().addShutdownHook(
        Thread {
            cleanUp()
            exitWithoutNativeHandlers()
        },
    )

    application(exitProcessOnExit = false) {
        Window(
            onCloseRequest = {
                cleanUp()
                exitApplication()
            },
            title = "Metrolist",
            state = rememberWindowState(size = DpSize(1100.dp, 760.dp)),
        ) {
            raiseWindow = { java.awt.EventQueue.invokeLater { window.toFront() } }
            DesktopApp()
        }
    }
    exitWithoutNativeHandlers()
}

// libGLX's exit handler closes the X display and can deadlock with AWT's X11 thread while the window is
// torn down, leaving a windowless process behind. Called once cleanup is done, so native exit handlers are skipped.
private fun exitWithoutNativeHandlers() {
    System.out.flush()
    System.err.flush()
    LibC.INSTANCE._exit(0)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopApp() {
    val player = koinInject<DesktopMusicPlayer>()
    // True while a text field has an input session, so Space types a space there instead of toggling playback.
    var typing by remember { mutableStateOf(false) }
    val platformActions = koinInject<DesktopPlatformActions>()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(platformActions) {
        platformActions.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            typing = true
            try {
                nextHandler.startInputMethod(request)
            } finally {
                typing = false
            }
        },
    ) {
    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            // Without this, Space "clicks" whatever was last focused, e.g. restarting the song that was clicked.
            val isPlayPauseKey = event.key == Key.MediaPlayPause || (event.key == Key.Spacebar && !typing)
            if (isPlayPauseKey && event.type == KeyEventType.KeyDown) player.togglePlayPause()
            isPlayPauseKey
        },
    ) {
        MetrolistApp(
            playerConnection = player,
            database = koinInject<MusicDatabase>(),
            downloadUtil = koinInject<SongDownloads>(),
            syncUtils = koinInject<SyncUtils>(),
            listenTogetherManager = null,
            platformRoutes = { navController ->
                composable("login") { DesktopLoginScreen(navController) }
                composable("switch_channel") { DesktopLoginScreen(navController) }
            },
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
    }
}

// Written into the jar by the build; see desktopVersion in desktop/build.gradle.kts.
private val DESKTOP_VERSION: String =
    object {}.javaClass.getResourceAsStream("/metrolist-desktop.properties")?.use { stream ->
        Properties().apply { load(stream) }.getProperty("version")
    } ?: "0.1.0-dev"
