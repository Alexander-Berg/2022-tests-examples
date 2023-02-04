package ru.auto.test.common.di

import android.content.Context

interface ComponentManagerProvider {
    val componentManager : ComponentManager
}

val Context.componentManager
    get() = (applicationContext as? ComponentManagerProvider)?.componentManager
