package com.example.unitv

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uClassifierTest {
    @Test
    fun `categoria kids vai para kids`() {
        assertEquals(
            CatalogKind.KIDS,
            M3uClassifier.classify("Kids | Desenhos", "Aventura infantil", "movie", "http://server/movie/1.mp4")
        )
    }

    @Test
    fun `categoria anime vai para anime mesmo sendo filme`() {
        assertEquals(
            CatalogKind.ANIME,
            M3uClassifier.classify("Anime | Filmes", "Filme de anime", "movie", "http://server/movie/2.mp4")
        )
    }

    @Test
    fun `conteudo adulto nao vai para catalogo publico`() {
        assertEquals(
            CatalogKind.ADULT,
            M3uClassifier.classify("Adultos 18+", "Conteúdo restrito", "live", "http://server/live/3.m3u8")
        )
    }

    @Test
    fun `serie continua sendo serie quando nao e kids anime ou adulto`() {
        assertEquals(
            CatalogKind.SERIES,
            M3uClassifier.classify("Series | Legendadas", "Black Torch S01E07", "series", "http://server/series/4.mkv")
        )
    }

    @Test
    fun `animacao de filme vai para kids`() {
        assertEquals(
            CatalogKind.KIDS,
            M3uClassifier.classify("Filmes | Animação", "Aventura", "movie", "http://server/movie/5.mp4")
        )
    }

    @Test
    fun `serie crunchyroll vai para anime`() {
        assertEquals(
            CatalogKind.ANIME,
            M3uClassifier.classify("Series | Crunchyroll", "Temporada 1", "series", "http://server/series/6.mkv")
        )
    }

    @Test
    fun `canal 24 horas continua em canais mesmo com nome de serie`() {
        assertEquals(
            CatalogKind.LIVE,
            M3uClassifier.classify(
                "Canais 24 Horas",
                "Novela S01E01",
                "live",
                "http://server/live/24.m3u8"
            )
        )
    }

    @Test
    fun `tipo live vence padrao de episodio e categoria series`() {
        assertEquals(
            CatalogKind.LIVE,
            M3uClassifier.classify(
                "Séries 24 Horas",
                "Canal Notícias S02E03",
                "tv",
                "http://server/live/news.m3u8"
            )
        )
    }

    @Test
    fun `origem movie nao vira serie por causa do titulo`() {
        assertEquals(
            CatalogKind.MOVIE,
            M3uClassifier.classify(
                "Filmes",
                "Canal 24 Horas S01E01",
                "movie",
                "http://server/movie/7.mp4"
            )
        )
    }

    @Test
    fun `origem series nao mistura com live por causa do grupo`() {
        assertEquals(
            CatalogKind.SERIES,
            M3uClassifier.classify(
                "Canais 24 Horas",
                "Minha Série S01E01",
                "series",
                "http://server/series/8.mkv"
            )
        )
    }
}
