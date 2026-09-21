package com.faselhd.restored.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.faselhd.restored.R
import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.*
import com.faselhd.restored.flow.ContentFlow
import com.faselhd.restored.flow.PaginatedContentLoader
import com.faselhd.restored.flow.PlaybackNavigator
import com.faselhd.restored.provider.ContentProvider
import com.faselhd.restored.provider.ProviderGateway
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var flow: ContentFlow
    private lateinit var results: ListView
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var loading: ProgressBar
    private lateinit var query: EditText
    private lateinit var play: Button
    private lateinit var loadMore: Button
    private lateinit var retry: Button
    private var items: List<MediaSummary> = emptyList()
    private var selected: MediaSummary? = null
    private var discoveryJob: Job? = null
    private var pager: PaginatedContentLoader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        flow = ContentFlow(ProviderGateway(DemoProvider()))
        results = findViewById(R.id.resultsList)
        status = findViewById(R.id.statusText)
        details = findViewById(R.id.detailsText)
        loading = findViewById(R.id.loading)
        query = findViewById(R.id.searchQuery)
        play = findViewById(R.id.playButton)
        loadMore = findViewById(R.id.loadMoreButton)
        retry = findViewById(R.id.retryButton)
        findViewById<Button>(R.id.catalogButton).setOnClickListener { loadCatalog() }
        findViewById<Button>(R.id.searchButton).setOnClickListener { search() }
        loadMore.setOnClickListener { requestPage { pager?.loadMore() } }
        retry.setOnClickListener { requestPage { pager?.retry() } }
        results.setOnItemClickListener { _, _, position, _ -> loadDetails(items[position]) }
        play.setOnClickListener { selected?.let(::playItem) }
        loadCatalog()
    }

    /** Android 15 enforces edge-to-edge for targetSdk 35. Keep bottom controls out of system navigation/taskbar hit targets. */
    private fun applySystemBarInsets() {
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    private fun loadCatalog() {
        pager = PaginatedContentLoader { page -> flow.catalog(MediaType.SERIES, page) }
        requestPage { pager?.refresh() }
    }

    private fun search() {
        val term = query.text.toString().trim()
        pager = PaginatedContentLoader { page -> flow.search(term, page) }
        requestPage { pager?.refresh() }
    }

    private fun requestPage(block: suspend () -> PaginatedContentLoader.State?) {
        discoveryJob?.cancel()
        discoveryJob = lifecycleScope.launch {
            render(PaginatedContentLoader.State.Loading(items))
            block()?.let(::render)
        }
    }

    private fun render(state: PaginatedContentLoader.State) {
        loading.visibility = if (state is PaginatedContentLoader.State.Loading) View.VISIBLE else View.GONE
        retry.visibility = View.GONE
        loadMore.visibility = View.GONE
        when (state) {
            PaginatedContentLoader.State.Idle -> status.text = "Ready"
            is PaginatedContentLoader.State.Loading -> status.text = if (state.previous.isEmpty()) "Loading" else "Loading more"
            PaginatedContentLoader.State.Empty -> {
                items = emptyList(); updateList(); status.text = "No results"
            }
            is PaginatedContentLoader.State.Content -> {
                items = state.items; updateList(); status.text = "${items.size} result(s)"
                loadMore.visibility = if (state.hasMore) View.VISIBLE else View.GONE
            }
            is PaginatedContentLoader.State.Error -> {
                items = state.previous; updateList(); status.text = state.message
                retry.visibility = if (state.retryable) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateList() {
        results.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.map { it.title })
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

    override fun onDestroy() {
        discoveryJob?.cancel()
        super.onDestroy()
    }

    /** Deterministic clean-room provider; live transport remains a separate P0 acceptance criterion. */
    private class DemoProvider : ContentProvider {
        private val sample = MediaSummary("demo-series", "Recovered Series", MediaType.SERIES)
        override suspend fun catalog(type: MediaType, page: Int) = Page(listOf(sample.copy(type = type)), page, false)
        override suspend fun search(query: String, page: Int) = Page(if (query.isBlank()) emptyList() else listOf(sample.copy(title = "Result: ${query.trim()}")), page, false)
        override suspend fun details(id: String, type: MediaType) = MediaDetails(sample.copy(id = id, type = type), overview = "Deterministic clean-room recovery fixture", seasons = listOf(1), episodes = listOf(Episode("demo-episode", 1, 1, "Episode 1")))
        override suspend fun sources(mediaId: String, episodeId: String?): List<PlaybackSource> = listOf(PlaybackClassifier.classify("https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"))
    }
}
