package com.example.unitv

/**
 * Configuração do produto. Preencha playlistsUrl com o endpoint autorizado do seu backend.
 * Enquanto estiver vazio, o APK usa as quatro playlists demonstrativas locais.
 */
object ProductConfig {
    val api: ApiConfig = ApiConfig(
        playlistsUrl = ""
    )
}
