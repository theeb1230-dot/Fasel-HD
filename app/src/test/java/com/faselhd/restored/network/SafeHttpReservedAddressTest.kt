package com.faselhd.restored.network

import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeHttpReservedAddressTest {
    @Test
    fun rejectsIpv4MappedPrivateAddress() {
        assertFalse(SafeHttp.isAllowedResolvedAddress(InetAddress.getByName("::ffff:192.168.1.20")))
    }

    @Test
    fun rejectsIpv4DocumentationAndBenchmarkRanges() {
        assertFalse(SafeHttp.isAllowedResolvedAddress(InetAddress.getByName("198.18.0.1")))
        assertFalse(SafeHttp.isAllowedResolvedAddress(InetAddress.getByName("192.0.2.10")))
        assertFalse(SafeHttp.isAllowedResolvedAddress(InetAddress.getByName("192.88.99.10")))
    }

    @Test
    fun allowsOrdinaryPublicIpv4Address() {
        assertTrue(SafeHttp.isAllowedResolvedAddress(InetAddress.getByName("1.1.1.1")))
    }
}
