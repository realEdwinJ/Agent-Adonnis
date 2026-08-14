package com.adonnis.app.ui.components

import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.markdown.MarkdownParseOptions
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material.RichText
import com.halilibo.richtext.ui.resolveDefaults

/**
 * Renders Markdown as rich, formatted text — bold, italics, code, headings,
 * lists, links, quotes, tables — instead of showing the raw `**`/`#` symbols.
 *
 * richtext-ui-material's `RichText` reads Material2's `LocalTextStyle` and
 * `LocalContentColor`, so we feed it our Material3 palette to keep the text
 * looking native in both light and dark theme.
 *
 * @param text     The raw Markdown string to render.
 * @param style    Base text style (color is overridden by [color]).
 * @param color    Text color — defaults to the theme's onSurfaceVariant.
 * @param modifier Applied to the root [RichText].
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (text.isBlank()) return

    val uriHandler = LocalUriHandler.current
    val textStyle = style.copy(color = color)
    val richTextStyle = remember { RichTextStyle().resolveDefaults() }
    val parseOptions = remember { MarkdownParseOptions.Default.copy(autolink = true) }

    CompositionLocalProvider(
        LocalTextStyle provides textStyle,
        LocalContentColor provides color
    ) {
        RichText(
            style = richTextStyle,
            modifier = modifier
        ) {
            Markdown(
                content = text,
                markdownParseOptions = parseOptions,
                onLinkClicked = { url -> uriHandler.openUri(url) }
            )
        }
    }
}
