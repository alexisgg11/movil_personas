package com.example.personas.domain

import java.net.URI

object ServidorUrl {
    fun validar(texto: String, debug: Boolean): String? {
        val uri = try { URI(texto.trim()) } catch (_: Exception) { return "Ingresa una URL válida." }
        if (uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null ||
            (uri.path != "" && uri.path != "/") || uri.port < -1 || uri.port == 0 || uri.port > 65535) {
            return "Usa solo el origen, por ejemplo http://127.0.0.1:8081."
        }
        if (uri.scheme == "https") return null
        if (debug && uri.scheme == "http" && local(uri.host)) return null
        return if (debug) "HTTP solo está permitido para un servidor local; usa HTTPS para otros servidores." else "La versión release requiere un servidor HTTPS."
    }
    private fun local(host: String): Boolean {
        if (host == "localhost" || host == "[::1]") return true
        val parts = host.split('.').map { it.toIntOrNull() ?: return false }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        return parts[0] == 127 || parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) || (parts[0] == 172 && parts[1] in 16..31)
    }
}
