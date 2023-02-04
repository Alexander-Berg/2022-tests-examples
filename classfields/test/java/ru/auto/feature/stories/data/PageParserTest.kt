package ru.auto.feature.stories.data

import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.yoga.YogaNodeData
import ru.auto.data.network.yoga.PullParser
import ru.auto.feature.stories.model.Page
import ru.auto.feature.stories.model.Stack
import ru.auto.feature.stories.model.pageTag
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals

@RunWith(AllureRobolectricRunner::class)
class PageParserTest {
    @Test
    fun `should parse page with null duration`() {
        val xml = """
            <page>
            <stack/>
            </page>
        """.trimIndent()

        val parsed = PullParser.fromString(xml).parse(pageTag)

        val expected = Page(Stack(yogaData = YogaNodeData(), children = emptyList()), durationMs = null)
        assertEquals(expected, parsed)
    }

    @Test
    fun `should parse page with expected duration`() {
        val expectedDuration = 1000L
        val xml = """
            <page duration="$expectedDuration">
            <stack/>
            </page>
        """.trimIndent()

        val parsed = PullParser.fromString(xml).parse(pageTag)

        assertEquals(expectedDuration, parsed.durationMs)
    }
}
