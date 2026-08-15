package com.example.unitv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private val backend = if (apiConfig.useDemoData) null else RenciaBackend(apiConfig)
    private val catalogClient = CatalogClient()
    private var heartbeatJob: Job? = null
    private val seenNotificationIds = mutableSetOf<Long>()

    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set
    var selectedSection by mutableStateOf(AppSection.HOME)
        private set
    var selectedVod by mutableStateOf<VodItem?>(null)
        private set
    var selectedCatalogItem by mutableStateOf<CatalogItem?>(null)
        private set
    var selectedCategory by mutableStateOf("Home")
        private set
    var deviceId by mutableStateOf("")
        private set
    var deviceAccess by mutableStateOf<DeviceAccess?>(null)
        private set
    var visualConfig by mutableStateOf(VisualConfig())
        private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var selectedPlaylistId by mutableStateOf<String?>(null)
        private set
    var playlistsLoading by mutableStateOf(false)
        private set
    var playlistsError by mutableStateOf<String?>(null)
        private set
    var catalog by mutableStateOf(CatalogSnapshot())
        private set
    var catalogLoading by mutableStateOf(false)
        private set
    var catalogError by mutableStateOf<String?>(null)
        private set
    var accessLoading by mutableStateOf(false)
        private set
    var backendNotifications by mutableStateOf<List<BackendNotification>>(emptyList())
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

    val allCatalog: List<CatalogItem>
        get() = catalog.live + catalog.movies + catalog.series

    val filteredCatalog: List<CatalogItem>
        get() = if (searchQuery.isBlank()) allCatalog else allCatalog.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }

    val selectedPlaylist: Playlist?
        get() = playlists.firstOrNull { it.id == selectedPlaylistId }

    val macAddress: String
        get() = DeviceIdentity.toMac(deviceId)

    fun updateDeviceId(raw: String) {
        val normalized = DeviceIdentity.normalize12(raw)
        if (deviceId == normalized) return
        deviceId = normalized
        refreshBackend()
        startHeartbeat()
    }

    fun refreshPlaylists() {
        refreshBackend()
    }

    fun refreshBackend() {
        if (deviceId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            accessLoading = true
            playlistsLoading = true
            playlistsError = null
            try {
                if (backend == null) {
                    playlists = DemoPlaylistRepository().fetchByDeviceId(deviceId)
                    selectFirstPlaylistIfNeeded()
                    catalog = CatalogSnapshot(
                        live = channels.map { CatalogItem(it.id, it.name, it.category, CatalogKind.LIVE) },
                        movies = vodItems.map { CatalogItem(it.id, it.title, it.category, CatalogKind.MOVIE) },
                        series = vodItems.map { CatalogItem(it.id, it.title, "Séries", CatalogKind.SERIES) }
                    )
                } else {
                    val access = backend.checkDevice(macAddress)
                    deviceAccess = access
                    if (!access.allowed) {
                        playlists = emptyList()
                        selectedPlaylistId = null
                        playlistsError = "Acesso indisponível para este aparelho."
                        notice = "Este aparelho não está liberado para reproduzir conteúdo."
                    } else {
                        visualConfig = runCatching { backend.fetchVisualConfig(macAddress) }.getOrDefault(VisualConfig())
                        val sources = backend.fetchSources(macAddress)
                        playlists = sources.mapIndexed { index, playlist ->
                            playlist.copy(directM3uUrl = if (index == 0) access.urlM3u8 else playlist.directM3uUrl)
                        }
                        selectFirstPlaylistIfNeeded()
                        loadSelectedCatalog()
                        backend.heartbeat(macAddress)
                        syncNotificationsAndCommands()
                    }
                }
            } catch (_: Exception) {
                playlistsError = "Não foi possível validar o aparelho agora. Tente novamente."
            } finally {
                playlistsLoading = false
                accessLoading = false
            }
        }
    }

    private fun selectFirstPlaylistIfNeeded() {
        if (selectedPlaylistId !in playlists.map { it.id }) {
            selectedPlaylistId = playlists.firstOrNull()?.id
        }
        playlists = playlists.map { it.copy(isActive = it.id == selectedPlaylistId) }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        if (backend == null) return
        heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    backend.heartbeat(macAddress)
                    syncNotificationsAndCommands()
                }
                delay(60_000)
            }
        }
    }

    private suspend fun syncNotificationsAndCommands() {
        val service = backend ?: return
        val notifications = service.listNotifications(macAddress)
        backendNotifications = notifications
        notifications.filter { it.id !in seenNotificationIds }.forEach { notification ->
            seenNotificationIds += notification.id
            notice = "${notification.title}: ${notification.message}"
            runCatching { service.acknowledgeNotification(macAddress, notification.id) }
        }

        val command = service.remoteCommands(macAddress).firstOrNull() ?: return
        val result = runCatching { executeRemoteCommand(command) }
        val status = if (result.isSuccess) "executed" else "failed"
        val message = result.getOrElse { "Não foi possível concluir o comando." }
        runCatching { service.acknowledgeCommand(macAddress, command.id, status, message) }
    }

    private suspend fun executeRemoteCommand(command: RemoteCommand): String {
        return when (command.command) {
            "refresh_playlist", "switch_playlist", "sync_access" -> {
                val loaded = backend?.fetchSources(macAddress).orEmpty()
                playlists = loaded
                selectFirstPlaylistIfNeeded()
                "Lista atualizada"
            }
            "show_message" -> {
                if (command.payload.isNotBlank()) notice = command.payload
                "Mensagem exibida"
            }
            "restart_player" -> "Player reiniciado"
            "update_dns" -> "Configuração de rede recebida"
            else -> error("Comando não suportado")
        }
    }

    fun reportPlaybackFailure() {
        val activeNumber = selectedPlaylist?.number ?: 1
        val service = backend ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { service.reportPlaybackFailure(macAddress, activeNumber) }
                .onSuccess { switchApplied ->
                    if (switchApplied) refreshBackend()
                }
        }
    }

    private fun loadSelectedCatalog() {
        val playlist = selectedPlaylist ?: return
        viewModelScope.launch(Dispatchers.IO) {
            catalogLoading = true
            catalogError = null
            try {
                catalog = catalogClient.load(playlist)
                if (catalog.total == 0) catalogError = "A lista respondeu sem canais, filmes ou séries."
            } catch (_: Exception) {
                catalogError = "Não foi possível carregar o conteúdo da lista."
            } finally {
                catalogLoading = false
            }
        }
    }

    fun refreshCatalog() {
        loadSelectedCatalog()
    }

    fun selectPlaylist(playlist: Playlist) {
        selectedPlaylistId = playlist.id
        playlists = playlists.map { it.copy(isActive = it.id == playlist.id) }
        notice = "Lista selecionada: ${playlist.name}"
        loadSelectedCatalog()
    }

    fun openLists() { currentScreen = AppScreen.LISTS }

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

    fun openVod(item: VodItem) { selectedVod = item; currentScreen = AppScreen.VOD_DETAILS }

    fun openCatalogItem(item: CatalogItem) {
        selectedCatalogItem = item
        notice = if (item.streamUrl.isBlank()) "${item.title} selecionado." else "${item.title} selecionado. O player pode abrir a URL da lista autorizada."
    }

    fun selectCategory(category: String) {
        selectedCategory = category
        currentScreen = when (category) {
            "Kids" -> AppScreen.KIDS
            "Anime" -> AppScreen.ANIME
            "Explorar" -> AppScreen.EXPLORE
            "Ao vivo" -> AppScreen.LIVE
            "Filmes", "Séries" -> AppScreen.VOD
            "Home", "Destaques" -> AppScreen.HOME
            else -> AppScreen.HOME
        }
    }

    fun openSearch() { searchQuery = ""; currentScreen = AppScreen.SEARCH }
    fun openLogin() { currentScreen = AppScreen.LOGIN }

    fun login(account: String) {
        session = UserSession(account.ifBlank { "Minha conta" }, true, "Plano demonstrativo")
        notice = "Login local concluído."
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
    fun backHome() { currentScreen = AppScreen.HOME; selectedSection = AppSection.HOME }
    fun openAccountScreen(screen: AppScreen) { currentScreen = screen }
    fun showNotice(message: String) { notice = message }
    fun dismissNotice() { notice = null }

    override fun onCleared() {
        heartbeatJob?.cancel()
        super.onCleared()
    }
}
