package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.commons.findAnnotation
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.TestDouble
import ru.yandex.market.processor.testinstance.toLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class DoubleInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    @Suppress("MagicNumber")
    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return (context.parameter.findAnnotation<TestDouble>()?.value ?: 0.5).toLiteral()
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.DOUBLE || type.canonicalNameOrNull == Double::class.javaObjectType.canonicalName
    }
}