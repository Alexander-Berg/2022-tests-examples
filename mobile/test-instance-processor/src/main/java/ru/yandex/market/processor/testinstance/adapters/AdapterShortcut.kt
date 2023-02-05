package ru.yandex.market.processor.testinstance.adapters

import javax.lang.model.type.TypeKind

sealed class AdapterShortcut

data class DeclaredTypeShortcut(private val clazz: Class<*>) : AdapterShortcut() {
    val canonicalName: String = clazz.canonicalName
}

data class TypeKindShortcut(val typeKind: TypeKind) : AdapterShortcut()