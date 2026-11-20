package com.nova.studio

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nova.studio.ui.IdeScreen
import com.nova.studio.ui.NovaColors

class MainActivity : ComponentActivity() {

    // configChanges в манифесте: активити не пересоздаётся при повороте, состояние живёт здесь.
    private val state = EditorState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NovaColors.accent,
                    background = NovaColors.bg,
                    surface = NovaColors.panel,
                    onSurface = NovaColors.text,
                    onBackground = NovaColors.text
                )
            ) {
                IdeScreen(state)
            }
        }
    }
}
