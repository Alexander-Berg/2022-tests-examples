package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.commons.findAnnotation
import ru.yandex.market.processor.testinstance.TestString
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.toStringLiteral
import javax.inject.Inject
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

class StringInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return context.parameter.run { annotationValue ?: simpleName }.toStringLiteral()
    }

    private val VariableElement.annotationValue: String? get() = findAnnotation<TestString>()?.value

    override fun isTypeSupported(type: TypeMirror) = SUPPORTED_TYPES.contains(type.canonicalNameOrNull)

    private companion object {
        private val SUPPORTED_TYPES = setOf(
            String::class.java.canonicalName,
            CharSequence::class.java.canonicalName
        )
    }
}