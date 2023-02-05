package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.commons.findAnnotation
import ru.yandex.market.processor.testinstance.TestFloat
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.toFloatLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class FloatInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    @Suppress("MagicNumber")
    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return (context.parameter.findAnnotation<TestFloat>()?.value ?: 42.0f).toFloatLiteral()
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.FLOAT || type.canonicalNameOrNull == Float::class.javaObjectType.canonicalName
    }
}