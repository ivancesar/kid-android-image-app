package com.kidsexplore.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.PolicyBlock
import com.kidsexplore.app.model.parsePolicy
import com.kidsexplore.app.ui.POLICY_LIST_TEST_TAG
import com.kidsexplore.app.ui.screens.PolicyScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bundled privacy policy: that it is actually in the APK, that it parses,
 * and that the screen renders it.
 *
 * On a device rather than the JVM because the point is the *asset* — Gradle
 * copying `docs/privacy-policy.md` into the package is the step that can
 * silently stop happening, and a JVM test reading the file off disk would pass
 * whether or not it ever reached the app. `PolicyDocumentTest` covers the
 * parsing rules; this covers the document really shipping.
 */
@RunWith(AndroidJUnit4::class)
class PolicyScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun policyMarkdown(): String =
        InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("privacy-policy.md").bufferedReader().use { it.readText() }

    @Test
    fun thePolicyIsPackagedWithTheApp() {
        val blocks = parsePolicy(policyMarkdown())
        assertTrue("policy asset parsed to nothing", blocks.size > 20)
        assertTrue(
            "policy has no title",
            blocks.filterIsInstance<PolicyBlock.Title>().isNotEmpty(),
        )
        assertTrue(
            "policy has no sections",
            blocks.filterIsInstance<PolicyBlock.Heading>().size >= 5,
        )
    }

    /**
     * The editor's note at the top of the source file explains how to edit the
     * document. Shipping it to a parent would be a small embarrassment and a
     * large tell that nothing renders this file before release.
     */
    @Test
    fun theEditorsNoteIsNotShipped() {
        val markdown = policyMarkdown()
        assertTrue("fixture no longer contains a comment", "<!--" in markdown)
        val rendered = parsePolicy(markdown).joinToString(" ") { block ->
            when (block) {
                is PolicyBlock.Title -> block.text
                is PolicyBlock.Heading -> block.text
                is PolicyBlock.Paragraph -> block.spans.joinToString("") { it.text }
                is PolicyBlock.Bullet -> block.spans.joinToString("") { it.text }
            }
        }
        assertTrue("editor note reached the screen", "PolicyScreen renders it" !in rendered)
        assertTrue("<!--" !in rendered)
    }

    @Test
    fun theScreenShowsTheDocumentAndCanBeDismissed() {
        var dismissed = false
        compose.setContent {
            KidsExploreTheme { PolicyScreen(onBack = { dismissed = true }) }
        }

        compose.onNodeWithTag(POLICY_LIST_TEST_TAG).assertIsDisplayed()
        // The first block of the real document is its title.
        val title = (parsePolicy(policyMarkdown()).first() as PolicyBlock.Title).text
        compose.onNodeWithText(title).assertIsDisplayed()

        compose.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.policy_done)
        ).performClick()
        assertTrue("Done did not close the policy", dismissed)
    }
}
