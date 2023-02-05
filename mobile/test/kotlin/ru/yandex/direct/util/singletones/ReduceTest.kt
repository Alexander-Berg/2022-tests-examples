// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util.singletones

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class ReduceTest {
    @Test
    fun reduce_withoutStartingValue_shouldWorkCorrectly_inGenericCase() {
        assertThat(CollectionUtils.reduce(listOf(1, 2, 3), { l, r -> l + r })).isEqualTo(6)
    }

    @Test
    fun reduce_withoutStartingValue_shouldReturnFirstElement_forSingleton() {
        assertThat(CollectionUtils.reduce(listOf(1), { l, r -> l + r })).isEqualTo(1)
    }

    @Test
    fun reduce_withoutStartingValue_shouldThrow_forEmpty() {
        assertThatThrownBy { CollectionUtils.reduce(emptyList<Int>(), { l, r -> l + r }) }
                .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun reduce_withStartingValue_shouldWorkCorrectly_inGenericCase() {
        assertThat(CollectionUtils.reduce(10, listOf(1, 2, 3), { l, r -> l + r })).isEqualTo(16)
    }

    @Test
    fun reduce_withStartingValue_shouldReduce_forSingleton() {
        assertThat(CollectionUtils.reduce(1, listOf(1), { l, r -> l + r })).isEqualTo(2)
    }

    @Test
    fun reduce_withStartingValue_shouldReturnStartingValue_forEmpty() {
        assertThat(CollectionUtils.reduce(1, emptyList<Int>(), { l, r -> l + r })).isEqualTo(1)
    }
}