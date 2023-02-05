// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util.singletones

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CollectionUtilsTest {
    @Test
    fun groupBy_shouldWorkCorrectly_inGeneralCase() {
        val source = listOf(1, 1, 2, 3, 1, 1, 2, 3)
        val destination = CollectionUtils.groupBy(source, { it })
        assertThat(destination).isEqualTo(mapOf(
                1 to listOf(1, 1, 1, 1),
                2 to listOf(2, 2),
                3 to listOf(3, 3)
        ))
    }

    @Test
    fun groupBy_shouldWorkCorrectly_withEmptySequence() {
        val source = emptyList<Int>()
        val destination = CollectionUtils.groupBy(source, { it })
        assertThat(destination).isEqualTo(emptyMap<Int, List<Int>>())
    }

    @Test
    fun groupBy_shouldWorkCorrectly_withValueSelector() {
        val source = listOf(
                1 to "a",
                2 to "b",
                2 to "c",
                1 to "d"
        )
        val destination = CollectionUtils.groupBy(source, { it.first }, { it.second })
        assertThat(destination).isEqualTo(mapOf(
                1 to listOf("a", "d"),
                2 to listOf("b", "c")
        ))
    }

    @Test
    fun getOrDefault_returnsValue_ifValuePresentInMap() {
        val map = mapOf(1 to 1)
        assertThat(CollectionUtils.getOrDefault(map, 1, 2)).isEqualTo(1)
    }

    @Test
    fun getOrDefault_returnsDefault_ifValueAbsentInMap() {
        val map = mapOf(1 to 1)
        assertThat(CollectionUtils.getOrDefault(map, 2, 1)).isEqualTo(1)
    }
}