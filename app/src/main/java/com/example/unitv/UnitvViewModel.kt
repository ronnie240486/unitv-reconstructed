package com.example.unitv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnitvViewModel(
    private val repository: ContentRepository = DemoContentRepository(),
    private val apiConfig: ApiConfig = ProductConfig.api
) : ViewModel() {
    val channels: List<Channel> = repository.channels()
    val vodItems: List<VodItem> = repository.vodItems()
    val matches: List<Match> = repository.matches()
    val plans: List<Plan> = repository.plans()
    val coupons: List<Coupon> = repository.coupons()

    private val playlistRepository: PlaylistRepository = if (apiConfig.playlistsUrl.isBlank()) {
        DemoPlaylistRepository()
    } else {
        HttpPlaylistRepository(apiConfig)
    }

    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set
    var selectedSection by mutableStateOf(AppSection.HOME)
        private set
    var selectedVod by mutableStateOf<VodItem?>(null)
        private set
    var selectedCategory by mutableStateOf("Destaques")
        private set
    var deviceId by mutableStateOf("")
        private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var selectedPlaylistId by mutableStateOf<String?>(null)
        private set
    var playlistsLoading by mutableStateOf(false)
        private set
    var playlistsError by mutableStateOf<String?>(null)
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

    val selectedPlaylist: Playlist?
        get() = playlists.firstOrNull { it.id == selectedPlaylistId }

    fun updateDeviceId(raw: String) {
        val normalized = DeviceIdentity.normalize12(raw)
        if (deviceId == normalized) return
        deviceId = normalized
        refreshPlaylists()
    }

    fun refreshPlaylists() {
        if (deviceId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            playlistsLoading = true
            playlistsError = null
            runCatching { playlistRepository.fetchByDeviceId(deviceId).take(4) }
                .onSuccess { loaded ->
                    playlists = loaded
                    if (selectedPlaylistId !in loaded.map { it.id }) {
                        selectedPlaylistId = loaded.firstOrNull()?.id
                    }
                }
                .onFailure { error ->
                    playlistsError = error.message ?: "Não foi possível carregar as listas."
                }
            playlistsLoading = false
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        selectedPlaylistId = playlist.id
        playlists = playlists.map { it.copy(isActive = it.id == playlist.id) }
        notice = "Lista selecionada: ${playlist.name}"
    }

    fun openLists() {
        currentScreen = AppScreen.LISTS
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

    fun selectCategory(category: String) {
        selectedCategory = category
        currentScreen = when (category) {
            "Kids" -> AppScreen.KIDS
            "Anime" -> AppScreen.ANIME
            "Explorar" -> AppScreen.EXPLORE
            "Ao vivo" -> AppScreen.LIVE
            "Filmes", "Séries", "Home", "Destaques" -> AppScreen.HOME
            else -> AppScreen.HOME
        }
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

    fun openNotifications() { currentScreen = AppScreen.NOTIFICATIONS }
    fun openHistory() { currentScreen = AppScreen.HISTORY }
    fun openFilters() { currentScreen = AppScreen.FILTERS }
    fun openHelp() { currentScreen = AppScreen.HELP }

    fun backHome() {
        currentScreen = AppScreen.HOME
        selectedSection = AppSection.HOME
    }

    fun openAccountScreen(screen: AppScreen) { currentScreen = screen }
    fun showNotice(message: String) { notice = message }
    fun dismissNotice() { notice = null }
}
