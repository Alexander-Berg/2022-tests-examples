package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.commons.findAnnotation
import ru.yandex.market.processor.commons.isNullable
import ru.yandex.market.processor.testinstance.CodeBlocks
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.TestNull
import javax.lang.model.type.TypeMirror

abstract class AbstractInstanceAdapter(
    protected val rootAdapter: InstanceAdapter
) : InstanceAdapter {

    override fun getInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        require(isTypeSupported(type)) {
            "Тип $type не поддерживается данным провайдером!"
        }

        val isNullValueRequired = context.parameter.isNullable && context.parameter.findAnnotation<TestNull>() != null
        return if (isNullValueRequired) {
            CodeBlocks.nullLiteral
        } else {
            constructInstance(type, context)
        }
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return getInstance(type, context)
    }

    protected abstract fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ): CodeBlock
}