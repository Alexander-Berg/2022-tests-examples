package ru.yandex.yandexmaps.tools.analyticsgenerator

import ru.yandex.yandexmaps.tools.analyticsgenerator.yaml.ParameterTypeParsingException
import ru.yandex.yandexmaps.tools.analyticsgenerator.yaml.YamlParser
import ru.yandex.yandexmaps.tools.analyticsgenerator.yaml.toParsedEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsMethodsParserTests {

    @Test
    fun `plain name`() = nameTest("name", "name")

    @Test
    fun `complex name`() = nameTest("application.open-by_urlscheme", "application.open-by_urlscheme")

    @Test(expected = IllegalArgumentException::class)
    fun `name with wrong symbol`() = nameTest("application:open-by-urlscheme", "application:open-by-urlscheme")

    @Test(expected = IllegalArgumentException::class)
    fun `empty name`() = nameTest("", "")

    @Test
    fun `single boolean parameter`() = parametersTest(
        """
            |    first_time:
            |      type: Bool
        """.trimMargin(),
        listOf(Parameter("first_time", ParameterType.Boolean))
    )

    @Test
    fun `single int parameter`() = parametersTest(
        """
            |    param_name:
            |      type: Int
        """.trimMargin(),
        listOf(Parameter("param_name", ParameterType.Int))
    )

    @Test
    fun `single string parameter`() = parametersTest(
        """
            |    param_name:
            |      type: String
        """.trimMargin(),
        listOf(Parameter("param_name", ParameterType.String))
    )

    @Test
    fun `single double parameter`() = parametersTest(
        """
            |    param_name:
            |      type: Double
        """.trimMargin(),
        listOf(Parameter("param_name", ParameterType.Double))
    )

    @Test
    fun `single float parameter`() = parametersTest(
        """
            |    param_name:
            |      type: Float
        """.trimMargin(),
        listOf(Parameter("param_name", ParameterType.Float))
    )

    @Test
    fun `dictionary parameter`() = parametersDictionaryTest(
        """
            |    dictionary:
            |      type: Dictionary
        """.trimMargin(),
        Parameters.Dictionary
    )

    @Test(expected = ParameterTypeParsingException::class)
    fun `single unknown parameter`() = parametersTest(
        """
            |    param_name:
            |      type: UnknownType
        """.trimMargin(),
        null,
    )

    @Test
    fun `parameter name with dash`() = parametersTest(
        """
            |    param-name:
            |      type: Float
        """.trimMargin(),
        listOf(Parameter("param-name", ParameterType.Float))
    )

    @Test
    fun `enum parameter`() = parametersTest(
        """
            |    mode:
            |      type: "Enum (all, nothing)"
        """.trimMargin(),
        listOf(Parameter("mode", ParameterType.Enum(listOf("all", "nothing"))))
    )

    @Test(expected = IllegalArgumentException::class)
    fun `enum with wrong symbol`() = parametersTest(
        """
            |    mode:
            |      type: "Enum (all, no thing)"
        """.trimMargin(),
        null
    )

    @Test
    fun `many parameters`() = parametersTest(
        """
            |    source:
            |     type: "Enum (place-card, snippet)"
            |    reqid:
            |     type: String
            |    search_number:
            |     type: Int
            |    logId:
            |     type: String
            |    type:
            |     type: "Enum (direct, orgdirect)"
        """.trimMargin(),
        listOf(
            Parameter("source", ParameterType.Enum(listOf("place-card", "snippet"))),
            Parameter("reqid", ParameterType.String),
            Parameter("search_number", ParameterType.Int),
            Parameter("logId", ParameterType.String),
            Parameter("type", ParameterType.Enum(listOf("direct", "orgdirect")))
        )
    )

    private fun nameTest(raw: String, expectation: String) = performTest(
        "$raw:",
        ParsedEvent(expectation, parameters(), "")
    )

    private fun parametersDictionaryTest(raw: String, expectation: Parameters.Dictionary?) = performTest(
        "test:\n  parameters:\n$raw",
        expectation?.let { ParsedEvent("test", it, "") }
    )

    private fun parametersTest(raw: String, expectation: List<Parameter>?) = performTest(
        "test:\n  parameters:\n$raw",
        expectation?.let { ParsedEvent("test", parameters(it), "") }
    )

    private fun performTest(raw: String, expectation: ParsedEvent?) {
        assertEquals(expectation, YamlParser.parse(raw).toParsedEvents().single())
    }
}
