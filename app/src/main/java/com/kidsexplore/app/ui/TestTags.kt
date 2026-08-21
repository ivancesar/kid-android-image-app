package com.kidsexplore.app.ui

import androidx.annotation.DrawableRes

/**
 * Tag shared by Home's theme grid and the Settings theme list — the two
 * vertically scrolling lists of themes. Tests scroll a theme into view by this
 * tag rather than by "the scrollable on screen", which is ambiguous the moment
 * a screen holds more than one scrollable.
 */
const val THEME_LIST_TEST_TAG = "theme-list"

/**
 * The Viewer's photograph, tagged with the drawable it is showing.
 *
 * A theme with artwork shows a picture and nothing else — no caption, and no
 * content description, because the app is for looking at rather than
 * listening to. That leaves nothing in the semantics tree for a test to match
 * on, and in particular nothing saying *which* of the fourteen is up. The
 * drawable id in the tag is that missing fact: what matters about this screen
 * is that the expected image is the one that loaded, not what is in it.
 *
 * Ids are assigned at build time, so the value means nothing across builds —
 * which is fine, because the only reader resolves it from the same `R` the
 * screen did.
 */
fun viewerImageTestTag(@DrawableRes image: Int): String = "$VIEWER_IMAGE_TEST_TAG:$image"

/** Prefix of [viewerImageTestTag]; the tag itself is what tests match on. */
const val VIEWER_IMAGE_TEST_TAG = "viewer-image"
