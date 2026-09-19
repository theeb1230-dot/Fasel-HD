package com.faselhd.restored.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.player.Media3Playback
import com.faselhd.restored.player.PlaybackRequest

/** Native playback surface. Inputs are validated again by PlaybackRequest before network access. */
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerView = PlayerView(this)
        setContentView(playerView)
    }

    override fun onStart() {
        super.onStart()
        if (player == null) startPlayback()
    }

    override fun onStop() {
        releasePlayer()
        super.onStop()
    }

    private fun startPlayback() {
        val uri = intent.getStringExtra(EXTRA_URI) ?: return finish()
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { PlaybackKind.valueOf(it) }.getOrNull() }
            ?: return finish()
        val request = runCatching { PlaybackRequest(PlaybackSource(uri, kind)) }.getOrElse { return finish() }
        player = Media3Playback(this).create(request).also {
            playerView.player = it
            it.playWhenReady = true
        }
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_URI = "playback_uri"
        const val EXTRA_KIND = "playback_kind"
    }
}
