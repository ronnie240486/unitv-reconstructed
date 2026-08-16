package com.example.unitv

/**
 * Classificação M3U com separação estrutural por origem.
 *
 * A pasta e o tvg-type são a fonte de verdade para live, movie e series.
 * Nome e group-title só são usados como fallback quando a fonte não informa
 * a origem. Kids, Anime e Adultos são subcategorias protegidas da origem
 * correspondente, nunca uma forma de transformar canal em série.
 */
object M3uClassifier {
    fun classify(group: String, title: String, mediaType: String, streamUrl: String): CatalogKind {
        val url = streamUrl.trim().lowercase()
        val type = mediaType.trim().lowercase()
        val category = group.trim().lowercase()
        val normalizedTitle = title.trim().lowercase()
        val sourceKind = explicitSourceKind(type, url)

        return when (sourceKind) {
            CatalogKind.LIVE -> if (isAdult(category, normalizedTitle, type, url)) CatalogKind.ADULT else CatalogKind.LIVE
            CatalogKind.MOVIE -> subcategoryFor(CatalogKind.MOVIE, category, normalizedTitle, type, url)
            CatalogKind.SERIES -> subcategoryFor(CatalogKind.SERIES, category, normalizedTitle, type, url)
            null -> fallbackClassification(category, normalizedTitle, type, url)
            else -> fallbackClassification(category, normalizedTitle, type, url)
        }
    }

    /**
     * Classifica itens vindos de endpoints Xtream já conhecidos.
     * O endpoint impede a migração entre Live, Filme e Série; somente as
     * subcategorias protegidas podem mudar o destino dentro daquela origem.
     */
    fun classifyWithinSource(
        sourceKind: CatalogKind,
        group: String,
        title: String,
        mediaType: String,
        streamUrl: String
    ): CatalogKind {
        val category = group.trim().lowercase()
        val normalizedTitle = title.trim().lowercase()
        val type = mediaType.trim().lowercase()
        val url = streamUrl.trim().lowercase()
        if (isAdult(category, normalizedTitle, type, url)) return CatalogKind.ADULT
        if (sourceKind == CatalogKind.LIVE) return CatalogKind.LIVE
        return subcategoryFor(sourceKind, category, normalizedTitle, type, url)
    }

    private fun explicitSourceKind(type: String, url: String): CatalogKind? = when {
        isLivePath(url) || type in LIVE_TYPES -> CatalogKind.LIVE
        isMoviePath(url) || type in MOVIE_TYPES -> CatalogKind.MOVIE
        isSeriesPath(url) || type in SERIES_TYPES -> CatalogKind.SERIES
        else -> null
    }

    private fun subcategoryFor(
        sourceKind: CatalogKind,
        category: String,
        title: String,
        type: String,
        url: String
    ): CatalogKind {
        if (isAdult(category, title, type, url)) return CatalogKind.ADULT
        if (isKids(category, title, type)) return CatalogKind.KIDS
        if (isAnime(category, title, type)) return CatalogKind.ANIME
        return sourceKind
    }

    private fun fallbackClassification(category: String, title: String, type: String, url: String): CatalogKind = when {
        isAdult(category, title, type, url) -> CatalogKind.ADULT
        isKids(category, title, type) -> CatalogKind.KIDS
        isAnime(category, title, type) -> CatalogKind.ANIME
        EPISODE_PATTERN.containsMatchIn(title) -> CatalogKind.SERIES
        type in SERIES_TYPES -> CatalogKind.SERIES
        type in MOVIE_TYPES -> CatalogKind.MOVIE
        type in LIVE_TYPES -> CatalogKind.LIVE
        isSeriesCategory(category) -> CatalogKind.SERIES
        isMovieCategory(category) -> CatalogKind.MOVIE
        isLiveCategory(category) -> CatalogKind.LIVE
        hasExplicitTitleTag(title, SERIES_TITLE_TAGS) -> CatalogKind.SERIES
        hasExplicitTitleTag(title, MOVIE_TITLE_TAGS) -> CatalogKind.MOVIE
        else -> CatalogKind.LIVE
    }

    private fun isLivePath(url: String): Boolean =
        url.contains("/live/") || url.contains("/live_") || url.contains("/channel/") || url.contains("/channels/")

    private fun isMoviePath(url: String): Boolean =
        url.contains("/movie/") || url.contains("/movies/") || url.contains("/filme/") || url.contains("/filmes/")

    private fun isSeriesPath(url: String): Boolean =
        url.contains("/series/") || url.contains("/serie/")

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
    private val KIDS_TOKENS = setOf("kids", "infantil", "infantis", "crianças", "criancas", "desenhos", "cartoons", "children", "family", "família", "familia", "animação", "animacao", "animation", "animated")
    private val ANIME_TOKENS = setOf("anime", "animes", "japones", "japonês", "japanese", "manga", "mangá", "crunchyroll")
    private val ADULT_TOKENS = setOf("adult", "adulto", "adultos", "18+", "xxx", "porn", "porno", "pornô", "erótico", "erotico", "sex", "night", "hot", "hentai")
    private val SERIES_TITLE_TAGS = setOf("series", "série", "serie")
    private val MOVIE_TITLE_TAGS = setOf("filme", "filmes", "movie", "movies")
}
