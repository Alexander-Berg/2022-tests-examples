package ru.yandex.market.processor.testinstance

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import ru.yandex.market.processor.commons.constructors
import ru.yandex.market.processor.commons.enclosingElements
import ru.yandex.market.processor.commons.getTypeElement
import ru.yandex.market.processor.commons.isAbstract
import ru.yandex.market.processor.commons.isAnnotatedBy
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

class SharedProcessorLogic @Inject constructor(
    private val configuration: ProcessorConfiguration,
    override val environment: ProcessingEnvironment,
    private val metadataCache: KotlinMetadataCache,
) : HasProcessingEnvironment {

    private val buffer = StringBuilder()

    fun defaultTestFactoryFor(className: ClassName): CodeBlock {
        return CodeBlock.of("%T.${configuration.handWrittenMethodName}()", className)
    }

    fun generatedTestFactoryFor(
        processedClassElement: Element,
        overrideParameters: Map<CharSequence, CodeBlock> = emptyMap()
    ): CodeBlock {

        val className = processedClassElement.asType().asClassName()
        return CodeBlock.of(
            "%M(%L)",
            MemberName(className.packageName, getFactoryMethodName(processedClassElement)),
            overrideParameters.toList().joinToString(separator = ", ") { (name, value) -> "$name = $value" }
        )
    }

    fun getFactoryMethodName(processedClassElement: Element): String {
        buffer.clear()

        processedClassElement.enclosingClassNames
            .mapIndexed { index, s -> if (index == 0) s.decapitalize() else s }
            .joinTo(
                buffer = buffer,
                separator = ENCLOSING_SEPARATOR,
                postfix = configuration.methodNameSuffix
            )
        return buffer.toString()
    }

    fun getFactoryFileName(processedClassElement: Element): String {
        buffer.clear()

        processedClassElement.enclosingClassNames
            .joinTo(
                buffer = buffer,
                separator = ENCLOSING_SEPARATOR,
                postfix = configuration.fileNameSuffix
            )
        return buffer.toString()
    }

    fun getFactoryFileJvmName(processedClassElement: Element): String {
        buffer.clear()

        processedClassElement.enclosingClassNames
            .joinTo(
                buffer = buffer,
                separator = ENCLOSING_SEPARATOR,
                postfix = configuration.fileNameJvmSuffix
            )
        return buffer.toString()
    }

    fun isTestFactoryGeneratedByUs(element: Element): Boolean {
        if (element.kind != ElementKind.CLASS) {
            return false
        }
        if (element.isAnnotatedBy<GenerateTestInstance>()) {
            return true
        }
        return element.constructors.any { it.isAnnotatedBy<GenerateTestInstance>() }
    }

    private val Element.enclosingClassNames: Sequence<String>
        get() {
            return (listOf(this) + enclosingElements)
                .asReversed()
                .asSequence()
                .map { it.asType() }
                .filter { it.kind == TypeKind.DECLARED }
                .map { it.getTypeElement().simpleName.toString() }
        }

    fun containsSuitableDefaultFactoryMethod(element: TypeElement): Boolean {
        return ElementFilter.methodsIn(element.enclosedElements)
            .asSequence()
            .filter {
                it.parameters.isEmpty() &&
                        it.modifiers.containsAll(setOf(Modifier.PUBLIC, Modifier.STATIC)) &&
                        it.returnType == element.asType() &&
                        it.simpleName.contentEquals(configuration.handWrittenMethodName)
            }
            .firstOrNull() != null
    }

    fun isSealedClass(classElement: Element): Boolean {
        if (classElement !is TypeElement || !classElement.isAbstract) {
            return false
        }
        val classMetadata = classElement.getAnnotation(Metadata::class.java) ?: return false
        return metadataCache.toKmClass(classMetadata).sealedSubclasses.isNotEmpty()
    }

    @Suppress("StringLiteralDuplication")
    fun isSealedClassProcessable(classElement: TypeElement): Boolean {
        require(isSealedClass(classElement)) {
            "Класс $classElement не является sealed классом!"
        }
        return findSuitableSealedClassImplementation(classElement) != null
    }

    private fun findSuitableSealedClassImplementation(classElement: TypeElement): TypeElement? {
        return classElement.findSealedClassImplementations()
            .firstOrNull { isTestFactoryGeneratedByUs(it) || containsSuitableDefaultFactoryMethod(it) }
    }

    @Suppress("StringLiteralDuplication")
    fun generateFactoryCallForSealedClass(classElement: TypeElement): CodeBlock {
        require(isSealedClass(classElement)) {
            "Класс $classElement не является sealed классом!"
        }
        val suitableImplementation = findSuitableSealedClassImplementation(classElement)
        check(suitableImplementation != null) {
            "Не удалось найти подходящую реализацию класса ${classElement.simpleName}!"
        }
        return if (isTestFactoryGeneratedByUs(suitableImplementation)) {
            generatedTestFactoryFor(suitableImplementation, emptyMap())
        } else {
            defaultTestFactoryFor(suitableImplementation.asClassName())
        }
    }

    companion object {
        private const val ENCLOSING_SEPARATOR = "_"
    }
}