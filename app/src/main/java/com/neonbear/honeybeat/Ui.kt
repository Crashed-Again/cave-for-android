package com.neonbear.honeybeat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size as GSize

object Cub {
    val Black = Color(0xFF000000)
    val Panel = Color(0xFF232323)
    val Card = Color(0xFF1B1B1B)
    val Hover = Color(0xFF2A2A2A)
    val Accent = Color(0xFF3B82F6)
    val Text = Color(0xFFF2F2F2)
    val Muted = Color(0xFF8C8C8C)
    val Button = Color(0xFF3A3A3A)
    val Dot = Color(0xFF5A5A5A)
}

@Composable
fun CubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cub.Accent,
            background = Cub.Panel,
            surface = Cub.Card,
            onSurface = Cub.Text,
            onBackground = Cub.Text,
        ),
        content = content,
    )
}

/** The pixel bear used for the logo (same grid as the launcher icon). */
private val BEAR = listOf(".XX..XX.", "XXXXXXXX", "XXXXXXXX", "X.XXXX.X", "XXXXXXXX", ".XXXXXX.", "..XXXX..")

@Composable
fun BearLogo(box: Dp = 44.dp) {
    Box(
        Modifier.size(box).clip(RoundedCornerShape(10.dp)).background(Cub.Black)
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(box * 0.62f)) {
            val cell = size.width / 8f
            BEAR.forEachIndexed { r, row ->
                row.forEachIndexed { c, ch ->
                    if (ch == 'X') drawRect(
                        Color.White,
                        Offset(c * cell + cell * 0.1f, r * cell + cell * 0.1f),
                        GSize(cell * 0.8f, cell * 0.8f),
                    )
                }
            }
        }
    }
}

private val LETTERS = mapOf(
    'N' to listOf("10001", "11001", "10101", "10101", "10011", "10001", "10001"),
    'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
    'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
)

/** Dot-matrix text, like the NEONBEAR mark at the bottom of Cub. */
@Composable
fun DotText(text: String, step: Dp = 3.5.dp, color: Color = Cub.Dot) {
    val cols = text.length * 6 - 1
    Canvas(Modifier.size(step * cols, step * 7)) {
        val s = step.toPx()
        text.forEachIndexed { i, ch ->
            val g = LETTERS[ch] ?: return@forEachIndexed
            g.forEachIndexed { r, row ->
                row.forEachIndexed { c, bit ->
                    if (bit == '1') drawCircle(color, s * 0.33f, Offset((i * 6 + c + 0.5f) * s, (r + 0.5f) * s))
                }
            }
        }
    }
}

enum class G { Play, Pause, Next, Prev }

@Composable
fun Glyph(g: G, color: Color, box: Dp = 24.dp) {
    Canvas(Modifier.size(box)) {
        val w = size.width
        val h = size.height
        fun tri(a: Float, b: Float, c: Float) = Path().apply {
            moveTo(w * a, h * 0.2f); lineTo(w * b, h * 0.5f); lineTo(w * a, h * 0.8f); close()
        }
        when (g) {
            G.Play -> drawPath(tri(0.3f, 0.82f, 0f), color)
            G.Pause -> {
                drawRect(color, Offset(w * 0.27f, h * 0.2f), GSize(w * 0.16f, h * 0.6f))
                drawRect(color, Offset(w * 0.57f, h * 0.2f), GSize(w * 0.16f, h * 0.6f))
            }
            G.Next -> {
                drawPath(tri(0.16f, 0.68f, 0f), color)
                drawRect(color, Offset(w * 0.72f, h * 0.2f), GSize(w * 0.12f, h * 0.6f))
            }
            G.Prev -> {
                drawRect(color, Offset(w * 0.16f, h * 0.2f), GSize(w * 0.12f, h * 0.6f))
                drawPath(Path().apply {
                    moveTo(w * 0.84f, h * 0.2f); lineTo(w * 0.32f, h * 0.5f); lineTo(w * 0.84f, h * 0.8f); close()
                }, color)
            }
        }
    }
}

@Composable
fun CubSwitch(on: Boolean) {
    val x by animateDpAsState(if (on) 24.dp else 0.dp, tween(140), label = "knob")
    val c by animateColorAsState(if (on) Cub.Accent else Cub.Muted, tween(140), label = "knobColor")
    Box(
        Modifier.size(46.dp, 24.dp).border(1.dp, c).padding(3.dp),
    ) {
        Box(Modifier.offset(x = x).size(16.dp).background(c))
    }
}

@Composable
fun ToggleCard(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Cub.Card)
            .clickable { onChange(!on) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Cub.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Cub.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        CubSwitch(on)
    }
}

@Composable
fun CubButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (primary) Cub.Accent else Cub.Button)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun PageTitle(title: String, sub: String) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)) {
        Text(title, color = Cub.Text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text(sub, color = Cub.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
