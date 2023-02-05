package ru.yandex.yandexmaps.tools.analyticsgenerator.kotlin

import ru.yandex.yandexmaps.tools.analyticsgenerator.Parameter
import ru.yandex.yandexmaps.tools.analyticsgenerator.ParameterType
import ru.yandex.yandexmaps.tools.analyticsgenerator.Parameters
import ru.yandex.yandexmaps.tools.analyticsgenerator.ParsedEvent
import ru.yandex.yandexmaps.tools.analyticsgenerator.parameters
import kotlin.test.Test

class KotlinCodeGeneratorTests {

    @Test
    fun `test empty`() = performGeneratorTest("", emptyList())

    @Test
    fun `complex name without parameters`() = performGeneratorTest(
        `complex name without parameters`,
        listOf(ParsedEvent(name = "map.add-bookmark.submit", parameters = parameters(), description = ""))
    )

    @Test
    fun `plain name without parameters with single line comment`() = performGeneratorTest(
        `plain name without parameters with single line comment`,
        listOf(ParsedEvent(name = "start", parameters = parameters(), description = singleLineComment))
    )

    @Test
    fun `plain name without parameters with multi line comment`() = performGeneratorTest(
        `plain name without parameters with multi line comment`,
        listOf(ParsedEvent(name = "start", parameters = parameters(), description = multiLineComment))
    )

    @Test(expected = IllegalArgumentException::class)
    fun `empty name`() = performGeneratorTest(
        "",
        listOf(ParsedEvent(name = "", parameters = parameters(), description = ""))
    )

    @Test(expected = IllegalArgumentException::class)
    fun `single noname parameter`() = performGeneratorTest(
        "",
        listOf(
            ParsedEvent(
                name = "map.add-bookmark.submit",
                parameters = parameters(Parameter("", ParameterType.Boolean)),
                description = ""
            )
        )
    )

    @Test
    fun `single bool parameter`() = performGeneratorTest(
        `single bool parameter`,
        listOf(
            ParsedEvent(
                name = "map.add-bookmark.submit",
                parameters = parameters(Parameter("authorized", ParameterType.Boolean)),
                description = ""
            )
        )
    )

    @Test
    fun `single int parameter`() = performGeneratorTest(
        `single int parameter`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(Parameter("parameter", ParameterType.Int)),
                description = ""
            )
        )
    )

    @Test
    fun `single double parameter`() = performGeneratorTest(
        `single double parameter`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(Parameter("parameter", ParameterType.Double)),
                description = ""
            )
        )
    )

    @Test
    fun `single float parameter`() = performGeneratorTest(
        `single float parameter`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(Parameter("parameter", ParameterType.Float)),
                description = ""
            )
        )
    )

    @Test
    fun `single string parameter`() = performGeneratorTest(
        `single string parameter`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(Parameter("parameter", ParameterType.String)),
                description = ""
            )
        )
    )

    @Test
    fun `dictionary parameter`() = performGeneratorTest(
        `dictionary parameter`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = Parameters.Dictionary,
                description = ""
            )
        )
    )

    @Test
    fun `single enum parameter`() = performGeneratorTest(
        `single enum parameter`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(
                    Parameter(
                        "application_layer_type",
                        ParameterType.Enum(
                            listOf("map", "satellite", "hybrid")
                        )
                    )
                ),
                description = ""
            )
        )
    )

    @Test
    fun `many parameters`() = performGeneratorTest(
        `many parameters`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(
                    Parameter("event_name", ParameterType.String),
                    Parameter(
                        "application_layer_type",
                        ParameterType.Enum(
                            listOf("map", "satellite", "hybrid")
                        )
                    ),
                    Parameter("amount", ParameterType.Int)
                ),
                description = ""
            )
        )
    )
}
