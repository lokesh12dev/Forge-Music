package com.forge

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.forge.data.Http
import com.forge.data.MusicRepository
import com.forge.data.subsonic.Subsonic
import com.forge.ui.ScreenCache
import okio.Path.Companion.toOkioPath

class ForgeApp : Application(), SingletonImageLoader.Factory {
    /**
     * Artwork on the app's shared HTTP client (warm connections to Google's
     * image hosts), a fifth of memory for decoded images and 100 MB on disk,
     * with a short cross-fade as each one lands.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Http.client }))
                add(coil3.svg.SvgDecoder.Factory())
                add(coil3.map.Mapper<String, String> { data, _ -> com.forge.data.library.LibraryArtwork.local(data) })
                // A cover on your own server is stored without credentials; sign it here.
                add(coil3.map.Mapper<String, String> { data, _ -> if (Subsonic.isCoverRef(data)) Subsonic.resolveCover(data) else null })
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(200)
            .build()

    override fun onCreate() {
        super.onCreate()
        // First, so a crash anywhere after this is kept for Export diagnostics.
        com.forge.data.CrashLog.install(this)
        // The home-screen widget follows the shared player service.
        com.forge.data.AppHooks.onPlayerChanged = { context, player -> com.forge.widget.NowPlayingWidget.refresh(context, player) }
        com.forge.data.AppHooks.onPlayerStopped = { context -> com.forge.widget.NowPlayingWidget.stopped(context) }
        java.util.Locale.setDefault(com.forge.ui.theme.AppLanguages.wrap(this).resources.configuration.locales[0])
        // The words on every screen, in the chosen language (the phone's own when none is chosen).
        com.forge.i18n.Tr.use(this, com.forge.ui.theme.AppLanguages.selected(this).tag)
        com.forge.data.Core.init(this)
        // Spotify's personal song radio, when Spotify is connected; the cookie stays with the WebView.
        com.forge.data.spotify.SpotifyWebPlayer.account = {
            if (com.forge.ui.spotify.SpotifyConnection.connected.value) com.forge.ui.spotify.SpotifyConnection.cookie() else null
        }
        // Last session's Home, shown at once and refreshed in the background.
        MusicRepository.init(this)?.let { ScreenCache.put("home:${com.forge.data.settings.AppSettings.playback.value.recommender.name}", it, at = 0L) }
        // After history, library, downloads and account: they tell an existing user from a new one.
        com.forge.data.Welcome.settle(this)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // The phone's language changed: follow it, when the app is set to.
        com.forge.i18n.Tr.use(this, com.forge.ui.theme.AppLanguages.selected(this).tag)
    }
}
