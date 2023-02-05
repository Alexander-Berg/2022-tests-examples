package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.testinstance.CodeBlocks
import ru.yandex.market.processor.commons.asDeclaredType
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.commons.firstTypeArgument
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class SetInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val genericArgumentType = type.asDeclaredType().firstTypeArgument
        return CodeBlocks.setOf(rootAdapter.getInstance(genericArgumentType, context))
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.emptySet
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == Set::class.java.canonicalName
}