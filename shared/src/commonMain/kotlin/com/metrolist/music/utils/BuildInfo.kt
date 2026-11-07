/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

/** Facts about the running build, set once by the platform at startup (Android copies them from BuildConfig). */
object BuildInfo {
    var versionName = ""
        private set
    var baseVersionName = ""
        private set
    var versionCode = 0
        private set
    var architecture = ""
        private set
    var isDebug = false
        private set
    var castAvailable = false
        private set
    var updaterAvailable = false
        private set

    /** OS and device lines for bug reports. */
    var platformDetails: List<String> = emptyList()
        private set

    fun init(
        versionName: String,
        baseVersionName: String,
        versionCode: Int,
        architecture: String,
        isDebug: Boolean,
        castAvailable: Boolean,
        updaterAvailable: Boolean,
        platformDetails: List<String>,
    ) {
        this.versionName = versionName
        this.baseVersionName = baseVersionName
        this.versionCode = versionCode
        this.architecture = architecture
        this.isDebug = isDebug
        this.castAvailable = castAvailable
        this.updaterAvailable = updaterAvailable
        this.platformDetails = platformDetails
    }
}
