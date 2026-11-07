/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:Suppress("FunctionName")

package com.metrolist.desktop.mpris

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal

// D-Bus method names must match the MPRIS spec exactly, hence the capitalised functions.

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2 : DBusInterface {
    fun Raise()

    fun Quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MediaPlayer2Player : DBusInterface {
    fun Next()

    fun Previous()

    fun Pause()

    fun PlayPause()

    fun Stop()

    fun Play()

    fun Seek(offset: Long)

    fun SetPosition(trackId: DBusPath, position: Long)

    fun OpenUri(uri: String)

    class Seeked(
        path: String,
        position: Long,
    ) : DBusSignal(path, position)
}
