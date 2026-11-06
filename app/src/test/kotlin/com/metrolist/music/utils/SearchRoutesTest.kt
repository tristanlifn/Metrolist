package com.metrolist.music.utils

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SearchRoutesTest {
    @Test
    fun `result route encodes queries like Android Uri encode`() {
        listOf("plain", "a b/c?d#e&f=g", "100% ~ 'quoted' (x)*!", "ünïcödé 日本語 🎵", "null", "%2F", "").forEach { query ->
            assertEquals(
                "search/" + Uri.encode("__metrolist_search_query__$query"),
                SearchRoutes.resultRoute(query),
            )
        }
    }
}
