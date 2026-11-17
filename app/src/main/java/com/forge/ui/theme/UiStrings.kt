package com.forge.ui.theme

import android.content.Context
import com.forge.i18n.Tr

/**
 * [text] in the app's language. The app's [Text] and [Icon] already translate what they
 * show, so this is for words that go elsewhere: a notification, a share sheet, a string
 * built before it is shown.
 */
fun tr(text: String): String = Tr.t(text)

@Suppress("UNUSED_PARAMETER")
fun translated(context: Context, text: String): String = Tr.t(text)
