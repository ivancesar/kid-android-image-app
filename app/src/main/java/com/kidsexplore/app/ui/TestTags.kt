package com.kidsexplore.app.ui

/**
 * Tag shared by Home's theme grid and the Settings theme list — the two
 * vertically scrolling lists of themes. Tests scroll a theme into view by this
 * tag rather than by "the scrollable on screen", which is ambiguous the moment
 * a screen holds more than one scrollable.
 */
const val THEME_LIST_TEST_TAG = "theme-list"
