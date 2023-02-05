package ru.yandex.market.test

import ru.yandex.market.common.toxin.app.scopes.appScope
import ru.yandex.market.mocks.StateFacade
import ru.yandex.market.mocks.local.mapper.LocalStateMapper
import toxin.Component

class TestOverridesBridgeComponent : Component(appScope), TestOverridesBridge {

    override fun stateFacade(): StateFacade = auto()

    override fun localStateMapper(): LocalStateMapper = auto()
}

interface TestOverridesBridge {

    fun stateFacade(): StateFacade

    fun localStateMapper(): LocalStateMapper
}
