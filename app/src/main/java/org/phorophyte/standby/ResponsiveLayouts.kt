package org.phorophyte.standby

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
internal fun AdaptiveTwoPane(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val horizontal = constraints.maxWidth >= constraints.maxHeight
        val paneWidth = if (horizontal) {
            max(0, (constraints.maxWidth - gap) / 2)
        } else {
            constraints.maxWidth
        }
        val paneHeight = if (horizontal) {
            constraints.maxHeight
        } else {
            max(0, (constraints.maxHeight - gap) / 2)
        }
        val paneConstraints = constraints.copy(
            minWidth = paneWidth,
            maxWidth = paneWidth,
            minHeight = paneHeight,
            maxHeight = paneHeight
        )
        val placeables = measurables.map { it.measure(paneConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val x = if (horizontal) index * (paneWidth + gap) else 0
                val y = if (horizontal) 0 else index * (paneHeight + gap)
                placeable.placeRelative(x, y)
            }
        }
    }
}
