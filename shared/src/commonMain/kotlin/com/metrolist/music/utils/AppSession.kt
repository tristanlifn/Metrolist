/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

/** App-level actions whose implementation depends on the platform. */
interface AppSession {
    /** Clears the signed-in account's credentials, including any platform cookies. */
    suspend fun forgetAccount()

    fun restart()
}
