package com.kidsexplore.app.model

/**
 * The privacy policy, as blocks a screen can lay out.
 *
 * The policy ships inside the app rather than behind a link. `docs/privacy-policy.md`
 * is the one source of truth and Gradle copies it into assets at build time, so
 * there is no second copy to drift — but that means the app has to render
 * Markdown, and Markdown is the one thing this file does.
 *
 * Deliberately not a Markdown library. The document is one known file in this
 * repository, written by us, and it uses a handful of constructs and no more.
 * A general parser would be a dependency, a size increase and a much larger
 * surface to be wrong about, in an app whose entire pitch is that it carries
 * nothing it does not need.
 */
sealed interface PolicyBlock {
    /** `# ` — the document's own title. */
    data class Title(val text: String) : PolicyBlock

    /** `## ` — a section heading. */
    data class Heading(val text: String) : PolicyBlock

    /** A run of non-blank lines, joined into one flowing paragraph. */
    data class Paragraph(val spans: List<PolicySpan>) : PolicyBlock

    /** `* ` — one item of a bulleted list. */
    data class Bullet(val spans: List<PolicySpan>) : PolicyBlock
}

/**
 * A stretch of text within a block, bold or not.
 *
 * Bold is the only inline styling the document uses that is worth keeping: it
 * carries meaning here ("**No advertisements.**" opens a bullet whose point is
 * that phrase). Italics never appear, and code spans are flattened to their
 * contents rather than styled — see [parseInline].
 */
data class PolicySpan(val text: String, val bold: Boolean)

private val COMMENT = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)

/** `[label](https://…)` — the label is what a reader needs; see [parseInline]. */
private val LINK = Regex("""\[([^\]]+)]\(([^)]+)\)""")

/** `<https://…>` — Markdown's bare-autolink form, used by the contact line. */
private val AUTOLINK = Regex("""<(https?://[^>]+)>""")

private val BOLD = Regex("""\*\*(.+?)\*\*""")

/**
 * `` `code` `` — the document uses backticks for permission names and package
 * ids. There is no monospace face bundled, so the backticks would be the only
 * thing a reader saw of the styling; the content reads fine as plain text.
 */
private val CODE = Regex("""`([^`]+)`""")

/**
 * Parse the policy Markdown into blocks.
 *
 * HTML comments are dropped first and whole: the source file opens with a long
 * note to whoever edits it next, which no reader of the app should ever see.
 */
fun parsePolicy(markdown: String): List<PolicyBlock> {
    val blocks = mutableListOf<PolicyBlock>()
    val paragraph = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += PolicyBlock.Paragraph(parseInline(paragraph.joinToString(" ")))
            paragraph.clear()
        }
    }

    // Bullets wrap onto continuation lines in the source; a line indented under
    // one belongs to it, not to a paragraph of its own.
    var lastWasBullet = false

    for (raw in COMMENT.replace(markdown, "").lines()) {
        val indented = raw.startsWith("  ") || raw.startsWith("\t")
        val line = raw.trim()
        when {
            line.isEmpty() -> {
                flushParagraph()
                lastWasBullet = false
            }

            line.startsWith("# ") -> {
                flushParagraph()
                blocks += PolicyBlock.Title(line.removePrefix("# ").trim())
                lastWasBullet = false
            }

            line.startsWith("## ") -> {
                flushParagraph()
                blocks += PolicyBlock.Heading(line.removePrefix("## ").trim())
                lastWasBullet = false
            }

            line.startsWith("* ") || line.startsWith("- ") -> {
                flushParagraph()
                blocks += PolicyBlock.Bullet(parseInline(line.drop(2).trim()))
                lastWasBullet = true
            }

            // A wrapped bullet line: fold it into the bullet it continues,
            // rather than starting a paragraph that would render outdented.
            lastWasBullet && indented -> {
                val previous = blocks.removeAt(blocks.lastIndex) as PolicyBlock.Bullet
                blocks += PolicyBlock.Bullet(joinSpans(previous.spans, parseInline(line)))
            }

            else -> {
                paragraph += line
                lastWasBullet = false
            }
        }
    }
    flushParagraph()
    return blocks
}

/**
 * Flatten links, autolinks and bold into styled spans.
 *
 * A link becomes its label and loses its address. Nothing in this app opens a
 * browser — that is the whole reason the policy is bundled rather than linked —
 * so an underlined address a reader cannot follow would be a worse lie than
 * plain text. The two addresses the document genuinely wants a reader to have
 * (Google's policy, and the issue tracker) are written out in full in the
 * source, so they survive as text. Code spans lose their backticks for the same
 * reason: no monospace face is bundled, so the markers would be all a reader
 * got of the styling.
 */
internal fun parseInline(text: String): List<PolicySpan> {
    val linked = AUTOLINK.replace(LINK.replace(text) { it.groupValues[1] }) { it.groupValues[1] }
    val flattened = CODE.replace(linked) { it.groupValues[1] }
    val spans = mutableListOf<PolicySpan>()
    var cursor = 0
    for (match in BOLD.findAll(flattened)) {
        if (match.range.first > cursor) {
            spans += PolicySpan(flattened.substring(cursor, match.range.first), bold = false)
        }
        spans += PolicySpan(match.groupValues[1], bold = true)
        cursor = match.range.last + 1
    }
    if (cursor < flattened.length) spans += PolicySpan(flattened.substring(cursor), bold = false)
    return spans.filter { it.text.isNotEmpty() }
}

/** Join two span runs with a space, merging across the seam where possible. */
private fun joinSpans(first: List<PolicySpan>, second: List<PolicySpan>): List<PolicySpan> {
    if (first.isEmpty()) return second
    if (second.isEmpty()) return first
    val last = first.last()
    return if (!last.bold && !second.first().bold) {
        first.dropLast(1) + PolicySpan(last.text + " " + second.first().text, bold = false) +
            second.drop(1)
    } else {
        first + PolicySpan(" ", bold = false) + second
    }
}
