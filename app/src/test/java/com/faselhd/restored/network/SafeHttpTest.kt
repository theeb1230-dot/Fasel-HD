package com.faselhd.restored.network

import org.junit.Assert.*
import org.junit.Test

class SafeHttpTest {
    @Test fun acceptsPublicHttps() = assertNotNull(SafeHttp.normalize("https://cdn.example.org/master.m3u8"))
    @Test fun rejectsJavascript() = assertNull(SafeHttp.normalize("javascript:alert(1)"))
    @Test fun rejectsCredentialsInUrl() = assertNull(SafeHttp.normalize("https://u:p@example.org/x"))
    @Test fun rejectsLoopback() = assertNull(SafeHttp.normalize("http://127.0.0.1/admin"))
    @Test fun rejectsRfc1918() = assertNull(SafeHttp.normalize("http://192.168.1.2/video"))
}
