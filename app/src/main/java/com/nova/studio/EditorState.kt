package com.nova.studio

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.engine.Scene
import com.nova.studio.gui.GuiDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class CenterTab { VIEWPORT, CODE }
enum class LeftTab { FILES, SCENE, GUI }
enum class InspectorTab { OBJECT, LIGHT, GUI }
enum class LogLevel { INFO, WARN, ERROR, CMD }

data class LogLine(val time: String, val level: LogLevel, val text: String)

/** Единое состояние IDE: сцена, HUD, файлы проекта, логи, выбранные вкладки. */
class EditorState {
    val scene = Scene.createDefault()
    val gui = GuiDocument.sample()

    val logs = mutableStateListOf<LogLine>()
    val files = mutableStateMapOf<String, String>()

    var centerTab by mutableStateOf(CenterTab.VIEWPORT)
    var leftTab by mutableStateOf(LeftTab.SCENE)
    var inspectorTab by mutableStateOf(InspectorTab.OBJECT)
    var guiEditMode by mutableStateOf(true)
    var terminalOpen by mutableStateOf(false)
    var leftOpen by mutableStateOf(true)
    var rightOpen by mutableStateOf(true)
    var openFile by mutableStateOf("hud.json")
    var codeError by mutableStateOf<String?>(null)

    private val main = Handler(Looper.getMainLooper())
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        files["scripts/main.lua"] = SAMPLE_LUA
        files["README.txt"] = "Nova Studio — проект.\nhud.json — HUD (двусторонняя синхронизация с визуальным редактором).\nhud.kt — Kotlin DSL (генерируется).\n"
        log(LogLevel.INFO, "Nova Studio 0.1 (Этап 1) запущена")
    }

    fun log(level: LogLevel, msg: String) {
        val line = LogLine(timeFmt.format(Date()), level, msg)
        val add = {
            logs.add(line)
            while (logs.size > 400) logs.removeAt(0)
        }
        if (Looper.myLooper() == Looper.getMainLooper()) add() else main.post { add() }
    }

    fun applyHudJson() {
        val src = files["hud.json"] ?: return
        gui.loadJson(src)
            .onSuccess {
                codeError = null
                log(LogLevel.INFO, "HUD: применено элементов — $it")
            }
            .onFailure {
                codeError = it.message ?: "Ошибка разбора JSON"
                log(LogLevel.ERROR, "HUD JSON: ${it.message}")
            }
    }

    fun runCommand(raw: String) {
        val cmd = raw.trim()
        if (cmd.isEmpty()) return
        log(LogLevel.CMD, "$ $cmd")
        when (cmd.lowercase()) {
            "help" -> log(LogLevel.INFO, "Команды: help, clear, scene, gui, build")
            "clear" -> logs.clear()
            "scene" -> scene.objects.forEach {
                log(LogLevel.INFO, "#${it.id} ${it.name} [${it.mesh}] pos=(%.1f, %.1f, %.1f)".format(it.x, it.y, it.z))
            }
            "gui" -> log(LogLevel.INFO, "HUD: ${gui.elements.size} элементов")
            "build" -> log(LogLevel.WARN, "Сборка APK появится на Этапе 2")
            else -> log(LogLevel.ERROR, "Неизвестная команда: $cmd (help)")
        }
    }

    fun exportZip(context: Context, uri: Uri) {
        runCatching {
            val snapshot = files.toMap()
            context.contentResolver.openOutputStream(uri)!!.use { out ->
                ZipOutputStream(out).use { z ->
                    for ((name, text) in snapshot) {
                        z.putNextEntry(ZipEntry(name))
                        z.write(text.toByteArray(Charsets.UTF_8))
                        z.closeEntry()
                    }
                    z.putNextEntry(ZipEntry("scene.json"))
                    z.write(scene.toJson().toByteArray(Charsets.UTF_8))
                    z.closeEntry()
                }
            }
        }.onSuccess { log(LogLevel.INFO, "Проект экспортирован в ZIP") }
            .onFailure { log(LogLevel.ERROR, "Экспорт ZIP: ${it.message}") }
    }

    fun importZip(context: Context, uri: Uri) {
        runCatching {
            val loaded = LinkedHashMap<String, String>()
            context.contentResolver.openInputStream(uri)!!.use { input ->
                ZipInputStream(input).use { z ->
                    var e = z.nextEntry
                    while (e != null) {
                        val n = e.name
                        val textual = n.endsWith(".json") || n.endsWith(".kt") || n.endsWith(".lua") ||
                            n.endsWith(".txt") || n.endsWith(".md")
                        if (!e.isDirectory && textual && !n.contains("..") && n.length < 200) {
                            val bytes = z.readBytes()
                            if (bytes.size <= 2_000_000) loaded[n] = bytes.toString(Charsets.UTF_8)
                        }
                        z.closeEntry()
                        e = z.nextEntry
                    }
                }
            }
            loaded
        }.onSuccess { loaded ->
            for ((n, t) in loaded) {
                if (n == "scene.json") {
                    log(LogLevel.WARN, "scene.json: импорт сцены появится на Этапе 2")
                } else files[n] = t
            }
            if (loaded.containsKey("hud.json")) applyHudJson()
            log(LogLevel.INFO, "Импортировано файлов: ${loaded.size}")
        }.onFailure { log(LogLevel.ERROR, "Импорт ZIP: ${it.message}") }
    }

    companion object {
        const val SAMPLE_LUA = """-- scripts/main.lua (запуск скриптов — Этап 2)
function onStart()
  print("Hello from Nova")
end

function onUpdate(dt)
  local cube = scene.find("Cube")
  cube.rotY = cube.rotY + 45 * dt
end
"""
    }
}
