/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import com.metrolist.music.db.DatabaseDao
import org.koin.mp.KoinPlatform

internal val LocalWrappedManager = compositionLocalOf<WrappedManager> { error("No WrappedManager found!") }

internal fun provideWrappedManager(context: Context): WrappedManager =
    WrappedManager(
        databaseDao = KoinPlatform.getKoin().get<DatabaseDao>(),
        context = context.applicationContext
    )
