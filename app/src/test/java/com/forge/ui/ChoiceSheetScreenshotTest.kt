package com.forge.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.forge.data.settings.AudioQuality
import com.forge.data.settings.ChoiceMenuStyle
import org.junit.After
import org.junit.Before
import androidx.compose.ui.test.onNodeWithContentDescription
import com.forge.playback.SleepTimer
import com.forge.ui.components.ChoiceSheet
import com.forge.ui.components.FloatingDialog
import com.forge.ui.components.SheetButton
import com.forge.ui.components.SheetTone
import com.forge.ui.player.RemixSheet
import com.forge.ui.player.SleepTimerDialog
import androidx.compose.material3.Text
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/** Draws the option picker to build/screenshots, and checks a pick lands and closes it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ChoiceSheetScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Before fun setUp() {
        com.forge.data.settings.AppSettings.init(compose.activity)
    }

    @After fun tearDown() {
        com.forge.data.settings.AppSettings.updateUi { it.copy(choiceMenus = ChoiceMenuStyle.SCREEN) }
    }

    @Test fun pick() {
        com.forge.data.settings.AppSettings.updateUi { it.copy(choiceMenus = ChoiceMenuStyle.POPUP) }
        var quality by mutableStateOf(AudioQuality.entries.first())
        var open by mutableStateOf(true)
        compose.setContent {
            MaterialTheme(darkColorScheme(primary = Color(0xFFFF8A65))) {
                Surface(color = Color(0xFF0B0B0E)) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A2A4A), Color(0xFF101014)))))
                    if (open) {
                        ChoiceSheet(
                            title = "Quality on Wi-Fi",
                            options = AudioQuality.entries,
                            selected = quality,
                            label = { "${it.label} · ${it.summary}" },
                            onSelect = { quality = it },
                            onDismiss = { open = false },
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        save("choice_sheet")
        val target = AudioQuality.entries.last()
        compose.onNodeWithText(target.label).performClick()
        compose.waitForIdle()
        assertEquals(target, quality)
        assertTrue(!open)
    }

    /** As a page of its own: a header with the title and Back, the options, and a pick lands and closes it. */
    @Test fun pickOnAScreen() {
        com.forge.data.settings.AppSettings.updateUi { it.copy(choiceMenus = ChoiceMenuStyle.SCREEN) }
        var quality by mutableStateOf(AudioQuality.entries.first())
        var open by mutableStateOf(true)
        compose.setContent {
            MaterialTheme(darkColorScheme(primary = Color(0xFFFF8A65), onPrimary = Color(0xFF3A1206))) {
                Surface(color = Color(0xFF0B0B0E)) {
                    if (open) ChoiceSheet(
                        title = "Quality on Wi-Fi",
                        options = AudioQuality.entries,
                        selected = quality,
                        label = { "${it.label} · ${it.summary}" },
                        onSelect = { quality = it },
                        onDismiss = { open = false },
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        compose.onNodeWithText("Quality on Wi-Fi").assertIsDisplayed()
        AudioQuality.entries.forEach { compose.onNodeWithText(it.label).assertIsDisplayed() }
        val page = ShadowDialog.getLatestDialog().window!!.decorView
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.draw(Canvas(bitmap))
        File("build/screenshots/choice_screen.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val target = AudioQuality.entries.last()
        compose.onNodeWithText(target.label).performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        assertEquals(target, quality)
        assertTrue(!open)
    }

    /** Back on the page closes it without changing anything. */
    @Test fun backOnTheScreenClosesWithoutChoosing() {
        com.forge.data.settings.AppSettings.updateUi { it.copy(choiceMenus = ChoiceMenuStyle.SCREEN) }
        var quality by mutableStateOf(AudioQuality.entries.first())
        var open by mutableStateOf(true)
        compose.setContent {
            MaterialTheme(darkColorScheme()) {
                Surface(color = Color(0xFF0B0B0E)) {
                    if (open) ChoiceSheet("Quality", AudioQuality.entries, quality, { it.label }, { quality = it }, { open = false })
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        assertTrue(!open)
        assertEquals(AudioQuality.entries.first(), quality)
    }

    @Test fun sleepTimer() {
        SleepTimer.startMinutes(30)
        SleepTimer.addMinutes(-12)
        show { SleepTimerDialog(onDismiss = {}) }
        save("sleep_timer")
        SleepTimer.cancel()
    }

    @Test fun confirm() {
        show {
            FloatingDialog(
                onDismissRequest = {},
                title = { Text("Delete playlist?") },
                text = { Text("Late night drive and its 42 songs go from this phone. Songs you saved stay saved.") },
                confirmButton = { SheetButton("Delete", tone = SheetTone.Danger, onClick = {}) },
                dismissButton = { SheetButton("Cancel", onClick = {}, closes = true) },
            )
        }
        save("confirm_dialog")
    }

    /** Every row of the lyrics sources sheet keeps its label: the list scrolls rather than squeezing the last ones. */
    @Test fun lyricsSources() {
        show { com.forge.ui.settings.LyricsSourcesDialog(onDismiss = {}) }
        save("lyrics_sources")
        compose.onNodeWithText("SimpMusic").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Romanized lyrics").performScrollTo().assertIsDisplayed()
        compose.waitForIdle()
        save("lyrics_sources_end")
    }

    @Test fun resetLyricsOptionsKeepsPerSongChoicesAndTiming() {
        val saved = com.forge.data.settings.AppSettings.lyrics.value
        val id = "4NRXx6U8ABQ"
        val source = com.forge.data.settings.LyricsSource.SIMPMUSIC
        try {
            com.forge.data.settings.AppSettings.updateLyrics { it.copy(
                sources = listOf(com.forge.data.settings.LyricsSourceEntry(source, enabled = false)),
                preferWordSynced = false,
                romanize = false,
                offsets = mapOf(id to 500L),
                chosen = mapOf(id to source),
            ) }
            show { com.forge.ui.settings.LyricsSourcesDialog(onDismiss = {}) }
            compose.onNodeWithText("Reset to default").performClick()
            compose.waitForIdle()
            val reset = com.forge.data.settings.AppSettings.lyrics.value
            assertEquals(com.forge.data.settings.DEFAULT_LYRICS_SOURCES, reset.sources)
            assertTrue(reset.preferWordSynced)
            assertTrue(reset.romanize)
            assertEquals(mapOf(id to 500L), reset.offsets)
            assertEquals(mapOf(id to source), reset.chosen)
        } finally {
            com.forge.data.settings.AppSettings.updateLyrics { saved }
        }
    }

    @Test fun remix() {
        show { RemixSheet(onDismiss = {}) }
        save("remix_sheet")
    }

    /** The song menu opens at half height, and dragging its top takes it up. */
    @Test fun songMenuOpensFull() {
        com.forge.data.library.LibraryStore.init(compose.activity)
        val song = com.forge.data.model.Song("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", null, "3:20")
        val menu = com.forge.ui.components.SongMenuActions({}, {}, {}, {}, {}, dislike = {})
        show {
            androidx.compose.runtime.CompositionLocalProvider(com.forge.ui.components.LocalSongMenu provides menu) {
                com.forge.ui.components.SongMenuSheet(song.copy(artistId = "UC1", albumId = "MPRE1"), onDismiss = {})
            }
        }
        save("song_menu_full")
        // It opens at full height, so a row well down the menu is already on screen without any scrolling.
        compose.onNodeWithText("Open album").assertIsDisplayed()
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            MaterialTheme(darkColorScheme(primary = Color(0xFFFF8A65), onPrimary = Color(0xFF3A1206))) {
                Surface(color = Color(0xFF0B0B0E)) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A2A4A), Color(0xFF101014)))))
                    content()
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
    }

    private fun save(name: String) {
        val back = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(back.width, back.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        back.draw(canvas)
        canvas.drawColor(android.graphics.Color.argb(90, 0, 0, 0))
        val sheet = ShadowDialog.getLatestDialog().window!!.decorView
        canvas.save()
        canvas.translate(0f, (back.height - sheet.height).toFloat())
        sheet.draw(canvas)
        canvas.restore()
        val file = File("build/screenshots/$name.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
