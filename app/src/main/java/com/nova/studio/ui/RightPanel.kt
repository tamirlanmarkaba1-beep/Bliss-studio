package com.nova.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.engine.MeshType
import com.nova.studio.EditorState
import com.nova.studio.InspectorTab
import com.nova.studio.gui.Anchor
import com.nova.studio.gui.GuiType

@Composable
fun InspectorPanel(state: EditorState, modifier: Modifier = Modifier) {
    Column(modifier.background(NovaColors.panel)) {
        Row {
            TabLabel("Объект", state.inspectorTab == InspectorTab.OBJECT) { state.inspectorTab = InspectorTab.OBJECT }
            TabLabel("Свет", state.inspectorTab == InspectorTab.LIGHT) { state.inspectorTab = InspectorTab.LIGHT }
            TabLabel("GUI", state.inspectorTab == InspectorTab.GUI) { state.inspectorTab = InspectorTab.GUI }
        }
        HDivider()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
            when (state.inspectorTab) {
                InspectorTab.OBJECT -> ObjectInspector(state)
                InspectorTab.LIGHT -> LightInspector(state)
                InspectorTab.GUI -> GuiInspector(state)
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = NovaColors.dim, fontSize = 11.sp)
}

@Composable
private fun ObjectInspector(state: EditorState) {
    val o = state.scene.selected
    if (o == null) {
        Hint("Выберите объект во вкладке «Сцена».")
        return
    }
    Text("${o.mesh.label} • #${o.id}", color = NovaColors.dim, fontSize = 10.sp)
    MiniTextField(o.name, { o.name = it }, Modifier.padding(top = 4.dp))

    SectionTitle("Позиция")
    PropSlider("X", o.x, -12f..12f) { o.x = it }
    PropSlider("Y", o.y, -2f..10f) { o.y = it }
    PropSlider("Z", o.z, -12f..12f) { o.z = it }
    PropSlider("Поворот Y°", o.rotY, 0f..360f) { o.rotY = it }

    SectionTitle("Размер")
    val maxS = if (o.mesh == MeshType.PLANE) 60f else 8f
    PropSlider("X", o.sx, 0.1f..maxS) { o.sx = it }
    if (o.mesh != MeshType.PLANE) PropSlider("Y", o.sy, 0.1f..maxS) { o.sy = it }
    PropSlider("Z", o.sz, 0.1f..maxS) { o.sz = it }

    SectionTitle("Материал")
    PropSlider("R", o.r, 0f..1f) { o.r = it }
    PropSlider("G", o.g, 0f..1f) { o.g = it }
    PropSlider("B", o.b, 0f..1f) { o.b = it }
    ToggleRow("Шахматная текстура", o.checker) { o.checker = it }

    SectionTitle("Тени и анимация")
    ToggleRow("Отбрасывает тень", o.castShadow) { o.castShadow = it }
    ToggleRow("Принимает тень", o.receiveShadow) { o.receiveShadow = it }
    PropSlider("Вращение °/с", o.spin, -180f..180f) { o.spin = it }

    SectionTitle("Физика (Этап 2)")
    Hint("RigidBody, коллайдер, гравитация.")
}

@Composable
private fun LightInspector(state: EditorState) {
    val l = state.scene.light
    SectionTitle("Направленный свет")
    PropSlider("Азимут °", l.azimuth, 0f..360f) { l.azimuth = it }
    PropSlider("Высота °", l.elevation, 5f..85f) { l.elevation = it }
    PropSlider("Яркость", l.intensity, 0f..2f) { l.intensity = it }
    PropSlider("Окружающий свет", l.ambient, 0f..1f) { l.ambient = it }
    ToggleRow("Динамические тени", l.shadowsEnabled) { l.shadowsEnabled = it }
    SectionTitle("Камера")
    PropSlider("Дистанция", state.scene.camDist, 3f..30f) { state.scene.camDist = it }
    Hint("Жест в 3D-окне: один палец — орбита, щипок — зум.")
}

private val presetColors = listOf(
    0xFF3574F0, 0xFF5FB865, 0xFFF75464, 0xFFF2C55C, 0xFF9B59B6, 0xFFFFFFFF, 0xFF000000
)

@Composable
private fun GuiInspector(state: EditorState) {
    val doc = state.gui
    val el = doc.selected
    if (el == null) {
        Hint("Выберите элемент HUD во вьюпорте (режим «Правка GUI») или в списке «GUI».")
        return
    }
    Text("${el.type.label} • ${el.id}", color = NovaColors.text, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)

    SectionTitle("Привязка к краю")
    for (row in Anchor.entries.chunked(3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 3.dp)) {
            for (a in row) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (el.anchor == a) NovaColors.accent else NovaColors.control)
                        .clickable { doc.setAnchor(el, a) }
                )
            }
        }
    }

    SectionTitle("Позиция и размер (dp)")
    PropSlider("Смещение X", el.x, -300f..300f) { el.x = it }
    PropSlider("Смещение Y", el.y, -200f..200f) { el.y = it }
    PropSlider("Ширина", el.w, 16f..400f) { el.w = it }
    PropSlider("Высота", el.h, 8f..300f) { el.h = it }
    if (el.type in listOf(GuiType.BUTTON, GuiType.SHAPE, GuiType.ICON, GuiType.HEALTH_BAR)) {
        PropSlider("Скругление", el.radius, 0f..100f) { el.radius = it }
    }
    if (el.type == GuiType.HEALTH_BAR) PropSlider("Значение", el.value, 0f..1f) { el.value = it }

    if (el.type in listOf(GuiType.BUTTON, GuiType.ICON, GuiType.TEXT)) {
        SectionTitle("Текст")
        MiniTextField(el.text, { el.text = it })
    }

    SectionTitle("Цвет")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (c in presetColors) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(11.dp))
                    .background(Color(c.toInt()))
                    .clickable { el.color = c }
            )
        }
    }

    Chip(
        "Удалить элемент", { doc.remove(el) },
        Modifier.padding(top = 14.dp).fillMaxWidth(), active = true, color = NovaColors.red
    )
}
