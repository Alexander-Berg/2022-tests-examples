package ru.yandex.market.toxin.stubbing

import org.mockito.Mockito
import toxin.Component
import toxin.tools.ConstructorStubbingProvider

class ToxinConstructorStubbing : ConstructorStubbingProvider {

    override fun stubComponentConstructorParameter(
        componentClass: Class<out Component>,
        parameterClass: Class<*>
    ): Any? = Mockito.mock(parameterClass)
}