package com.forge.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import com.forge.ui.theme.Icon
import com.forge.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import java.util.concurrent.TimeUnit

/** Ways to back the project: a star on GitHub, or a sponsorship. */
object Support {
    val REPO: String get() = com.forge.data.config.RemoteConfig.current.github
    val DISCUSSIONS: String get() = com.forge.data.config.RemoteConfig.current.community

    /**
     * The monthly invitation: a GitHub star after five days of use. It comes
     * at most once every 30 days, so it never nags.
     */
    enum class Invite(internal val daysBefore: Int, internal val askedKey: String, internal val doneKey: String) {
        STAR(5, "askedAt", "done"),
    }
    private val MIN_GAP_MS = TimeUnit.DAYS.toMillis(30)
    private val APART_MS = TimeUnit.DAYS.toMillis(14)
    private fun prefs(context: Context) = context.getSharedPreferences("support", Context.MODE_PRIVATE)

    fun usedToday(context: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        val day = TimeUnit.MILLISECONDS.toDays(now)
        if (p.getLong("lastDay", -1) == day) return
        p.edit { putLong("lastDay", day); putInt("days", p.getInt("days", 0) + 1) }
    }

    /** The invitation that's due now, if any; the star first when both are. */
    fun due(context: Context, now: Long = System.currentTimeMillis()): Invite? {
        if (!com.forge.data.config.RemoteConfig.current.communityPrompts) return null
        val p = prefs(context)
        val other = Invite.entries.maxOf { p.getLong(it.askedKey, 0) }
        return Invite.entries.firstOrNull { invite ->
            val asked = p.getLong(invite.askedKey, 0)
            shouldAsk(p.getBoolean(invite.doneKey, false), p.getInt("days", 0), asked, now, invite.daysBefore) &&
                (other == asked || now - other >= APART_MS)
        }
    }

    internal fun shouldAsk(done: Boolean, days: Int, askedAt: Long, now: Long, daysBefore: Int = Invite.STAR.daysBefore): Boolean =
        !done && days >= daysBefore && now - askedAt >= MIN_GAP_MS

    /** Record an impression too, so closing the app cannot trigger another prompt. */
    fun answered(context: Context, invite: Invite, never: Boolean, now: Long = System.currentTimeMillis()) = prefs(context).edit {
        putLong(invite.askedKey, now)
        if (never) putBoolean(invite.doneKey, true)
    }

}

/** A monthly invitation to star the project on GitHub. */
@Composable
fun InviteCard(invite: Support.Invite, onDismiss: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(invite) { Support.answered(context, invite, never = false) }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        onDismiss()
    }
    FloatingDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Star, null) },
        title = { Text("Made for music. Built together.") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Forge is free and source-available. A star on GitHub helps more listeners find the app, and takes a second."
                )
                androidx.compose.material3.FilledTonalButton(
                    onClick = { open(Support.REPO) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(18.dp))
                    Text("Star on GitHub", Modifier.padding(start = 8.dp))
                }
                androidx.compose.material3.TextButton(onClick = { Support.answered(context, invite, never = true); onDismiss() }) {
                    Text("Don't show again")
                }
            }
        },
        confirmButton = { SheetButton("Maybe later", onClick = onDismiss, closes = true) },
    )
}
