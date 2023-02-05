package ru.yandex.market.processor.testinstance.adapters

import ru.yandex.market.processor.commons.getTypeElement
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.SharedProcessorLogic
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class SealedClassInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
    private val processorLogic: SharedProcessorLogic
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ) = processorLogic.generateFactoryCallForSealedClass(type.getTypeElement())

    override fun isTypeSupported(type: TypeMirror): Boolean {
        val classElement = type.getTypeElement()
        return processorLogic.isSealedClass(classElement) && processorLogic.isSealedClassProcessable(classElement)
    }
}