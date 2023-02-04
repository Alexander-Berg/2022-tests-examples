package ru.auto.ara.core.rules.di

import org.junit.rules.ExternalResource
import kotlin.random.Random

object TestRandomProvider {
    var factory: () -> Int = { Random.nextInt() }
    operator fun invoke(max: Int): Int = factory()
}

class TestRandomProviderRule(var testRandomInt: () -> Int = { 0 }) : ExternalResource() {

    override fun before() {
        TestRandomProvider.factory = testRandomInt
    }

    override fun after() {
        TestRandomProvider.factory = {
            throw IllegalStateException("Access to random provider after it is disposed by the TestRandomProviderRule")
        }
    }
}
