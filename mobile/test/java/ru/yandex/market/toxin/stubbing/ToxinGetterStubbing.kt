package ru.yandex.market.toxin.stubbing

import org.mockito.Mockito
import toxin.Component
import toxin.tools.GetterStubbingProvider

class ToxinGetterStubbing : GetterStubbingProvider {

    override fun stubComponentGetterParameter(
        component: Component,
        parameterClass: Class<*>
    ): Any? = Mockito.mock(parameterClass)
}