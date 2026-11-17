package com.forge.data

import android.app.Application
import com.forge.data.account.AccountStore
import com.forge.data.autoeq.AutoEq
import com.forge.data.covers.AlbumCovers
import com.forge.data.download.Downloads
import com.forge.data.history.History
import com.forge.data.innertube.InnerTubeXResolver
import com.forge.data.innertube.UpgradedTracks
import com.forge.data.lastfm.LastFm
import com.forge.data.library.LibraryStore
import com.forge.data.listenbrainz.ListenBrainz
import com.forge.data.settings.AppSettings
import com.forge.data.subsonic.Subsonic
import com.forge.playback.QueueStore

/** Starts the shared services, in the order they depend on each other. */
object Core {
    /**
     * [phone] is false on the watch, which leaves out what only the phone
     * and TV app does: new-release alerts, update checks and usage counts.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun init(app: Application, phone: Boolean = true) {
        // Media3's warnings and errors (a failed load, a decoder that won't
        // start) into the app's own log as well as logcat, for "Copy log".
        androidx.media3.common.util.Log.setLogger(object : androidx.media3.common.util.Log.Logger {
            private val system = androidx.media3.common.util.Log.Logger.DEFAULT
            override fun d(tag: String, message: String, throwable: Throwable?) = system.d(tag, message, throwable)
            override fun i(tag: String, message: String, throwable: Throwable?) = system.i(tag, message, throwable)
            override fun w(tag: String, message: String, throwable: Throwable?) {
                AppLog.add('W', tag, message, throwable)
                system.w(tag, message, throwable)
            }
            override fun e(tag: String, message: String, throwable: Throwable?) {
                AppLog.add('E', tag, message, throwable)
                system.e(tag, message, throwable)
            }
        })
        AppSettings.init(app)
        com.forge.data.config.RemoteConfig.init(app)
        com.forge.data.addons.AddonStore.init(app)
        com.forge.data.addons.AddonOutput.init(app)
        com.forge.data.addons.AddonStore.onChanged = com.forge.data.lossless.ExternalStreams::retryLookups
        History.init(app)
        com.forge.data.history.ListenedCollections.init(app)
        com.forge.data.covers.ArtistPhotos.init(app)
        com.forge.data.metadata.ArtistGenres.init(app)
        com.forge.data.history.MixSnapshots.init(app)
        com.forge.data.reco.HomeEngagement.init(app)
        LoudnessStore.init(app)
        UpgradedTracks.init(app)
        QueueStore.init(app)
        com.forge.data.together.Together.init(app)
        LastFm.init(app)
        Subsonic.init(app)
        ListenBrainz.init(app)
        com.forge.data.discord.DiscordRpc.init(app)
        com.forge.data.config.HomeDismissals.init(app)
        com.forge.data.reco.TasteModel.init(app)
        com.forge.data.audiobooks.Audiobooks.init(app)
        com.forge.data.radio.Radio.init(app)
        if (phone) {
            com.forge.data.addons.AddonRefreshWorker.schedule(app)
            com.forge.data.releases.NewReleases.init(app)
            com.forge.data.UpdateCheck.init(app)
            com.forge.data.UpdateLock.init(app)
            com.forge.data.UpdateCheck.schedule(app)
            com.forge.data.Usage.init(app)
        }
        // Spotify sign-in is gone; drop any tokens an earlier version kept.
        app.deleteSharedPreferences("spotify")
        // The waveform seek bar is gone; drop the waveforms an earlier version saved.
        java.io.File(app.filesDir, "waveforms").takeIf { it.exists() }?.let { dir -> Thread { dir.deleteRecursively() }.start() }
        com.forge.data.podcasts.Podcasts.init(app)
        com.forge.data.library.ResumePoints.init(app)
        AlbumCovers.init(app)
        com.forge.data.spotify.SpotifyWebPlayer.init(app)
        com.forge.data.spotify.SpotifySync.init(app)
        AutoEq.init(app)
        com.forge.data.library.LibraryArtwork.init(app)
        LibraryStore.init(app)
        Downloads.init(app)
        if (phone) com.forge.data.download.SmartDownloads.init(app)
        com.forge.data.download.DownloadedLists.init(app)
        AccountStore.init(app)
        com.forge.data.library.LibraryCache.init(app)
        // After library and account: it sends playlist changes still waiting, when signed in.
        com.forge.data.library.PlaylistSync.init(app)
        com.forge.data.library.LikedSync.init()
        InnerTubeXResolver.init(app)
    }

    /**
     * Saves at once what's still waiting for its save (each store waits a moment
     * after a change, so a run of changes is written once): the app is leaving
     * the screen or the player is closing, and the process may go any moment.
     */
    fun flush() {
        LibraryStore.flush()
        History.flush()
        com.forge.data.history.ListenedCollections.flush()
        Downloads.flush()
    }
}
