/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.savedstate.read
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.CheckForUpdatesKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DensityScaleKey
import com.metrolist.music.constants.DismissedKmpUpdateKey
import com.metrolist.music.constants.DismissedStandaloneUpdateKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.EnableLandscapeScalingKey
import com.metrolist.music.constants.ForceBottomNavBarKey
import com.metrolist.music.constants.LastSeenVersionKey
import com.metrolist.music.constants.ListenTogetherInTopBarKey
import com.metrolist.music.constants.MiniPlayerBottomSpacing
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarAnimationSpec
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.constants.SlimNavBarHeight
import com.metrolist.music.constants.SlimNavBarKey
import com.metrolist.music.constants.UpdateNotificationsEnabledKey
import com.metrolist.music.constants.UseNewMiniPlayerDesignKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.listentogether.ListenTogetherSession
import com.metrolist.music.playback.MusicPlayer
import com.metrolist.music.playback.SongDownloads
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.account
import com.metrolist.music.resources.changelog
import com.metrolist.music.resources.changelog_empty
import com.metrolist.music.resources.filter_library
import com.metrolist.music.resources.group_outlined
import com.metrolist.music.resources.history
import com.metrolist.music.resources.home
import com.metrolist.music.resources.kmp_upgrade_action
import com.metrolist.music.resources.kmp_upgrade_later
import com.metrolist.music.resources.kmp_upgrade_title
import com.metrolist.music.resources.kmp_upgrade_warning
import com.metrolist.music.resources.search
import com.metrolist.music.resources.stats
import com.metrolist.music.resources.together
import com.metrolist.music.resources.update_action
import com.metrolist.music.resources.update_available_message
import com.metrolist.music.resources.update_available_title
import com.metrolist.music.ui.component.AccountSettingsDialog
import com.metrolist.music.ui.component.AppNavigationBar
import com.metrolist.music.ui.component.AppNavigationRail
import com.metrolist.music.ui.component.BottomSheetMenu
import com.metrolist.music.ui.component.BottomSheetPage
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.component.shimmer.ShimmerTheme
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.player.BottomSheetPlayer
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.navigationBuilder
import com.metrolist.music.ui.screens.settings.ChangelogScreen
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.NavigationTab
import com.metrolist.music.ui.theme.ColorSaver
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.ui.theme.artworkThemeColor
import com.metrolist.music.ui.utils.AppWindowEffects
import com.metrolist.music.ui.utils.appBarScrollBehavior
import com.metrolist.music.ui.utils.resetHeightOffset
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.BuildInfo
import com.metrolist.music.utils.ReleaseInfo
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.get
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeEdit
import com.metrolist.music.viewmodels.HomeViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class AvailableUpdate(
    val release: ReleaseInfo,
    val downloadUrl: String,
    val isKmp: Boolean,
) {
    val dismissalKey = if (isKmp) DismissedKmpUpdateKey else DismissedStandaloneUpdateKey
}

/**
 * The whole app UI: theme, navigation, player sheet and dialogs. Platforms add their own screens through
 * [platformRoutes], react to launch intents and deep links in [platformNavigationEffects], and post
 * system notifications for updates in [onUpdateAvailable].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetrolistApp(
    playerConnection: MusicPlayer?,
    database: MusicDatabase,
    downloadUtil: SongDownloads,
    syncUtils: SyncUtils,
    listenTogetherManager: ListenTogetherSession?,
    startTab: NavigationTab? = null,
    onUpdateAvailable: (AvailableUpdate) -> Unit = {},
    playerSettingsPlatformSection: @Composable () -> Unit = {},
    platformRoutes: NavGraphBuilder.(NavHostController) -> Unit = {},
    platformNavigationEffects: @Composable (NavHostController) -> Unit = {},
) {
    val dataStore = koinInject<DataStore<Preferences>>()
    val uriHandler = LocalUriHandler.current
    var latestVersionName by rememberSaveable { mutableStateOf(BuildInfo.baseVersionName) }
    val onLatestVersionNameChange: (String) -> Unit = { latestVersionName = it }
    val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)
    var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }

    if (BuildInfo.updaterAvailable) {
        LaunchedEffect(checkForUpdates) {
            if (checkForUpdates) {
                val preferences = dataStore.data.first()
                val notificationsEnabled = preferences[UpdateNotificationsEnabledKey] ?: true
                val (releaseInfo, hasUpdate) = Updater.checkForUpdate().getOrNull() ?: (null to false)
                releaseInfo?.let { onLatestVersionNameChange(it.versionName) }

                val standaloneUpdate =
                    releaseInfo
                        ?.takeIf { hasUpdate }
                        ?.let { release ->
                            Updater.getDownloadUrlForCurrentVariant(release)?.let { downloadUrl ->
                                AvailableUpdate(release, downloadUrl, isKmp = false)
                            }
                        }
                val update = standaloneUpdate
                availableUpdate = update?.takeUnless {
                    it.release.tagName == preferences[it.dismissalKey]
                }

                if (update != null && notificationsEnabled) {
                    onUpdateAvailable(update)
                }
            } else {
                onLatestVersionNameChange(BuildInfo.baseVersionName)
                availableUpdate = null
            }
        }
    }

    val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    AppWindowEffects(darkTheme = useDarkTheme, highRefreshRate = enableHighRefreshRate)

    val enableLandscapeScaling by rememberPreference(EnableLandscapeScalingKey, defaultValue = false)
    val userDensityScale by rememberPreference(DensityScaleKey, defaultValue = 1f)
    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val pureBlack =
        remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }

    val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
    val selectedThemeColor = Color(selectedThemeColorInt)

    val showChangelog = rememberSaveable { mutableStateOf(false) }

    var themeColor by rememberSaveable(stateSaver = ColorSaver) {
        mutableStateOf(selectedThemeColor)
    }

    val themeColorCache = remember { mutableMapOf<String, Color>() }
    val platformContext = LocalPlatformContext.current

    LaunchedEffect(selectedThemeColor) {
        if (!enableDynamicTheme) {
            themeColor = selectedThemeColor
        }
    }

    LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
        val playerConnection = playerConnection
        if (!enableDynamicTheme || playerConnection == null) {
            themeColor = selectedThemeColor
            return@LaunchedEffect
        }

        playerConnection.mediaMetadata
            .distinctUntilChanged { old, new -> old?.id == new?.id }
            .collectLatest { song ->
                if (song?.thumbnailUrl != null) {
                    val cached = themeColorCache[song.thumbnailUrl]
                    if (cached != null) {
                        withFrameNanos { }
                        themeColor = cached
                        return@collectLatest
                    }
                    val extractedColor =
                        withContext(Dispatchers.IO) {
                            artworkThemeColor(platformContext, song.thumbnailUrl!!, DefaultThemeColor.toArgb())
                        } ?: selectedThemeColor
                    song.thumbnailUrl?.let { themeColorCache[it] = extractedColor }
                    withFrameNanos { }
                    themeColor = extractedColor
                } else {
                    themeColor = selectedThemeColor
                }
            }
    }

    MetrolistTheme(
        darkTheme = useDarkTheme,
        pureBlack = pureBlack,
        themeColor = themeColor,
    ) {
        val currentDensity = LocalDensity.current
        val windowInfo = LocalWindowInfo.current
        val containerSize = windowInfo.containerDpSize
        val smallestDimensionDp = minOf(containerSize.width, containerSize.height)

        val landscapeDensityScale =
            remember(smallestDimensionDp, enableLandscapeScaling) {
                if (enableLandscapeScaling) {
                    when {
                        smallestDimensionDp >= 840.dp -> 1.15f
                        smallestDimensionDp >= 720.dp -> 1.1f
                        smallestDimensionDp >= 600.dp -> 1.05f
                        else -> 1.0f
                    }
                } else {
                    1.0f
                }
            }
        val scaledDensity: Density = remember(currentDensity, landscapeDensityScale, userDensityScale) {
            Density(
                density = currentDensity.density * landscapeDensityScale * userDensityScale,
                fontScale = currentDensity.fontScale,
            )
        }

        CompositionLocalProvider(LocalDensity provides scaledDensity) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface),
        ) {
            val density = LocalDensity.current
            val configuration = LocalWindowInfo.current
            val cutoutInsets = WindowInsets.displayCutout
            val windowsInsets = WindowInsets.systemBars
            val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
            val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                val lastSeenVersion = dataStore.data.first()[LastSeenVersionKey] ?: ""
                val currentVersion = BuildInfo.baseVersionName
                if (lastSeenVersion != currentVersion) {
                    showChangelog.value = true
                }
            }

            val homeViewModel: HomeViewModel = koinViewModel()
            val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

            val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
            val navigationItems =
                remember(listenTogetherInTopBar) {
                    if (listenTogetherInTopBar) {
                        Screens.MainScreens.filter { it != Screens.ListenTogether }
                    } else {
                        Screens.MainScreens
                    }
                }
            val routeIndexMap = remember(navigationItems) {
                navigationItems.mapIndexed { i, s -> s.route to i }.toMap()
            }
            val (forceBottomNav) = rememberPreference(ForceBottomNavBarKey, defaultValue = false)
            val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
            val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
            val (defaultOpenTabInt) = rememberPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME.name)
            val defaultOpenTab = remember(defaultOpenTabInt) {
                try {
                    NavigationTab.valueOf(defaultOpenTabInt)
                } catch (_: IllegalArgumentException) {
                    NavigationTab.HOME
                }
            }
            val tabOpenedFromShortcut = startTab

            val topLevelScreens =
                remember {
                    listOf(
                        Screens.Home.route,
                        Screens.Library.route,
                        Screens.ListenTogether.route,
                        "settings",
                    )
                }

            val (query, onQueryChange) =
                rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }

            val searchScope = rememberCoroutineScope()
            val onSearch: (String) -> Unit =
                remember {
                    { searchQuery ->
                        if (searchQuery.isNotEmpty()) {
                            navController.navigate(SearchRoutes.resultRoute(searchQuery))

                            if (dataStore[PauseSearchHistoryKey] != true) {
                                searchScope.launch(Dispatchers.IO) {
                                    runCatching {
                                        database.insert(SearchHistory(query = searchQuery))
                                    }.onFailure { throwable ->
                                        Logger.withTag("MetrolistApp").w(throwable) { "Failed to save search history for query: $searchQuery" }
                                    }
                                }
                            }
                        }
                    }
                }

            val currentRoute by remember {
                derivedStateOf { navBackStackEntry?.destination?.route }
            }

            val inSearchScreen by remember {
                derivedStateOf { currentRoute?.startsWith("search/") == true }
            }
            val navigationItemRoutes =
                remember(navigationItems) {
                    navigationItems.map { it.route }.toSet()
                }

            val shouldShowNavigationBar =
                remember(currentRoute, navigationItemRoutes) {
                    currentRoute == null ||
                        navigationItemRoutes.contains(currentRoute) ||
                        currentRoute!!.startsWith("search/")
                }

            val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height
            val isTablet = configuration.containerDpSize.width >= 600.dp

            val showRail = !forceBottomNav && (isLandscape || isTablet) && !inSearchScreen

            val navPadding =
                if (shouldShowNavigationBar && !showRail) {
                    if (slimNav) SlimNavBarHeight else NavigationBarHeight
                } else {
                    0.dp
                }

            val navigationBarHeight by animateDpAsState(
                targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                animationSpec = NavigationBarAnimationSpec,
                label = "navBarHeight",
            )

            val playerBottomSheetState =
                rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound =
                        bottomInset +
                            (if (!showRail && shouldShowNavigationBar) navPadding else 0.dp) +
                            (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                            MiniPlayerHeight,
                    expandedBound = maxHeight,
                )

            val playerReadyState =
                playerConnection?.isReady?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(false) }
            val playerReady by playerReadyState
            val activePlayerConnection = if (playerReady) playerConnection else null

            val playerAwareWindowInsets =
                remember(
                    bottomInset,
                    shouldShowNavigationBar,
                    playerBottomSheetState.isDismissed,
                    showRail,
                ) {
                    var bottom = bottomInset
                    if (shouldShowNavigationBar && !showRail) {
                        bottom += NavigationBarHeight
                    }
                    if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                    windowsInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                }
            appBarScrollBehavior(
                canScroll = {
                    !inSearchScreen &&
                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                },
            )

            val topAppBarScrollBehavior =
                appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

            // Navigation tracking
            LaunchedEffect(navBackStackEntry) {
                if (inSearchScreen) {
                    val searchQuery =
                        SearchRoutes.decodeQuery(
                            navBackStackEntry?.arguments?.read { getStringOrNull("query") }.orEmpty(),
                        )
                    onQueryChange(
                        TextFieldValue(
                            searchQuery,
                            TextRange(searchQuery.length),
                        ),
                    )
                } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                    onQueryChange(TextFieldValue())
                }

                // Reset scroll behavior for main navigation items
                if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                    if (navigationItems.fastAny { it.route == previousTab }) {
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                }

                topAppBarScrollBehavior.state.resetHeightOffset()

                // Collapse player when navigating to equalizer
                if (navBackStackEntry?.destination?.route == "equalizer" &&
                    playerBottomSheetState.isExpanded
                ) {
                    playerBottomSheetState.collapseSoft()
                }

                // Track previous tab for animations
                navController.currentBackStackEntry?.destination?.route?.let {
                    setPreviousTab(it)
                }
            }

            LaunchedEffect(activePlayerConnection) {
                if (activePlayerConnection?.mediaMetadata?.value == null) {
                    if (!playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.dismiss()
                    }
                    return@LaunchedEffect
                }

                if (playerBottomSheetState.isDismissed) {
                    playerBottomSheetState.collapseSoft()
                }
            }

            LaunchedEffect(activePlayerConnection, playerBottomSheetState) {
                activePlayerConnection?.newQueueStarted?.collect {
                    if (playerBottomSheetState.isDismissed) playerBottomSheetState.collapseSoft()
                }
            }
            var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(navBackStackEntry, listenTogetherInTopBar) {
                val currentRoute = navBackStackEntry?.destination?.route
                val isListenTogetherScreen =
                    currentRoute == Screens.ListenTogether.route ||
                        currentRoute == "listen_together_from_topbar"
                shouldShowTopBar = currentRoute in topLevelScreens &&
                    currentRoute != "settings" &&
                    !(isListenTogetherScreen && listenTogetherInTopBar)
            }

            val coroutineScope = rememberCoroutineScope()
            var sharedSong: SongItem? by remember {
                mutableStateOf(null)
            }
            val snackbarHostState = remember { SnackbarHostState() }

            platformNavigationEffects(navController)
            val currentTitleRes =
                remember(navBackStackEntry) {
                    when (navBackStackEntry?.destination?.route) {
                        Screens.Home.route -> Res.string.home
                        Screens.Search.route -> Res.string.search
                        Screens.Library.route -> Res.string.filter_library
                        Screens.ListenTogether.route -> Res.string.together
                        else -> null
                    }
                }

            var showAccountDialog by remember { mutableStateOf(false) }

            val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
            val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
            val showHistoryButton =
                remember(pauseListenHistory, eventCount) {
                    !(pauseListenHistory && eventCount == 0)
                }

            val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
            val artistNameAliases by ArtistNameAliases.aliases.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalDatabase provides database,
                LocalNavController provides navController,
                LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                LocalPlayerConnection provides playerConnection,
                LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                LocalDownloadUtil provides downloadUtil,
                LocalShimmerTheme provides ShimmerTheme,
                LocalSyncUtils provides syncUtils,
                LocalListenTogetherManager provides listenTogetherManager,
                LocalChangelogState provides showChangelog,
                LocalArtistNameAliases provides artistNameAliases,
            ) {
                if (showChangelog.value) {
                    ChangelogScreen(onDismiss = { showChangelog.value = false })
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        AnimatedVisibility(
                            visible = shouldShowTopBar,
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                        ) {
                            Row {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = currentTitleRes?.let { stringResource(it) } ?: "",
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                    },
                                    actions = {
                                        if (showHistoryButton) {
                                            IconButton(onClick = { navController.navigate("history") }) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.history),
                                                    contentDescription = stringResource(Res.string.history),
                                                )
                                            }
                                        }
                                        IconButton(onClick = { navController.navigate("stats") }) {
                                            Icon(
                                                painter = painterResource(Res.drawable.stats),
                                                contentDescription = stringResource(Res.string.stats),
                                            )
                                        }
                                        if (listenTogetherInTopBar) {
                                            IconButton(onClick = { navController.navigate("listen_together_from_topbar") }) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.group_outlined),
                                                    contentDescription = stringResource(Res.string.together),
                                                )
                                            }
                                        }
                                        IconButton(onClick = { showAccountDialog = true }) {
                                            BadgedBox(badge = {
                                                if (latestVersionName != BuildInfo.baseVersionName) {
                                                    Badge()
                                                }
                                            }) {
                                                if (accountImageUrl != null) {
                                                    AsyncImage(
                                                        model = accountImageUrl,
                                                        contentDescription = stringResource(Res.string.account),
                                                        modifier =
                                                            Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape),
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.account),
                                                        contentDescription = stringResource(Res.string.account),
                                                        modifier = Modifier.size(24.dp),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    scrollBehavior = topAppBarScrollBehavior,
                                    colors =
                                        TopAppBarDefaults.topAppBarColors(
                                            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    modifier =
                                        Modifier.windowInsetsPadding(
                                            if (showRail) {
                                                WindowInsets(left = NavigationBarHeight)
                                                    .add(cutoutInsets.only(WindowInsetsSides.Start))
                                            } else {
                                                cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                            },
                                        ),
                                )
                            }
                        }
                    },
                    bottomBar = {
                        val currentBackStackEntry = navController.currentBackStackEntry // reads reactively outside remember

                        val onNavItemClick: (Screens, Boolean) -> Unit =
                            remember(
                                navController,
                                coroutineScope,
                                topAppBarScrollBehavior,
                                playerBottomSheetState,
                                currentBackStackEntry,
                            ) {
                                { screen: Screens, isSelected: Boolean ->
                                    if (playerBottomSheetState.isExpanded) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                    if (isSelected) {
                                        val targetEntry =
                                            try {
                                                val route = navController.currentBackStackEntry?.destination?.route
                                                if (route == SearchRoutes.ROUTE || route == "search_input") {
                                                    // For search screens, use search_input entry
                                                    navController.getBackStackEntry("search_input")
                                                } else {
                                                    // For other screens, use current entry
                                                    navController.currentBackStackEntry
                                                }
                                            } catch (e: Exception) {
                                                null
                                            }

                                        // Use appropriate key based on screen type
                                        if (screen == Screens.Search) {
                                            val current = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                            targetEntry?.savedStateHandle?.set("scrollToTopCount", current + 1)
                                        } else {
                                            targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                        }

                                        coroutineScope.launch {
                                            topAppBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }

                        val onSearchLongClick: () -> Unit =
                            remember(navController) {
                                {
                                    navController.navigate("recognition") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                        // Pre-calculate values for graphicsLayer to avoid reading state during composition
                        val navBarTotalHeight = bottomInset + NavigationBarHeight

                        if (!showRail && currentRoute != "wrapped") {
                            Box {
                                if (activePlayerConnection != null) {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack,
                                    )
                                }

                                AppNavigationBar(
                                    navigationItems = navigationItems,
                                    currentRoute = currentRoute,
                                    onItemClick = onNavItemClick,
                                    pureBlack = pureBlack,
                                    slimNav = slimNav,
                                    onSearchLongClick = onSearchLongClick,
                                    onHomeLongHold = { showAccountDialog = true },
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInset + navPadding)
                                            // Use graphicsLayer instead of offset to avoid recomposition
                                            // graphicsLayer runs during draw phase, not composition phase
                                            .graphicsLayer {
                                                val navBarHeightPx = navigationBarHeight.toPx()
                                                val totalHeightPx = navBarTotalHeight.toPx()

                                                translationY =
                                                    if (navBarHeightPx == 0f) {
                                                        totalHeightPx
                                                    } else {
                                                        // Read progress only during draw phase
                                                        val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                        val slideOffset = totalHeightPx * progress
                                                        val hideOffset =
                                                            totalHeightPx * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                                        slideOffset + hideOffset
                                                    }
                                            },
                                )

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInsetDp)
                                            // Use graphicsLayer for background color changes
                                            .graphicsLayer {
                                                val progress = playerBottomSheetState.progress
                                                alpha =
                                                    if (progress > 0f ||
                                                        (useNewMiniPlayerDesign && !shouldShowNavigationBar)
                                                    ) {
                                                        0f
                                                    } else {
                                                        1f
                                                    }
                                            }.background(baseBg),
                                )
                            }
                        } else {
                            if (currentRoute != "wrapped") {
                                if (activePlayerConnection != null) {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack,
                                    )
                                }
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .height(bottomInsetDp)
                                        // Use graphicsLayer for background color changes
                                        .graphicsLayer {
                                            val progress = playerBottomSheetState.progress
                                            alpha =
                                                if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                        }.background(baseBg),
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                ) {
                    Row(Modifier.fillMaxSize()) {
                        val onRailItemClick: (Screens, Boolean) -> Unit =
                            remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                { screen: Screens, isSelected: Boolean ->
                                    if (playerBottomSheetState.isExpanded) {
                                        playerBottomSheetState.collapseSoft()
                                    }

                                    if (isSelected) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                        coroutineScope.launch {
                                            topAppBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }

                        val onRailSearchLongClick: () -> Unit =
                            remember(navController) {
                                {
                                    navController.navigate("recognition") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                        if (showRail && currentRoute != "wrapped") {
                            AppNavigationRail(
                                navigationItems = navigationItems,
                                currentRoute = currentRoute,
                                onItemClick = onRailItemClick,
                                pureBlack = pureBlack,
                                onSearchLongClick = onRailSearchLongClick,
                                onHomeLongHold = { showAccountDialog = true },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            // NavHost with animations (Material 3 Expressive style)
                            NavHost(
                                navController = navController,
                                startDestination =
                                    when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.SEARCH -> Screens.Search
                                        NavigationTab.LIBRARY -> Screens.Library
                                    }.route,
                                enterTransition = {
                                    val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                    val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

                                    if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                                        slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                    } else {
                                        slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                    }
                                },
                                exitTransition = {
                                    val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                    val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

                                    if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                                        slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                    } else {
                                        slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                    }
                                },
                                popEnterTransition = {
                                    val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                    val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

                                    if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
                                        slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                    } else {
                                        slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                    }
                                },
                                popExitTransition = {
                                    val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                    val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

                                    if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
                                        slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                    } else {
                                        slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                    }
                                },
                                modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                            ) {
                                navigationBuilder(
                                    navController = navController,
                                    scrollBehavior = topAppBarScrollBehavior,
                                    latestVersionName = latestVersionName,
                                    snackbarHostState = snackbarHostState,
                                        playerSettingsPlatformSection = playerSettingsPlatformSection,
                                    )
                                    platformRoutes(navController)
                            }
                        }
                    }
                }

                BottomSheetMenu(
                    state = LocalMenuState.current,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                BottomSheetPage(
                    state = LocalBottomSheetPageState.current,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                if (showAccountDialog) {
                    AccountSettingsDialog(
                        onDismiss = {
                            showAccountDialog = false
                            homeViewModel.refresh()
                        },
                        latestVersionName = latestVersionName,
                    )
                }

                sharedSong?.let { song ->
                    playerConnection?.let {
                        Dialog(
                            onDismissRequest = { sharedSong = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            Surface(
                                modifier = Modifier.padding(24.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = AlertDialogDefaults.containerColor,
                                tonalElevation = AlertDialogDefaults.TonalElevation,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    YouTubeSongMenu(
                                        song = song,
                                        onDismiss = { sharedSong = null },
                                    )
                                }
                            }
                        }
                    }
                }

                if (!showChangelog.value) {
                    availableUpdate?.let { update ->
                        val dismissUpdate: () -> Unit = {
                            availableUpdate = null
                            coroutineScope.launch { dataStore.safeEdit {
                                    it[update.dismissalKey] = update.release.tagName
                                }
                            }
                        }
                        AlertDialog(
                            onDismissRequest = dismissUpdate,
                            title = { Text(stringResource(Res.string.update_available_title)) },
                            text = {
                                Column(
                                    modifier =
                                        Modifier
                                            .heightIn(max = 480.dp)
                                            .verticalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        text =
                                            stringResource(
                                                if (update.isKmp) {
                                                    Res.string.kmp_upgrade_title
                                                } else {
                                                    Res.string.update_available_message
                                                },
                                                update.release.versionName,
                                            ),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    if (update.isKmp) {
                                        Text(
                                            text = stringResource(Res.string.kmp_upgrade_warning),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(top = 12.dp),
                                        )
                                    }
                                    Text(
                                        text = stringResource(Res.string.changelog),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                    )
                                    Text(
                                        text = update.release.description.ifBlank { stringResource(Res.string.changelog_empty) },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dismissUpdate()
                                        uriHandler.openUri(update.downloadUrl)
                                    },
                                ) {
                                    Text(
                                        stringResource(
                                            if (update.isKmp) Res.string.kmp_upgrade_action else Res.string.update_action,
                                        ),
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = dismissUpdate) {
                                    Text(stringResource(Res.string.kmp_upgrade_later))
                                }
                            },
                        )
                    }
                }
            }
        }
        }
    }
}
