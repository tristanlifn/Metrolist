/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.utils.get

fun DataStore<Preferences>.hasLoginCookie(): Boolean = "SAPISID" in parseCookieString(this[InnerTubeCookieKey] ?: "")
