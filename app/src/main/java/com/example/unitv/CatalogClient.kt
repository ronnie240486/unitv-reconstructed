package com.example.unitv

import android.util.JsonReader
import android.util.JsonToken
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Carrega catálogos sem manter a resposta Xtream/M3U inteira em memória.
 * O limite evita que uma fonte com milhões de itens derrube dispositivos móveis.
 */
class CatalogClient {
    suspend fun load(
        playlist: Playlist,
        onProgress: (CatalogLoadProgress) -> Unit = {},
        onPartial: (CatalogSnapshot) -> Unit = {}
    ): CatalogSnapshot = withContext(Dispatchers.IO) {
        onProgress(CatalogLoadProgress(stage = "Conectando ao servidor"))
        if (playlist.directM3uUrl.isNotBlank()) {
            val direct = runCatching { loadM3u(playlist.directM3uUrl, onProgress, onPartial) }.getOrNull()
            direct?.takeIf { it.total > 0 } ?: loadFromFallbackCandidates(playlist, onProgress, onPartial)
        } else {
            loadFromFallbackCandidates(playlist, onProgress, onPartial)
        }
    }

    private fun loadFromFallbackCandidates(
        playlist: Playlist,
        onProgress: (CatalogLoadProgress) -> Unit,
        onPartial: (CatalogSnapshot) -> Unit
    ): CatalogSnapshot {
        for (candidate in m3uCandidates(playlist)) {
            val parsed = runCatching { loadM3u(candidate, onProgress, onPartial) }.getOrNull()
            if (parsed != null && parsed.total > 0) return parsed
        }
        onProgress(CatalogLoadProgress(stage = "Tentando outra fonte"))
        return runCatching { loadXtream(playlist) }.getOrElse { CatalogSnapshot() }
    }

    private fun m3uCandidates(playlist: Playlist): List<String> {
        val candidates = linkedSetOf<String>()
        val server = playlist.url.substringBefore("?").trimEnd('/')
            .removeSuffix("/player_api.php").removeSuffix("/get.php")
        if (playlist.username.isNotBlank() && playlist.password.isNotBlank() && server.isNotBlank()) {
            candidates += "$server/get.php?username=${encode(playlist.username)}&password=${encode(playlist.password)}&type=m3u_plus&output=ts"
        }
        if (playlist.directM3uUrl.isNotBlank()) candidates += playlist.directM3uUrl
        if (playlist.url.contains("get.php", true) || playlist.url.contains(".m3u", true) || playlist.url.contains("m3u8", true)) {
            candidates += playlist.url
        }
        return candidates.toList()
    }

    private fun loadXtream(playlist: Playlist): CatalogSnapshot {
        require(playlist.username.isNotBlank() && playlist.password.isNotBlank())
        val base = playlist.url.trimEnd('/')
        val liveCategories = categoryMap("$base/player_api.php", playlist, "get_live_categories")
        val vodCategories = categoryMap("$base/player_api.php", playlist, "get_vod_categories")
        val seriesCategories = categoryMap("$base/player_api.php", playlist, "get_series_categories")

        val live = buildList {
            streamArray("$base/player_api.php", playlist, "get_live_streams", MAX_LIVE_ITEMS) { item, index ->
                val id = item.optString("stream_id").ifBlank { item.optString("num", "live-$index") }
                val name = item.optString("name").ifBlank { "Canal ${index + 1}" }
                val category = liveCategories[item.optString("category_id")] ?: "Canais"
                add(
                    CatalogItem(
                        id = id,
                        title = name,
                        category = category,
                        kind = CatalogKind.LIVE,
                        imageUrl = item.optString("stream_icon"),
                        streamUrl = "$base/live/${encode(playlist.username)}/${encode(playlist.password)}/$id.m3u8"
                    )
                )
            }
        }
        val movies = buildList {
            streamArray("$base/player_api.php", playlist, "get_vod_streams", MAX_VOD_ITEMS) { item, index ->
                val id = item.optString("stream_id").ifBlank { item.optString("num", "movie-$index") }
                val name = item.optString("name").ifBlank { "Filme ${index + 1}" }
                val category = vodCategories[item.optString("category_id")] ?: "Filmes"
                add(
                    CatalogItem(
                        id = id,
                        title = name,
                        category = category,
                        kind = CatalogKind.MOVIE,
                        imageUrl = item.optString("stream_icon"),
                        streamUrl = "$base/movie/${encode(playlist.username)}/${encode(playlist.password)}/$id.mp4",
                        year = item.optString("year").toIntOrNull(),
                        rating = item.optString("rating").toDoubleOrNull(),
                        description = item.optString("plot")
                    )
                )
            }
        }
        val series = buildList {
            streamArray("$base/player_api.php", playlist, "get_series", MAX_VOD_ITEMS) { item, index ->
                val id = item.optString("series_id").ifBlank { item.optString("num", "series-$index") }
                val name = item.optString("name").ifBlank { "Série ${index + 1}" }
                val category = seriesCategories[item.optString("category_id")] ?: "Séries"
                add(
                    CatalogItem(
                        id = id,
                        title = name,
                        category = category,
                        kind = CatalogKind.SERIES,
                        imageUrl = item.optString("cover"),
                        year = item.optString("releaseDate").take(4).toIntOrNull(),
                        rating = item.optString("rating").toDoubleOrNull(),
                        description = item.optString("plot")
                    )
                )
            }
        }
        val grouped = (live + movies + series).groupBy { item ->
            M3uClassifier.classify(
                group = item.category,
                title = item.title,
                mediaType = when {
                    item.kind == CatalogKind.LIVE -> "live"
                    item.kind == CatalogKind.SERIES -> "series"
                    else -> "movie"
                },
                streamUrl = item.streamUrl
            )
        }
        return CatalogSnapshot(
            live = grouped[CatalogKind.LIVE].orEmpty(),
            movies = grouped[CatalogKind.MOVIE].orEmpty(),
            series = grouped[CatalogKind.SERIES].orEmpty(),
            kids = grouped[CatalogKind.KIDS].orEmpty(),
            anime = grouped[CatalogKind.ANIME].orEmpty(),
            adult = grouped[CatalogKind.ADULT].orEmpty()
        )
    }

        private fun loadM3u(url: String, onProgress: (CatalogLoadProgress) -> Unit, onPartial: (CatalogSnapshot) -> Unit): CatalogSnapshot {
        val connection = open(url).apply { requestMethod = "GET" }
        return try {
            val tracker = ProgressTracker(connection.contentLengthLong, onProgress)
            val stream = CountingInputStream(responseStream(connection), tracker)
            InputStreamReader(stream, Charsets.UTF_8).use { parseM3u(it, tracker, onPartial) }
                .also { tracker.finish() }
        } finally {
            connection.disconnect()
        }
    }
    private fun parseM3u(reader: InputStreamReader, tracker: ProgressTracker, onPartial: (CatalogSnapshot) -> Unit): CatalogSnapshot {

        val live = mutableListOf<CatalogItem>()
        val movies = mutableListOf<CatalogItem>()
        val series = mutableListOf<CatalogItem>()
        val kids = mutableListOf<CatalogItem>()
        val anime = mutableListOf<CatalogItem>()
        val adult = mutableListOf<CatalogItem>()
        val seriesEpisodes = LinkedHashMap<String, MutableList<SeriesEpisode>>()
        var metadata = ""
        var image = ""
        var group = ""
        var mediaType = ""
        var externalSeriesId = ""
        var index = 0
        var total = 0
        var lastPartialAt = 0
        val buffered = reader.buffered()
        while (total < MAX_M3U_ITEMS) {
            val raw = buffered.readLine() ?: break
            val line = raw.trim()
            if (line.isBlank()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                metadata = line.substringAfterLast(',').trim().ifBlank { "Conteúdo ${index + 1}" }
                image = attribute(line, "tvg-logo")
                group = attribute(line, "group-title").ifBlank { "Catálogo" }
                mediaType = attribute(line, "tvg-type")
                externalSeriesId = firstAttribute(line, "series-id", "series_id")
                continue
            }
            if (line.startsWith("#")) continue
            if (metadata.isBlank()) continue
            val kind = M3uClassifier.classify(
                group = group,
                title = metadata,
                mediaType = mediaType,
                streamUrl = line
            )
            when (kind) {
                CatalogKind.LIVE -> if (live.size < MAX_LIVE_ITEMS) {
                    live += CatalogItem("m3u-$index", metadata, group, kind, image, line)
                }
                CatalogKind.MOVIE -> if (movies.size < MAX_VOD_ITEMS) {
                    movies += CatalogItem("m3u-$index", metadata, group, kind, image, line)
                }
                CatalogKind.SERIES -> if (series.size < MAX_VOD_ITEMS) {
                    val identity = externalSeriesId.ifBlank { seriesIdentity(metadata, group) }
                    val seriesId = "m3u-series-${identity.hashCode()}"
                    if (series.none { it.id == seriesId }) {
                        series += CatalogItem(
                            id = seriesId,
                            title = seriesTitle(metadata),
                            category = group,
                            kind = CatalogKind.SERIES,
                            imageUrl = image,
                            streamUrl = ""
                        )
                    }
                    val episode = episodeFromM3u("m3u-episode-$index", metadata, image, line)
                    seriesEpisodes.getOrPut(seriesId) { mutableListOf() } += episode
                }
                CatalogKind.KIDS -> if (kids.size < MAX_VOD_ITEMS) {
                    kids += CatalogItem("m3u-$index", metadata, group, CatalogKind.KIDS, image, line)
                }
                CatalogKind.ANIME -> if (anime.size < MAX_VOD_ITEMS) {
                    anime += CatalogItem("m3u-$index", metadata, group, CatalogKind.ANIME, image, line)
                }
                CatalogKind.ADULT -> if (adult.size < MAX_VOD_ITEMS) {
                    adult += CatalogItem("m3u-$index", metadata, group, CatalogKind.ADULT, image, line)
                }
            }
            total++
            tracker.onItem(total)
            if (total >= FIRST_VISIBLE_BATCH && (total - lastPartialAt >= PARTIAL_BATCH || lastPartialAt == 0)) {
                lastPartialAt = total
                onPartial(CatalogSnapshot(
                    live = live.toList(),
                    movies = movies.toList(),
                    series = series.toList(),
                    kids = kids.toList(),
                    anime = anime.toList(),
                    adult = adult.toList(),
                    seriesEpisodes = seriesEpisodes.mapValues { it.value.toList() }
                ))
            }
            index++
            metadata = ""
            image = ""
            group = ""
            mediaType = ""
            externalSeriesId = ""
        }
        return CatalogSnapshot(
            live = live,
            movies = movies,
            series = series,
            kids = kids,
            anime = anime,
            adult = adult,
            seriesEpisodes = seriesEpisodes.mapValues { it.value.toList() }
        )
    }

    private fun categoryMap(endpoint: String, playlist: Playlist, action: String): Map<String, String> {
        val categories = LinkedHashMap<String, String>()
        streamArray(endpoint, playlist, action, MAX_CATEGORY_ITEMS) { item, _ ->
            val id = item.optString("category_id")
            val name = item.optString("category_name").ifBlank { "Categoria" }
            if (id.isNotBlank()) categories[id] = name
        }
        return categories
    }

    private fun streamArray(
        endpoint: String,
        playlist: Playlist,
        action: String,
        maxItems: Int,
        onItem: (JSONObject, Int) -> Unit
    ) {
        val url = "$endpoint?${query(playlist)}&action=${encode(action)}"
        val connection = open(url).apply { requestMethod = "GET" }
        try {
            val reader = JsonReader(InputStreamReader(responseStream(connection), Charsets.UTF_8))
            reader.use {
                it.beginArray()
                var index = 0
                while (it.hasNext()) {
                    if (index >= maxItems) {
                        it.skipValue()
                        continue
                    }
                    readObject(it)?.let { item -> onItem(item, index) }
                    index++
                }
                it.endArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readObject(reader: JsonReader): JSONObject? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        val result = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            val value = readValue(reader)
            if (value != null) result.put(name, value)
        }
        reader.endObject()
        return result
    }

    private fun readValue(reader: JsonReader): Any? = when (reader.peek()) {
        JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NULL -> { reader.nextNull(); null }
        else -> { reader.skipValue(); null }
    }

    suspend fun fetchSeriesEpisodes(playlist: Playlist, seriesId: String): List<SeriesEpisode> = withContext(Dispatchers.IO) {
        val base = playlist.url.trimEnd('/')
        val endpoint = "$base/player_api.php?${query(playlist)}&action=${encode("get_series_info")}&series_id=${encode(seriesId)}"
        val connection = open(endpoint).apply { requestMethod = "GET" }
        val episodes = mutableListOf<SeriesEpisode>()
        try {
            val reader = JsonReader(InputStreamReader(responseStream(connection), Charsets.UTF_8))
            reader.use { json ->
                json.beginObject()
                while (json.hasNext()) {
                    when (json.nextName()) {
                        "episodes" -> readEpisodesObject(json, playlist, base, episodes)
                        else -> json.skipValue()
                    }
                }
                json.endObject()
            }
        } finally {
            connection.disconnect()
        }
        episodes.take(MAX_EPISODES)
    }

    private fun readEpisodesObject(reader: JsonReader, playlist: Playlist, base: String, result: MutableList<SeriesEpisode>) {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return
        }
        reader.beginObject()
        while (reader.hasNext() && result.size < MAX_EPISODES) {
            val seasonKey = reader.nextName()
            val season = seasonKey.toIntOrNull() ?: 1
            if (reader.peek() != JsonToken.BEGIN_ARRAY) {
                reader.skipValue()
                continue
            }
            reader.beginArray()
            while (reader.hasNext() && result.size < MAX_EPISODES) {
                val episode = readEpisode(reader, playlist, base, season, result.size)
                if (episode != null) result += episode
            }
            reader.endArray()
        }
        reader.endObject()
    }

    private fun readEpisode(reader: JsonReader, playlist: Playlist, base: String, season: Int, index: Int): SeriesEpisode? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        var id = ""
        var title = "Episódio ${index + 1}"
        var episodeNumber = index + 1
        var extension = "mp4"
        var image = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id", "stream_id" -> id = nextText(reader)
                "title", "name" -> title = nextText(reader).ifBlank { title }
                "episode_num" -> episodeNumber = nextText(reader).toIntOrNull() ?: episodeNumber
                "container_extension" -> extension = nextText(reader).ifBlank { extension }
                "info" -> image = readEpisodeInfo(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (id.isBlank()) return null
        return SeriesEpisode(
            id = id,
            title = title,
            season = season,
            episode = episodeNumber,
            imageUrl = image,
            streamUrl = "$base/series/${encode(playlist.username)}/${encode(playlist.password)}/$id.$extension"
        )
    }

    private fun readEpisodeInfo(reader: JsonReader): String {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return ""
        }
        var image = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "movie_image", "cover_big", "cover" -> image = nextText(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return image
    }

    private fun nextText(reader: JsonReader): String = when (reader.peek()) {
        JsonToken.NULL -> { reader.nextNull(); "" }
        JsonToken.STRING, JsonToken.NUMBER, JsonToken.BOOLEAN -> reader.nextString()
        else -> { reader.skipValue(); "" }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/json, audio/x-mpegurl, text/plain, */*")
        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Prestigie) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
        setRequestProperty("Connection", "close")
    }

    private fun responseStream(connection: HttpURLConnection) = if (connection.responseCode in 200..299) {
        connection.inputStream
    } else {
        val code = connection.responseCode
        connection.errorStream?.close()
        error("Fonte indisponível (HTTP $code)")
    }

    private fun query(playlist: Playlist): String = "username=${encode(playlist.username)}&password=${encode(playlist.password)}"

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private class ProgressTracker(
        private val totalBytes: Long,
        private val callback: (CatalogLoadProgress) -> Unit
    ) {
        private val startedAt = System.currentTimeMillis()
        private var lastReportAt = 0L
        private var bytesRead = 0L
        private var itemsRead = 0
        private var stage = "Baixando catálogo"

        fun onBytes(count: Int) {
            if (count <= 0) return
            bytesRead += count
            report()
        }

        fun onItem(count: Int) {
            itemsRead = count
            stage = "Processando catálogo"
            report()
        }

        fun finish() {
            stage = "Preparando cache local"
            report(force = true)
        }

        private fun report(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && now - lastReportAt < 350) return
            lastReportAt = now
            val elapsed = elapsedSeconds()
            val knownSize = totalBytes > 0
            val percent = if (knownSize) {
                // Os últimos 10% ficam reservados para parsing e gravação do cache.
                ((bytesRead * 90L) / totalBytes).toInt().coerceIn(0, 90)
            } else 0
            val remaining = if (knownSize && percent > 0) {
                (elapsed * (90 - percent) / percent).coerceAtLeast(0)
            } else null
            callback(CatalogLoadProgress(percent, elapsed, remaining, estimated = !knownSize, itemsRead = itemsRead, stage = stage))
        }

        private fun elapsedSeconds(): Long =
            ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0)
    }

    private class CountingInputStream(
        input: InputStream,
        private val tracker: ProgressTracker
    ) : FilterInputStream(input) {
        override fun read(): Int {
            val value = super.read()
            if (value >= 0) tracker.onBytes(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, length)
            if (count > 0) tracker.onBytes(count)
            return count
        }
    }

    private fun attribute(line: String, name: String): String {
        val prefix = "$name=\""
        val start = line.indexOf(prefix, ignoreCase = true)
        if (start < 0) return ""
        val from = start + prefix.length
        return line.substring(from).substringBefore('"')
    }

    private fun firstAttribute(line: String, vararg names: String): String =
        names.asSequence().map { attribute(line, it) }.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun seriesIdentity(title: String, group: String): String {
        val cleaned = title
            .replace(SEASON_EPISODE_PATTERN, "")
            .replace(YEAR_PATTERN, "")
            .replace(BRACKET_PATTERN, "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return (cleaned.ifBlank { group }).lowercase()
    }

    private fun seriesTitle(title: String): String {
        val cleaned = title
            .replace(SEASON_EPISODE_PATTERN, "")
            .replace(YEAR_PATTERN, "")
            .replace(BRACKET_PATTERN, "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '|', '·')
        return cleaned.ifBlank { "Série" }
    }

    private fun episodeFromM3u(id: String, title: String, image: String, streamUrl: String): SeriesEpisode {
        val match = SEASON_EPISODE_PATTERN.find(title)
        return SeriesEpisode(
            id = id,
            title = title,
            season = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1,
            episode = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 1,
            imageUrl = image,
            streamUrl = streamUrl
        )
    }

    private companion object {
        val SEASON_EPISODE_PATTERN = Regex("(?i)\\bS(\\d{1,2})\\s*E(\\d{1,3})\\b")
        val YEAR_PATTERN = Regex("\\(?\\b(?:19|20)\\d{2}\\b\\)?")
        val BRACKET_PATTERN = Regex("\\[[^]]*]|\\([^)]*\\)")
        const val MAX_CATEGORY_ITEMS = 500_000
        const val MAX_LIVE_ITEMS = 100_000
        const val MAX_VOD_ITEMS = 500_000
        const val MAX_M3U_ITEMS = 500_000
        const val MAX_EPISODES = 500
        const val FIRST_VISIBLE_BATCH = 120
        const val PARTIAL_BATCH = 240
    }
}
