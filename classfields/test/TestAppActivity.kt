package ru.auto.test

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import ru.auto.core_ui.compose.theme.AutoTheme
import ru.auto.test.common.di.componentManager
import ru.auto.test.common.ui.LocalComponentManager

class TestAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        setContent {
            CompositionLocalProvider(LocalComponentManager provides requireNotNull(componentManager)) {
                AutoTheme { TestAppNavGraph() }
            }
        }
    }
}
