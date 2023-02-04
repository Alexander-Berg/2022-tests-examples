@file:kotlin.jvm.JvmName("RxTestExtKt")

package ru.auto.ara.util

import rx.observers.AssertableSubscriber


fun <T> AssertableSubscriber<T>.assertValuesNoErrors(vararg values: T): AssertableSubscriber<T> = this
    .assertNoErrors()
    .assertValues(*values)
