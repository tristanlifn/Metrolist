/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.playback

import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.metrolist.innertube.YouTube
import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.db.entities.FormatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.metrolist.music.constants.AudioQuality as AppAudioQuality

/** A playable audio stream: the URL and the request headers YouTube expects with it. */
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String>,
    val clientName: String,
    val format: FormatEntity,
)

/** Finds an audio stream for a video, using clients that work without a PoToken (desktop has no BotGuard). */
class StreamResolver {
    private val logger =
        InnerTubeLogger { event ->
            if (event.level >= InnerTubeLogLevel.WARN) Logger.withTag("InnerTube").w { "${event.tag}: ${event.message} ${event.details}" }
        }

    private val extractor by lazy {
        val transport = YouTube.extractionTransport()
        val configStore = RemotePlayerConfigStore(transport.httpClient, InMemoryPlayerConfigRepository(), logger)
        InnerTubeExtractor(
            configParser = YtConfigParserImpl(transport.httpClient, transport.innerTube, configStore, logger),
            cipherService = YouTubeCipherService(transport.httpClient, configStore, logger),
            innerTube = transport.innerTube,
            tokenProvider = NoPoTokenProvider,
            logger = logger,
        )
    }

    suspend fun resolve(
        videoId: String,
        quality: AppAudioQuality,
    ): ResolvedStream? =
        withContext(Dispatchers.IO) {
            extractor
                .extract(
                    videoId = videoId,
                    hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
                    excludedClients = emptySet(),
                    // Desktop has no metered-network check, so AUTO behaves like Android on an unmetered network.
                    audioQuality =
                        when (quality) {
                            AppAudioQuality.AUTO -> AudioQuality.AUTO
                            AppAudioQuality.LOW -> AudioQuality.LOW
                            AppAudioQuality.HIGH -> AudioQuality.HIGH
                        },
                    clientPlaybackNonce = generateClientPlaybackNonce(),
                )?.let {
                    ResolvedStream(
                        url = it.audioUrl,
                        headers = it.headers,
                        clientName = it.clientName,
                        format =
                            FormatEntity(
                                id = videoId,
                                itag = it.itag,
                                mimeType = it.mimeType.orEmpty(),
                                codecs = it.codecs.orEmpty(),
                                bitrate = it.bitrate ?: 0,
                                sampleRate = it.sampleRate,
                                contentLength = it.contentLengthBytes ?: 0L,
                                loudnessDb = it.loudnessDb,
                                perceptualLoudnessDb = it.perceptualLoudnessDb,
                                playbackUrl = null,
                            ),
                    )
                }
        }

    companion object {
        /** The audio quality setting, reading the old VERY_HIGH value as HIGH like Android does. */
        fun audioQuality(preferences: Preferences): AppAudioQuality =
            preferences[AudioQualityKey]?.let { value ->
                if (value == "VERY_HIGH") AppAudioQuality.HIGH else AppAudioQuality.entries.find { it.name == value }
            } ?: AppAudioQuality.AUTO
    }
}

private object NoPoTokenProvider : TokenProvider {
    override val capabilities = TokenProviderCapabilities()

    override suspend fun getPoToken(
        videoId: String,
        visitorData: String,
        cookie: String?,
    ): PoTokenResult? = null
}

private class InMemoryPlayerConfigRepository : PlayerConfigRepository {
    override val enabled = true
    override val sourceUrl = PLAYER_CONFIG_URL
    override val defaultSourceUrl = PLAYER_CONFIG_URL
    override var cachedJson = ""
    override var cachedAtMs = 0L
    override var cachedSourceUrl = ""
    override var cachedEtag = ""

    private companion object {
        const val PLAYER_CONFIG_URL =
            "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
    }
}
