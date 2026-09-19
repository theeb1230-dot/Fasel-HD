package com.faselhd.restored.network

import java.net.URI

object SafeHttp {
    fun isAllowed(raw: String): Boolean = normalize(raw) != null

    fun normalize(raw: String): String? {
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (uri.userInfo != null || uri.host.isNullOrBlank()) return null
        val host = uri.host.lowercase()
        if (host == "localhost" || host.endsWith(".localhost") || isPrivateIpv4(host)) return null
        return uri.normalize().toASCIIString()
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val p = host.split('.').mapNotNull { it.toIntOrNull() }
        if (p.size != 4 || p.any { it !in 0..255 }) return false
        return p[0] == 10 || p[0] == 127 ||
            (p[0] == 169 && p[1] == 254) ||
            (p[0] == 172 && p[1] in 16..31) ||
            (p[0] == 192 && p[1] == 168)
    }
}
