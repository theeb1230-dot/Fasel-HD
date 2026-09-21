package com.faselhd.restored.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.PlaybackException
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
    private lateinit var errorView: TextView
    private lateinit var retryButton: Button
    private var resumePositionMs = 0L
    private var resumePlayWhenReady = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resumePositionMs = savedInstanceState?.getLong(STATE_POSITION_MS) ?: 0L
        resumePlayWhenReady = savedInstanceState?.getBoolean(STATE_PLAY_WHEN_READY) ?: true

        playerView = PlayerView(this).apply { id = PLAYER_VIEW_ID }
        errorView = TextView(this).apply {
            id = ERROR_VIEW_ID
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        retryButton = Button(this).apply {
            id = RETRY_BUTTON_ID
            text = "إعادة المحاولة"
            visibility = View.GONE
            setOnClickListener { retryPlayback() }
        }
        val root = FrameLayout(this).apply {
            addView(playerView, FrameLayout.LayoutParams(-1, -1))
            addView(errorView, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
            addView(retryButton, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM).apply {
                bottomMargin = 48
            })
        }
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        if (player == null) startPlayback()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        capturePlaybackState()
        outState.putLong(STATE_POSITION_MS, resumePositionMs)
        outState.putBoolean(STATE_PLAY_WHEN_READY, resumePlayWhenReady)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        capturePlaybackState()
        releasePlayer()
        super.onStop()
    }

    internal fun runtimePlaybackState(): Int? = player?.playbackState
    internal fun runtimePlaybackPositionMs(): Long = player?.currentPosition ?: resumePositionMs

    private fun startPlayback() {
        val uri = intent.getStringExtra(EXTRA_URI) ?: return finish()
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { PlaybackKind.valueOf(it) }.getOrNull() }
            ?: return finish()
        val request = runCatching { PlaybackRequest(PlaybackSource(uri, kind)) }.getOrElse { return finish() }
        hideError()
        player = Media3Playback(this).create(request).also { exoPlayer ->
            playerView.player = exoPlayer
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    showError("تعذر تشغيل الفيديو. تحقق من الاتصال أو جرّب مرة أخرى.")
                }
            })
            if (resumePositionMs > 0L) exoPlayer.seekTo(resumePositionMs)
            exoPlayer.playWhenReady = resumePlayWhenReady
        }
    }

    private fun retryPlayback() {
        hideError()
        val current = player
        if (current != null) {
            current.prepare()
            current.playWhenReady = resumePlayWhenReady
        } else {
            startPlayback()
        }
    }

    private fun capturePlaybackState() {
        player?.let {
            resumePositionMs = it.currentPosition.coerceAtLeast(0L)
            resumePlayWhenReady = it.playWhenReady
        }
    }

    private fun showError(message: String) {
        capturePlaybackState()
        errorView.text = message
        errorView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorView.visibility = View.GONE
        retryButton.visibility = View.GONE
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_URI = "playback_uri"
        const val EXTRA_KIND = "playback_kind"
        const val PLAYER_VIEW_ID = 0x5f000001
        const val ERROR_VIEW_ID = 0x5f000002
        const val RETRY_BUTTON_ID = 0x5f000003
        private const val STATE_POSITION_MS = "player_position_ms"
        private const val STATE_PLAY_WHEN_READY = "player_play_when_ready"
    }
}
