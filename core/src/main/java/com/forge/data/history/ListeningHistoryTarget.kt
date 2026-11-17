package com.forge.data.history

import kotlinx.serialization.Serializable

@Serializable
enum class ListeningHistoryTarget(val label: String, val summary: String) {
    DEVICE_ONLY("This device", "Keep listening history in Forge"),
    YOUTUBE_MUSIC("YouTube Music", "Also send listened songs to your signed-in YouTube Music account"),
}
