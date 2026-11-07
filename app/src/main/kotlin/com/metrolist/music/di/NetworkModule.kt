/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.di

import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.NetworkConnectivity
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    singleOf(::NetworkConnectivityObserver) bind NetworkConnectivity::class
}
