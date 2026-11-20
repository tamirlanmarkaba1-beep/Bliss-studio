package com.nova.studio.gui

import android.graphics.Color as AColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GuiType(val label: String, val dsl: String) {
    BUTTON("Кнопка", "button"),
    SHAPE("Фигура", "shape"),
    ICON("Иконка", "icon"),
    TEXT("Текст", "text"),
    HEALTH_BAR("Полоса здоровья", "healthBar"),
    CROSSHAIR("Прицел", "crosshair"),
    JOYSTICK("Джойстик", "joystick")
}

/** Привязка к краю/центру экрана: ax, ay ∈ {0, 0.5, 1}. */
enum class Anchor(val ax: Float, val ay: Float) {
    TOP_LEFT(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

    companion object {
        fun of(ax: Float, ay: Float): Anchor = entries.first { it.ax == ax && it.ay == ay }
    }
}

/**
 * Элемент HUD. Позиция левого верхнего угла (dp) = anchor * (контейнер - размер) + (x, y).
 * Поэтому при повороте/смене размера экрана элемент остаётся привязанным к своему краю.
 */
class GuiElement(val id: String, val type: GuiType) {
    var anchor by mutableStateOf(Anchor.CENTER)
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var w by mutableFloatStateOf(80f)
    var h by mutableFloatStateOf(80f)
    var text by mutableStateOf("")
    var color by mutableLongStateOf(0xFF3574F0)
    var radius by mutableFloatStateOf(12f)
    var value by mutableFloatStateOf(1f)
}

object GuiDefaults {
    const val MARGIN = 12f

    fun apply(e: GuiElement) {
        when (e.type) {
            GuiType.BUTTON -> { e.w = 96f; e.h = 48f; e.text = "Кнопка"; e.color = 0xFF3574F0; e.radius = 14f; e.anchor = Anchor.BOTTOM_RIGHT; e.x = -MARGIN; e.y = -MARGIN }
            GuiType.SHAPE -> { e.w = 80f; e.h = 80f; e.color = 0xFF9B59B6; e.radius = 20f; e.anchor = Anchor.CENTER }
            GuiType.ICON -> { e.w = 52f; e.h = 52f; e.text = "★"; e.color = 0xFFF2C55C; e.radius = 26f; e.anchor = Anchor.TOP_RIGHT; e.x = -MARGIN; e.y = MARGIN }
            GuiType.TEXT -> { e.w = 140f; e.h = 28f; e.text = "Текст"; e.color = 0xFFFFFFFF; e.anchor = Anchor.TOP_CENTER; e.y = MARGIN }
            GuiType.HEALTH_BAR -> { e.w = 180f; e.h = 16f; e.color = 0xFF5FB865; e.radius = 8f; e.value = 0.75f; e.anchor = Anchor.TOP_LEFT; e.x = MARGIN; e.y = MARGIN }
            GuiType.CROSSHAIR -> { e.w = 36f; e.h = 36f; e.color = 0xFFFFFFFF; e.anchor = Anchor.CENTER }
            GuiType.JOYSTICK -> { e.w = 110f; e.h = 110f; e.color = 0xFFFFFFFF; e.anchor = Anchor.BOTTOM_LEFT; e.x = MARGIN + 8f; e.y = -(MARGIN + 8f) }
        }
    }
}

class GuiDocument {
    val elements = mutableStateListOf<GuiElement>()
    var selected by mutableStateOf<GuiElement?>(null)

    /** Размер вьюпорта в dp — выставляет оверлей, нужен для пересчёта привязок. */
    var viewW = 640f
    var viewH = 360f

    private var counter = 1

    private fun nextId(type: GuiType): String {
        var id: String
        do { id = "${type.dsl}_${counter++}" } while (elements.any { it.id == id })
        return id
    }

    fun add(type: GuiType): GuiElement {
        val el = GuiElement(nextId(type), type)
        GuiDefaults.apply(el)
        elements.add(el)
        selected = el
        return el
    }

    fun remove(el: GuiElement) {
        elements.remove(el)
        if (selected === el) selected = null
    }

    /** Смена привязки из инспектора без визуального «прыжка». */
    fun setAnchor(el: GuiElement, a: Anchor) {
        val left = el.anchor.ax * (viewW - el.w) + el.x
        val top = el.anchor.ay * (viewH - el.h) + el.y
        el.anchor = a
        el.x = left - a.ax * (viewW - el.w)
        el.y = top - a.ay * (viewH - el.h)
    }

    /** После перетаскивания: выбрать ближайшую из 9 привязок (по третям экрана) и прилипнуть к полю. */
    fun snapToAnchor(el: GuiElement) {
        val left = el.anchor.ax * (viewW - el.w) + el.x
        val top = el.anchor.ay * (viewH - el.h) + el.y
        val cx = left + el.w / 2f
        val cy = top + el.h / 2f
        val ax = if (cx < viewW / 3f) 0f else if (cx > viewW * 2f / 3f) 1f else 0.5f
        val ay = if (cy < viewH / 3f) 0f else if (cy > viewH * 2f / 3f) 1f else 0.5f
        el.anchor = Anchor.of(ax, ay)
        el.x = snapAxis(left - ax * (viewW - el.w), ax)
        el.y = snapAxis(top - ay * (viewH - el.h), ay)
    }

    private fun snapAxis(v: Float, a: Float): Float {
        val target = when (a) { 0f -> GuiDefaults.MARGIN; 1f -> -GuiDefaults.MARGIN; else -> 0f }
        return if (abs(v - target) < 10f) target else v
    }

    // ---------------- JSON ----------------

    private fun r1(v: Float): Double = (v * 10f).roundToInt() / 10.0
    private fun hex(c: Long): String = String.format("#%08X", c and 0xFFFFFFFFL)

    fun toJson(): String {
        val arr = JSONArray()
        for (e in elements) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("type", e.type.name)
                    .put("anchor", e.anchor.name)
                    .put("x", r1(e.x)).put("y", r1(e.y))
                    .put("w", r1(e.w)).put("h", r1(e.h))
                    .put("text", e.text)
                    .put("color", hex(e.color))
                    .put("radius", r1(e.radius))
                    .put("value", r1(e.value))
            )
        }
        return JSONObject().put("version", 1).put("elements", arr).toString(2)
    }

    /** Разбирает JSON целиком и только при успехе заменяет HUD. Возвращает число элементов. */
    fun loadJson(src: String): Result<Int> = runCatching {
        val root = JSONObject(src)
        val arr = root.getJSONArray("elements")
        val list = ArrayList<GuiElement>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val tName = o.getString("type")
            val type = GuiType.entries.firstOrNull { it.name == tName } ?: error("Неизвестный type: $tName (элемент #$i)")
            val el = GuiElement(o.optString("id", "el_$i"), type)
            GuiDefaults.apply(el)
            if (o.has("anchor")) {
                val aName = o.getString("anchor")
                el.anchor = Anchor.entries.firstOrNull { it.name == aName } ?: error("Неизвестный anchor: $aName (элемент #$i)")
            }
            el.x = o.optDouble("x", el.x.toDouble()).toFloat()
            el.y = o.optDouble("y", el.y.toDouble()).toFloat()
            el.w = o.optDouble("w", el.w.toDouble()).toFloat().coerceIn(4f, 2000f)
            el.h = o.optDouble("h", el.h.toDouble()).toFloat().coerceIn(4f, 2000f)
            el.text = o.optString("text", el.text)
            if (o.has("color")) el.color = AColor.parseColor(o.getString("color")).toLong() and 0xFFFFFFFFL
            el.radius = o.optDouble("radius", el.radius.toDouble()).toFloat()
            el.value = o.optDouble("value", el.value.toDouble()).toFloat().coerceIn(0f, 1f)
            list.add(el)
        }
        elements.clear()
        elements.addAll(list)
        selected = null
        list.size
    }

    // ---------------- Kotlin DSL (генерируется) ----------------

    private fun f(v: Float) = "${r1(v)}f"
    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")

    fun toKotlinDsl(): String = buildString {
        appendLine("// Сгенерировано визуальным редактором (только чтение).")
        appendLine("// Источник правды для правок кодом — hud.json.")
        appendLine("val hud = hud {")
        for (e in elements) {
            append("    ${e.type.dsl}(id = \"${esc(e.id)}\", anchor = Anchor.${e.anchor.name}, ")
            append("x = ${f(e.x)}, y = ${f(e.y)}, w = ${f(e.w)}, h = ${f(e.h)}")
            if (e.text.isNotEmpty()) append(", text = \"${esc(e.text)}\"")
            append(", color = 0x${String.format("%08X", e.color and 0xFFFFFFFFL)}")
            if (e.type == GuiType.BUTTON || e.type == GuiType.SHAPE || e.type == GuiType.ICON || e.type == GuiType.HEALTH_BAR) {
                append(", radius = ${f(e.radius)}")
            }
            if (e.type == GuiType.HEALTH_BAR) append(", value = ${f(e.value)}")
            appendLine(")")
        }
        appendLine("}")
    }

    companion object {
        fun sample(): GuiDocument {
            val d = GuiDocument()
            d.add(GuiType.HEALTH_BAR)
            d.add(GuiType.CROSSHAIR)
            d.add(GuiType.JOYSTICK)
            d.add(GuiType.BUTTON).text = "FIRE"
            d.add(GuiType.TEXT).text = "Счёт: 0"
            d.selected = null
            return d
        }
    }
}
