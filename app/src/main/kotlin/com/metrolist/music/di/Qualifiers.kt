/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.di

import org.koin.core.qualifier.named

val PlayerCache = named("PlayerCache")

val DownloadCache = named("DownloadCache")

val ApplicationScope = named("ApplicationScope")
