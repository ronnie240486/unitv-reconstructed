package com.example.unitv

import android.util.JsonReader
import android.util.JsonToken
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
    suspend fun load(playlist: Playlist): CatalogSnapshot = withContext(Dispatchers.IO) {
        if (playlist.directM3uUrl.isNotBlank()) {
            runCatching { loadM3u(playlist.directM3uUrl) }
                .getOrElse { loadFromFallbackCandidates(playlist) }
        } else {
            loadFromFallbackCandidates(playlist)
        }
    }

    private fun loadFromFallbackCandidates(playlist: Playlist): CatalogSnapshot {
        for (candidate in m3uCandidates(playlist)) {
            val parsed = runCatching { loadM3u(candidate) }.getOrNull()
            if (parsed != null && parsed.total > 0) return parsed
        }
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
        return CatalogSnapshot(live = live, movies = movies, series = series)
    }

    private fun loadM3u(url: String): CatalogSnapshot {
        val connection = open(url).apply { requestMethod = "GET" }
        return try {
            val stream = responseStream(connection)
            InputStreamReader(stream, Charsets.UTF_8).use { parseM3u(it) }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseM3u(reader: InputStreamReader): CatalogSnapshot {
        val live = mutableListOf<CatalogItem>()
        val movies = mutableListOf<CatalogItem>()
        val series = mutableListOf<CatalogItem>()
        val seriesEpisodes = LinkedHashMap<String, MutableList<SeriesEpisode>>()
        var metadata = ""
        var image = ""
        var group = ""
        var mediaType = ""
        var externalSeriesId = ""
        var index = 0
        var total = 0
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
            }
            total++
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
    }
}
