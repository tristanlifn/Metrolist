package com.metrolist.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
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
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

// Proof of concept: search YouTube Music, extract an audio stream without a PoToken provider, play it with mpv.
fun main(args: Array<String>): Unit = runBlocking {
    val query = args.joinToString(" ").ifBlank { "Daft Punk Get Lucky" }
    val song =
        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
            .items.filterIsInstance<SongItem>().firstOrNull()
            ?: run {
                System.err.println("No song found for \"$query\"")
                exitProcess(1)
            }
    println("Found: ${song.title} - ${song.artists.joinToString { it.name }} (${song.id})")

    val logger =
        InnerTubeLogger { event ->
            if (event.level >= InnerTubeLogLevel.WARN) System.err.println("[${event.level}] ${event.tag}: ${event.message} ${event.details}")
        }
    val transport = YouTube.extractionTransport()
    val configStore = RemotePlayerConfigStore(transport.httpClient, InMemoryPlayerConfigRepository(), logger)
    val cipherService = YouTubeCipherService(transport.httpClient, configStore, logger)
    val extractor =
        InnerTubeExtractor(
            configParser = YtConfigParserImpl(transport.httpClient, transport.innerTube, configStore, logger),
            cipherService = cipherService,
            innerTube = transport.innerTube,
            tokenProvider = NoPoTokenProvider,
            logger = logger,
        )
    val stream =
        extractor.extract(
            videoId = song.id,
            hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
            excludedClients = emptySet(),
            audioQuality = AudioQuality.HIGH,
            clientPlaybackNonce = generateClientPlaybackNonce(),
        ) ?: run {
            System.err.println("No playable stream for ${song.id}")
            exitProcess(1)
        }
    println("Stream: client=${stream.clientName} itag=${stream.itag} ${stream.mimeType} ${stream.bitrate}bps")

    val command =
        listOf("mpv", "--no-video", "--term-osd-bar") +
            stream.headers.map { (key, value) -> "--http-header-fields-append=$key: $value" } +
            stream.audioUrl
    exitProcess(ProcessBuilder(command).inheritIO().start().waitFor())
}

// Lets the extractor pick clients that don't need a PoToken; desktop has no WebView for BotGuard.
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
