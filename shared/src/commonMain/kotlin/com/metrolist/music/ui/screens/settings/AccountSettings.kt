/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.metrolist.music.utils.reportException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.InfoLabel
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.account
import com.metrolist.music.resources.action_logout
import com.metrolist.music.resources.advanced_login
import com.metrolist.music.resources.app_name
import com.metrolist.music.resources.cached
import com.metrolist.music.resources.check
import com.metrolist.music.resources.close
import com.metrolist.music.resources.integration
import com.metrolist.music.resources.integrations
import com.metrolist.music.resources.login
import com.metrolist.music.resources.logout_clear
import com.metrolist.music.resources.logout_dialog_message
import com.metrolist.music.resources.logout_dialog_title
import com.metrolist.music.resources.logout_keep
import com.metrolist.music.resources.more_content
import com.metrolist.music.resources.new_version_available
import com.metrolist.music.resources.settings
import com.metrolist.music.resources.switch_youtube_channel
import com.metrolist.music.resources.token
import com.metrolist.music.resources.token_adv_login_description
import com.metrolist.music.resources.token_hidden
import com.metrolist.music.resources.token_shown
import com.metrolist.music.resources.update
import com.metrolist.music.resources.yt_sync
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.metrolist.music.utils.BuildInfo
import co.touchlab.kermit.Logger

@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String
) {
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (authUser, onAuthUserChange) = rememberPreference(InnerTubeAuthUserKey, "0")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = koinViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = koinViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(painterResource(Res.drawable.close), contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Logout confirmation dialog
        if (showLogoutDialog) {
            DefaultDialog(
                onDismiss = { showLogoutDialog = false },
                title = { Text(stringResource(Res.string.logout_dialog_title)) },
                content = {
                    Text(
                        text = stringResource(Res.string.logout_dialog_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                },
                buttons = {
                    TextButton(
                        onClick = {
                            Logger.d { "[LOGOUT_CLEAR] User chose to clear data" }
                            scope.launch {
                                try {
                                    Logger.d { "[LOGOUT_CLEAR] Starting clear and logout process" }
                                    // Forget account first (stops all sync), then clear data.
                                    // This prevents background syncs from re-adding songs.
                                    accountSettingsViewModel.logoutAndClearLibraryData()
                                    Logger.d { "[LOGOUT_CLEAR] Library data cleared and account forgotten" }
                                } catch (e: Exception) {
                                    Logger.e(e) { "[LOGOUT_CLEAR] Error clearing library data, proceeding with logout" }
                                    reportException(e)
                                }
                                onInnerTubeCookieChange("")
                                Logger.d { "[LOGOUT_CLEAR] Logout complete" }
                                showLogoutDialog = false
                                onClose()
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.logout_clear))
                    }
                    TextButton(
                        onClick = {
                            Logger.d { "[LOGOUT_KEEP] User chose to keep data" }
                            scope.launch {
                                Logger.d { "[LOGOUT_KEEP] Starting logout process (keeping data)" }
                                accountSettingsViewModel.logoutKeepData(onInnerTubeCookieChange)
                                Logger.d { "[LOGOUT_KEEP] Logout complete" }
                                showLogoutDialog = false
                                onClose()
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.logout_keep))
                    }
                }
            )
        }

        if (showTokenEditor) {
            val text = """
                ***INNERTUBE COOKIE*** =$innerTubeCookie
                ***VISITOR DATA*** =$visitorData
                ***DATASYNC ID*** =$dataSyncId
                ***AUTH USER*** =$authUser
                ***ACCOUNT NAME*** =$accountNamePref
                ***ACCOUNT EMAIL*** =$accountEmail
                ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
            """.trimIndent()

            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(text),
                onDone = { data ->
                    var cookie = ""
                    var visitorDataValue = ""
                    var dataSyncIdValue = ""
                    var authUserValue = "0"
                    var accountNameValue = ""
                    var accountEmailValue = ""
                    var accountChannelHandleValue = ""

                    data.split("\n").forEach {
                        when {
                            it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                            it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                            it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                            it.startsWith("***AUTH USER*** =") -> authUserValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                        }
                    }
                    // Write all credentials atomically to DataStore and wait for completion
                    // before restarting, preventing the race condition where the process
                    // would be killed before async DataStore coroutines finished writing.
                    accountSettingsViewModel.saveTokenAndRestart(
                        cookie = cookie,
                        visitorData = visitorDataValue,
                        dataSyncId = dataSyncIdValue,
                        authUser = authUserValue,
                        accountName = accountNameValue,
                        accountEmail = accountEmailValue,
                        accountChannelHandle = accountChannelHandleValue,
                    )
                },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 20,
                isInputValid = { fullText ->
                    // Extract the cookie value from the formatted template line,
                    // then validate it separately — avoids the bug where parseCookieString
                    // received the entire multi-line template and failed to find "SAPISID"
                    // as a key because the "***INNERTUBE COOKIE*** =" prefix shadowed it.
                    val cookieLine = fullText.lines()
                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                    val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                    cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
                },
                extraContent = {
                    Spacer(Modifier.height(8.dp))
                    InfoLabel(text = stringResource(Res.string.token_adv_login_description))
                }
            )
        }

        Material3SettingsGroup(
            items = listOfNotNull(
                Material3SettingsItem(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLoggedIn && accountImageUrl != null) {
                                AsyncImage(
                                    model = accountImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )

                                Spacer(Modifier.width(12.dp))
                            }

                            Text(
                                text = if (isLoggedIn) accountName else stringResource(Res.string.login),
                            )
                        }
                    },
                    icon = if (!isLoggedIn) painterResource(Res.drawable.login) else null,
                    trailingContent = {
                        if (isLoggedIn) {
                            OutlinedButton(
                                onClick = {
                                    Logger.d { "[LOGOUT] User clicked logout button, showing dialog" }
                                    showLogoutDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(stringResource(Res.string.action_logout))
                            }
                        }
                    },
                    onClick = {
                        onClose()
                        if (isLoggedIn) {
                            navController.navigate("account")
                        } else {
                            navController.navigate("login")
                        }
                    }
                ),
                if (isLoggedIn) {
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.switch_youtube_channel)) },
                        icon = painterResource(Res.drawable.account),
                        onClick = {
                            onClose()
                            navController.navigate("switch_channel")
                        },
                    )
                } else null,
            ),
            useLowContrast = true
        )

        Spacer(Modifier.height(8.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Text(
                            when {
                                !isLoggedIn -> stringResource(Res.string.advanced_login)
                                showToken -> stringResource(Res.string.token_shown)
                                else -> stringResource(Res.string.token_hidden)
                            }
                        )
                    },
                    icon = painterResource(Res.drawable.token),
                    onClick = {
                        if (!isLoggedIn) showTokenEditor = true
                        else if (!showToken) showToken = true
                        else showTokenEditor = true
                    }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(Res.string.more_content)) },
                    icon = painterResource(Res.drawable.cached),
                    trailingContent = {
                        Switch(
                            enabled = isLoggedIn,
                            checked = useLoginForBrowse,
                            onCheckedChange = {
                                YouTube.useLoginForBrowse = it
                                onUseLoginForBrowseChange(it)
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (useLoginForBrowse) Res.drawable.check else Res.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    enabled = isLoggedIn
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(Res.string.yt_sync)) },
                    icon = painterResource(Res.drawable.cached),
                    trailingContent = {
                        Switch(
                            enabled = isLoggedIn,
                            checked = ytmSync,
                            onCheckedChange = onYtmSyncChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (ytmSync) Res.drawable.check else Res.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    enabled = isLoggedIn
                )
            ),
            useLowContrast = true
        )

        Spacer(Modifier.height(12.dp))

        Material3SettingsGroup(
            items = buildList {
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.integrations)) },
                        icon = painterResource(Res.drawable.integration),
                        onClick = {
                            onClose()
                            navController.navigate("settings/integrations")
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.settings)) },
                        icon = painterResource(Res.drawable.settings),
                        showBadge = BuildInfo.updaterAvailable &&
                            latestVersionName != BuildInfo.baseVersionName,
                        onClick = {
                            onClose()
                            navController.navigate("settings")
                        }
                    )
                )

                if (BuildInfo.updaterAvailable && latestVersionName != BuildInfo.baseVersionName) {
                    val releaseInfo = Updater.getCachedLatestRelease()
                    val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
                    if (downloadUrl != null) {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.new_version_available)) },
                                description = { Text(latestVersionName) },
                                icon = painterResource(Res.drawable.update),
                                showBadge = true,
                                onClick = { uriHandler.openUri(downloadUrl) }
                            )
                        )
                    }
                }
            },
            useLowContrast = true
        )
    }
}
