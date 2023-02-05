package ru.yandex.yandexbus.inhouse.common.session

import java.util.concurrent.CopyOnWriteArraySet

class TestAppStateNotifier : AppStateNotifier {

    private enum class State {
        INIT,
        FOREGROUND,
        BACKGROUND
    }

    private val listeners = CopyOnWriteArraySet<AppStateNotifier.Listener>()

    private var state: State? = null

    override val isAppInForeground: Boolean
        get() = state == State.FOREGROUND

    override val createdActivitiesCount
        get() = throw NotImplementedError()

    override fun registerListener(listener: AppStateNotifier.Listener) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: AppStateNotifier.Listener) {
        listeners.add(listener)
    }

    fun onAppGoesInit() {
        state = State.BACKGROUND
        listeners.forEach { it.onAppGoesInit() }
    }

    fun onAppGoesForeground() {
        state = State.FOREGROUND
        listeners.forEach { it.onAppGoesForeground() }
    }

    fun onAppGoesBackground() {
        state = State.BACKGROUND
        listeners.forEach { it.onAppGoesBackground() }
    }
}
