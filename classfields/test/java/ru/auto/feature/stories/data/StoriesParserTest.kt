package ru.auto.feature.stories.data

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.yoga.YogaNodeData
import ru.auto.data.network.yoga.PullParser
import ru.auto.feature.stories.model.Page
import ru.auto.feature.stories.model.Stack
import ru.auto.feature.stories.model.Video
import ru.auto.feature.stories.model.storyTag
import ru.auto.test.runner.AllureRobolectricRunner


@RunWith(AllureRobolectricRunner::class)
class StoriesParserTest {
    @Test
    fun `should parse video url correctly`() {
        val url = "http://auto.ru"
        val xml = """
            <story>
                <page>
                    <stack>
                        <video url="$url"/>
                    </stack>
                </page>
            </story>
        """.trimIndent()

        val parsed = PullParser.fromString(xml).parse(storyTag)
        val expected = listOf(
            Page(
                Stack(
                    children = listOf(
                        Video(
                            url,
                            playMode = Video.PlaybackMode.ONCE,
                            cornerRadius = 0,
                            yogaData = YogaNodeData()
                        )
                    ),
                    yogaData = YogaNodeData()
                )
            )
        )
        assertEquals(expected, parsed)
    }

    @Test
    fun `should parse video corners correctly`() {
        val radius = 16
        val xml = """
            <story>
                <page>
                    <stack>
                        <video cornerRadius="$radius"/>
                    </stack>
                </page>
            </story>
        """.trimIndent()

        val parsed = PullParser.fromString(xml).parse(storyTag)
        val expected = listOf(
            Page(
                Stack(
                    children = listOf(
                        Video(
                            url = "",
                            playMode = Video.PlaybackMode.ONCE,
                            cornerRadius = radius,
                            yogaData = YogaNodeData()
                        )
                    ),
                    yogaData = YogaNodeData()
                )
            )
        )
        assertEquals(expected, parsed)
    }

    @Test
    fun `should parse video playmode correctly`() {
        val xml = """
            <story>
                <page>
                    <stack>
                        <video playMode="loop"/>
                    </stack>
                </page>
            </story>
        """.trimIndent()

        val parsed = PullParser.fromString(xml).parse(storyTag)
        val expected = listOf(
            Page(
                Stack(
                    children = listOf(
                        Video(
                            url = "",
                            playMode = Video.PlaybackMode.LOOP,
                            cornerRadius = 0,
                            yogaData = YogaNodeData()
                        )
                    ),
                    yogaData = YogaNodeData()
                )
            )
        )
        assertEquals(expected, parsed)
    }
}
