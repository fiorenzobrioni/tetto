package com.callbackdev.tetto.core.designsystem.components

/**
 * Marks targets packed tighter than a finger's 48dp on purpose: Material's clock dial (the
 * reminder's time: its hours stand on a circle and follow a drag). Each is
 * still its own labelled node, reached one by one by swiping in TalkBack; the UI tests'
 * accessibility checks (`:core:testing`) measure everything else, and these for their labels.
 */
const val DENSE_TARGETS_TAG = "tetto_dense_targets"
