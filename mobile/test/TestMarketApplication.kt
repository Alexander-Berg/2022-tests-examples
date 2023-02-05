package ru.yandex.market.test

import ru.yandex.market.utils.defensiveCopy
import ru.yandex.market.utils.replaceAll
import ru.yandex.market.application.ApplicationDelegate
import ru.yandex.market.application.MarketApplication
import ru.yandex.market.mocks.State

class TestMarketApplication : MarketApplication() {

    private val statesLock = Any()
    private val initialStates = mutableListOf<State>()

    fun setInitialStates(states: List<State>) {
        synchronized(statesLock) {
            initialStates.replaceAll(states)
        }
    }

    override fun createMainDelegate(): ApplicationDelegate<out Enum<*>> {
        return TestApplicationDelegate(this, synchronized(statesLock) { initialStates.defensiveCopy() })
    }

    fun updateStates(states: List<State>) {
        (delegate as? TestApplicationDelegate)?.updateStates(states)
    }
}