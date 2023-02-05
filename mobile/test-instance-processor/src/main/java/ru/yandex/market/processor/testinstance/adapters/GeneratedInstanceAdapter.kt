package ru.yandex.market.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.yandex.market.processor.testinstance.CodeBlocks
import ru.yandex.market.processor.commons.getTypeElement
import ru.yandex.market.processor.commons.getTypeElementOrNull
import ru.yandex.market.processor.commons.isAbstract
import ru.yandex.market.processor.testinstance.HasProcessingEnvironment
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import ru.yandex.market.processor.testinstance.SharedProcessorLogic
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class GeneratedInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
    private val processorLogic: SharedProcessorLogic,
    override val environment: ProcessingEnvironment
) : AbstractInstanceAdapter(rootAdapter), HasProcessingEnvironment {

    override fun isTypeSupported(type: TypeMirror): Boolean {
        val typeElement = type.getTypeElementOrNull() ?: return false
        return !typeElement.isAbstract && processorLogic.isTestFactoryGeneratedByUs(typeElement)
    }

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val typeElement = type.getTypeElement()
        return if (type.isSameTypeAs(context.processingClassType)) {
            val overriddenParameters = context.recursiveParameters
                .groupBy(keySelector = { it.simpleName.toString() as CharSequence }) {
                    rootAdapter.getRecursiveInstance(it.asType(), context)
                }
                .mapValues { (_, value) -> value.first() }
            processorLogic.generatedTestFactoryFor(typeElement, overriddenParameters)
        } else {
            processorLogic.generatedTestFactoryFor(typeElement)
        }
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.nullLiteral
    }
}