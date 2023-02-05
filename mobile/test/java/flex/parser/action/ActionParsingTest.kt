package flex.parser.action

import flex.core.model.Action
import flex.debug.DebugAction
import flex.parser.DocumentParserFactory
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ActionParsingTest {

    private val issues = mock<ActionParserIssueHandler>()

    private val parser by lazy {
        DocumentParserFactory(
            actionSerializerSelector = ActionSerializerSelectorImpl(),
            actionParserIssueHandler = issues,
            sectionSerializerSelector = mock(),
            sectionParserIssueHandler = mock()
        ).getParser()
    }

    @Test
    fun `Parsing of correct supported action`() {
        val expected = SingleActionContainer(TestAction())
        val actual = parser.decodeFromString<SingleActionContainer>(
            """
                {
                    "action": {
                        "type": "TestAction"
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of correct supported actions`() {
        val expected = MultipleActionContainer(listOf(TestAction()))
        val actual = parser.decodeFromString<MultipleActionContainer>(
            """
                {
                    "actions": [{
                        "type": "TestAction"
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of correct supported action with properties`() {
        val expected = SingleActionContainer(TestAction(boolean = true, number = 43))
        val actual = parser.decodeFromString<SingleActionContainer>(
            """
                {
                    "action": {
                        "type": "TestAction",
                        "number": 43,
                        "boolean": true
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of correct supported actions with properties`() {
        val expected = MultipleActionContainer(listOf(TestAction(boolean = true, number = 43)))
        val actual = parser.decodeFromString<MultipleActionContainer>(
            """
                {
                    "actions": [{
                        "type": "TestAction",
                        "number": 43,
                        "boolean": true
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of incorrect supported action`() {
        val actual = parser.decodeFromString<SingleActionContainer>(
            """
                {
                    "action": {
                        "type": "TestAction",
                        "number": true,
                        "boolean": true
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.action }
            .extracting { action -> action as DebugAction }
            .apply {
                extracting { action -> action.message }
                    .isEqualTo("Произошла ошибка во время парсинга действия типа 'TestAction' (неизвестная ошибка)")
                extracting { action -> action.details }
                    .isEqualTo("""{"type":"TestAction","number":true,"boolean":true}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test
    fun `Parsing of incorrect supported actions`() {
        val actual = parser.decodeFromString<MultipleActionContainer>(
            """
                {
                    "actions": [{
                        "id": "1234214",
                        "type": "TestAction",
                        "number": true,
                        "boolean": true
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.actions.first() }
            .extracting { action -> action as DebugAction }
            .apply {
                extracting { action -> action.message }
                    .isEqualTo("Произошла ошибка во время парсинга действия типа 'TestAction' (неизвестная ошибка)")
                extracting { action -> action.details }
                    .isEqualTo("""{"id":"1234214","type":"TestAction","number":true,"boolean":true}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test
    fun `Parsing of unsupported action`() {
        val actual = parser.decodeFromString<SingleActionContainer>(
            """
                {
                    "action": {
                        "type": "some-type"
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.action }
            .extracting { action -> action as DebugAction }
            .apply {
                extracting { action -> action.message }
                    .isEqualTo("Не удалось распарсить действие типа 'some-type': тип действия не поддержан в приложении")
                extracting { action -> action.details }
                    .isEqualTo("""{"type":"some-type"}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test
    fun `Parsing of unsupported actions`() {
        val actual = parser.decodeFromString<MultipleActionContainer>(
            """
                {
                    "actions": [{
                        "type": "some-type"
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.actions.first() }
            .extracting { action -> action as DebugAction }
            .apply {
                extracting { action -> action.message }
                    .isEqualTo("Не удалось распарсить действие типа 'some-type': тип действия не поддержан в приложении")
                extracting { action -> action.details }
                    .isEqualTo("""{"type":"some-type"}""")
            }
        verify(issues, times(1)).handle(any())
    }

    private class ActionSerializerSelectorImpl : ActionSerializerSelector {
        override fun select(typeName: String): KSerializer<out Action>? {
            return when (typeName) {
                "TestAction" -> TestAction.serializer()
                else -> null
            }
        }
    }

    @Serializable
    private data class SingleActionContainer(
        val action: @Contextual Action
    )

    @Serializable
    private data class MultipleActionContainer(
        val actions: List<@Contextual Action>
    )

    @Serializable
    private data class TestAction(
        val string: String = "test-string",
        val number: Int = 42,
        val boolean: Boolean = false,
        val optionalString: String? = null
    ) : Action()
}
