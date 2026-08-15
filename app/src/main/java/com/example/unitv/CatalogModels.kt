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
    SERIES
}

data class SeriesEpisode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val imageUrl: String = "",
    val streamUrl: String = ""
)

data class CatalogSnapshot(
    val live: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val series: List<CatalogItem> = emptyList()
) {
    val total: Int
        get() = live.size + movies.size + series.size
}
