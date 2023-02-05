package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.testinstance.CodeBlocks
import ru.yandex.market.processor.commons.asDeclaredType
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.commons.firstTypeArgument
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

internal class ListInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.canonicalNameOrNull == List::class.java.canonicalName
                || type.canonicalNameOrNull == Collection::class.java.canonicalName
                && type.asDeclaredType().typeArguments.size == 1
    }

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ): CodeBlock {
        val genericArgumentType = type.asDeclaredType().firstTypeArgument
        return CodeBlocks.listOf(rootAdapter.getInstance(genericArgumentType, context))
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.emptyList
    }
}