package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.commons.asDeclaredTypeOrNull
import ru.yandex.market.processor.commons.isAnnotatedBy
import ru.yandex.market.processor.testinstance.TestRecursiveWarning
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.SharedProcessorLogic
import ru.yandex.market.processor.testinstance.asClassName
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

internal class RootInstanceAdapter @Inject constructor(
    private val adaptersRegistry: InstanceAdaptersRegistry,
    private val processorLogic: SharedProcessorLogic
) : InstanceAdapter {

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return adaptersRegistry.getProvider(type)?.getRecursiveInstance(type, context) ?: getForUnknown(type)
    }

    override fun getInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return adaptersRegistry.getProvider(type)?.let {
            if (context.parameter.isAnnotatedBy<TestRecursiveWarning>()) {
                it.getRecursiveInstance(type, context)
            } else {
                it.getInstance(type, context)
            }
        } ?: getForUnknown(type)
    }

    private fun getForUnknown(type: TypeMirror): CodeBlock {
        val declaredType = type.asDeclaredTypeOrNull()
        return if (declaredType != null) {
            processorLogic.defaultTestFactoryFor(declaredType.asClassName())
        } else {
            error("Не удалось обработать тип \"$type\"!")
        }
    }

    override fun isTypeSupported(type: TypeMirror) = true
}