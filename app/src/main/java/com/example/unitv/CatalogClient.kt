package com.example.unitv

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CatalogClient {
    suspend fun load(playlist: Playlist): CatalogSnapshot = withContext(Dispatchers.IO) {
        if (playlist.directM3uUrl.isNotBlank()) {
            runCatching { loadM3u(playlist.directM3uUrl) }.getOrElse { loadXtream(playlist) }
        } else {
            runCatching { loadXtream(playlist) }.getOrElse { loadM3u(playlist.url) }
        }
    }

    private fun loadXtream(playlist: Playlist): CatalogSnapshot {
        require(playlist.username.isNotBlank() && playlist.password.isNotBlank())
        val base = playlist.url.trimEnd('/')
        val liveCategories = categoryMap(getJsonArray("$base/player_api.php", playlist, "get_live_categories"))
        val vodCategories = categoryMap(getJsonArray("$base/player_api.php", playlist, "get_vod_categories"))
        val seriesCategories = categoryMap(getJsonArray("$base/player_api.php", playlist, "get_series_categories"))

        val live = buildList {
            val streams = getJsonArray("$base/player_api.php", playlist, "get_live_streams")
            for (index in 0 until streams.length()) {
                val item = streams.optJSONObject(index) ?: continue
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
            val streams = getJsonArray("$base/player_api.php", playlist, "get_vod_streams")
            for (index in 0 until streams.length()) {
                val item = streams.optJSONObject(index) ?: continue
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
            val items = getJsonArray("$base/player_api.php", playlist, "get_series")
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
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
        val text = try { read(connection) } finally { connection.disconnect() }
        return parseM3u(text)
    }

    private fun parseM3u(text: String): CatalogSnapshot {
        val live = mutableListOf<CatalogItem>()
        val movies = mutableListOf<CatalogItem>()
        val series = mutableListOf<CatalogItem>()
        var metadata = ""
        var image = ""
        var group = ""
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        for (index in lines.indices) {
            val line = lines[index]
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                metadata = line.substringAfterLast(',').trim().ifBlank { "Conteúdo ${index + 1}" }
                image = attribute(line, "tvg-logo")
                group = attribute(line, "group-title").ifBlank { "Catálogo" }
                continue
            }
            if (line.startsWith("#")) continue
            if (metadata.isBlank()) continue
            val lower = "$group $metadata".lowercase()
            val kind = when {
                listOf("series", "série", "temporada", "season").any { lower.contains(it) } -> CatalogKind.SERIES
                listOf("filme", "filmes", "movie", "vod", "cinema").any { lower.contains(it) } -> CatalogKind.MOVIE
                else -> CatalogKind.LIVE
            }
            val item = CatalogItem("m3u-$index", metadata, group, kind, image, line)
            when (kind) {
                CatalogKind.LIVE -> live += item
                CatalogKind.MOVIE -> movies += item
                CatalogKind.SERIES -> series += item
            }
            metadata = ""
            image = ""
            group = ""
        }
        return CatalogSnapshot(live, movies, series)
    }

    private fun categoryMap(array: JSONArray): Map<String, String> = buildMap {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            put(item.optString("category_id"), item.optString("category_name").ifBlank { "Categoria" })
        }
    }

    private fun getJsonArray(endpoint: String, playlist: Playlist, action: String): JSONArray {
        val url = "$endpoint?${query(playlist)}&action=${encode(action)}"
        val connection = open(url).apply { requestMethod = "GET" }
        val body = try { read(connection) } finally { connection.disconnect() }
        return JSONArray(body)
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/json, audio/x-mpegurl, text/plain, */*")
        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Prestigie) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
        setRequestProperty("Connection", "close")
    }

    private fun read(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("Fonte indisponível (HTTP ${connection.responseCode})")
        return body
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
}
