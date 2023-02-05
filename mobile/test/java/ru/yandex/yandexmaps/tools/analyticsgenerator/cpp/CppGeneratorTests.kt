package ru.yandex.yandexmaps.tools.analyticsgenerator.cpp

import ru.yandex.yandexmaps.tools.analyticsgenerator.Parameter
import ru.yandex.yandexmaps.tools.analyticsgenerator.ParameterType
import kotlin.test.Test
import kotlin.test.assertEquals

class CppGeneratorTests {

    @Test
    fun `enum generate test`() {
        val enumParameter = Parameter(name = "parameter", type = ParameterType.Enum("one,two,three".split(",")))
        val resultText = CppEnumCodeGenerator.run { StringBuilder().apply { appendEnumDefinition(enumParameter, "enum") }.toString() }
        val expectedText = """
        enum class EnumParameter {
            One,
            Two,
            Three,
        };
        
        
        """.trimIndent()

        assertEquals(expectedText, resultText)
    }
}
