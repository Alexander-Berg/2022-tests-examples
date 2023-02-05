package flex.parser.section

import flex.core.model.Section
import flex.debug.DebugSection
import flex.parser.DocumentParserFactory
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SectionParsingTest {

    private val issues = mock<SectionParserIssueHandler>()

    private val parser by lazy {
        DocumentParserFactory(
            sectionSerializerSelector = SectionSerializerSelectorImpl(),
            sectionParserIssueHandler = issues,
            actionSerializerSelector = mock(),
            actionParserIssueHandler = mock()
        ).getParser()
    }

    @Test
    fun `Parsing of correct supported section`() {
        val expected = SingleSectionContainer(TestSection())
        val actual = parser.decodeFromString<SingleSectionContainer>(
            """
                {
                    "section": {
                        "type": "TestSection"
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of correct supported sections`() {
        val expected = MultipleSectionContainer(listOf(TestSection()))
        val actual = parser.decodeFromString<MultipleSectionContainer>(
            """
                {
                    "sections": [{
                        "type": "TestSection"
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of correct supported section with properties`() {
        val expected = SingleSectionContainer(TestSection(boolean = true, number = 43))
        val actual = parser.decodeFromString<SingleSectionContainer>(
            """
                {
                    "section": {
                        "type": "TestSection",
                        "number": 43,
                        "boolean": true
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of correct supported sections with properties`() {
        val expected = MultipleSectionContainer(listOf(TestSection(boolean = true, number = 43)))
        val actual = parser.decodeFromString<MultipleSectionContainer>(
            """
                {
                    "sections": [{
                        "type": "TestSection",
                        "number": 43,
                        "boolean": true
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Parsing of incorrect supported section`() {
        val actual = parser.decodeFromString<SingleSectionContainer>(
            """
                {
                    "section": {
                        "id": "1234214",
                        "type": "TestSection",
                        "number": true,
                        "boolean": true
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.section }
            .extracting { section -> section as DebugSection }
            .apply {
                extracting { section -> section.message }
                    .isEqualTo("Произошла ошибка во время парсинга секции типа 'TestSection' (неизвестная ошибка)")
                extracting { section -> section.details }
                    .isEqualTo("""{"id":"1234214","type":"TestSection","number":true,"boolean":true}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test
    fun `Parsing of incorrect supported sections`() {
        val actual = parser.decodeFromString<MultipleSectionContainer>(
            """
                {
                    "sections": [{
                        "id": "1234214",
                        "type": "TestSection",
                        "number": true,
                        "boolean": true
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.sections.first() }
            .extracting { section -> section as DebugSection }
            .apply {
                extracting { section -> section.message }
                    .isEqualTo("Произошла ошибка во время парсинга секции типа 'TestSection' (неизвестная ошибка)")
                extracting { section -> section.details }
                    .isEqualTo("""{"id":"1234214","type":"TestSection","number":true,"boolean":true}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test
    fun `Parsing of unsupported section`() {
        val actual = parser.decodeFromString<SingleSectionContainer>(
            """
                {
                    "section": {
                        "type": "some-type"
                    }
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.section }
            .extracting { section -> section as DebugSection }
            .apply {
                extracting { section -> section.message }
                    .isEqualTo("Не удалось распарсить секцию типа 'some-type': тип секции не поддержан в приложении")
                extracting { section -> section.details }
                    .isEqualTo("""{"type":"some-type"}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test
    fun `Parsing of unsupported sections`() {
        val actual = parser.decodeFromString<MultipleSectionContainer>(
            """
                {
                    "sections": [{
                        "type": "some-type"
                    }]
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.sections.first() }
            .extracting { section -> section as DebugSection }
            .apply {
                extracting { section -> section.message }
                    .isEqualTo("Не удалось распарсить секцию типа 'some-type': тип секции не поддержан в приложении")
                extracting { section -> section.details }
                    .isEqualTo("""{"type":"some-type"}""")
            }
        verify(issues, times(1)).handle(any())
    }

    @Test(expected = SerializationException::class)
    fun `Parsing of missing section`() {
        val actual = parser.decodeFromString<SingleSectionContainer>(
            """
                {
                    "section": null
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.section }
            .extracting { section -> section as DebugSection }
            .isEqualTo(null)
    }

    @Test
    fun `Parsing of missing section in list`() {
        val actual = parser.decodeFromString<MultipleSectionContainer>(
            """
                {
                    "sections": [null]
                }
            """.trimIndent()
        )
        assertThat(actual).extracting { container -> container.sections.first() }
            .extracting { section -> section as DebugSection }
            .apply {
                extracting { section -> section.message }
                    .isEqualTo("Произошла ошибка во время парсинга секции типа 'null' (неизвестная ошибка)")
                extracting { section -> section.details }
                    .isEqualTo("null")
            }
    }

    private class SectionSerializerSelectorImpl : SectionSerializerSelector {
        override fun select(typeName: String): KSerializer<out Section>? {
            return when (typeName) {
                "TestSection" -> TestSection.serializer()
                else -> null
            }
        }
    }

    @Serializable
    private data class SingleSectionContainer(
        val section: @Contextual Section
    )

    @Serializable
    private data class MultipleSectionContainer(
        val sections: List<@Contextual Section>
    )

    @Serializable
    private data class TestSection(
        val string: String = "test-string",
        val number: Int = 42,
        val boolean: Boolean = false,
        val optionalString: String? = null
    ) : Section() {
        override val id: String = ""
        override val reloadable: Boolean = false
    }
}
