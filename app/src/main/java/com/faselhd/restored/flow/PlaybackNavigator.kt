package com.faselhd.restored.flow

import android.content.Context
import android.content.Intent
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.ui.PlayerActivity

data class PlayerLaunch(val uri: String, val kind: String)

/**
 * Converts only validated native decisions into the internal player route.
 * Resolver and rejected decisions never open an external browser or PlayerActivity.
 */
object PlaybackNavigator {
    fun plan(decision: PlaybackDecision): PlayerLaunch? {
        val native = decision as? PlaybackDecision.Native ?: return null
        return PlayerLaunch(native.request.uri, native.request.kind.name)
    }

    fun intent(context: Context, decision: PlaybackDecision): Intent? =
        plan(decision)?.let { launch ->
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_URI, launch.uri)
                putExtra(PlayerActivity.EXTRA_KIND, launch.kind)
            }
        }
}
