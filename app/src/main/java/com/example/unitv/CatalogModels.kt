package com.example.unitv

data class CatalogItem(
    val id: String,
    val title: String,
    val category: String,
    val kind: CatalogKind,
    val imageUrl: String = "",
    val streamUrl: String = "",
    val year: Int? = null,
    val rating: Double? = null,
    val description: String = ""
)

enum class CatalogKind {
    LIVE,
    MOVIE,
    SERIES,
    KIDS,
    ANIME,
    ADULT
}

data class SeriesEpisode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val imageUrl: String = "",
    val streamUrl: String = ""
)

data class CatalogLoadProgress(
    val percent: Int = 0,
    val elapsedSeconds: Long = 0,
    val remainingSeconds: Long? = null,
    val estimated: Boolean = false
)

data class CatalogSnapshot(
    val live: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val series: List<CatalogItem> = emptyList(),
    val kids: List<CatalogItem> = emptyList(),
    val anime: List<CatalogItem> = emptyList(),
    val adult: List<CatalogItem> = emptyList(),
    val seriesEpisodes: Map<String, List<SeriesEpisode>> = emptyMap()
) {
    val total: Int
        get() = live.size + movies.size + series.size + kids.size + anime.size + adult.size
}
