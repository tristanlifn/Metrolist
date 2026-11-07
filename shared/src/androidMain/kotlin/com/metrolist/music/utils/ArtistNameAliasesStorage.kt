/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.SharedPreferences

private const val PREFERENCES_NAME = "artist_name_aliases"

@Volatile
private var preferences: SharedPreferences? = null

fun ArtistNameAliases.initialize(context: Context) {
    if (preferences != null) return
    synchronized(this) {
        if (preferences != null) return
        val sharedPreferences =
            context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        replaceAll(
            sharedPreferences.all.mapNotNull { (artistId, value) ->
                (value as? String)?.let { artistId to it }
            }.toMap(),
        )
        preferences = sharedPreferences
    }
}

fun ArtistNameAliases.set(
    context: Context,
    artistId: String,
    channelId: String?,
    originalName: String,
    name: String,
) {
    initialize(context)
    val aliasKeys = aliasKeys(artistId, channelId, originalName)
    preferences?.edit()?.apply {
        aliasKeys.forEach { putString(it, name) }
        apply()
    }
    putAll(aliasKeys, name)
}

fun ArtistNameAliases.restore(
    context: Context,
    restoredAliases: Map<String, String>,
) {
    initialize(context)
    preferences?.edit()?.apply {
        clear()
        restoredAliases.forEach(::putString)
        commit()
    }
    replaceAll(restoredAliases)
}
