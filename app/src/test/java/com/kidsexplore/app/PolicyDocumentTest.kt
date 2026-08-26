package com.kidsexplore.app

import com.kidsexplore.app.model.PolicyBlock
import com.kidsexplore.app.model.parseInline
import com.kidsexplore.app.model.parsePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Markdown the privacy policy is allowed to use.
 *
 * The parser is deliberately partial — it handles what `docs/privacy-policy.md`
 * contains and nothing else — so these are the constructs that file may keep
 * using, written down. The instrumented `PolicyAssetTest` is what checks the
 * real document still parses; this checks the rules it has to obey.
 */
class PolicyDocumentTest {

    @Test
    fun headingsAndTitleAreTheirOwnBlocks() {
        val blocks = parsePolicy("# Privacy Policy\n\n## The short version\n\nIt collects nothing.")
        assertEquals(
            listOf<Any>(PolicyBlock.Title::class, PolicyBlock.Heading::class, PolicyBlock.Paragraph::class),
            blocks.map { it::class },
        )
        assertEquals("Privacy Policy", (blocks[0] as PolicyBlock.Title).text)
        assertEquals("The short version", (blocks[1] as PolicyBlock.Heading).text)
    }

    @Test
    fun theEditorsCommentNeverReachesAReader() {
        val blocks = parsePolicy(
            """
            # Title

            <!--
              One placeholder has to be filled in before this is published.
            -->

            Real text.
            """.trimIndent()
        )
        val rendered = blocks.joinToString(" ") { block ->
            when (block) {
                is PolicyBlock.Title -> block.text
                is PolicyBlock.Heading -> block.text
                is PolicyBlock.Paragraph -> block.spans.joinToString("") { it.text }
                is PolicyBlock.Bullet -> block.spans.joinToString("") { it.text }
            }
        }
        assertTrue("editor note leaked into the document: $rendered", "placeholder" !in rendered)
        assertEquals("Title Real text.", rendered)
    }

    @Test
    fun wrappedLinesBecomeOneParagraph() {
        val blocks = parsePolicy("Kids Explore collects nothing.\nIt has no internet access.")
        assertEquals(1, blocks.size)
        assertEquals(
            "Kids Explore collects nothing. It has no internet access.",
            (blocks[0] as PolicyBlock.Paragraph).spans.single().text,
        )
    }

    @Test
    fun aBulletKeepsItsWrappedContinuation() {
        val blocks = parsePolicy("* **No advertisements.** The app shows\n  none, of any kind.")
        val bullet = blocks.single() as PolicyBlock.Bullet
        assertEquals("No advertisements.", bullet.spans.first().text)
        assertTrue(bullet.spans.first().bold)
        assertEquals(
            "No advertisements. The app shows none, of any kind.",
            bullet.spans.joinToString("") { it.text },
        )
    }

    @Test
    fun boldSurvivesAndItsMarkersDoNot() {
        val spans = parseInline("Two things, **in the app's own private storage**, readable only by the app.")
        assertEquals(3, spans.size)
        assertEquals("in the app's own private storage", spans[1].text)
        assertTrue(spans[1].bold)
        assertTrue("** leaked", spans.none { "*" in it.text })
    }

    @Test
    fun codeSpansLoseTheirBackticks() {
        assertEquals(
            "the app declares no INTERNET permission",
            parseInline("the app declares no `INTERNET` permission").joinToString("") { it.text },
        )
        assertTrue(
            "backtick reached a reader",
            parseInline("`com.kidsexplore.app`").none { "`" in it.text },
        )
    }

    /**
     * Links keep their label and lose their address, because the app opens
     * nothing — an underlined address a reader cannot follow would be worse
     * than plain text. An autolink is all address, so its address is the label.
     */
    @Test
    fun linksFlattenToSomethingAReaderCanUse() {
        assertEquals(
            "governed by Google's privacy policy, and the developer has no access.",
            parseInline(
                "governed by [Google's privacy policy](https://policies.google.com/privacy), " +
                    "and the developer has no access."
            ).joinToString("") { it.text },
        )
        assertEquals(
            "Open an issue at https://github.com/ivancesar/kid-android-image-app/issues.",
            parseInline("Open an issue at <https://github.com/ivancesar/kid-android-image-app/issues>.")
                .joinToString("") { it.text },
        )
    }
}
