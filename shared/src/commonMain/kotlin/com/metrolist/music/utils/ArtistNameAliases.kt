/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** In-memory artist name aliases; each platform loads and persists them (see ArtistNameAliasesStorage). */
object ArtistNameAliases {
    const val BACKUP_FILENAME = "artist_name_aliases.json"

    private val _aliases = MutableStateFlow<Map<String, String>>(emptyMap())
    val aliases = _aliases.asStateFlow()

    fun aliasKeys(
        artistId: String,
        channelId: String?,
        originalName: String,
    ): Set<String> {
        val currentAliases = _aliases.value
        return buildSet {
            add(artistId)
            channelId?.let(::add)
            add(nameKey(originalName))
            currentAliases.filterValues { it == originalName }.keys.forEach(::add)
        }
    }

    fun putAll(
        aliasKeys: Set<String>,
        name: String,
    ) {
        _aliases.update { aliases -> aliases + aliasKeys.associateWith { name } }
    }

    fun replaceAll(aliases: Map<String, String>) {
        _aliases.value = aliases
    }

    fun resolve(
        artistId: String?,
        fallback: String,
    ): String = resolve(_aliases.value, artistId, fallback)

    fun resolve(
        aliases: Map<String, String>,
        artistId: String?,
        fallback: String,
    ): String {
        var resolvedName = artistId?.let(aliases::get) ?: aliases[nameKey(fallback)] ?: fallback
        var nextName = aliases[nameKey(resolvedName)] ?: return resolvedName
        val visitedNames = mutableSetOf(resolvedName)
        while (visitedNames.add(nextName)) {
            resolvedName = nextName
            nextName = aliases[nameKey(resolvedName)] ?: return resolvedName
        }
        return resolvedName
    }

    fun serialize(): String = Json.encodeToString(_aliases.value)

    fun deserialize(serializedAliases: String): Map<String, String> = Json.decodeFromString(serializedAliases)

    private fun nameKey(name: String) = "name:$name"
}
