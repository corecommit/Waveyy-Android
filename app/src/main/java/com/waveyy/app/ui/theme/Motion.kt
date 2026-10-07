package com.waveyy.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * Motion tokens ported from `tailwind.config.js` and `app/globals.css`.
 */
object WaveyyMotion {
    /** `smooth` easing: `cubic-bezier(0.25, 0.1, 0.25, 1)`. */
    val SmoothEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** `spring` easing: `cubic-bezier(0.34, 1.56, 0.64, 1)`. */
    val SpringEasing: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Accordion expand/collapse duration. */
    const val ACCORDION_MILLIS = 200

    /** Sidebar/padding transition (`layout-padding`, 0.3s). */
    const val LAYOUT_MILLIS = 300

    /** Image un-blur reveal (`.blurz`, 0.7s). */
    const val REVEAL_MILLIS = 700

    /** Shine sweep cycle (8s loop). */
    const val SHINE_MILLIS = 8000

    /**
     * Smooth tween for layout/visibility transitions.
     *
     * @param durationMillis duration override, defaults to [LAYOUT_MILLIS].
     */
    fun <T> smooth(durationMillis: Int = LAYOUT_MILLIS) = tween<T>(
        durationMillis = durationMillis,
        easing = SmoothEasing
    )
}
