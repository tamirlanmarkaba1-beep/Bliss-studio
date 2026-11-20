# Nova Studio — Этап 1

Мобильная IDE + свой лёгкий 3D-движок (NovaEngine) на Kotlin / Jetpack Compose / OpenGL ES 3.0.
Только альбомная ориентация, тёмная тема.

## Сборка
Откройте папку в Android Studio (Ladybug+). Gradle wrapper подтянется по `gradle/wrapper/gradle-wrapper.properties`
(если нет gradle-wrapper.jar — Studio предложит скачать, либо выполните `gradle wrapper` один раз).
minSdk 26, compileSdk 35, AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.10.01. Нужно устройство/эмулятор с GLES 3.0.

## Архитектура
```
com.nova.engine            — движок (без зависимостей от UI, кроме Compose State для полей сцены)
  Scene.kt                 Scene / SceneObject / Light / орбитальная камера, toJson()
  Mesh.kt                  VAO/VBO/EBO, генераторы куб / плоскость / сфера
  Shaders.kt               GLSL ES 3.00: depth-pass и основной (Blinn-Phong + PCF 3x3)
  ShadowMap.kt             FBO + DEPTH_COMPONENT24, GL_COMPARE_REF_TO_TEXTURE
  SceneRenderer.kt         GLSurfaceView.Renderer: проход 1 (тень) → проход 2 (сцена)

com.nova.studio
  MainActivity.kt          иммерсивный landscape, держит EditorState
  EditorState.kt           единое состояние IDE, логи, команды терминала, ZIP импорт/экспорт
  gui/GuiModel.kt          модель HUD (элементы, 9 привязок), JSON, генератор Kotlin DSL
  gui/GuiEditor.kt         оверлей поверх 3D: drag, привязка к краям, живой джойстик
  ui/IdeScreen.kt          раскладка: TopBar | Left | Center | Inspector
  ui/LeftPanel.kt          Файлы / Сцена / GUI-палитра
  ui/CenterPanel.kt        3D-вьюпорт + HUD, редактор кода, терминал
  ui/RightPanel.kt         Инспектор: объект, свет, GUI-элемент
  ui/CodeEditor.kt         редактор с подсветкой (JSON, Kotlin, Lua)
  ui/Terminal.kt           лог + команды (help, clear, scene, gui, build)
  ui/Widgets.kt, Theme.kt  компактные контролы и цвета
```

## Синхронизация режимов GUI
- Визуальный → код: `snapshotFlow { gui.toJson() }` пересобирает `hud.json` и `hud.kt` при любой правке.
- Код → визуальный: правите `hud.json`, нажимаете «Применить к GUI». Ошибки показываются под редактором и в терминале.
- `hud.kt` (Kotlin DSL) пока генерируется только для чтения; парсинг DSL — Этап 2.

## Этап 2 (по команде)
Физика (гравитация, коллайдеры, RigidBody, Raycast), скрипты Lua, импорт .obj/.gltf, текстуры и иконки,
постобработка бодикамеры (fisheye, виньетка, шум), сборка APK, импорт scene.json.
