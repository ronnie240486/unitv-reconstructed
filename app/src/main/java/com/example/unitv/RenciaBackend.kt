package com.example.unitv

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Sentinela que limpa o "Assistindo" no painel (omitir current_content mantém o último conteúdo). */
const val HEARTBEAT_IDLE_SENTINEL = "__idle__"

/** Erro HTTP do painel. 4xx é uma resposta de verdade (não adianta tentar o endereço reserva). */
class PanelHttpException(val code: Int) : Exception("Backend indisponível (HTTP $code)")

class RenciaBackend(private val config: ApiConfig) {
    /** Endereço principal (Railway) primeiro; o antigo (Manus) só como reserva. */
    private val bases: List<String> = listOf(config.baseUrl, config.fallbackBaseUrl)
        .map { it.trim().trimEnd('/') }
        .filter { it.isNotBlank() }
        .distinct()

    @Volatile
    private var sendAppId = config.appId.isNotBlank()

    init {
        require(bases.isNotEmpty() && bases.all { it.startsWith("https://") }) { "O backend Rencia deve usar HTTPS." }
    }

    suspend fun checkDevice(mac: String): DeviceAccess = withContext(Dispatchers.IO) {
        val json = getJson("/api/device/check", mapOf("mac" to mac))
        DeviceAccess(
            found = json.optBooleanFlexible("found"),
            allowed = json.optBooleanFlexible("allowed"),
            status = json.optString("status"),
            app = json.optString("app"),
            urlM3u8 = json.optString("urlM3u8"),
            urlEpg = json.optString("urlEpg"),
            expiration = json.optString("dataExpiracao")
        )
    }

    suspend fun fetchSources(mac: String): List<Playlist> = withContext(Dispatchers.IO) {
        val paths = listOf("/api/guim.php", "/api/v5/guim.php", "/api/v4/guim.php")
        for (path in paths) {
            val json = runCatching { getJson(path, mapOf("mac" to mac)) }.getOrNull() ?: continue
            val data = json.optJSONArray("data") ?: continue
            val sources = buildList {
                for (index in 0 until data.length()) {
                    val source = data.optJSONObject(index) ?: continue
                    val url = source.optString("url").ifBlank { source.optString("playlist_url") }.trim()
                    if (url.isBlank()) continue
                    add(
                        Playlist(
                            id = source.optString("id", "list-$index"),
                            name = source.optString("name").ifBlank { source.optString("playlist_name") }.ifBlank { "Lista ${index + 1}" },
                            url = url,
                            username = source.optString("username"),
                            password = source.optString("password"),
                            type = source.optString("type"),
                            number = index + 1
                        )
                    )
                }
            }.take(4)
            if (sources.isNotEmpty()) return@withContext sources
        }
        emptyList()
    }

    suspend fun fetchVisualConfig(mac: String): VisualConfig = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/ultra-config", mapOf("mac" to mac))
        val icons = json.optJSONObject("icons") ?: JSONObject()
        VisualConfig(
            appName = json.optString("app_name", "Prestigie"),
            logoUrl = json.optString("logo_url").ifBlank { json.optString("ultra_logo_url") },
            bannerUrl = json.optString("banner_url").ifBlank { json.optString("ultra_banner_url") },
            backgroundUrl = json.optString("background_url").ifBlank { json.optString("ultra_background_url") },
            messageTitle = json.optString("message_title"),
            messageText = json.optString("message_text"),
            messageImageUrl = json.optString("message_image_url"),
            liveIconUrl = icons.optString("live_tv"),
            moviesIconUrl = icons.optString("movies"),
            seriesIconUrl = icons.optString("series"),
            serverApiUrl = json.optString("server_api_url"),
            updateUrl = json.optString("apk_download_url"),
            updateVersion = json.optString("apk_version")
        )
    }

    /**
     * Heartbeat: [currentContent] vazio manda a sentinela __idle__ (limpa o "Assistindo").
     * A resposta pode trazer uma ordem remota já reservada para este aparelho.
     */
    suspend fun heartbeat(mac: String, currentContent: String? = null): HeartbeatResult = withContext(Dispatchers.IO) {
        val content = currentContent?.trim().orEmpty().ifBlank { HEARTBEAT_IDLE_SENTINEL }
        val json = getJsonWithApp("/api/v5/heartbeat", mapOf("mac" to mac, "current_content" to content))
        HeartbeatResult(
            success = json.optBooleanFlexible("success"),
            command = parseCommand(json.optJSONObject("command"))
        )
    }

    suspend fun listNotifications(mac: String): BackendSyncSnapshot = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/list-notifications", mapOf("mac" to mac))
        val array = json.optJSONArray("notifications") ?: JSONArray()
        val notifications = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val message = item.optString("message").trim()
                if (message.isBlank()) continue
                if (item.optBooleanFlexible("acknowledged")) continue
                add(
                    BackendNotification(
                        id = item.optLong("id"),
                        severity = item.optString("severity", "info"),
                        title = item.optString("title", "Aviso"),
                        message = message,
                        status = item.optString("status")
                    )
                )
            }
        }
        val expiration = json.optJSONObject("expiration")?.let { item ->
            ExpirationNotice(
                showModal = item.optBooleanFlexible("show_modal"),
                modalKey = item.optString("modal_key"),
                title = item.optString("modal_title"),
                message = item.optString("modal_message"),
                daysRemaining = item.optIntFlexible("days_remaining")
            )
        } ?: ExpirationNotice()
        val failover = FailoverState(
            active = json.optBooleanFlexible("failover_active"),
            state = json.optString("failover_state"),
            playlistSyncRequired = json.optBooleanFlexible("playlist_sync_required"),
            playlistSyncMessage = json.optString("playlist_sync_message"),
            transitionId = json.optString("failover_transition_id"),
            activeListNumber = json.optIntFlexible("active_list_number") ?: 0
        )
        BackendSyncSnapshot(notifications, expiration, failover)
    }

    /** O painel entrega UMA ordem por vez, no campo "command" (objeto), não em lista. */
    suspend fun remoteCommands(mac: String): List<RemoteCommand> = withContext(Dispatchers.IO) {
        val json = getJsonWithApp("/api/v5/remote-commands", mapOf("mac" to mac))
        val single = parseCommand(json.optJSONObject("command"))
        if (single != null) return@withContext listOf(single)
        val array = json.optJSONArray("commands") ?: json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                parseCommand(array.optJSONObject(index))?.let { add(it) }
            }
        }
    }

    suspend fun acknowledgeNotification(mac: String, alertId: Long) = withContext(Dispatchers.IO) {
        postJson("/api/v5/list-notifications/ack", JSONObject().apply {
            put("mac", mac)
            put("alert_id", alertId)
        })
    }

    suspend fun acknowledgeCommand(mac: String, commandId: Long, status: String, resultMessage: String) = withContext(Dispatchers.IO) {
        postJson("/api/v5/remote-commands/ack", JSONObject().apply {
            put("mac", mac)
            put("command_id", commandId)
            put("status", status)
            put("result_message", resultMessage)
            if (sendAppId) put("app", config.appId)
        })
    }

    suspend fun reportPlaybackFailure(mac: String, activeListNumber: Int): PlaybackFailureResult = withContext(Dispatchers.IO) {
        val response = postJson("/api/v5/playback-failure", JSONObject().apply {
            put("mac", mac)
            put("active_list_number", activeListNumber)
        })
        PlaybackFailureResult(
            switchApplied = response.optBooleanFlexible("switch_applied"),
            dnsFailoverApplied = response.optBooleanFlexible("dns_failover_applied"),
            retryCurrentPlaylist = response.optBooleanFlexible("retry_current_playlist"),
            listNumber = response.optIntFlexible("list_index")
                ?: response.optIntFlexible("next_playlist_number")
                ?: 0,
            transitionId = response.optString("failover_transition_id")
        )
    }

    // ------------------------------------------------------------------ HTTP (Railway primeiro, Manus de reserva)

    private class PanelResponse(val code: Int, val body: String)

    private fun parseCommand(item: JSONObject?): RemoteCommand? {
        if (item == null) return null
        val id = item.optLong("id")
        val type = item.optString("type").ifBlank { item.optString("command") }
        if (id <= 0L || type.isBlank()) return null
        val payload = item.opt("payload")
        val payloadText = when (payload) {
            null, JSONObject.NULL -> ""
            is JSONObject -> payload.toString()
            else -> payload.toString()
        }
        return RemoteCommand(
            id = id,
            command = type,
            payload = payloadText,
            expiresAt = item.optString("expires_at").ifBlank { item.optString("expiresAt") }
        )
    }

    /** GET que manda app_id; se o painel disser que o MAC não é deste app (403), repete sem app_id. */
    private fun getJsonWithApp(path: String, query: Map<String, String>): JSONObject {
        if (sendAppId) {
            val response = execute("GET", path, query + ("app_id" to config.appId), null)
            if (response.code != 403) return toJson(response)
            sendAppId = false
        }
        return toJson(execute("GET", path, query, null))
    }

    private fun getJson(path: String, query: Map<String, String>): JSONObject =
        toJson(execute("GET", path, query, null))

    private fun postJson(path: String, body: JSONObject): JSONObject =
        toJson(execute("POST", path, emptyMap(), body))

    private fun toJson(response: PanelResponse): JSONObject {
        if (response.code !in 200..299) throw PanelHttpException(response.code)
        return if (response.body.isBlank()) JSONObject() else JSONObject(response.body)
    }

    /** Tenta cada endereço; só cai para o próximo em erro de rede, 5xx ou resposta que não é JSON do painel. */
    private fun execute(method: String, path: String, query: Map<String, String>, body: JSONObject?): PanelResponse {
        var last: Exception? = null
        for (base in bases) {
            try {
                val response = singleRequest(base, method, path, query, body)
                if (response.code >= 500) {
                    last = PanelHttpException(response.code)
                    continue
                }
                if (response.code in 200..299 && response.body.isNotBlank() && !response.body.trimStart().startsWith("{")) {
                    last = IllegalStateException("Resposta inválida do painel")
                    continue
                }
                return response
            } catch (error: Exception) {
                last = error
            }
        }
        throw last ?: IllegalStateException("Painel indisponível")
    }

    private fun singleRequest(base: String, method: String, path: String, query: Map<String, String>, body: JSONObject?): PanelResponse {
        val connection = (URL(buildUrl(base, path, query)).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 10000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            PanelResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(base: String, path: String, query: Map<String, String>): String {
        val encoded = query.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, Charsets.UTF_8.name())}=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
        }
        return if (encoded.isBlank()) "$base$path" else "$base$path?$encoded"
    }
}

private fun JSONObject.optIntFlexible(key: String): Int? {
    val value = opt(key)
    return when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
}

private fun JSONObject.optBooleanFlexible(key: String): Boolean {
    val value = opt(key)
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1" || value.equals("yes", ignoreCase = true)
        else -> false
    }
}
