package com.example.unitv

interface ContentRepository {
    fun channels(): List<Channel>
    fun vodItems(): List<VodItem>
    fun matches(): List<Match>
    fun plans(): List<Plan>
    fun coupons(): List<Coupon>
}

/** Dados locais usados somente quando ProductConfig.api.useDemoData = true. */
class DemoContentRepository : ContentRepository {
    override fun channels() = listOf(
        Channel("news", "Notícias 24h", "Notícias", "Boletim da manhã"),
        Channel("cine", "Cinema Livre", "Filmes", "Sessão de destaque"),
        Channel("sports", "Arena Sports", "Esportes", "Pré-jogo"),
        Channel("kids", "Kids Club", "Infantil", "Aventuras da tarde"),
        Channel("music", "Music Box", "Música", "Clipes selecionados")
    )

    override fun vodItems() = listOf(
        VodItem("v1", "Horizonte de Inverno", "Drama · 2024", "Em destaque", 2024, 8.6),
        VodItem("v2", "Código da Aurora", "Ação · 2023", "Mais assistidos", 2023, 8.1),
        VodItem("v3", "Pequenos Exploradores", "Infantil · 2024", "Família", 2024, 7.9),
        VodItem("v4", "Além do Horizonte", "Série · 2022", "Séries", 2022, 8.8),
        VodItem("v5", "A Última Estação", "Suspense · 2021", "Recomendados", 2021, 8.3)
    )

    override fun matches() = listOf(
        Match("m1", "Liga Nacional", "Aurora FC", "Atlético Central", "Hoje · 20:30", "Em breve"),
        Match("m2", "Copa Continental", "Real Norte", "União Sul", "Amanhã · 16:00", "Agendado"),
        Match("m3", "Liga Nacional", "Porto Azul", "Estrela Vermelha", "Sáb · 18:45", "Agendado")
    )

    override fun plans() = listOf(
        Plan("p1", "Plano mensal", "30 dias", "R$ 29,90", "Acesso completo"),
        Plan("p2", "Plano trimestral", "90 dias", "R$ 74,90", "Economize 17%"),
        Plan("p3", "Plano anual", "365 dias", "R$ 249,90", "Melhor valor")
    )

    override fun coupons() = listOf(
        Coupon("c1", "Boas-vindas", "10% de desconto no próximo plano", "Válido por 7 dias"),
        Coupon("c2", "Fim de semana", "R$ 5 de desconto em planos selecionados", "Válido até domingo"),
        Coupon("c3", "Indique um amigo", "Benefício liberado após o primeiro acesso", "Disponível", claimed = true)
    )
}

interface PlaylistRepository {
    suspend fun fetchByDeviceId(deviceId: String): List<Playlist>
}

class DemoPlaylistRepository : PlaylistRepository {
    override suspend fun fetchByDeviceId(deviceId: String): List<Playlist> = listOf(
        Playlist("list-1", "Lista principal", "https://backend.example.invalid/playlists/$deviceId/1", type = "demo", number = 1),
        Playlist("list-2", "Filmes e séries", "https://backend.example.invalid/playlists/$deviceId/2", type = "demo", number = 2),
        Playlist("list-3", "Kids", "https://backend.example.invalid/playlists/$deviceId/3", type = "demo", number = 3),
        Playlist("list-4", "Esportes", "https://backend.example.invalid/playlists/$deviceId/4", type = "demo", number = 4)
    )
}

/** Configuração da integração definida pelo guia do backend Rencia. */
data class ApiConfig(
    val baseUrl: String = "https://renciaapp.manus.space",
    val updateUrl: String = "",
    val appVersion: String = "0.2.0-prestigie",
    val deviceType: String = "prestigie",
    val useDemoData: Boolean = false
)

/** Até cinco servidores DNS podem ser configurados pelo produto que integrar este scaffold. */
data class DnsConfig(
    val servers: List<String> = emptyList()
) {
    init {
        require(servers.size <= 5) { "DnsConfig aceita no máximo cinco servidores." }
    }
}

interface PlayerGateway {
    fun play(streamUrl: String)
    fun stop()
}

class NoOpPlayerGateway : PlayerGateway {
    override fun play(streamUrl: String) = Unit
    override fun stop() = Unit
}
