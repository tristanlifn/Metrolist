package com.metrolist.music.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import java.io.File

class KoinModulesTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `all definitions resolve their constructor dependencies`() {
        module { includes(appModule, networkModule, wrappedModule, viewModelModule) }
            // File is SyncUtils' data dir, passed in its definition rather than resolved from Koin.
            .verify(extraTypes = listOf(Context::class, SavedStateHandle::class, File::class))
    }
}
