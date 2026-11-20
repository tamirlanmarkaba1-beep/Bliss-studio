package com.nova.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.engine.MeshType
import com.nova.studio.CenterTab
import com.nova.studio.EditorState
import com.nova.studio.InspectorTab
import com.nova.studio.LeftTab
import com.nova.studio.LogLevel
import com.nova.studio.gui.GuiType

@Composable
fun LeftPanel(state: EditorState, modifier: Modifier = Modifier) {
    Column(modifier.background(NovaColors.panel)) {
        Row {
            TabLabel("Файлы", state.leftTab == LeftTab.FILES) { state.leftTab = LeftTab.FILES }
            TabLabel("Сцена", state.leftTab == LeftTab.SCENE) { state.leftTab = LeftTab.SCENE }
            TabLabel("GUI", state.leftTab == LeftTab.GUI) { state.leftTab = LeftTab.GUI }
        }
        HDivider()
        when (state.leftTab) {
            LeftTab.FILES -> FilesTab(state)
            LeftTab.SCENE -> SceneTab(state)
            LeftTab.GUI -> GuiPaletteTab(state)
        }
    }
}

@Composable
private fun RowItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else NovaColors.text,
        fontSize = 12.sp,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) NovaColors.accent.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun FilesTab(state: EditorState) {
    Column(Modifier.fillMaxSize()) {
        Text("📁 Проект", color = NovaColors.dim, fontSize = 11.sp, modifier = Modifier.padding(10.dp, 8.dp, 10.dp, 4.dp))
        val names = state.files.keys.sorted()
        LazyColumn(Modifier.weight(1f)) {
            items(names) { n ->
                val icon = when {
                    n.endsWith(".json") -> "{ }"
                    n.endsWith(".kt") -> "Kt"
                    n.endsWith(".lua") -> "Lua"
                    else -> "📄"
                }
                RowItem("$icon  $n", state.openFile == n && state.centerTab == CenterTab.CODE) {
                    state.openFile = n
                    state.centerTab = CenterTab.CODE
                }
            }
        }
        Text(
            "Модели .obj/.gltf, текстуры, иконки и сборка APK — Этап 2",
            color = NovaColors.dim, fontSize = 10.sp, modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun SceneTab(state: EditorState) {
    val scene = state.scene
    @Suppress("UNUSED_VARIABLE") val v = scene.version // подписка на изменения структуры
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip("+Куб", { scene.add("Cube", MeshType.CUBE); state.inspectorTab = InspectorTab.OBJECT })
            Chip("+Сфера", { scene.add("Sphere", MeshType.SPHERE); state.inspectorTab = InspectorTab.OBJECT })
            Chip("+Плоск.", { scene.add("Plane", MeshType.PLANE); state.inspectorTab = InspectorTab.OBJECT })
        }
        Text("▾ Scene", color = NovaColors.dim, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
        val objs = scene.objects.toList()
        LazyColumn(Modifier.weight(1f)) {
            items(objs) { o ->
                val icon = when (o.mesh) { MeshType.CUBE -> "▣"; MeshType.SPHERE -> "●"; MeshType.PLANE -> "▭" }
                RowItem("   $icon  ${o.name}", scene.selected === o) {
                    scene.selected = o
                    state.inspectorTab = InspectorTab.OBJECT
                }
            }
        }
        scene.selected?.let { sel ->
            Chip(
                "Удалить «${sel.name}»",
                { scene.remove(sel); state.log(LogLevel.INFO, "Удалён объект ${sel.name}") },
                Modifier.padding(8.dp),
                active = true,
                color = NovaColors.red
            )
        }
    }
}

@Composable
private fun GuiPaletteTab(state: EditorState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("ПАЛИТРА", color = NovaColors.dim, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 8.dp, 10.dp, 4.dp))
        for (pair in GuiType.entries.chunked(2)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (t in pair) {
                    Chip(t.label, {
                        state.gui.add(t)
                        state.guiEditMode = true
                        state.centerTab = CenterTab.VIEWPORT
                        state.inspectorTab = InspectorTab.GUI
                    })
                }
            }
        }
        Text("ИЕРАРХИЯ HUD", color = NovaColors.dim, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 12.dp, 10.dp, 4.dp))
        for (el in state.gui.elements) {
            RowItem("${el.type.label} • ${el.id}", state.gui.selected === el) {
                state.gui.selected = el
                state.inspectorTab = InspectorTab.GUI
                state.guiEditMode = true
            }
        }
        Text(
            "Нажмите блок — он появится по центру/у края; перетащите во вьюпорте. Отпустите у края — привяжется.",
            color = NovaColors.dim, fontSize = 10.sp, modifier = Modifier.padding(10.dp)
        )
    }
}
