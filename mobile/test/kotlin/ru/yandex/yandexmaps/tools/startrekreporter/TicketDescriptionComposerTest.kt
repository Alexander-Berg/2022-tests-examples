package ru.yandex.yandexmaps.tools.startrekreporter

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TicketDescriptionComposerTest {

    private lateinit var testingSubject: TicketDescriptionComposer

    @BeforeTest
    fun beforeTest() {
        testingSubject = TicketDescriptionComposer()
    }

    @Test
    fun `adds initial build information`() {
        val initial = """
            Тестовое описание.
        """.trimIndent()

        val keyToAdd = "Master Build"
        val valueToAdd = "https://master-build.god"

        val expected = """
            Тестовое описание.

            <#<build>
            <a href="$valueToAdd">$keyToAdd</a>
            </build>#>
        """.trimIndent()

        val actual = testingSubject.updateBuildEntry(initial, keyToAdd, valueToAdd)

        assertEquals(expected, actual)
    }

    @Test
    fun `adds new key to existing build table`() {
        val initial = """
            Тестовое описание.

            <#<build>
            <a href="https://master-build.god">Master Build</a>
            </build>#>
        """.trimIndent()

        val keyToAdd = "Slave Build"
        val valueToAdd = "https://slave-build.god"

        val expected = """
            Тестовое описание.

            <#<build>
            <a href="https://master-build.god">Master Build</a> |
            <a href="$valueToAdd">$keyToAdd</a>
            </build>#>
        """.trimIndent()

        val actual = testingSubject.updateBuildEntry(initial, keyToAdd, valueToAdd)

        assertEquals(expected, actual)
    }

    @Test
    fun `updates existing key in existing build table`() {
        val initial = """
            Тестовое описание.

            <#<build>
            <a href="https://master-build.god">Master Build</a>
            </build>#>
        """.trimIndent()

        val keyToAdd = "Master Build"
        val valueToAdd = "https://master-build.father"

        val expected = """
            Тестовое описание.

            <#<build>
            <a href="https://master-build.father">Master Build</a>
            </build>#>
        """.trimIndent()

        val actual = testingSubject.updateBuildEntry(initial, keyToAdd, valueToAdd)

        assertEquals(expected, actual)
    }

    @Test
    fun `sorts build keys in ascending order`() {
        val initial = """
            Тестовое описание.
        """.trimIndent()

        val pairsToAdd = listOf(
            "b" to "1",
            "c" to "2",
            "a" to "3"
        )

        val expected = """
            Тестовое описание.

            <#<build>
            <a href="3">a</a> |
            <a href="1">b</a> |
            <a href="2">c</a>
            </build>#>
        """.trimIndent()

        val actual = pairsToAdd.fold(initial) { description, pair ->
            testingSubject.updateBuildEntry(description, pair.first, pair.second)
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `removes every build information before writing new`() {
        val initial = """
            Тесто<#<build>Здесь была устаревшая инфа о билдах</build>#>вое описание.
            <#<build>Здесь поновее, но все равно устарело
            </build>#>
        """.trimIndent()

        val keyToAdd = "Master Build"
        val valueToAdd = "https://master-build.god"

        val expected = """
            Тестовое описание.

            <#<build>
            <a href="$valueToAdd">$keyToAdd</a>
            </build>#>
        """.trimIndent()

        val actual = testingSubject.updateBuildEntry(initial, keyToAdd, valueToAdd)

        assertEquals(expected, actual)
    }
}
