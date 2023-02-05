package ru.yandex.market.processor.testinstance.adapters

import ru.yandex.market.processor.testinstance.CodeBlocks
import ru.yandex.market.processor.commons.canonicalNameOrNull
import ru.yandex.market.processor.testinstance.ParameterProcessingContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class BigDecimalInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext) = CodeBlocks.bigDecimalTen

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == BigDecimal::class.java.canonicalName
}