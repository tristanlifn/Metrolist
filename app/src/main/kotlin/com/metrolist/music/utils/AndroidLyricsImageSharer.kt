/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.text.Layout
import com.metrolist.music.lyrics.LyricsImageSharer
import com.metrolist.music.ui.component.LyricsBackgroundStyle
import com.metrolist.music.ui.screens.settings.LyricsPosition

class AndroidLyricsImageSharer(
    private val context: Context,
) : LyricsImageSharer {
    override suspend fun share(
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int,
        backgroundStyle: LyricsBackgroundStyle,
        textColor: Int,
        secondaryTextColor: Int,
        alignment: LyricsPosition,
        chooserTitle: String,
    ) {
        val image =
            ComposeToImage.createLyricsImage(
                context = context,
                coverArtUrl = coverArtUrl,
                songTitle = songTitle,
                artistName = artistName,
                lyrics = lyrics,
                width = width,
                height = height,
                backgroundColor = backgroundColor,
                backgroundStyle = backgroundStyle,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                lyricsAlignment =
                    when (alignment) {
                        LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                        LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                        else -> Layout.Alignment.ALIGN_OPPOSITE
                    },
            )
        val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_${System.currentTimeMillis()}")
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
