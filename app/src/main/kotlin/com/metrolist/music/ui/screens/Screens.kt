/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.runtime.Immutable
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.filter_library
import com.metrolist.music.resources.group_filled
import com.metrolist.music.resources.group_outlined
import com.metrolist.music.resources.home
import com.metrolist.music.resources.home_filled
import com.metrolist.music.resources.home_outlined
import com.metrolist.music.resources.library_music_filled
import com.metrolist.music.resources.library_music_outlined
import com.metrolist.music.resources.search
import com.metrolist.music.resources.together
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
sealed class Screens(
    val titleId: StringResource,
    val iconIdInactive: DrawableResource,
    val iconIdActive: DrawableResource,
    val route: String,
) {
    object Home : Screens(
        titleId = Res.string.home,
        iconIdInactive = Res.drawable.home_outlined,
        iconIdActive = Res.drawable.home_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = Res.string.search,
        iconIdInactive = Res.drawable.search,
        iconIdActive = Res.drawable.search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = Res.string.together,
        iconIdInactive = Res.drawable.group_outlined,
        iconIdActive = Res.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = Res.string.filter_library,
        iconIdInactive = Res.drawable.library_music_outlined,
        iconIdActive = Res.drawable.library_music_filled,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Search, ListenTogether, Library)
    }
}
