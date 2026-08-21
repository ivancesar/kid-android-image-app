package com.kidsexplore.app.ui

/**
 * Tag shared by Home's theme grid and the Settings theme list — the two
 * vertically scrolling lists of themes. Tests scroll a theme into view by this
 * tag rather than by "the scrollable on screen", which is ambiguous the moment
 * a screen holds more than one scrollable.
 */
const val THEME_LIST_TEST_TAG = "theme-list"

/**
 * The Viewer's photograph.
 *
 * A theme with artwork shows a picture and nothing else — no caption, and no
 * content description, because the app is for looking at rather than
 * listening to. That leaves the tests with nothing in the semantics tree to
 * match on, so the image carries a tag purely so they can find it. It cannot
 * tell them *which* photograph is on screen; the ViewModel's index is what
 * says that now.
 */
const val VIEWER_IMAGE_TEST_TAG = "viewer-image"
