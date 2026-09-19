package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeHttp
import org.junit.Assert.*
import org.junit.Test

class ProviderTransportPolicyTest {
    @Test fun rejectsPrivateAndScriptEndpointsBeforeTransport() {
        assertFalse(SafeHttp.isAllowed("http://127.0.0.1/api"))
        assertFalse(SafeHttp.isAllowed("http://192.168.1.2/api"))
        assertFalse(SafeHttp.isAllowed("javascript:alert(1)"))
        assertFalse(SafeHttp.isAllowed("file:///etc/passwd"))
    }

    @Test fun acceptsPublicHttpsEndpointShape() {
        assertEquals("https://example.org/api/catalog?page=2", SafeHttp.normalize("https://example.org/api/catalog?page=2"))
    }
}
