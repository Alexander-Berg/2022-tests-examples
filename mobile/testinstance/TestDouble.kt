package ru.yandex.market.processor.testinstance

@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER])
annotation class TestDouble(val value: Double = 0.0)
