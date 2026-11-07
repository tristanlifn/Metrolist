package com.metrolist.innertube.utils

/** Platform-neutral log hook; each app routes it to its own logger (Timber on Android). */
object Logger {
    /** Priorities follow android.util.Log: 3 = debug, 5 = warn, 6 = error. */
    var sink: (priority: Int, error: Throwable?, message: String) -> Unit = { _, _, _ -> }

    fun d(message: String) = sink(3, null, message)

    fun w(message: String) = sink(5, null, message)

    fun e(error: Throwable, message: String) = sink(6, error, message)
}
