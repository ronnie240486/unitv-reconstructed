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
    val isActive: Boolean = false
)

data class UserSession(
    val accountLabel: String = "Visitante",
    val isAuthenticated: Boolean = false,
    val membershipLabel: String = "Conteúdo gratuito"
)
