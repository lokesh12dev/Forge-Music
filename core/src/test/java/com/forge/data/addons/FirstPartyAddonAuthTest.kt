package com.forge.data.addons

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstPartyAddonAuthTest {
    @Test fun onlyExactOfficialOriginNeedsProof() {
        val origin = FirstPartyAddonAuth.ORIGIN
        assertTrue(FirstPartyAddonAuth.protects("$origin/monochrome/search?q=hello".toHttpUrl()))
        assertTrue(FirstPartyAddonAuth.protects("$origin/archive/stream/recording".toHttpUrl()))
        for (url in listOf(
            "$origin/monochrome/manifest.json", "$origin/archive/health", "$origin/",
            "https://public-addon.example/search?q=Forge",
            "https://public-addon.example/auth/web/info",
            "https://losslessbridge.de.deplexo.com/stream/123",
            "https://bluebit.jucepe0929.workers.dev/search?q=test",
            "$origin.evil.example/search", "$origin:8443/search",
            origin.replace("https://", "http://") + "/search",
            "https://public-addon.example/redirect?to=$origin/monochrome/search"
        )) assertFalse(url, FirstPartyAddonAuth.protects(url.toHttpUrl()))
        // No Context, AndroidKeyStore, network or token setup is needed for public sources.
    }

    @Test fun bothOfForgesServersNeedProof() {
        for (origin in FirstPartyAddonAuth.ORIGINS) {
            assertTrue(origin, FirstPartyAddonAuth.protects("$origin/netease/search?q=hello".toHttpUrl()))
            assertFalse(origin, FirstPartyAddonAuth.protects("$origin/netease/manifest.json".toHttpUrl()))
        }
        assertTrue(FirstPartyAddonAuth.protects("https://forge-addons.invalid/archive/stream/x".toHttpUrl()))
        assertTrue(FirstPartyAddonAuth.protects("https://forge-lossless-addon.invalid/archive/stream/x".toHttpUrl()))
        assertFalse(FirstPartyAddonAuth.protects("https://other.forge-addons.invalid/archive/stream/x".toHttpUrl()))
    }
}
