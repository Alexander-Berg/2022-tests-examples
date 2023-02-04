package ru.auto.test

import android.app.Application
import ru.auto.test.common.di.ComponentManager
import ru.auto.test.common.di.ComponentManagerProvider

class TestAppApplication : Application(), ComponentManagerProvider {

    override val componentManager: ComponentManager by lazy { ComponentManager(this) }

}
