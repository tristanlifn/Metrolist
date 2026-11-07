/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import org.koin.android.ext.android.inject
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.DisableScreenshotKey
import com.metrolist.music.constants.LastSeenVersionKey
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.constants.LyricsProviderOrderKey
import com.metrolist.music.constants.PreferredLyricsProvider
import com.metrolist.music.constants.PreferredLyricsProviderKey
import com.metrolist.music.constants.SYSTEM_DEFAULT
import com.metrolist.music.constants.SimpMusicMigrationDoneKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.constants.VideoThumbnailMigrationDoneKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.screens.settings.NavigationTab
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.setAppLocale
import com.metrolist.music.widget.PlaylistWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import com.metrolist.music.ui.component.CastButton
import com.metrolist.music.ui.menu.ListenTogetherDialog
import com.metrolist.music.ui.menu.rememberSystemEqualizerLauncher
import com.metrolist.music.ui.utils.LocalAudioFilesPicker
import com.metrolist.music.ui.utils.LocalPlaylistCoverPicker
import com.metrolist.music.ui.utils.rememberAndroidAudioFilesPicker
import com.metrolist.music.ui.utils.rememberAndroidPlaylistCoverPicker
import com.metrolist.music.ui.screens.androidNavigationRoutes
import com.metrolist.music.ui.screens.settings.AlarmSettingsSection

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
class MainActivity : FragmentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.metrolist.music.action.SEARCH"
        private const val ACTION_LIBRARY = "com.metrolist.music.action.LIBRARY"
        const val ACTION_RECOGNITION = "com.metrolist.music.action.RECOGNITION"
        const val ACTION_OPEN_WIDGET_TARGET = "com.metrolist.music.action.OPEN_WIDGET_TARGET"
        const val EXTRA_AUTO_START_RECOGNITION = "auto_start_recognition"
        const val EXTRA_WIDGET_TARGET_TYPE = "widget_target_type"
        const val EXTRA_WIDGET_TARGET_ID = "widget_target_id"
    }

    val database: MusicDatabase by inject()

    val downloadUtil: DownloadUtil by inject()

    val syncUtils: SyncUtils by inject()

    val listenTogetherManager: com.metrolist.music.listentogether.ListenTogetherManager by inject()

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null

    // Keep PlayerConnection as regular property - NOT mutableStateOf to prevent UI recomposition
    // when it becomes null during onStop. Only update the snapshot for Compose when needed.
    private var playerConnection: PlayerConnection? = null

    // This is the snapshot we pass to Compose - changes here trigger recomposition
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)

    private var isServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playerConnectionSnapshot = playerConnection
                    listenTogetherManager.setPlayerConnection(playerConnection)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Disconnect Listen Together manager
                listenTogetherManager.setPlayerConnection(null)
                playerConnection?.dispose()
                // DO NOT null out playerConnection here - keep it for when service reconnects
                // DO NOT update playerConnectionSnapshot - this is the key to preventing recomposition
            }
        }

    private fun safeUnbindService(source: String) {
        if (!isServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.tag("MainActivity").w(e, "Service was not bound when attempting to unbind in $source")
        } finally {
            isServiceBound = false
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            // DO NOT null out playerConnection here - keep it for reconnection
            // DO NOT update playerConnectionSnapshot - this prevents UI recomposition
        }
    }

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        // Start the playback service explicitly once so it can outlive binding.
        // Re-issuing startForegroundService() while an existing service instance is already
        // running can trigger "did not then call startForeground" on some Android 9 devices
        // when the framework expects a fresh foreground promotion for that start request.
        if (!MusicService.isRunning) {
            val serviceIntent = Intent(this, MusicService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Timber.w(e, "Cannot start foreground service from background")
            } catch (e: IllegalStateException) {
                Timber.w(e, "Failed to start foreground service")
            }
        }

        // Bind to service - if already bound, this is a no-op but ensures we stay connected
        if (!isServiceBound) {
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
            isServiceBound = true
        }
    }

    override fun onStop() {
        // Keep the service binding, PlayerConnection and Listen Together wiring alive while
        // the Activity is backgrounded. The MusicService is a foreground service and keeps
        // running, so the host must keep reporting playback state to the LT server; detaching
        // the player listener here used to break LT for any host that wasn't staring at the
        // app the whole session. Full teardown happens in onDestroy() via safeUnbindService().
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            listenTogetherManager.disconnect()
        }
        super.onDestroy()
        val stopServiceOnClear = dataStore.get(StopMusicOnTaskClearKey, false) && isFinishing

        // Full cleanup - only on actual destroy
        playerConnection?.dispose()
        playerConnection = null
        playerConnectionSnapshot = null

        // Unbind before stopService: a started+bound service does not stop until all clients unbind.
        safeUnbindService("onDestroy()")

        if (stopServiceOnClear) {
            stopService(Intent(this, MusicService::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleWidgetTargetIntent(intent, navController)
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Listen Together manager
        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale =
                dataStore[AppLanguageKey]
                    ?.takeUnless { it == SYSTEM_DEFAULT }
                    ?.let { Locale.forLanguageTag(it) }
                    ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // Defer migration and version tracking to avoid blocking first frame
        lifecycleScope.launch(Dispatchers.IO) {
            val preferences = dataStore.data.first()
            val currentVersion = BuildConfig.BASE_VERSION_NAME

            // SimpMusic Removal Migration
            if (preferences[SimpMusicMigrationDoneKey] != true) {
                safeDataStoreEdit { settings ->
                    val currentOrder = settings[LyricsProviderOrderKey] ?: ""
                    if (currentOrder.contains("SimpMusic")) {
                        val orderList =
                            currentOrder
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() && it != "SimpMusic" }
                                .toMutableList()
                        if (orderList.isEmpty()) {
                            settings[LyricsProviderOrderKey] = ""
                        } else {
                            settings[LyricsProviderOrderKey] = orderList.joinToString(",")
                        }
                    }
                    if (settings[PreferredLyricsProviderKey] == "SIMPMUSIC") {
                        settings[PreferredLyricsProviderKey] = PreferredLyricsProvider.LRCLIB.name
                    }
                    settings[SimpMusicMigrationDoneKey] = true
                    settings[LastSeenVersionKey] = currentVersion
                }
            }

            if (preferences[VideoThumbnailMigrationDoneKey] != true) {
                database.repairMissingVideoThumbnails()
                safeDataStoreEdit { settings ->
                    settings[VideoThumbnailMigrationDoneKey] = true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            safeDataStoreEdit { settings ->
                settings[LastSeenVersionKey] = BuildConfig.BASE_VERSION_NAME
            }
        }

        setContent {
            val startTab =
                remember {
                    when (intent?.action) {
                        ACTION_SEARCH -> NavigationTab.SEARCH
                        ACTION_LIBRARY -> NavigationTab.LIBRARY
                        else -> null
                    }
                }
            CompositionLocalProvider(
                LocalCastButton provides { modifier, tintColor -> CastButton(modifier, tintColor) },
                LocalListenTogetherDialog provides { visible, mediaMetadata, onDismiss -> ListenTogetherDialog(visible, mediaMetadata, onDismiss) },
                LocalSystemEqualizerLauncher provides { rememberSystemEqualizerLauncher() },
                LocalAudioFilesPicker provides { onPicked -> rememberAndroidAudioFilesPicker(onPicked) },
                LocalPlaylistCoverPicker provides { title, onPicked -> rememberAndroidPlaylistCoverPicker(title, onPicked) },
            ) {
                MetrolistApp(
                    playerConnection = playerConnectionSnapshot,
                    database = database,
                    downloadUtil = downloadUtil,
                    syncUtils = syncUtils,
                    listenTogetherManager = listenTogetherManager,
                    startTab = startTab,
                    onUpdateAvailable = ::showUpdateNotification,
                    playerSettingsPlatformSection = { AlarmSettingsSection() },
                    platformRoutes = { navController -> androidNavigationRoutes(navController) },
                    platformNavigationEffects = { navController -> IntentNavigationEffects(navController) },
                )
            }
        }
    }

    /** Applies launch, widget, recognition and deep link intents to the shared navigation. */
    @Composable
    private fun IntentNavigationEffects(navController: NavHostController) {
        LaunchedEffect(Unit) {
            val launchIntent = pendingIntent ?: intent
            pendingIntent = null
            handleWidgetTargetIntent(launchIntent, navController)
            handleRecognitionIntent(launchIntent, navController)
            handleDeepLinkIntent(launchIntent, navController)
        }

        DisposableEffect(Unit) {
            val listener =
                Consumer<Intent> { intent ->
                    handleWidgetTargetIntent(intent, navController)
                    handleRecognitionIntent(intent, navController)
                    handleDeepLinkIntent(intent, navController)
                }
            addOnNewIntentListener(listener)
            onDispose { removeOnNewIntentListener(listener) }
        }
    }

    private fun showUpdateNotification(update: AvailableUpdate) {
        val intent = Intent(Intent.ACTION_VIEW, update.downloadUrl.toUri())
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(this, 1001, intent, flags)
        val notificationText =
            if (update.isKmp) {
                getString(R.string.kmp_upgrade_warning)
            } else {
                update.release.versionName
            }
        val notification =
            NotificationCompat
                .Builder(this, "updates")
                .setSmallIcon(R.drawable.update)
                .setContentTitle(
                    if (update.isKmp) {
                        getString(R.string.kmp_upgrade_title, update.release.versionName)
                    } else {
                        getString(R.string.update_available_title)
                    },
                )
                .setContentText(notificationText)
                .apply {
                    if (update.isKmp) {
                        setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                    }
                }
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1001, notification)
        }
    }

    /**
     * Handles the ACTION_RECOGNITION intent sent from the Music Recognizer Widget.
     * Always navigates to the recognition screen to show the result.
     */
    private fun handleRecognitionIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_RECOGNITION) return
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START_RECOGNITION, false)
        intent.action = null
        intent.removeExtra(EXTRA_AUTO_START_RECOGNITION)
        navController.navigate(if (autoStart) "recognition?autoStart=true" else "recognition") {
            launchSingleTop = true
        }
    }

    private sealed class WidgetTargetRoute(val route: String) {
        data class LocalPlaylist(val id: String) : WidgetTargetRoute("local_playlist/$id")
        data class OnlinePlaylist(val id: String) : WidgetTargetRoute("online_playlist/$id")
        data object LikedSongs : WidgetTargetRoute("auto_playlist/liked")
        data object DownloadedSongs : WidgetTargetRoute("auto_playlist/downloaded")
        data class TopSongs(val limit: String) : WidgetTargetRoute("top_playlist/$limit")
    }

    private fun handleWidgetTargetIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_OPEN_WIDGET_TARGET) return

        val targetType = intent.getStringExtra(EXTRA_WIDGET_TARGET_TYPE)
        val targetId = intent.getStringExtra(EXTRA_WIDGET_TARGET_ID)
        intent.action = null
        intent.removeExtra(EXTRA_WIDGET_TARGET_TYPE)
        intent.removeExtra(EXTRA_WIDGET_TARGET_ID)

        val normalizedTargetId = targetId?.takeIf { it.isNotBlank() }

        val targetRoute = when (targetType) {
            PlaylistWidgetReceiver.TARGET_TYPE_LOCAL ->
                normalizedTargetId?.let { WidgetTargetRoute.LocalPlaylist(it) }

            PlaylistWidgetReceiver.TARGET_TYPE_ONLINE ->
                normalizedTargetId?.let { WidgetTargetRoute.OnlinePlaylist(it) }

            PlaylistWidgetReceiver.TARGET_TYPE_LIKED ->
                WidgetTargetRoute.LikedSongs

            PlaylistWidgetReceiver.TARGET_TYPE_DOWNLOADED ->
                WidgetTargetRoute.DownloadedSongs

            PlaylistWidgetReceiver.TARGET_TYPE_TOP ->
                WidgetTargetRoute.TopSongs(normalizedTargetId ?: "50")

            else -> null
        } ?: return

        navController.navigate(targetRoute.route)
    }

    private fun handleDeepLinkIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val listenCode =
            uri.getQueryParameter("code")
                ?: uri.getQueryParameter("room")
                ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> {
                val playlistId = uri.getQueryParameter("list")
                if (playlistId.isNullOrBlank() || playlistId.equals("null", ignoreCase = true)) {
                    Toast.makeText(this, R.string.playlist_unavailable, Toast.LENGTH_SHORT).show()
                    return
                }

                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .albumSongs(playlistId)
                            .onSuccess { songs ->
                                songs.firstOrNull()?.album?.id?.let { browseId ->
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("album/$browseId")
                                    }
                                }
                            }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> {
                uri.lastPathSegment?.let { browseId ->
                    navController.navigate("album/$browseId")
                }
            }

            "channel", "c" -> {
                uri.lastPathSegment?.let { artistId ->
                    navController.navigate("artist/$artistId")
                }
            }

            "search" -> {
                uri.getQueryParameter("q")?.let {
                    navController.navigate(SearchRoutes.resultRoute(it))
                }
            }

            else -> {
                val videoId =
                    when {
                        path == "watch" -> uri.getQueryParameter("v")
                        uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                        else -> null
                    }

                val playlistId = uri.getQueryParameter("list")

                if (videoId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(listOf(videoId), playlistId)
                            .onSuccess { queue ->
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                            queue.firstOrNull()?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else if (playlistId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(null, playlistId)
                            .onSuccess { queue ->
                                val firstItem = queue.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                            firstItem?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

}

