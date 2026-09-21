package com.faselhd.restored.provider

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.flow.PlaybackNavigator
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.playback.PlaybackPipeline
import com.faselhd.restored.ui.PlayerActivity
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderRuntimeSmokeTest {
    @Test
    fun authorizedProviderTransportReachesRealMedia3Playback() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"runtime-series","title":"Runtime Series"}]}"""
        val provider = ConfiguredContentProvider(
            pageLoader = ProviderPageLoader(ProviderTransport(client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("owned fixture").body(json.toResponseBody()).build()
            }.build())),
            catalogUrl = { type, page -> "https://example.org/catalog/${type.name.lowercase()}?page=$page" },
            searchUrl = { query, type, page -> "https://example.org/search/${type.name.lowercase()}?q=$query&page=$page" },
            detailsLoader = { id, type -> MediaDetails(MediaSummary(id, "Runtime Series", type)) },
            sourcesLoader = { _, _ ->
                listOf(PlaybackClassifier.classify("https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"))
            }
        )
        val item = provider.search("runtime", MediaType.SERIES, 1).items.single()
        assertEquals(MediaType.SERIES, item.type)
        assertEquals(MediaType.SERIES, provider.details(item.id, item.type).media.type)
        val source = ProviderGateway(provider).sources(item.id, "e1").single()
        val decision = PlaybackPipeline.prepare(source)
        assertTrue(decision is PlaybackDecision.Native)

        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = PlaybackNavigator.intent(context, decision)
            ?: fail("Native provider decision must produce an internal PlayerActivity intent")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<PlayerActivity>(intent).use { scenario ->
            val deadline = SystemClock.uptimeMillis() + 20_000
            var ready = false
            var advanced = false
            while (SystemClock.uptimeMillis() < deadline && !(ready && advanced)) {
                scenario.onActivity { activity ->
                    val player = activity.findViewById<PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player
                    if (player != null) {
                        ready = ready || player.playbackState == Player.STATE_READY
                        advanced = advanced || player.currentPosition >= 250L
                    }
                }
                if (!(ready && advanced)) SystemClock.sleep(200)
            }
            assertTrue("provider-selected Media3 source must reach STATE_READY", ready)
            assertTrue("provider-selected Media3 source must advance currentPosition >=250ms", advanced)
        }
    }
}
