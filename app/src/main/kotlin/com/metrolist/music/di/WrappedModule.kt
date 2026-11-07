package com.metrolist.music.di

import com.metrolist.music.ui.screens.wrapped.AndroidWrappedPreloader
import com.metrolist.music.ui.screens.wrapped.WrappedAudioService
import com.metrolist.music.utils.WrappedPreloader
import org.koin.dsl.bind
import com.metrolist.music.ui.screens.wrapped.WrappedManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val wrappedModule = module {
    singleOf(::WrappedManager)
    singleOf(::WrappedAudioService)
    singleOf(::AndroidWrappedPreloader) bind WrappedPreloader::class
}
