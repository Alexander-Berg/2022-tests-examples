package ru.yandex.metro

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.KCallable

abstract class CallableSpek(callable: KCallable<*>, body: Suite.() -> Unit) : Spek({
    describe(callable.name, body = body)
})
