package com.waveyy.app.ui.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Seek/value slider. Ports `components/ui/slider.jsx`.
 *
 * @param value current value in [valueRange].
 * @param onValueChange change handler.
 * @param modifier layout modifier.
 * @param valueRange bounds, defaults to 0..1.
 * @param enabled enabled flag.
 * @param activeTrackColor optional accent for the active track and thumb
 *   (defaults to the theme primary when null).
 */
@Composable
fun WaveyySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    activeTrackColor: Color? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        enabled = enabled,
        colors = activeTrackColor?.let {
            SliderDefaults.colors(activeTrackColor = it, thumbColor = it)
        } ?: SliderDefaults.colors()
    )
}
