package com.faselhd.restored.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * DNS boundary for provider traffic. Every answer must be publicly routable according to
 * SafeHttp policy. Rejecting the entire answer set avoids fallback to a private/link-local
 * address when a hostname returns mixed or rebound DNS records.
 */
class SafeDns(
    private val delegate: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { !SafeHttp.isAllowedResolvedAddress(it) }) {
            throw UnknownHostException("unsafe_dns_answer")
        }
        return addresses
    }
}
