/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.mpris

import co.touchlab.kermit.Logger
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.MusicPlayer
import com.metrolist.music.playback.PlaybackState
import com.metrolist.music.playback.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/**
 * Publishes [player] over MPRIS so media keys, playerctl, lock screens and status bars can show and control it.
 * D-Bus calls arrive on dbus-java's threads; MusicPlayer commands are safe to call from any thread.
 */
class MprisService private constructor(
    private val player: MusicPlayer,
    private val connection: DBusConnection,
    private val onRaise: () -> Unit,
) : MediaPlayer2, MediaPlayer2Player, Properties {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun getObjectPath() = OBJECT_PATH

    override fun Raise() = onRaise()

    override fun Quit() = Unit

    override fun Next() = player.skipToNextItem()

    override fun Previous() = player.seekToPrevious()

    override fun Pause() = player.pause()

    override fun PlayPause() = player.togglePlayPause()

    override fun Stop() = player.pause()

    override fun Play() = player.play()

    override fun Seek(offset: Long) = seekTo(player.currentPosition + offset / 1000)

    override fun SetPosition(
        trackId: DBusPath,
        position: Long,
    ) {
        // The spec says to ignore requests for a track that is no longer playing.
        if (trackId.path == trackPath(player.mediaMetadata.value)) seekTo(position / 1000)
    }

    override fun OpenUri(uri: String) = Unit

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(
        interfaceName: String,
        propertyName: String,
    ): A = properties(interfaceName)[propertyName]?.value as A

    override fun <A : Any?> Set(
        interfaceName: String,
        propertyName: String,
        value: A,
    ) {
        if (interfaceName != PLAYER_INTERFACE) return
        when (propertyName) {
            "LoopStatus" ->
                player.setRepeatMode(
                    when (value) {
                        "Track" -> RepeatMode.ONE
                        "Playlist" -> RepeatMode.ALL
                        else -> RepeatMode.OFF
                    },
                )
            "Shuffle" -> player.setShuffleModeEnabled(value as Boolean)
            "Volume" -> player.setVolume((value as Double).toFloat())
        }
    }

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> = properties(interfaceName)

    private fun properties(interfaceName: String): Map<String, Variant<*>> =
        when (interfaceName) {
            ROOT_INTERFACE ->
                mapOf(
                    "CanQuit" to Variant(false),
                    "CanRaise" to Variant(true),
                    "HasTrackList" to Variant(false),
                    "Identity" to Variant("Metrolist"),
                    "DesktopEntry" to Variant("com.metrolist.music"),
                    "SupportedUriSchemes" to Variant(emptyList<String>(), "as"),
                    "SupportedMimeTypes" to Variant(emptyList<String>(), "as"),
                )
            PLAYER_INTERFACE ->
                mapOf(
                    "PlaybackStatus" to Variant(playbackStatus()),
                    "LoopStatus" to Variant(loopStatus()),
                    "Rate" to Variant(1.0),
                    "Shuffle" to Variant(player.shuffleModeEnabled.value),
                    "Metadata" to Variant(metadata(player.mediaMetadata.value), "a{sv}"),
                    "Volume" to Variant(player.volume.value.toDouble()),
                    "Position" to Variant(player.currentPosition * 1000),
                    "MinimumRate" to Variant(1.0),
                    "MaximumRate" to Variant(1.0),
                    "CanGoNext" to Variant(player.canSkipNext.value),
                    "CanGoPrevious" to Variant(player.canSkipPrevious.value),
                    "CanPlay" to Variant(player.mediaMetadata.value != null),
                    "CanPause" to Variant(player.mediaMetadata.value != null),
                    "CanSeek" to Variant(player.mediaMetadata.value != null),
                    "CanControl" to Variant(true),
                )
            else -> emptyMap()
        }

    @Volatile private var lastStatus = "Stopped"

    // Buffering between songs keeps the last status, so bars don't flash "Paused" on every track change.
    private fun playbackStatus(): String {
        lastStatus =
            when {
                player.mediaMetadata.value == null || player.playbackState.value == PlaybackState.IDLE -> "Stopped"
                player.playbackState.value == PlaybackState.BUFFERING -> lastStatus
                player.isPlaying.value -> "Playing"
                else -> "Paused"
            }
        return lastStatus
    }

    private fun loopStatus(): String =
        when (player.repeatMode.value) {
            RepeatMode.OFF -> "None"
            RepeatMode.ONE -> "Track"
            RepeatMode.ALL -> "Playlist"
        }

    private fun metadata(song: MediaMetadata?): Map<String, Variant<*>> {
        if (song == null) return mapOf("mpris:trackid" to Variant(DBusPath(NO_TRACK)))
        return buildMap {
            put("mpris:trackid", Variant(DBusPath(trackPath(song))))
            put("xesam:title", Variant(song.title))
            put("xesam:artist", Variant(song.artists.map { it.name }, "as"))
            song.album?.title?.let { put("xesam:album", Variant(it)) }
            song.thumbnailUrl?.let { put("mpris:artUrl", Variant(it)) }
            if (song.duration > 0) put("mpris:length", Variant(song.duration * 1_000_000L))
        }
    }

    private fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0)
        player.seekTo(target)
        connection.sendMessage(MediaPlayer2Player.Seeked(OBJECT_PATH, target * 1000))
    }

    private fun observe() {
        emitOnChange(player.isPlaying, player.playbackState, player.mediaMetadata) { "PlaybackStatus" to Variant(playbackStatus()) }
        emitOnChange(player.mediaMetadata) { "Metadata" to Variant(metadata(player.mediaMetadata.value), "a{sv}") }
        emitOnChange(player.repeatMode) { "LoopStatus" to Variant(loopStatus()) }
        emitOnChange(player.shuffleModeEnabled) { "Shuffle" to Variant(player.shuffleModeEnabled.value) }
        emitOnChange(player.volume) { "Volume" to Variant(player.volume.value.toDouble()) }
        emitOnChange(player.canSkipNext) { "CanGoNext" to Variant(player.canSkipNext.value) }
        emitOnChange(player.canSkipPrevious) { "CanGoPrevious" to Variant(player.canSkipPrevious.value) }
    }

    private fun emitOnChange(
        vararg sources: Flow<*>,
        property: () -> Pair<String, Variant<*>>,
    ) {
        combine(*sources) { property() }
            .distinctUntilChanged { old, new -> old.second.value == new.second.value }
            .drop(1)
            .onEach { changed ->
                runCatching {
                    connection.sendMessage(Properties.PropertiesChanged(OBJECT_PATH, PLAYER_INTERFACE, mapOf(changed), emptyList()))
                }.onFailure { Logger.withTag("Mpris").w(it) { "Could not send a property change" } }
            }.launchIn(scope)
    }

    fun close() {
        scope.cancel()
        runCatching {
            connection.releaseBusName(BUS_NAME)
            connection.close()
        }
    }

    companion object {
        private const val BUS_NAME = "org.mpris.MediaPlayer2.metrolist"
        private const val OBJECT_PATH = "/org/mpris/MediaPlayer2"
        private const val ROOT_INTERFACE = "org.mpris.MediaPlayer2"
        private const val PLAYER_INTERFACE = "org.mpris.MediaPlayer2.Player"
        private const val NO_TRACK = "/org/mpris/MediaPlayer2/TrackList/NoTrack"

        // Object paths only allow [A-Za-z0-9_], so other characters in YouTube ids are hex-escaped.
        private fun trackPath(song: MediaMetadata?): String =
            song?.let { "/org/metrolist/track/" + it.id.map { c -> if (c.isLetterOrDigit()) "$c" else "_%02x".format(c.code) }.joinToString("") }
                ?: NO_TRACK

        /** Connects to the session bus and publishes [player]; returns null where there's no session bus. */
        fun start(
            player: MusicPlayer,
            onRaise: () -> Unit,
        ): MprisService? =
            runCatching {
                val connection = DBusConnectionBuilder.forSessionBus().build()
                MprisService(player, connection, onRaise).also {
                    connection.exportObject(OBJECT_PATH, it)
                    connection.requestBusName(BUS_NAME)
                    it.observe()
                }
            }.onFailure { Logger.withTag("Mpris").w(it) { "MPRIS unavailable" } }.getOrNull()
    }
}
