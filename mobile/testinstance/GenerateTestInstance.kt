package ru.yandex.market.processor.testinstance

@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS])
annotation class GenerateTestInstance(val jvmOverloads: JvmOverloadsMode = JvmOverloadsMode.None)

enum class JvmOverloadsMode { None, Full, NoArgOnly }