package ru.yandex.market.processor.testinstance

@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER])
annotation class TestLong(val value: Long = 42L)