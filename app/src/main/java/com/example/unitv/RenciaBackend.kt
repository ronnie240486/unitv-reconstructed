package com.example.unitv

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RenciaBackend(private val config: ApiConfig) {
    private val baseUrl = config.baseUrl.trimEnd('/')

    init {
        require(baseUrl.startsWith("https://")) { "O backend Rencia deve usar HTTPS." }
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
        val json = getJson("/api/guim.php", mapOf("mac" to mac))
        val data = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val source = data.optJSONObject(index) ?: continue
                val url = source.optString("url").trim()
                if (url.isBlank()) continue
                add(
                    Playlist(
                        id = source.optString("id", "list-$index"),
                        name = source.optString("name").ifBlank { "Lista ${index + 1}" },
                        url = url,
                        username = source.optString("username"),
                        password = source.optString("password"),
                        type = source.optString("type"),
                        number = index + 1
                    )
                )
            }
        }.take(4)
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
            updateUrl = json.optString("apk_download_url"),
            updateVersion = json.optString("apk_version")
        )
    }

    suspend fun heartbeat(mac: String, currentContent: String? = null) = withContext(Dispatchers.IO) {
        val query = buildMap {
            put("mac", mac)
            if (!currentContent.isNullOrBlank()) put("current_content", currentContent)
        }
        getJson("/api/v5/heartbeat", query)
    }

    suspend fun listNotifications(mac: String): List<BackendNotification> = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/list-notifications", mapOf("mac" to mac))
        val array = json.optJSONArray("notifications") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val message = item.optString("message").trim()
                if (message.isBlank()) continue
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
    }

    suspend fun remoteCommands(mac: String): List<RemoteCommand> = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/remote-commands", mapOf("mac" to mac))
        val array = json.optJSONArray("commands") ?: json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val command = item.optString("command").ifBlank { item.optString("type") }
                if (command.isBlank()) continue
                add(RemoteCommand(item.optLong("id"), command, item.optString("payload"), item.optString("expires_at")))
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
        })
    }

    suspend fun reportPlaybackFailure(mac: String, activeListNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val response = postJson("/api/v5/playback-failure", JSONObject().apply {
            put("mac", mac)
            put("active_list_number", activeListNumber)
        })
        response.optBooleanFlexible("switch_applied")
    }

    private fun getJson(path: String, query: Map<String, String>): JSONObject {
        val endpoint = buildUrl(path, query)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val body = readResponse(connection)
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun postJson(path: String, body: JSONObject): JSONObject {
        val connection = (URL(buildUrl(path, emptyMap())).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            val response = readResponse(connection)
            if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("Backend indisponível (HTTP ${connection.responseCode})")
        return body
    }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val encoded = query.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, Charsets.UTF_8.name())}=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
        }
        return if (encoded.isBlank()) "$baseUrl$path" else "$baseUrl$path?$encoded"
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
