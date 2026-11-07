/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.metrolist.paxsenix.Paxsenix
import com.metrolist.music.constants.EnablePaxsenixKey
import com.metrolist.music.utils.get

object PaxsenixLyricsProvider : LyricsProvider {
    private const val TAG = "PaxsenixProvider"
    
    override val name = "Paxsenix"

    override fun isEnabled(dataStore: DataStore<Preferences>): Boolean = dataStore[EnablePaxsenixKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        Logger.withTag(TAG).d { "getLyrics called: title='$title', artist='$artist', duration=$duration" }
        
        try {
            Paxsenix.init()
            val result = Paxsenix.getLyrics(title, artist, duration, album)
            
            result.onSuccess { lyrics ->
                Logger.withTag(TAG).i { "Success! Got ${lyrics.length} chars of lyrics" }
            }.onFailure { e ->
                Logger.withTag(TAG).e(e) { "Failed to get lyrics" }
            }
            
            return result
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Exception in getLyrics" }
            return Result.failure(e)
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        Logger.withTag(TAG).d { "getAllLyrics called" }
        try {
            Paxsenix.init()
            Paxsenix.getAllLyrics(title, artist, duration, album, callback)
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error fetching lyrics from Paxsenix" }
            callback("")
        }
    }
}
