package com.example.unitv

/** Domínios observáveis na superfície funcional do aplicativo reconstruído. */
enum class AppSection(val label: String) {
    HOME("Início"),
    LIVE("Ao vivo"),
    VOD("Filmes e séries"),
    SPORTS("Esportes"),
    PROFILE("Perfil")
}

enum class AppScreen {
    HOME,
    HIGHLIGHTS,
    LIVE,
    PLAYER,
    SERIES_EPISODES,
    VOD,
    SPORTS,
    PROFILE,
    LISTS,
    KIDS,
    ANIME,
    ADULT,
    EXPLORE,
    NOTIFICATIONS,
    HISTORY,
    FILTERS,
    HELP,
    VOD_DETAILS,
    SEARCH,
    LOGIN,
    PURCHASE,
    COUPONS,
    SETTINGS,
    ACCOUNT_SECURITY
}

data class Channel(
    val id: String,
    val name: String,
    val category: String,
    val program: String,
    val isLive: Boolean = true
)

data class VodItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val year: Int,
    val rating: Double
)

data class Match(
    val id: String,
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val dateLabel: String,
    val status: String
)

data class Plan(
    val id: String,
    val title: String,
    val duration: String,
    val price: String,
    val highlight: String
)

data class Coupon(
    val id: String,
    val title: String,
    val description: String,
    val expires: String,
    val claimed: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = "",
    val type: String = "",
    val directM3uUrl: String = "",
    val number: Int = 1,
    val isActive: Boolean = false
)

data class DeviceAccess(
    val found: Boolean,
    val allowed: Boolean,
    val status: String,
    val app: String,
    val urlM3u8: String,
    val urlEpg: String,
    val expiration: String
)

data class BackendNotification(
    val id: Long,
    val severity: String,
    val title: String,
    val message: String,
    val status: String
)

data class ExpirationNotice(
    val showModal: Boolean = false,
    val modalKey: String = "",
    val title: String = "",
    val message: String = "",
    val daysRemaining: Int? = null
)

data class FailoverState(
    val active: Boolean = false,
    val state: String = "",
    val playlistSyncRequired: Boolean = false,
    val playlistSyncMessage: String = "",
    val transitionId: String = "",
    /** Número (1, 2, 3...) da lista que o painel colocou em uso para este aparelho; 0 = não informado. */
    val activeListNumber: Int = 0
)

/** Resposta do heartbeat: o painel pode entregar uma ordem remota junto com ela. */
data class HeartbeatResult(
    val success: Boolean = false,
    val command: RemoteCommand? = null
)

/** Resposta do aviso de falha de reprodução. */
data class PlaybackFailureResult(
    val switchApplied: Boolean = false,
    val dnsFailoverApplied: Boolean = false,
    val retryCurrentPlaylist: Boolean = false,
    /** Lista (1, 2, 3...) para a qual o painel trocou; 0 = não informado. */
    val listNumber: Int = 0,
    val transitionId: String = ""
)

data class BackendSyncSnapshot(
    val notifications: List<BackendNotification> = emptyList(),
    val expiration: ExpirationNotice = ExpirationNotice(),
    val failover: FailoverState = FailoverState()
)

enum class NotificationSource {
    PANEL,
    CATALOG
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val source: NotificationSource,
    val createdAt: Long = System.currentTimeMillis()
)

data class RemoteCommand(
    val id: Long,
    val command: String,
    val payload: String = "",
    val expiresAt: String = ""
)

data class VisualConfig(
    val appName: String = "Prestigie",
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val backgroundUrl: String = "",
    val messageTitle: String = "",
    val messageText: String = "",
    val messageImageUrl: String = "",
    val liveIconUrl: String = "",
    val moviesIconUrl: String = "",
    val seriesIconUrl: String = "",
    val serverApiUrl: String = "",
    val updateUrl: String = "",
    val updateVersion: String = ""
)

data class UserSession(
    val accountLabel: String = "Visitante",
    val isAuthenticated: Boolean = false,
    val membershipLabel: String = "Conteúdo gratuito"
)
