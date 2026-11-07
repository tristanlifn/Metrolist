/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.browser_login_description
import com.metrolist.music.resources.browser_login_failed
import com.metrolist.music.resources.browser_login_not_signed_in
import com.metrolist.music.resources.browser_login_open
import com.metrolist.music.resources.browser_login_title
import com.metrolist.music.resources.browser_login_unsupported
import com.metrolist.music.resources.browser_login_waiting
import com.metrolist.music.resources.retry
import com.metrolist.music.utils.safeEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private sealed interface LoginState {
    data object Checking : LoginState

    data object NotSignedIn : LoginState

    data object SigningIn : LoginState

    data class Failed(val message: String) : LoginState
}

private const val SIGN_IN_URL =
    "https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"
private const val POLL_INTERVAL_MS = 3_000L

/**
 * Signs in with the YouTube session of the user's default browser. When the browser isn't signed in, it opens
 * Google's own sign-in page there and keeps checking until the session appears.
 */
@Composable
fun DesktopLoginScreen(navController: NavController) {
    val dataStore = koinInject<DataStore<Preferences>>()
    val uriHandler = LocalUriHandler.current
    val browser = remember { BrowserCookies.defaultBrowser() }
    var state by remember { mutableStateOf<LoginState>(LoginState.Checking) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(browser, attempt) {
        if (browser !is DefaultBrowser.Firefox) return@LaunchedEffect
        while (true) {
            val cookie = withContext(Dispatchers.IO) { runCatching { BrowserCookies.youTubeMusicCookie(browser) }.getOrNull() }
            if (cookie != null) {
                state = LoginState.SigningIn
                withContext(Dispatchers.IO) { signIn(cookie, dataStore) }
                    .onSuccess {
                        navController.popBackStack()
                        return@LaunchedEffect
                    }.onFailure {
                        state = LoginState.Failed(it.message.orEmpty())
                        return@LaunchedEffect
                    }
            }
            state = LoginState.NotSignedIn
            delay(POLL_INTERVAL_MS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.browser_login_title), style = MaterialTheme.typography.headlineSmall)
        if (browser !is DefaultBrowser.Firefox) {
            Text(stringResource(Res.string.browser_login_unsupported, browser.name), textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 480.dp))
            return@Column
        }
        val message =
            when (val current = state) {
                LoginState.NotSignedIn -> stringResource(Res.string.browser_login_not_signed_in, browser.name)
                is LoginState.Failed -> stringResource(Res.string.browser_login_failed, current.message)
                else -> stringResource(Res.string.browser_login_description, browser.name)
            }
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 480.dp))
        when (state) {
            LoginState.Checking, LoginState.SigningIn -> CircularProgressIndicator()
            LoginState.NotSignedIn -> {
                Button(onClick = { uriHandler.openUri(SIGN_IN_URL) }) { Text(stringResource(Res.string.browser_login_open)) }
                Text(stringResource(Res.string.browser_login_waiting), style = MaterialTheme.typography.bodySmall)
            }
            is LoginState.Failed ->
                TextButton(onClick = {
                    state = LoginState.Checking
                    attempt++
                }) { Text(stringResource(Res.string.retry)) }
        }
    }
}

// Mirrors the Android login: the cookie plus the account details and ids YouTube needs for signed-in requests.
private suspend fun signIn(
    cookie: String,
    dataStore: DataStore<Preferences>,
): Result<Unit> =
    runCatching {
        YouTube.cookie = cookie
        val account = YouTube.accountInfo().getOrThrow()
        val dataSyncId = YouTube.accountsList().getOrNull()?.let { accounts -> accounts.firstOrNull { it.isSelected } ?: accounts.firstOrNull() }?.dataSyncId
        val visitorData = YouTube.visitorData().getOrNull()
        val saved =
            dataStore.safeEdit { settings ->
                settings[InnerTubeCookieKey] = cookie
                visitorData?.let { settings[VisitorDataKey] = it }
                dataSyncId?.let { settings[DataSyncIdKey] = it }
                settings[InnerTubeAuthUserKey] = "0"
                settings[AccountNameKey] = account.name
                settings[AccountEmailKey] = account.email.orEmpty()
                settings[AccountChannelHandleKey] = account.channelHandle.orEmpty()
            }
        check(saved) { "Couldn't save the account" }
    }.onFailure { YouTube.cookie = null }
