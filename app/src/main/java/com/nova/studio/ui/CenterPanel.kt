package com.nova.studio.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nova.engine.SceneRenderer
import com.nova.studio.CenterTab
import com.nova.studio.EditorState
import com.nova.studio.LogLevel
import com.nova.studio.gui.GuiOverlay
import kotlinx.coroutines.delay

@Composable
fun CenterPanel(state: EditorState, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().background(NovaColors.panel), verticalAlignment = Alignment.CenterVertically) {
            TabLabel("3D / GUI", state.centerTab == CenterTab.VIEWPORT) { state.centerTab = CenterTab.VIEWPORT }
            TabLabel("Код", state.centerTab == CenterTab.CODE) { state.centerTab = CenterTab.CODE }
            Spacer(Modifier.weight(1f))
            Chip(
                if (state.terminalOpen) "Терминал ▾" else "Терминал ▴",
                { state.terminalOpen = !state.terminalOpen },
                Modifier.padding(end = 6.dp), active = state.terminalOpen
            )
        }
        HDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.centerTab) {
                CenterTab.VIEWPORT -> ViewportPane(state)
                CenterTab.CODE -> CodePane(state)
            }
        }
        if (state.terminalOpen) TerminalPane(state, Modifier.height(120.dp).fillMaxWidth())
    }
}

@Composable
private fun ViewportPane(state: EditorState) {
    val scene = state.scene
    val renderer = remember { SceneRenderer(scene) { isErr, msg -> state.log(if (isErr) LogLevel.ERROR else LogLevel.INFO, msg) } }
    val holder = remember { object { var view: GLSurfaceView? = null } }
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> holder.view?.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.view?.onPause()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    var fps by remember { mutableIntStateOf(0) }
    LaunchedEffect(renderer) {
        while (true) {
            delay(500)
            fps = renderer.fps
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(3)
                    setEGLConfigChooser(8, 8, 8, 8, 24, 0)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    holder.view = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Камера: один палец — орбита, щипок — зум. Элементы HUD выше по Z перехватывают свои касания.
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scene.camYaw -= pan.x * 0.4f
                        scene.camPitch = (scene.camPitch + pan.y * 0.4f).coerceIn(-10f, 85f)
                        scene.camDist = (scene.camDist / zoom).coerceIn(3f, 30f)
                    }
                }
                .pointerInput(Unit) { detectTapGestures { state.gui.selected = null } }
        )

        GuiOverlay(state.gui, state.guiEditMode, Modifier.fillMaxSize())

        Row(Modifier.align(Alignment.TopStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip(
                if (state.guiEditMode) "✎ Правка GUI" else "▶ Просмотр",
                { state.guiEditMode = !state.guiEditMode },
                active = state.guiEditMode
            )
            Chip("☀ Тени", { scene.light.shadowsEnabled = !scene.light.shadowsEnabled }, active = scene.light.shadowsEnabled)
        }
        Text(
            "$fps FPS",
            color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        )
    }
}

@Composable
private fun CodePane(state: EditorState) {
    val name = state.openFile
    val text = state.files[name] ?: ""
    val readOnly = name == "hud.kt"
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(NovaColors.panelDark).horizontalScroll(rememberScrollState())) {
            for (n in state.files.keys.sorted()) {
                TabLabel(n.substringAfterLast('/'), n == name) { state.openFile = n }
            }
        }
        CodeEditor(
            text = text,
            onChange = { state.files[name] = it },
            lang = langOf(name),
            readOnly = readOnly,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth().background(NovaColors.panel).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (name) {
                "hud.json" -> {
                    Chip("Применить к GUI", { state.applyHudJson() }, active = true)
                    val err = state.codeError
                    Text(
                        err ?: "Правки визуального редактора попадают сюда автоматически",
                        color = if (err != null) NovaColors.red else NovaColors.dim,
                        fontSize = 10.sp, maxLines = 2
                    )
                }
                "hud.kt" -> Text("Kotlin DSL генерируется из HUD (только чтение)", color = NovaColors.dim, fontSize = 10.sp)
                else -> Text(name, color = NovaColors.dim, fontSize = 10.sp)
            }
        }
    }
}
