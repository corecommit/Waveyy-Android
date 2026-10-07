package com.waveyy.app.ui.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Seek/value slider. Ports `components/ui/slider.jsx`.
 *
 * @param value current value in [valueRange].
 * @param onValueChange change handler.
 * @param modifier layout modifier.
 * @param valueRange bounds, defaults to 0..1.
 * @param enabled enabled flag.
 */
@Composable
fun WaveyySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        enabled = enabled
    )
}
