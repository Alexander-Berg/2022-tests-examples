package com.edadeal.android.helpers

import com.nhaarman.mockito_kotlin.whenever

class StatefulMock<T>(initial: T, methodSetter: () -> Unit, methodGetter: () -> T) {
    var value: T = initial
        private set

    init {
        whenever(methodGetter.invoke()).then { value }
        whenever(methodSetter.invoke()).then {
            value = it.arguments.first() as T
            Unit
        }
    }
}
