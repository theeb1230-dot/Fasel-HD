package com.faselhd.restored.network

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class SafeDnsTest {
    @Test
    fun `allows answer set containing only public addresses`() {
        val public = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
        val dns = SafeDns(Dns { listOf(public) })

        assertEquals(listOf(public), dns.lookup("provider.example"))
    }

    @Test
    fun `rejects public hostname rebound to private address`() {
        val private = InetAddress.getByAddress(byteArrayOf(10, 0, 0, 7))
        val dns = SafeDns(Dns { listOf(private) })

        assertThrows(UnknownHostException::class.java) {
            dns.lookup("provider.example")
        }
    }

    @Test
    fun `rejects mixed public and private answer instead of falling back`() {
        val public = InetAddress.getByAddress(byteArrayOf(8, 8, 4, 4))
        val loopback = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val dns = SafeDns(Dns { listOf(public, loopback) })

        assertThrows(UnknownHostException::class.java) {
            dns.lookup("provider.example")
        }
    }

    @Test
    fun `rejects ipv6 unique-local answer`() {
        val ula = InetAddress.getByAddress(byteArrayOf(
            0xfd.toByte(), 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 1,
        ))
        val dns = SafeDns(Dns { listOf(ula) })

        assertThrows(UnknownHostException::class.java) {
            dns.lookup("provider.example")
        }
    }
}
