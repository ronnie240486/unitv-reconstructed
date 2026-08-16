package com.example.unitv

import java.security.MessageDigest

object PlaylistSourceIdentity {
    fun identity(playlist: Playlist): String = listOf(
        playlist.id,
        playlist.url.trim(),
        playlist.directM3uUrl.trim(),
        playlist.username,
        playlist.password,
        playlist.type.trim().lowercase()
    ).joinToString("|")

    fun fingerprint(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
