package com.waveyy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii ported from `tailwind.config.js` (`--radius: 0.5rem` = 8dp
 * base; `sm` 4dp, `md` 6dp, `lg` 8dp). Larger slots keep M3 defaults.
 */
val WaveyyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
