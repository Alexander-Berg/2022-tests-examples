package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.testinstance.CodeBlocks
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class ThrowableInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) :
    AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.constructorInvocation(Throwable::class.asTypeName(), emptyList())
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == Throwable::class.java.canonicalName

}