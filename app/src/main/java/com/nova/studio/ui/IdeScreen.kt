package com.nova.studio.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.studio.CenterTab
import com.nova.studio.EditorState
import com.nova.studio.LogLevel

@Composable
fun IdeScreen(state: EditorState) {
    val context = LocalContext.current

    // Визуальный редактор → код: любое изменение HUD пересобирает hud.json и hud.kt.
    LaunchedEffect(state) {
        snapshotFlow { state.gui.toJson() }.collect { json ->
            state.files["hud.json"] = json
            state.files["hud.kt"] = state.gui.toKotlinDsl()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) state.exportZip(context, uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) state.importZip(context, uri) }

    Column(
        Modifier.fillMaxSize().background(NovaColors.bg).windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        TopBar(
            state,
            onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            onExport = { exportLauncher.launch("nova_project.zip") }
        )
        HDivider()
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (state.leftOpen) {
                LeftPanel(state, Modifier.width(176.dp).fillMaxSize())
                VDivider()
            }
            CenterPanel(state, Modifier.weight(1f).fillMaxSize())
            if (state.rightOpen) {
                VDivider()
                InspectorPanel(state, Modifier.width(210.dp).fillMaxSize())
            }
        }
    }
}

@Composable
private fun TopBar(state: EditorState, onImport: () -> Unit, onExport: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(34.dp).background(NovaColors.panel).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Chip("◧", { state.leftOpen = !state.leftOpen }, active = state.leftOpen)
        Text("Nova Studio", color = NovaColors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Этап 1", color = NovaColors.dim, fontSize = 10.sp)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("ZIP ⬇ Импорт", onImport)
                Chip("ZIP ⬆ Экспорт", onExport)
                Chip("▶ Запуск", {
                    state.centerTab = CenterTab.VIEWPORT
                    state.guiEditMode = false
                    state.log(LogLevel.INFO, "Предпросмотр: HUD в режиме игры (джойстик активен)")
                }, active = !state.guiEditMode && state.centerTab == CenterTab.VIEWPORT, color = NovaColors.green)
                Chip("🛠 APK", {
                    state.terminalOpen = true
                    state.log(LogLevel.WARN, "Сборка APK появится на Этапе 2")
                })
            }
        }
        Chip("◨", { state.rightOpen = !state.rightOpen }, active = state.rightOpen)
    }
}
