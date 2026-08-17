package com.example.unitv

import android.content.Context
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
    private val syncMutex = Mutex()
    private var heartbeatJob: Job? = null
    private var catalogJob: Job? = null
    private var episodesJob: Job? = null
    private var catalogClockJob: Job? = null
    private val seenNotificationIds = mutableSetOf<Long>()
    private var appContext: Context? = null

    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set
    var selectedSection by mutableStateOf(AppSection.HOME)
        private set
    var selectedVod by mutableStateOf<VodItem?>(null)
        private set
    var selectedCatalogItem by mutableStateOf<CatalogItem?>(null)
        private set
    var playingUrl by mutableStateOf("")
        private set
    var playingTitle by mutableStateOf("")
        private set
    var seriesEpisodes by mutableStateOf<List<SeriesEpisode>>(emptyList())
        private set
    var episodesLoading by mutableStateOf(false)
        private set
    var episodesError by mutableStateOf<String?>(null)
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
    var catalogProgress by mutableStateOf(CatalogLoadProgress())
        private set
    var catalogReady by mutableStateOf(false)
        private set
    var catalogError by mutableStateOf<String?>(null)
        private set
    var accessLoading by mutableStateOf(false)
        private set
    var backendNotifications by mutableStateOf<List<BackendNotification>>(emptyList())
        private set
    var appNotifications by mutableStateOf<List<AppNotification>>(emptyList())
        private set
    private var notificationsLoaded = false
    private var lastLoadedPlaylistId: String? = null
    private val maxCatalogNotifications = 24
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

    /** Catálogo público: Adultos nunca entra nesta coleção. */
    val allCatalog: List<CatalogItem>
        get() = catalog.live + catalog.movies + catalog.series + catalog.kids + catalog.anime

    val adultCatalog: List<CatalogItem>
        get() = catalog.adult

    var parentalUnlocked by mutableStateOf(false)
        private set
    var parentalPinRequested by mutableStateOf(false)
        private set
    private var pendingAdultItem: CatalogItem? = null

    val filteredCatalog: List<CatalogItem>
        get() = if (searchQuery.isBlank()) allCatalog else allCatalog.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }

    val selectedPlaylist: Playlist?
        get() = playlists.firstOrNull { it.id == selectedPlaylistId }

    val macAddress: String
        get() = DeviceIdentity.toMac(deviceId)

    fun attachContext(context: Context) {
        appContext = context.applicationContext
        if (!notificationsLoaded) {
            appNotifications = loadPersistedNotifications(context.applicationContext)
            notificationsLoaded = true
        }
    }

    fun updateDeviceId(raw: String) {
        val normalized = DeviceIdentity.normalize12(raw)
        if (deviceId == normalized) return
        deviceId = normalized
        if (backend == null) refreshBackend() else startHeartbeat()
    }

    fun refreshPlaylists() {
        refreshBackend()
    }

    fun refreshBackend() {
        if (deviceId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { refreshBackendOnce() }
    }

    private suspend fun refreshBackendOnce() {
        syncMutex.withLock {
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
                    catalogError = null
                    catalogReady = true
                } else {
                    val access = backend.checkDevice(macAddress)
                    deviceAccess = access
                    accessLoading = false
                    if (!access.allowed) {
                        if (catalog.total == 0) {
                            playlists = emptyList()
                            selectedPlaylistId = null
                            catalogReady = false
                        }
                        playlistsError = "Acesso indisponível para este aparelho."
                    } else {
                        val service = backend ?: return@withLock
                        visualConfig = runCatching { service.fetchVisualConfig(macAddress) }.getOrDefault(visualConfig)
                        val panelSources = service.fetchSources(macAddress)
                        val sources = if (panelSources.isNotEmpty()) {
                            panelSources
                        } else if (access.urlM3u8.isNotBlank()) {
                            listOf(
                                Playlist(
                                    id = "device-access-m3u",
                                    name = "Lista principal",
                                    url = access.urlM3u8,
                                    directM3uUrl = access.urlM3u8,
                                    number = 1
                                )
                            )
                        } else {
                            emptyList()
                        }
                        if (sources.isNotEmpty()) {
                            playlists = sources.mapIndexed { index, playlist ->
                                playlist.copy(directM3uUrl = if (index == 0 && access.urlM3u8.isNotBlank()) access.urlM3u8 else playlist.directM3uUrl)
                            }
                            selectFirstPlaylistIfNeeded()
                            loadSelectedCatalogNow()
                        } else if (catalog.total == 0) {
                            playlistsError = "Aparelho liberado, aguardando listas do painel…"
                            catalogReady = false
                            catalogLoading = false
                        }
                        service.heartbeat(macAddress)
                        syncNotificationsAndCommands()
                    }
                }
            } catch (_: Exception) {
                if (catalog.total == 0) {
                    catalogReady = false
                    playlistsError = "Aguardando resposta do painel e da lista…"
                }
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
                refreshBackendOnce()
                val ready = catalogReady && catalog.total > 0 && catalogError == null
                delay(if (ready) 60_000 else 5_000)
            }
        }
    }

    private suspend fun syncNotificationsAndCommands() {
        val service = backend ?: return
        val notifications = service.listNotifications(macAddress)
        backendNotifications = notifications
        notifications.filter { it.id !in seenNotificationIds }.forEach { notification ->
            seenNotificationIds += notification.id
            val appNotification = AppNotification(
                id = "panel-${notification.id}",
                title = notification.title.ifBlank { "Aviso do painel" },
                message = notification.message,
                source = NotificationSource.PANEL
            )
            val wasNew = addAppNotification(appNotification)
            if (wasNew) notice = "${appNotification.title}: ${appNotification.message}"
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
                if (command.payload.isNotBlank()) {
                    val appNotification = AppNotification(
                        id = "command-${command.id}",
                        title = "Aviso do painel",
                        message = command.payload,
                        source = NotificationSource.PANEL
                    )
                    addAppNotification(appNotification)
                    notice = command.payload
                }
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
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch(Dispatchers.IO) {
            loadSelectedCatalogNow()
        }
    }

    private suspend fun loadSelectedCatalogNow() {
        val playlist = selectedPlaylist ?: return
        val sourceIdentity = PlaylistSourceIdentity.identity(playlist)
        val fallbackFingerprint = PlaylistSourceIdentity.fingerprint(sourceIdentity)
        val cached = appContext?.let { CatalogCache.load(it, playlist.id) }
        val metadata = appContext?.let { CatalogCache.loadMetadata(it, playlist.id) }
        var observedFingerprint = ""
        val cacheIsCurrent = if (cached?.hasCoreContent == true && metadata?.sourceIdentity == sourceIdentity) {
            observedFingerprint = runCatching { catalogClient.sourceFingerprint(playlist) }.getOrElse { fallbackFingerprint }
            metadata.sourceFingerprint.isNotBlank() && metadata.sourceFingerprint == observedFingerprint
        } else {
            false
        }
        if (cacheIsCurrent && cached != null) {
            catalog = cached
            lastLoadedPlaylistId = playlist.id
            selectedCatalogItem?.takeIf { it.kind == CatalogKind.SERIES }?.let { openSeries ->
                seriesEpisodes = cached.seriesEpisodes[openSeries.id].orEmpty()
                episodesLoading = false
                episodesError = if (seriesEpisodes.isEmpty()) "Nenhum episódio disponível no cache." else null
            }
            catalogReady = true
            catalogError = null
            catalogProgress = CatalogLoadProgress(100, 0, 0, estimated = false, stage = "Catálogo pronto")
            catalogLoading = false
            return
        }
        catalogReady = false
        catalogLoading = true
        catalogError = null
        catalogProgress = CatalogLoadProgress(stage = "Carregando sua lista")
        startCatalogClock()
        val previousCatalog = when {
            cached?.hasCoreContent == true -> cached
            lastLoadedPlaylistId == playlist.id && catalog.hasCoreContent -> catalog
            else -> null
        }
        try {
            val loaded = catalogClient.load(
                playlist,
                onProgress = { progress -> catalogProgress = progress },
                onInitial = { initial ->
                    if (initial.hasCoreContent) {
                        catalog = initial
                        selectedCatalogItem?.takeIf { it.kind == CatalogKind.SERIES }?.let { openSeries ->
                            seriesEpisodes = initial.seriesEpisodes[openSeries.id].orEmpty()
                            if (seriesEpisodes.isNotEmpty()) {
                                episodesLoading = false
                                episodesError = null
                            } else if (openSeries.id.startsWith("m3u-series-")) {
                                episodesLoading = true
                                episodesError = null
                            }
                        }
                        // Snapshot parcial apenas atualiza o catálogo em memória. A TV permanece bloqueada
                        // até o parser terminar canais, filmes e séries e o cache ser salvo.
                        catalogReady = false
                        catalogLoading = true
                        catalogError = null
                        catalogProgress = catalogProgress.copy(
                            stage = "Carregando sua lista",
                            itemsRead = initial.total
                        )
                    }
                }
            )
            if (loaded.hasCoreContent) {
                val newItems = if (previousCatalog != null) newCatalogItems(previousCatalog, loaded) else emptyList()
                catalog = loaded
                lastLoadedPlaylistId = playlist.id
                if (newItems.isNotEmpty()) {
                    val added = newItems.take(maxCatalogNotifications)
                    added.forEach { item ->
                        addAppNotification(
                            AppNotification(
                                id = "catalog-${PlaylistSourceIdentity.fingerprint(item.kind.name + "|" + item.title + "|" + item.streamUrl)}",
                                title = "Novo conteúdo em ${item.category.ifBlank { item.kind.name.lowercase() }}",
                                message = item.title,
                                source = NotificationSource.CATALOG
                            )
                        )
                    }
                    notice = "${newItems.size} novidade(s) adicionada(s) às Notificações."
                }
                selectedCatalogItem?.takeIf { it.kind == CatalogKind.SERIES }?.let { openSeries ->
                    seriesEpisodes = loaded.seriesEpisodes[openSeries.id].orEmpty()
                    episodesLoading = false
                    episodesError = if (seriesEpisodes.isEmpty()) "Nenhum episódio disponível para esta série." else null
                }
                catalogError = null
                catalogReady = true
                catalogProgress = catalogProgress.copy(
                    percent = 91,
                    stage = "Carregando sua lista",
                    itemsRead = loaded.total,
                    estimated = false
                )
                if (observedFingerprint.isBlank()) {
                    observedFingerprint = runCatching { catalogClient.sourceFingerprint(playlist) }.getOrElse { fallbackFingerprint }
                }
                appContext?.let { context ->
                    CatalogCache.save(
                        context,
                        playlist.id,
                        loaded,
                        sourceIdentity = sourceIdentity,
                        sourceFingerprint = observedFingerprint
                    ) { written, total ->
                        catalogProgress = catalogProgress.copy(
                            percent = if (total > 0) (91 + (written * 8 / total)).coerceIn(91, 99) else 91,
                            stage = "Carregando sua lista",
                            itemsRead = written,
                            estimated = false
                        )
                    }
                }
                catalogProgress = catalogProgress.copy(
                    percent = 100,
                    stage = "Catálogo pronto",
                    itemsRead = loaded.total,
                    remainingSeconds = 0,
                    estimated = false
                )
                catalogLoading = false
            } else if (cached == null || !cached.hasCoreContent) {
                catalogError = "A lista ainda não entregou canais, filmes e séries completos."
            }
        } catch (_: Exception) {
            if (cached?.hasCoreContent == true) {
                catalog = cached
                catalogError = null
                catalogReady = true
            } else {
                catalogError = "Não foi possível carregar o conteúdo da lista."
            }
        } finally {
            stopCatalogClock()
            catalogReady = catalog.hasCoreContent && catalogError == null
            catalogLoading = !catalogReady
        }
    }

    private fun startCatalogClock() {
        catalogClockJob?.cancel()
        val startedAt = System.currentTimeMillis()
        catalogClockJob = viewModelScope.launch {
            while (isActive && catalogLoading) {
                val elapsed = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0)
                catalogProgress = catalogProgress.copy(elapsedSeconds = maxOf(catalogProgress.elapsedSeconds, elapsed))
                delay(1_000)
            }
        }
    }

    private fun stopCatalogClock() {
        catalogClockJob?.cancel()
        catalogClockJob = null
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

    fun requestAdultAccess(item: CatalogItem? = null) {
        pendingAdultItem = item
        parentalPinRequested = true
    }

    fun cancelParentalPin() {
        pendingAdultItem = null
        parentalPinRequested = false
    }

    fun verifyParentalPin(input: String): Boolean {
        val expected = appContext?.getSharedPreferences("prestigie_parental", Context.MODE_PRIVATE)
            ?.getString("pin", "1234") ?: "1234"
        val valid = input == expected
        if (valid) {
            parentalUnlocked = true
            parentalPinRequested = false
            val pending = pendingAdultItem
            pendingAdultItem = null
            if (pending != null) openCatalogItemUnlocked(pending) else currentScreen = AppScreen.ADULT
        }
        return valid
    }

    fun changeParentalPin(input: String): Boolean {
        val pin = input.filter(Char::isDigit).take(8)
        if (pin.length < 4) return false
        appContext?.getSharedPreferences("prestigie_parental", Context.MODE_PRIVATE)
            ?.edit()?.putString("pin", pin)?.apply()
        parentalUnlocked = true
        return true
    }

    fun lockParentalContent() {
        parentalUnlocked = false
        backHome()
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

    fun openVod(item: VodItem) { selectedVod = item; currentScreen = AppScreen.VOD_DETAILS }

    fun openCatalogItem(item: CatalogItem) {
        if (item.kind == CatalogKind.ADULT && !parentalUnlocked) {
            requestAdultAccess(item)
            return
        }
        openCatalogItemUnlocked(item)
    }

    private fun openCatalogItemUnlocked(item: CatalogItem) {
        selectedCatalogItem = item
        if (item.kind == CatalogKind.SERIES) {
            val localEpisodes = catalog.seriesEpisodes[item.id].orEmpty()
            seriesEpisodes = localEpisodes
            episodesError = null
            currentScreen = AppScreen.SERIES_EPISODES
            if (localEpisodes.isNotEmpty()) {
                episodesLoading = false
                return
            }
            if (item.id.startsWith("m3u-series-")) {
                if (catalogLoading) {
                    episodesLoading = true
                    episodesError = null
                } else {
                    episodesLoading = false
                    episodesError = "Esta série não possui episódios disponíveis na fonte M3U."
                }
            } else if (selectedPlaylist?.username?.isNotBlank() == true && item.id.isNotBlank()) {
                loadSeriesEpisodes(item)
            } else {
                episodesLoading = false
                episodesError = "Esta série não possui episódios disponíveis na fonte."
            }
            return
        }
        if (item.streamUrl.isNotBlank()) {
            openPlayer(item.title, item.streamUrl)
        } else {
            showNotice("Este conteúdo não possui uma URL de reprodução válida na lista.")
        }
    }

    private fun loadSeriesEpisodes(item: CatalogItem) {
        val playlist = selectedPlaylist ?: return
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch(Dispatchers.IO) {
            episodesLoading = true
            episodesError = null
            try {
                val loaded = catalogClient.fetchSeriesEpisodes(playlist, item.id)
                seriesEpisodes = loaded
                if (loaded.isEmpty()) episodesError = "Nenhum episódio foi retornado para esta série."
            } catch (_: Exception) {
                episodesError = "Não foi possível carregar os episódios desta série."
            } finally {
                episodesLoading = false
            }
        }
    }

    fun openEpisode(episode: SeriesEpisode) {
        if (episode.streamUrl.isBlank()) {
            showNotice("Este episódio não possui uma URL de reprodução válida.")
        } else {
            openPlayer(episode.title, episode.streamUrl)
        }
    }

    private fun openPlayer(title: String, url: String) {
        playingTitle = title
        playingUrl = url
        currentScreen = AppScreen.PLAYER
    }

    fun closePlayer() {
        currentScreen = when {
            selectedCatalogItem?.kind == CatalogKind.SERIES -> AppScreen.SERIES_EPISODES
            selectedSection == AppSection.LIVE -> AppScreen.LIVE
            else -> AppScreen.HOME
        }
    }

    fun selectCategory(category: String) {
        selectedCategory = category
        if (category == "Ao vivo" || category == "Canais") selectedSection = AppSection.LIVE
        currentScreen = when (category) {
            "Kids" -> AppScreen.KIDS
            "Anime" -> AppScreen.ANIME
            "Explorar" -> AppScreen.EXPLORE
            "Ao vivo", "Canais" -> AppScreen.LIVE
            "Filmes", "Séries" -> AppScreen.VOD
            "Destaques" -> AppScreen.HIGHLIGHTS
            "Home" -> AppScreen.HOME
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

    private fun allCatalogItems(snapshot: CatalogSnapshot): List<CatalogItem> =
        snapshot.live + snapshot.movies + snapshot.series + snapshot.kids + snapshot.anime + snapshot.adult

    private fun catalogItemKey(item: CatalogItem): String =
        PlaylistSourceIdentity.fingerprint(item.kind.name + "|" + item.title + "|" + item.streamUrl)

    private fun newCatalogItems(previous: CatalogSnapshot, updated: CatalogSnapshot): List<CatalogItem> {
        val known = allCatalogItems(previous).mapTo(mutableSetOf(), ::catalogItemKey)
        return allCatalogItems(updated).filter { catalogItemKey(it) !in known }
    }

    private fun loadPersistedNotifications(context: Context): List<AppNotification> {
        val file = File(context.filesDir, "prestigie_notifications.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText(Charsets.UTF_8))
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val source = runCatching { NotificationSource.valueOf(json.optString("source")) }
                        .getOrDefault(NotificationSource.PANEL)
                    add(
                        AppNotification(
                            id = json.optString("id"),
                            title = json.optString("title"),
                            message = json.optString("message"),
                            source = source,
                            createdAt = json.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }.filter { it.id.isNotBlank() && it.message.isNotBlank() }.take(100)
        }.getOrDefault(emptyList())
    }

    private fun persistNotifications() {
        val context = appContext ?: return
        runCatching {
            val array = JSONArray()
            appNotifications.take(100).forEach { item ->
                array.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("title", item.title)
                        .put("message", item.message)
                        .put("source", item.source.name)
                        .put("createdAt", item.createdAt)
                )
            }
            val target = File(context.filesDir, "prestigie_notifications.json")
            val temporary = File(context.filesDir, "prestigie_notifications.json.tmp")
            temporary.writeText(array.toString(), Charsets.UTF_8)
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        }
    }

    private fun addAppNotification(notification: AppNotification): Boolean {
        if (notification.id.isBlank() || appNotifications.any { it.id == notification.id }) return false
        appNotifications = (listOf(notification) + appNotifications).take(100)
        persistNotifications()
        return true
    }

    fun openNotifications() { currentScreen = AppScreen.NOTIFICATIONS }
    fun openHistory() { currentScreen = AppScreen.HISTORY }
    fun openFilters() { currentScreen = AppScreen.FILTERS }
    fun openHelp() { currentScreen = AppScreen.HELP }
    fun backHome() { currentScreen = AppScreen.HOME; selectedSection = AppSection.HOME }
    fun openAccountScreen(screen: AppScreen) {
        if (screen == AppScreen.ADULT && !parentalUnlocked) {
            requestAdultAccess()
        } else {
            currentScreen = screen
        }
    }
    fun showNotice(message: String) { notice = message }
    fun dismissNotice() { notice = null }

    override fun onCleared() {
        heartbeatJob?.cancel()
        catalogJob?.cancel()
        episodesJob?.cancel()
        catalogClockJob?.cancel()
        super.onCleared()
    }
}
