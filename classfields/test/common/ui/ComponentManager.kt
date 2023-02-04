package ru.auto.test.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import ru.auto.core_ui.compose.tea.provide
import ru.auto.core_ui.util.Disposable
import ru.auto.test.common.di.ComponentManager

val LocalComponentManager = staticCompositionLocalOf<ComponentManager> { error("ComponentManager not provided") }

@Composable
fun <T : Disposable> provide(factory: (componentManager: ComponentManager) -> T, key: String? = null): T {
    val componentManager = LocalComponentManager.current
    return provide(key) { factory(componentManager) }
}
