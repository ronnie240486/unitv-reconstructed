package com.example.unitv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class UnitvViewModel(
    private val repository: ContentRepository = DemoContentRepository()
) : ViewModel() {
    val channels: List<Channel> = repository.channels()
    val vodItems: List<VodItem> = repository.vodItems()
    val matches: List<Match> = repository.matches()
    val plans: List<Plan> = repository.plans()
    val coupons: List<Coupon> = repository.coupons()

    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set
    var selectedSection by mutableStateOf(AppSection.HOME)
        private set
    var selectedVod by mutableStateOf<VodItem?>(null)
        private set
    var searchQuery by mutableStateOf("")
    var session by mutableStateOf(UserSession())
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    val filteredVod: List<VodItem>
        get() = if (searchQuery.isBlank()) vodItems else vodItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
        }

    fun selectSection(section: AppSection) {
        selectedSection = section
        currentScreen = when (section) {
            AppSection.HOME -> AppScreen.HOME
            AppSection.LIVE -> AppScreen.LIVE
            AppSection.VOD -> AppScreen.VOD
            AppSection.SPORTS -> AppScreen.SPORTS
            AppSection.PROFILE -> AppScreen.PROFILE
        }
    }

    fun openVod(item: VodItem) {
        selectedVod = item
        currentScreen = AppScreen.VOD_DETAILS
    }

    fun openSearch() {
        searchQuery = ""
        currentScreen = AppScreen.SEARCH
    }

    fun openLogin() {
        currentScreen = AppScreen.LOGIN
    }

    fun login(account: String) {
        session = UserSession(
            accountLabel = account.ifBlank { "Minha conta" },
            isAuthenticated = true,
            membershipLabel = "Plano demonstrativo"
        )
        notice = "Login local concluído. Conecte um AuthRepository para autenticação real."
        currentScreen = AppScreen.PROFILE
    }

    fun logout() {
        session = UserSession()
        notice = "Sessão encerrada."
        currentScreen = AppScreen.HOME
        selectedSection = AppSection.HOME
    }

    fun openAccountScreen(screen: AppScreen) {
        currentScreen = screen
    }

    fun showNotice(message: String) {
        notice = message
    }

    fun dismissNotice() {
        notice = null
    }
}
