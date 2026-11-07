/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.playback

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

/** The slice of libmpv's C API the player uses (see mpv/client.h). */
@Suppress("FunctionName")
internal interface LibMpv : Library {
    fun mpv_create(): Pointer?

    fun mpv_initialize(ctx: Pointer): Int

    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int

    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int

    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?

    fun mpv_command(ctx: Pointer, args: Array<String?>): Int

    fun mpv_observe_property(ctx: Pointer, replyUserdata: Long, name: String, format: Int): Int

    fun mpv_wait_event(ctx: Pointer, timeout: Double): MpvEvent

    fun mpv_error_string(error: Int): String

    fun mpv_free(data: Pointer)

    fun mpv_terminate_destroy(ctx: Pointer)

    companion object {
        val INSTANCE: LibMpv by lazy { Native.load("mpv", LibMpv::class.java) }

        const val FORMAT_NONE = 0
        const val FORMAT_FLAG = 3

        const val EVENT_NONE = 0
        const val EVENT_SHUTDOWN = 1
        const val EVENT_END_FILE = 7
        const val EVENT_FILE_LOADED = 8
        const val EVENT_PROPERTY_CHANGE = 22

        const val END_FILE_REASON_EOF = 0
        const val END_FILE_REASON_ERROR = 4
    }
}

@Suppress("FunctionName")
internal interface LibC : Library {
    fun setlocale(category: Int, locale: String): String?

    companion object {
        val INSTANCE: LibC by lazy { Native.load("c", LibC::class.java) }

        // glibc's value for LC_NUMERIC.
        const val LC_NUMERIC = 1
    }
}

@Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
internal open class MpvEvent : Structure() {
    @JvmField var event_id: Int = 0

    @JvmField var error: Int = 0

    @JvmField var reply_userdata: Long = 0

    @JvmField var data: Pointer? = null
}

@Structure.FieldOrder("reason", "error")
internal open class MpvEventEndFile(pointer: Pointer) : Structure(pointer) {
    @JvmField var reason: Int = 0

    @JvmField var error: Int = 0

    init {
        read()
    }
}
