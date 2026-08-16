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
}
