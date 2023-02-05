package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import javax.lang.model.type.TypeMirror

interface InstanceAdapter {

    fun getInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock

    fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock

    fun isTypeSupported(type: TypeMirror): Boolean
}