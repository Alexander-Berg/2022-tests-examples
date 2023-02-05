package ru.yandex.disk.feed

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.test.TestCase2

class PhotoSelectionBlocksLoaderTest : TestCase2() {

    @Test
    fun `should distribute equal elements in list`() {
        assertThat("".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(emptyList()))
        assertThat("A".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A')))
        assertThat("ZABCD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('Z', 'A', 'B')))
        assertThat("ABCDDD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'C')))

        assertThat("AY".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'Y')))
        assertThat("AYD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'Y', 'D')))
        assertThat("AYDD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'Y', 'D')))
        assertThat("AABCD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'C')))

        assertThat("ABCD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'C')))
        assertThat("ABCDD".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'C')))

        assertThat("BAABBB".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('B', 'A', 'B')))
        assertThat("AAABBB".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'A')))
        assertThat("AAAABB".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'A')))
        assertThat("AAAAAB".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'B', 'A')))
        assertThat("BAAAAA".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('B', 'A', 'A')))
        assertThat("AAAAAA".toCharList().takeFirstWithoutSequences(3) { it }, equalTo(listOf('A', 'A', 'A')))

        assertThat("AABB".toCharList().takeFirstWithoutSequences(4) { it }, equalTo(listOf('A', 'B', 'A', 'B')))
        assertThat("AAABBBCCCDDDEEE".toCharList().takeFirstWithoutSequences(10) { it }, equalTo(listOf('A', 'B', 'C', 'D', 'E', 'A', 'A', 'B', 'B', 'C')))
        assertThat("ABCDE".toCharList().takeFirstWithoutSequences(10) { it }, equalTo(listOf('A', 'B', 'C', 'D', 'E')))
        assertThat("AB".toCharList().takeFirstWithoutSequences(1) { it }, equalTo(listOf('A')))
        assertThat("AB".toCharList().takeFirstWithoutSequences(0) { it }, equalTo(emptyList()))
        assertThat("ABCDE".toCharList().takeFirstWithoutSequences(0) { it }, equalTo(emptyList()))
        assertThat("ABCDE".toCharList().takeFirstWithoutSequences(0) { it }, equalTo(emptyList()))
    }

}

private fun String.toCharList(): List<Char> = this.split("").filter { it != "" }.map { it[0] }
