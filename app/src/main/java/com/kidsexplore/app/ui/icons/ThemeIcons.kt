package com.kidsexplore.app.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.kidsexplore.app.model.ThemeIcon
import kotlin.math.min

/**
 * Renders one of the theme icons, ported shape-for-shape from the source
 * design's inline SVGs (each drawn in a 64x64 viewBox). [accent] is the
 * theme's cardBorder color; the base shapes are always white, matching the
 * source. Sizing is entirely up to the caller's [modifier] so the glyph
 * scales with whatever container it's placed in.
 */
@Composable
fun ThemeIconGlyph(icon: ThemeIcon, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Scale to the shorter side and centre the 64x64 viewBox inside the
        // given space, so a non-square modifier crops nothing.
        val s = min(size.width, size.height) / 64f
        val originX = (size.width - 64f * s) / 2f
        val originY = (size.height - 64f * s) / 2f
        val white = Color.White

        fun offset(x: Float, y: Float) = Offset(originX + x * s, originY + y * s)
        fun circle(cx: Float, cy: Float, r: Float, color: Color) =
            drawCircle(color = color, radius = r * s, center = offset(cx, cy))
        fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
            drawOval(color = color, topLeft = offset(cx - rx, cy - ry), size = Size(2 * rx * s, 2 * ry * s))
        fun roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, color: Color) = drawRoundRect(
            color = color, topLeft = offset(x, y), size = Size(w * s, h * s),
            cornerRadius = CornerRadius(rx * s, rx * s),
        )
        fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Color) {
            val path = Path().apply {
                moveTo(offset(x1, y1).x, offset(x1, y1).y)
                lineTo(offset(x2, y2).x, offset(x2, y2).y)
                lineTo(offset(x3, y3).x, offset(x3, y3).y)
                close()
            }
            drawPath(path, color)
        }
        fun eyeDot(cx: Float, cy: Float, accentR: Float) {
            circle(cx, cy, accentR, accent)
            circle(cx + 1f, cy + 1f, accentR * 0.4f, white)
        }

        when (icon) {
            ThemeIcon.CAR -> {
                roundRect(4f, 30f, 56f, 16f, 8f, white)
                roundRect(14f, 18f, 30f, 16f, 8f, white)
                circle(16f, 48f, 8f, accent)
                circle(46f, 48f, 8f, accent)
                eyeDot(22f, 26f, 4f)
                eyeDot(34f, 26f, 4f)
            }

            ThemeIcon.DOZER -> {
                roundRect(8f, 20f, 44f, 24f, 10f, white)
                roundRect(4f, 44f, 52f, 8f, 4f, accent)
                circle(16f, 52f, 6f, accent)
                circle(48f, 52f, 6f, accent)
                eyeDot(22f, 30f, 4f)
                eyeDot(38f, 30f, 4f)
            }

            ThemeIcon.ANIMAL -> {
                circle(32f, 36f, 20f, white)
                circle(14f, 16f, 9f, white)
                circle(50f, 16f, 9f, white)
                eyeDot(24f, 34f, 4.5f)
                eyeDot(40f, 34f, 4.5f)
                ellipse(32f, 45f, 4f, 3f, accent)
            }

            ThemeIcon.DINO -> {
                ellipse(28f, 42f, 20f, 14f, white)
                circle(48f, 26f, 12f, white)
                triangle(10f, 30f, 20f, 12f, 28f, 30f, white)
                eyeDot(44f, 23f, 3.5f)
                eyeDot(54f, 23f, 3.5f)
            }

            ThemeIcon.PLANET -> {
                rotate(degrees = -20f, pivot = offset(32f, 32f)) {
                    drawOval(
                        color = white,
                        topLeft = offset(3f, 23f),
                        size = Size(58f * s, 18f * s),
                        style = Stroke(width = 5f * s),
                    )
                }
                circle(32f, 32f, 17f, white)
                eyeDot(26f, 28f, 3.5f)
                eyeDot(38f, 28f, 3.5f)
            }

            ThemeIcon.OCEAN -> {
                ellipse(28f, 32f, 22f, 16f, white)
                triangle(50f, 32f, 64f, 20f, 64f, 44f, white)
                eyeDot(18f, 28f, 4.5f)
            }

            ThemeIcon.FARM -> {
                circle(32f, 34f, 20f, white)
                circle(14f, 18f, 8f, white)
                circle(50f, 18f, 8f, white)
                ellipse(32f, 42f, 9f, 7f, accent)
                circle(28f, 42f, 1.8f, white)
                circle(36f, 42f, 1.8f, white)
                eyeDot(23f, 30f, 4f)
                eyeDot(41f, 30f, 4f)
            }

            ThemeIcon.TRAIN -> {
                roundRect(10f, 20f, 44f, 28f, 8f, white)
                roundRect(18f, 8f, 12f, 14f, 3f, white)
                circle(20f, 52f, 7f, accent)
                circle(44f, 52f, 7f, accent)
                eyeDot(24f, 30f, 4f)
                eyeDot(40f, 30f, 4f)
            }
        }
    }
}
