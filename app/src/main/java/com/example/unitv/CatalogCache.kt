package com.example.unitv

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Cache local em JSONL: uma entrada por linha para não duplicar o catálogo em uma String gigante. */
data class CatalogCacheMetadata(
    val sourceIdentity: String,
    val sourceFingerprint: String
)

object CatalogCache {
    fun load(context: Context, playlistId: String): CatalogSnapshot? {
        val file = cacheFile(context, playlistId)
        if (!file.exists() || file.length() == 0L) return null
        val live = mutableListOf<CatalogItem>()
        val movies = mutableListOf<CatalogItem>()
        val series = mutableListOf<CatalogItem>()
        val kids = mutableListOf<CatalogItem>()
        val anime = mutableListOf<CatalogItem>()
        val adult = mutableListOf<CatalogItem>()
        val episodes = LinkedHashMap<String, MutableList<SeriesEpisode>>()
        return runCatching {
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    if (line.isBlank()) return@forEach
                    val json = JSONObject(line)
                    when (json.optString("record")) {
                        "item" -> {
                            val item = itemFromJson(json)
                            when (item.kind) {
                                CatalogKind.LIVE -> live += item
                                CatalogKind.MOVIE -> movies += item
                                CatalogKind.SERIES -> series += item
                                CatalogKind.KIDS -> kids += item
                                CatalogKind.ANIME -> anime += item
                                CatalogKind.ADULT -> adult += item
                            }
                        }
                        "episode" -> {
                            val seriesId = json.optString("seriesId")
                            if (seriesId.isBlank()) return@forEach
                            episodes.getOrPut(seriesId) { mutableListOf() } += SeriesEpisode(
                                id = json.optString("id"),
                                title = json.optString("title"),
                                season = json.optInt("season"),
                                episode = json.optInt("episode"),
                                imageUrl = json.optString("imageUrl"),
                                streamUrl = json.optString("streamUrl")
                            )
                        }
                    }
                }
            }
            CatalogSnapshot(
                live = live,
                movies = movies,
                series = series,
                kids = kids,
                anime = anime,
                adult = adult,
                seriesEpisodes = episodes.mapValues { it.value.toList() }
            ).takeIf { it.total > 0 }
        }.getOrNull()
    }

    fun loadMetadata(context: Context, playlistId: String): CatalogCacheMetadata? {
        val file = metadataFile(context, playlistId)
        if (!file.exists() || file.length() == 0L) return null
        return runCatching {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            CatalogCacheMetadata(
                sourceIdentity = json.optString("sourceIdentity"),
                sourceFingerprint = json.optString("sourceFingerprint")
            )
        }.getOrNull()
    }

    fun save(
        context: Context,
        playlistId: String,
        snapshot: CatalogSnapshot,
        sourceIdentity: String = "",
        sourceFingerprint: String = "",
        onProgress: (written: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val file = cacheFile(context, playlistId)
        val temporary = File(file.parentFile, "${file.name}.tmp")
        runCatching {
            temporary.bufferedWriter(Charsets.UTF_8).use { writer ->
                val totalRecords = snapshot.total + snapshot.seriesEpisodes.values.sumOf { it.size }
                var writtenRecords = 0
                fun writeItem(item: CatalogItem) {
                    val json = JSONObject()
                        .put("record", "item")
                        .put("id", item.id)
                        .put("title", item.title)
                        .put("category", item.category)
                        .put("kind", item.kind.name)
                        .put("imageUrl", item.imageUrl)
                        .put("streamUrl", item.streamUrl)
                        .put("year", item.year ?: JSONObject.NULL)
                        .put("rating", item.rating ?: JSONObject.NULL)
                        .put("description", item.description)
                    writer.appendLine(json.toString())
                    writtenRecords++
                    if (writtenRecords == totalRecords || writtenRecords % 500 == 0) onProgress(writtenRecords, totalRecords)
                }
                snapshot.live.forEach(::writeItem)
                snapshot.movies.forEach(::writeItem)
                snapshot.series.forEach(::writeItem)
                snapshot.kids.forEach(::writeItem)
                snapshot.anime.forEach(::writeItem)
                snapshot.adult.forEach(::writeItem)
                snapshot.seriesEpisodes.forEach { (seriesId, list) ->
                    list.forEach { episode ->
                        writer.appendLine(
                            JSONObject()
                                .put("record", "episode")
                                .put("seriesId", seriesId)
                                .put("id", episode.id)
                                .put("title", episode.title)
                                .put("season", episode.season)
                                .put("episode", episode.episode)
                                .put("imageUrl", episode.imageUrl)
                                .put("streamUrl", episode.streamUrl)
                                .toString()
                        )
                        writtenRecords++
                        if (writtenRecords == totalRecords || writtenRecords % 500 == 0) onProgress(writtenRecords, totalRecords)
                    }
                }
            }
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
            if (sourceIdentity.isNotBlank()) {
                val metadata = JSONObject()
                    .put("sourceIdentity", sourceIdentity)
                    .put("sourceFingerprint", sourceFingerprint)
                    .put("savedAt", System.currentTimeMillis())
                metadataFile(context, playlistId).writeText(metadata.toString(), Charsets.UTF_8)
            }
        }
    }

    private fun itemFromJson(json: JSONObject): CatalogItem = CatalogItem(
        id = json.optString("id"),
        title = json.optString("title"),
        category = json.optString("category"),
        kind = runCatching { CatalogKind.valueOf(json.optString("kind")) }.getOrDefault(CatalogKind.LIVE),
        imageUrl = json.optString("imageUrl"),
        streamUrl = json.optString("streamUrl"),
        year = json.optInt("year").takeIf { !json.isNull("year") },
        rating = json.optDouble("rating").takeIf { !json.isNull("rating") },
        description = json.optString("description")
    )

    private fun cacheFile(context: Context, playlistId: String): File {
        val safe = playlistId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "default" }
        return File(context.filesDir, "prestigie_catalog_$safe.jsonl")
    }

    private fun metadataFile(context: Context, playlistId: String): File {
        val safe = playlistId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "default" }
        return File(context.filesDir, "prestigie_catalog_$safe.meta.json")
    }
}
