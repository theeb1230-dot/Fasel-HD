package com.faselhd.restored.flow

import android.content.Context
import android.content.Intent
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.ui.PlayerActivity

/**
 * Converts only validated native decisions into the internal player Intent.
 * Resolver and rejected decisions never open an external browser or PlayerActivity.
 */
object PlaybackNavigator {
    fun intent(context: Context, decision: PlaybackDecision): Intent? {
        val native = decision as? PlaybackDecision.Native ?: return null
        return Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URI, native.request.uri)
            putExtra(PlayerActivity.EXTRA_KIND, native.request.kind.name)
        }
    }
}
