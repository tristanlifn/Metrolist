/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.playback

import co.touchlab.kermit.Logger
import com.sun.jna.Pointer

/**
 * One libmpv instance used as an audio-only player. Events arrive on a dedicated thread through [onEvent];
 * the other calls are thread-safe per libmpv's API.
 */
internal class Mpv(
    private val onEvent: (Event) -> Unit,
) {
    sealed interface Event {
        data object FileLoaded : Event

        data class EndFile(val reason: Int, val error: String?) : Event

        data class PropertyChanged(val name: String) : Event
    }

    private val lib = LibMpv.INSTANCE
    private val ctx: Pointer

    init {
        // libmpv refuses to start unless numbers are formatted the C way, and the JVM adopts the user's locale.
        LibC.INSTANCE.setlocale(LibC.LC_NUMERIC, "C")
        ctx = checkNotNull(lib.mpv_create()) { "mpv_create failed" }
    }

    init {
        lib.mpv_set_option_string(ctx, "video", "no")
        lib.mpv_set_option_string(ctx, "ytdl", "no")
        lib.mpv_set_option_string(ctx, "terminal", "no")
        lib.mpv_set_option_string(ctx, "idle", "yes")
        // The PipeWire stream name; Omarchy's shell and mixers match it to the MPRIS player by this name.
        lib.mpv_set_option_string(ctx, "audio-client-name", "Metrolist")
        check(lib.mpv_initialize(ctx) >= 0) { "mpv_initialize failed" }
        OBSERVED_FLAGS.forEachIndexed { index, name -> lib.mpv_observe_property(ctx, index.toLong(), name, LibMpv.FORMAT_FLAG) }
        Thread(::eventLoop, "mpv-events").apply { isDaemon = true }.start()
    }

    fun command(vararg args: String) {
        val result = lib.mpv_command(ctx, arrayOf(*args, null))
        if (result < 0) Logger.withTag("Mpv").w { "${args.first()} failed: ${lib.mpv_error_string(result)}" }
    }

    fun setProperty(name: String, value: String) {
        lib.mpv_set_property_string(ctx, name, value)
    }

    fun getProperty(name: String): String? {
        val pointer = lib.mpv_get_property_string(ctx, name) ?: return null
        return try {
            pointer.getString(0)
        } finally {
            lib.mpv_free(pointer)
        }
    }

    fun getDouble(name: String): Double? = getProperty(name)?.toDoubleOrNull()

    fun getFlag(name: String): Boolean = getProperty(name) == "yes"

    /** Stops playback; the event thread frees the handle once mpv confirms it has shut down. */
    fun destroy() = command("quit")

    private fun eventLoop() {
        while (true) {
            val event = lib.mpv_wait_event(ctx, -1.0)
            when (event.event_id) {
                LibMpv.EVENT_SHUTDOWN -> {
                    // libmpv forbids destroying the handle while another thread waits on it, so it happens here.
                    lib.mpv_terminate_destroy(ctx)
                    return
                }
                LibMpv.EVENT_FILE_LOADED -> onEvent(Event.FileLoaded)
                LibMpv.EVENT_END_FILE -> {
                    val endFile = event.data?.let(::MpvEventEndFile)
                    val error = endFile?.error?.takeIf { it < 0 }?.let(lib::mpv_error_string)
                    onEvent(Event.EndFile(endFile?.reason ?: LibMpv.END_FILE_REASON_EOF, error))
                }
                LibMpv.EVENT_PROPERTY_CHANGE ->
                    OBSERVED_FLAGS.getOrNull(event.reply_userdata.toInt())?.let { onEvent(Event.PropertyChanged(it)) }
            }
        }
    }

    companion object {
        val OBSERVED_FLAGS = listOf("pause", "paused-for-cache", "eof-reached")
    }
}
