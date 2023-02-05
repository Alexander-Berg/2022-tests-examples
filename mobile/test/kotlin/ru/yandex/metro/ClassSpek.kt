package ru.yandex.metro

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe

abstract class ClassSpek(clazz: Class<*>, body: Suite.() -> Unit) : Spek({
    describe(clazz.name, body = body)
})
