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
    LIVE,
    VOD,
    SPORTS,
    PROFILE,
    LISTS,
    KIDS,
    ANIME,
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
    val updateUrl: String = "",
    val updateVersion: String = ""
)

data class UserSession(
    val accountLabel: String = "Visitante",
    val isAuthenticated: Boolean = false,
    val membershipLabel: String = "Conteúdo gratuito"
)
