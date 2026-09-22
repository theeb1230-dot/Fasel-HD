package com.faselhd.restored.network

import org.junit.Assert.*
import org.junit.Test

class SafeHttpTest {
    @Test fun acceptsPublicHttps() = assertNotNull(SafeHttp.normalize("https://cdn.example.org/master.m3u8"))
    @Test fun rejectsJavascript() = assertNull(SafeHttp.normalize("javascript:alert(1)"))
    @Test fun rejectsCredentialsInUrl() = assertNull(SafeHttp.normalize("https://u:p@example.org/x"))
    @Test fun rejectsLoopback() = assertNull(SafeHttp.normalize("http://127.0.0.1/admin"))
    @Test fun rejectsRfc1918() = assertNull(SafeHttp.normalize("http://192.168.1.2/video"))
    @Test fun rejectsIpv4Unspecified() = assertNull(SafeHttp.normalize("http://0.0.0.0/video"))
    @Test fun rejectsCarrierGradeNat() = assertNull(SafeHttp.normalize("http://100.64.0.1/video"))
    @Test fun rejectsLinkLocal() = assertNull(SafeHttp.normalize("http://169.254.169.254/latest/meta-data"))
    @Test fun rejectsDocumentationIpv4() = assertNull(SafeHttp.normalize("http://203.0.113.10/video"))
    @Test fun rejectsIpv4Multicast() = assertNull(SafeHttp.normalize("http://224.0.0.1/video"))
    @Test fun rejectsIpv6Loopback() = assertNull(SafeHttp.normalize("http://[::1]/admin"))
    @Test fun rejectsIpv6UniqueLocal() = assertNull(SafeHttp.normalize("http://[fd00::1]/video"))
    @Test fun rejectsIpv6LinkLocal() = assertNull(SafeHttp.normalize("http://[fe80::1]/video"))
    @Test fun rejectsIpv6Documentation() = assertNull(SafeHttp.normalize("http://[2001:db8::1]/video"))
}
