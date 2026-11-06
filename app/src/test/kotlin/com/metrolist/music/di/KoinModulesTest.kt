package com.metrolist.music.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class KoinModulesTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `all definitions resolve their constructor dependencies`() {
        module { includes(appModule, networkModule, wrappedModule, viewModelModule) }
            .verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
