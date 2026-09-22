package com.faselhd.restored.network

import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

object SafeHttp {
    fun isAllowed(raw: String): Boolean = normalize(raw) != null

    fun normalize(raw: String): String? {
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (uri.userInfo != null || uri.host.isNullOrBlank()) return null
        val host = uri.host.lowercase()
        if (host == "localhost" || host.endsWith(".localhost") || isBlockedLiteralAddress(host)) return null
        return uri.normalize().toASCIIString()
    }

    private fun isBlockedLiteralAddress(host: String): Boolean {
        val literal = parseLiteralAddress(host) ?: return false
        return literal.isAnyLocalAddress ||
            literal.isLoopbackAddress ||
            literal.isLinkLocalAddress ||
            literal.isSiteLocalAddress ||
            literal.isMulticastAddress ||
            isIpv4Reserved(literal.address) ||
            isIpv6Reserved(literal)
    }

    private fun parseLiteralAddress(host: String): InetAddress? {
        val ipv4 = host.split('.')
        if (ipv4.size == 4 && ipv4.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
            return runCatching { InetAddress.getByAddress(ipv4.map { it.toInt().toByte() }.toByteArray()) }.getOrNull()
        }
        if (!host.contains(':')) return null
        return runCatching { InetAddress.getByName(host) }.getOrNull()
    }

    private fun isIpv4Reserved(bytes: ByteArray): Boolean {
        if (bytes.size != 4) return false
        val a = bytes[0].toInt() and 0xff
        val b = bytes[1].toInt() and 0xff
        val c = bytes[2].toInt() and 0xff
        return a == 0 ||
            (a == 100 && b in 64..127) ||
            (a == 192 && b == 0 && c == 0) ||
            (a == 192 && b == 0 && c == 2) ||
            (a == 198 && b in 18..19) ||
            (a == 198 && b == 51 && c == 100) ||
            (a == 203 && b == 0 && c == 113) ||
            a >= 224
    }

    private fun isIpv6Reserved(address: InetAddress): Boolean {
        if (address !is Inet6Address) return false
        val bytes = address.address
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        val uniqueLocal = (first and 0xfe) == 0xfc
        val documentation = first == 0x20 && second == 0x01 &&
            (bytes[2].toInt() and 0xff) == 0x0d && (bytes[3].toInt() and 0xff) == 0xb8
        return uniqueLocal || documentation
    }
}
