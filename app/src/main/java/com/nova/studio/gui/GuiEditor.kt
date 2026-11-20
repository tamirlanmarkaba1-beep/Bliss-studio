package com.nova.studio.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.studio.ui.NovaColors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Оверлей HUD поверх 3D-вьюпорта.
 * editMode = true: перетаскивание/выбор элементов, сетка третей. false: «просмотр» (джойстик живой).
 */
@Composable
fun GuiOverlay(doc: GuiDocument, editMode: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val cw = maxWidth.value
        val ch = maxHeight.value
        SideEffect {
            doc.viewW = cw
            doc.viewH = ch
        }
        if (editMode) GuideLines(Modifier.fillMaxSize())
        for (el in doc.elements) {
            key(el) { GuiElementView(doc, el, cw, ch, editMode) }
        }
    }
}

@Composable
private fun GuideLines(modifier: Modifier) {
    Canvas(modifier) {
        val c = Color.White.copy(alpha = 0.14f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        for (k in 1..2) {
            val x = size.width * k / 3f
            val y = size.height * k / 3f
            drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f, pathEffect = dash)
            drawLine(c, Offset(0f, y), Offset(size.width, y), 1f, pathEffect = dash)
        }
    }
}

@Composable
private fun GuiElementView(doc: GuiDocument, el: GuiElement, cw: Float, ch: Float, editMode: Boolean) {
    val dens = LocalDensity.current.density
    val left = el.anchor.ax * (cw - el.w) + el.x
    val top = el.anchor.ay * (ch - el.h) + el.y
    val selected = doc.selected === el

    Box(
        Modifier
            .offset { IntOffset((left * dens).roundToInt(), (top * dens).roundToInt()) }
            .size(el.w.dp, el.h.dp)
            .pointerInput(editMode) {
                if (editMode) {
                    detectDragGestures(
                        onDragStart = { doc.selected = el },
                        onDragEnd = { doc.snapToAnchor(el) },
                        onDragCancel = { doc.snapToAnchor(el) }
                    ) { change, drag ->
                        change.consume()
                        el.x += drag.x / dens
                        el.y += drag.y / dens
                    }
                }
            }
            .pointerInput(editMode) {
                if (editMode) detectTapGestures { doc.selected = el }
            }
    ) {
        ElementContent(el, editMode)
        if (editMode) {
            val outline = if (selected) Modifier.border(2.dp, NovaColors.accent, RoundedCornerShape(4.dp))
            else Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            Box(Modifier.fillMaxSize().then(outline))
        }
    }
}

@Composable
private fun ElementContent(el: GuiElement, editMode: Boolean) {
    val color = Color(el.color.toInt())
    val shape = RoundedCornerShape(el.radius.coerceAtLeast(0f).dp)
    when (el.type) {
        GuiType.BUTTON -> Box(Modifier.fillMaxSize().clip(shape).background(color), Alignment.Center) {
            Text(el.text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (el.h * 0.34f).coerceIn(8f, 28f).sp, maxLines = 1)
        }
        GuiType.SHAPE -> Box(Modifier.fillMaxSize().clip(shape).background(color))
        GuiType.ICON -> Box(Modifier.fillMaxSize().clip(shape).background(color.copy(alpha = 0.9f)), Alignment.Center) {
            Text(el.text.ifEmpty { "★" }, color = Color.White, fontSize = (el.h * 0.5f).coerceAtLeast(8f).sp, maxLines = 1)
        }
        GuiType.TEXT -> Box(Modifier.fillMaxSize(), Alignment.CenterStart) {
            Text(el.text, color = color, fontWeight = FontWeight.Bold, fontSize = (el.h * 0.6f).coerceAtLeast(8f).sp, maxLines = 1)
        }
        GuiType.HEALTH_BAR -> Box(Modifier.fillMaxSize().clip(shape).background(Color(0x99000000))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(el.value.coerceIn(0f, 1f)).background(color))
        }
        GuiType.CROSSHAIR -> Canvas(Modifier.fillMaxSize()) {
            val c = center
            val s = size.minDimension / 2f
            val gap = s * 0.28f
            val sw = max(2f, s * 0.08f)
            drawLine(color, Offset(c.x - s, c.y), Offset(c.x - gap, c.y), sw)
            drawLine(color, Offset(c.x + gap, c.y), Offset(c.x + s, c.y), sw)
            drawLine(color, Offset(c.x, c.y - s), Offset(c.x, c.y - gap), sw)
            drawLine(color, Offset(c.x, c.y + gap), Offset(c.x, c.y + s), sw)
            drawCircle(color, sw * 0.6f, c)
        }
        GuiType.JOYSTICK -> Joystick(el, editMode)
    }
}

@Composable
private fun Joystick(el: GuiElement, editMode: Boolean) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    val color = Color(el.color.toInt())
    Canvas(
        Modifier.fillMaxSize().pointerInput(editMode) {
            if (!editMode) {
                detectDragGestures(
                    onDragEnd = { knob = Offset.Zero },
                    onDragCancel = { knob = Offset.Zero }
                ) { change, _ ->
                    change.consume()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val maxLen = size.minDimension * 0.28f
                    var d = change.position - c
                    val len = d.getDistance()
                    if (len > maxLen) d = d * (maxLen / len)
                    knob = d
                }
            }
        }
    ) {
        val r = size.minDimension / 2f
        drawCircle(color.copy(alpha = 0.18f), r)
        drawCircle(color.copy(alpha = 0.55f), r - 1.5f, style = Stroke(width = 3f))
        drawCircle(color.copy(alpha = 0.75f), r * 0.38f, center + knob)
    }
}
