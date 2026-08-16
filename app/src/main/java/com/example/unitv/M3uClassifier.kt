package com.example.unitv

/** Classificação M3U por origem, metadados e categoria, com proteção para conteúdo adulto. */
object M3uClassifier {
    fun classify(group: String, title: String, mediaType: String, streamUrl: String): CatalogKind {
        val url = streamUrl.lowercase()
        val type = mediaType.trim().lowercase()
        val category = group.trim().lowercase()
        val normalizedTitle = title.trim().lowercase()
        return when {
            isAdult(category, normalizedTitle, type, url) -> CatalogKind.ADULT
            isKids(category, normalizedTitle, type) -> CatalogKind.KIDS
            isAnime(category, normalizedTitle, type) -> CatalogKind.ANIME
            url.contains("/series/") || url.contains("/serie/") -> CatalogKind.SERIES
            url.contains("/movie/") || url.contains("/movies/") || url.contains("/filme/") -> CatalogKind.MOVIE
            url.contains("/live/") || url.contains("/live_") -> CatalogKind.LIVE
            EPISODE_PATTERN.containsMatchIn(normalizedTitle) -> CatalogKind.SERIES
            type in SERIES_TYPES -> CatalogKind.SERIES
            type in MOVIE_TYPES -> CatalogKind.MOVIE
            type in LIVE_TYPES -> CatalogKind.LIVE
            isSeriesCategory(category) -> CatalogKind.SERIES
            isMovieCategory(category) -> CatalogKind.MOVIE
            isLiveCategory(category) -> CatalogKind.LIVE
            hasExplicitTitleTag(normalizedTitle, SERIES_TITLE_TAGS) -> CatalogKind.SERIES
            hasExplicitTitleTag(normalizedTitle, MOVIE_TITLE_TAGS) -> CatalogKind.MOVIE
            else -> CatalogKind.LIVE
        }
    }

    private fun isAdult(category: String, title: String, type: String, url: String): Boolean {
        val value = "$category $title $type $url"
        return ADULT_TOKENS.any { token -> Regex("(^|[^a-z0-9])${Regex.escape(token)}([^a-z0-9]|$)").containsMatchIn(value) }
    }

    private fun isKids(category: String, title: String, type: String): Boolean {
        val value = "$category $title $type"
        return KIDS_TOKENS.any { token -> value.contains(token) }
    }

    private fun isAnime(category: String, title: String, type: String): Boolean {
        val value = "$category $title $type"
        return ANIME_TOKENS.any { token -> value.contains(token) }
    }

    private fun isSeriesCategory(value: String): Boolean = containsCategoryToken(value, SERIES_CATEGORY_TOKENS)
    private fun isMovieCategory(value: String): Boolean = containsCategoryToken(value, MOVIE_CATEGORY_TOKENS)
    private fun isLiveCategory(value: String): Boolean = containsCategoryToken(value, LIVE_CATEGORY_TOKENS)

    private fun containsCategoryToken(value: String, tokens: Set<String>): Boolean {
        if (value.isBlank()) return false
        val parts = value.split(Regex("[|>/\\\\:_\\-]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        return parts.any { part ->
            part in tokens || tokens.any { token -> part.startsWith("$token ") || part.endsWith(" $token") }
        }
    }

    private fun hasExplicitTitleTag(value: String, tags: Set<String>): Boolean =
        tags.any { tag -> value.startsWith("[$tag]") || value.startsWith("$tag:") || value.startsWith("$tag -") }

    private val EPISODE_PATTERN = Regex("(?i)\\bS\\d{1,2}\\s*E\\d{1,3}\\b|\\bT\\d{1,2}\\s*E\\d{1,3}\\b")
    private val SERIES_TYPES = setOf("series", "serie", "tv_series", "show", "episodios", "episode")
    private val MOVIE_TYPES = setOf("movie", "movies", "filme", "filmes", "vod")
    private val LIVE_TYPES = setOf("live", "channel", "channels", "tv", "live_tv", "live_channel")
    private val SERIES_CATEGORY_TOKENS = setOf("series", "série", "séries", "serie", "seriados", "temporadas", "temporada", "shows", "episódios", "episodios")
    private val MOVIE_CATEGORY_TOKENS = setOf("filme", "filmes", "movie", "movies", "vod", "cinema", "longas")
    private val LIVE_CATEGORY_TOKENS = setOf("canais", "canal", "channels", "channel", "tv", "tv ao vivo", "ao vivo", "live", "live tv")
    private val KIDS_TOKENS = setOf("kids", "infantil", "infantis", "crianças", "criancas", "desenhos", "cartoons", "children", "family", "família", "familia")
    private val ANIME_TOKENS = setOf("anime", "animes", "japones", "japonês", "japanese", "manga", "mangá")
    private val ADULT_TOKENS = setOf("adult", "adulto", "adultos", "18+", "xxx", "porn", "porno", "pornô", "erótico", "erotico", "sex", "night", "hot", "hentai")
    private val SERIES_TITLE_TAGS = setOf("series", "série", "serie")
    private val MOVIE_TITLE_TAGS = setOf("filme", "filmes", "movie", "movies")
}
