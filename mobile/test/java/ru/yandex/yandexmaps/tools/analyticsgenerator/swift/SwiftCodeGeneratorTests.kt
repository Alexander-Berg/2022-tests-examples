package ru.yandex.yandexmaps.tools.analyticsgenerator.swift

import ru.yandex.yandexmaps.tools.analyticsgenerator.Parameter
import ru.yandex.yandexmaps.tools.analyticsgenerator.ParameterType
import ru.yandex.yandexmaps.tools.analyticsgenerator.Parameters
import ru.yandex.yandexmaps.tools.analyticsgenerator.ParsedEvent
import ru.yandex.yandexmaps.tools.analyticsgenerator.parameters
import kotlin.test.Test

class SwiftCodeGeneratorTests {

    @Test
    fun `test empty`() = performSwiftGeneratorTest("extension GenaMetricsEventTracker {\n}", emptyList())

    @Test
    fun `complex name without parameters`() = performSwiftGeneratorTest(
        `complex name without parameters`,
        listOf(ParsedEvent(name = "map.add-bookmark.submit", parameters = parameters(), description = ""))
    )

    @Test(expected = IllegalArgumentException::class)
    fun `empty name`() = performSwiftGeneratorTest(
        "",
        listOf(ParsedEvent(name = "", parameters = parameters(), description = ""))
    )

    @Test(expected = IllegalArgumentException::class)
    fun `single noname parameter`() = performSwiftGeneratorTest(
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
    fun `single bool parameter`() = performSwiftGeneratorTest(
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
    fun `single int parameter`() = performSwiftGeneratorTest(
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
    fun `single double parameter`() = performSwiftGeneratorTest(
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
    fun `single float parameter`() = performSwiftGeneratorTest(
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
    fun `single string parameter`() = performSwiftGeneratorTest(
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
    fun `dictionary parameter`() = performSwiftGeneratorTest(
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
    fun `single enum parameter`() = performSwiftGeneratorTest(
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
    fun `single enum parameter with true and false`() = performSwiftGeneratorTest(
        `single enum parameter with true and false`,
        listOf(
            ParsedEvent(
                name = "event",
                parameters = parameters(
                    Parameter(
                        "boolean_variants",
                        ParameterType.Enum(
                            listOf("true", "false")
                        )
                    )
                ),
                description = ""
            )
        )
    )

    @Test
    fun `many parameters`() = performSwiftGeneratorTest(
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
