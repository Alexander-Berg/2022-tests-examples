package ru.yandex.yandexmaps.tools.analyticsgenerator

import org.junit.Test
import ru.yandex.yandexmaps.tools.analyticsgenerator.yaml.YamlParser

class MarkdownEndToEndTest {

    @Test
    fun `yaml to md`() {
        assertZeroDiff(
            expected = "testmetrics.md".readResource(),
            actual = MarkdownGenerator.generate(YamlParser.parse("testmetrics.yaml".readResource())),
        )
    }
}
