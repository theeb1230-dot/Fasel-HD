package com.faselhd.restored.network

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class SafeDnsTest {
    private fun dnsOf(vararg addresses: InetAddress): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = addresses.toList()
    }

    private fun assertUnsafeDns(block: () -> Unit) {
        try {
            block()
            fail("Expected unsafe DNS answer to be rejected")
        } catch (_: UnknownHostException) {
            // Expected fail-closed behavior.
        }
    }

    @Test
    fun `allows answer set containing only public addresses`() {
        val public = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
        val dns = SafeDns(dnsOf(public))

        assertEquals(listOf(public), dns.lookup("provider.example"))
    }

    @Test
    fun `rejects public hostname rebound to private address`() {
        val private = InetAddress.getByAddress(byteArrayOf(10, 0, 0, 7))
        val dns = SafeDns(dnsOf(private))

        assertUnsafeDns { dns.lookup("provider.example") }
    }

    @Test
    fun `rejects mixed public and private answer instead of falling back`() {
        val public = InetAddress.getByAddress(byteArrayOf(8, 8, 4, 4))
        val loopback = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val dns = SafeDns(dnsOf(public, loopback))

        assertUnsafeDns { dns.lookup("provider.example") }
    }

    @Test
    fun `rejects ipv6 unique-local answer`() {
        val ula = InetAddress.getByAddress(byteArrayOf(
            0xfd.toByte(), 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 1,
        ))
        val dns = SafeDns(dnsOf(ula))

        assertUnsafeDns { dns.lookup("provider.example") }
    }
}
