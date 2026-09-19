package com.faselhd.restored.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.faselhd.restored.R
import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.*
import com.faselhd.restored.flow.ContentFlow
import com.faselhd.restored.flow.PlaybackNavigator
import com.faselhd.restored.provider.ContentProvider
import com.faselhd.restored.provider.ProviderGateway
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var flow: ContentFlow
    private lateinit var results: ListView
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var loading: ProgressBar
    private lateinit var query: EditText
    private lateinit var play: Button
    private var items: List<MediaSummary> = emptyList()
    private var selected: MediaSummary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        flow = ContentFlow(ProviderGateway(DemoProvider()))
        results = findViewById(R.id.resultsList)
        status = findViewById(R.id.statusText)
        details = findViewById(R.id.detailsText)
        loading = findViewById(R.id.loading)
        query = findViewById(R.id.searchQuery)
        play = findViewById(R.id.playButton)
        findViewById<Button>(R.id.catalogButton).setOnClickListener { loadCatalog() }
        findViewById<Button>(R.id.searchButton).setOnClickListener { search() }
        results.setOnItemClickListener { _, _, position, _ -> loadDetails(items[position]) }
        play.setOnClickListener { selected?.let(::playItem) }
        loadCatalog()
    }

    private fun loadCatalog() = runUi("Loading catalog") { flow.catalog(MediaType.SERIES).items }
    private fun search() = runUi("Searching") { flow.search(query.text.toString()).items }

    private fun runUi(message: String, block: suspend () -> List<MediaSummary>) {
        loading.visibility = View.VISIBLE
        status.text = message
        lifecycleScope.launch {
            runCatching { block() }.onSuccess { found ->
                items = found
                results.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, found.map { it.title })
                status.text = if (found.isEmpty()) "No results" else "${found.size} result(s)"
            }.onFailure { status.text = "Unable to load content" }
            loading.visibility = View.GONE
        }
    }

    private fun loadDetails(item: MediaSummary) {
        selected = item
        play.isEnabled = false
        lifecycleScope.launch {
            runCatching { flow.details(item) }.onSuccess { value ->
                val episodeText = value.episodes.joinToString { "S${it.seasonNumber}E${it.episodeNumber}" }
                details.text = listOfNotNull(value.media.title, value.overview, episodeText.takeIf(String::isNotBlank)).joinToString("\n")
                play.isEnabled = true
            }.onFailure { details.text = "Unable to load details" }
        }
    }

    private fun playItem(item: MediaSummary) {
        lifecycleScope.launch {
            when (val decision = flow.play(item)) {
                is com.faselhd.restored.playback.PlaybackDecision.Native -> PlaybackNavigator.intent(this@MainActivity, decision)?.let(::startActivity)
                is com.faselhd.restored.playback.PlaybackDecision.ResolverRequired -> status.text = "Source requires safe internal resolution"
                is com.faselhd.restored.playback.PlaybackDecision.Rejected -> status.text = "No playable source"
            }
        }
    }

    /** Deterministic clean-room provider; live transport remains a separate P0 acceptance criterion. */
    private class DemoProvider : ContentProvider {
        private val sample = MediaSummary("demo-series", "Recovered Series", MediaType.SERIES)
        override suspend fun catalog(type: MediaType, page: Int) = Page(listOf(sample.copy(type = type)), page, false)
        override suspend fun search(query: String, page: Int) = Page(if (query.isBlank()) emptyList() else listOf(sample.copy(title = "Result: ${query.trim()}")), page, false)
        override suspend fun details(id: String, type: MediaType) = MediaDetails(
            sample.copy(id = id, type = type), overview = "Deterministic clean-room recovery fixture",
            seasons = listOf(1), episodes = listOf(Episode("demo-episode", 1, 1, "Episode 1"))
        )
        override suspend fun sources(mediaId: String, episodeId: String?): List<PlaybackSource> =
            listOf(PlaybackClassifier.classify("https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"))
    }
}
