package ru.yandex.yandexmaps.tools.analyticsgenerator.cpp

import ru.yandex.yandexmaps.tools.analyticsgenerator.ParsedEvent
import ru.yandex.yandexmaps.tools.analyticsgenerator.parameters
import ru.yandex.yandexmaps.tools.analyticsgenerator.rootCppNamespace
import kotlin.test.Test
import kotlin.test.assertEquals

class CppNamespaceParserTests {

    @Test
    fun `parsed events to namespace test`() {
        val parsedEvents = listOf(
            ParsedEvent(
                name = "rootA.funA",
                parameters = parameters(),
                description = "",
            ),
            ParsedEvent(
                name = "rootA.rootB.funB",
                parameters = parameters(),
                description = "",
            ),
            ParsedEvent(
                name = "rootA.rootB.funC",
                parameters = parameters(),
                description = "",
            ),
            ParsedEvent(
                name = "rootA.funE",
                parameters = parameters(),
                description = "",
            ),
            ParsedEvent(
                name = "rootC.funX",
                parameters = parameters(),
                description = "",
            ),
        )
        val expectedNamespace = CppNamespace(
            name = rootCppNamespace,
            innerNamespaces = setOf(
                CppNamespace(
                    name = "rootA",
                    innerNamespaces = setOf(
                        CppNamespace(
                            name = "rootB",
                            parsedEvents = listOf(
                                ParsedEvent(
                                    name = "funB",
                                    parameters = parameters(),
                                    description = "",
                                    originalName = "rootA.rootB.funB",
                                ),
                                ParsedEvent(
                                    name = "funC",
                                    parameters = parameters(),
                                    description = "",
                                    originalName = "rootA.rootB.funC",
                                ),
                            )
                        )
                    ),
                    parsedEvents = listOf(
                        ParsedEvent(
                            name = "funA",
                            parameters = parameters(),
                            description = "",
                            originalName = "rootA.funA",
                        ),
                        ParsedEvent(
                            name = "funE",
                            parameters = parameters(),
                            description = "",
                            originalName = "rootA.funE",
                        ),
                    )
                ),
                CppNamespace(
                    name = "rootC",
                    parsedEvents = listOf(
                        ParsedEvent(
                            name = "funX",
                            parameters = parameters(),
                            description = "",
                            originalName = "rootC.funX",
                        )
                    )
                )
            )
        )

        val result = CppNamespaceParser.convertToNamespace(parsedEvents, rootCppNamespace)

        assertEquals(expected = expectedNamespace, actual = result)
    }
}
