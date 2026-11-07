/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.di

import com.metrolist.music.ui.screens.equalizer.EQViewModel
import com.metrolist.music.ui.screens.equalizer.wizard.WizardViewModel
import com.metrolist.music.viewmodels.BackupRestoreViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels for Android-only screens; the rest are in sharedViewModelModule. */
val viewModelModule = module {
    viewModelOf(::BackupRestoreViewModel)
    viewModelOf(::EQViewModel)
    viewModelOf(::WizardViewModel)
}
