package ru.yandex.market.utils

import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.instanceOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.util.LinkedList

@RunWith(Enclosed::class)
class CollectionsExtensionsTest {

    class ToArrayListTest {

        @Test
        fun `When list is already an ArrayList just return it`() {
            val list = ArrayList<String>()
            assertThat(list.toArrayList()).isSameAs(list)
        }

        @Test
        fun `When list is not an ArrayList wrap it into ArrayList`() {
            val elements = arrayOf("1", "2", "3")
            val list = LinkedList(elements.toList())
            assertThat(list.toArrayList()).`is`(
                HamcrestCondition(
                    allOf(
                        contains(*elements),
                        instanceOf(ArrayList::class.java)
                    )
                )
            )
        }
    }

    class DistinctByTest {

        @Test
        fun `Return empty list for empty iterable`() {
            assertThat(emptyList<String>().distinctBy({ it.length }, { it[0] })).isEmpty()
        }

        @Test
        fun `Return singleton list for iterable with one element`() {
            assertThat(listOf("").distinctBy({ it.length })).isEqualTo(listOf(""))
        }

        @Test
        fun `Distinct elements by passed selectors`() {
            val list = listOf(
                TestClass(),
                TestClass(),
                TestClass(s = "s"),
                TestClass(s = "s")
            )
            assertThat(list.distinctBy({ it.i }, { it.s })).isEqualTo(listOf(TestClass(), TestClass(s = "s")))
        }
    }

    class LimitElementsCountTest {

        @Test
        fun `Properly removes single element`() {
            assertThat(mutableListOf(1, 2).limitElementsCount(1)).contains(1)
        }

        @Test
        fun `Properly removes multiple elements`() {
            assertThat(mutableListOf(1, 2, 3).limitElementsCount(1)).contains(1)
        }

        @Test
        fun `Returns same instance`() {
            val list = mutableListOf(1, 2, 3)
            assertThat(list.limitElementsCount(1)).isSameAs(list)
        }

        @Test
        fun `Properly removes all elements`() {
            assertThat(mutableListOf(1, 2, 3).limitElementsCount(0)).isEmpty()
        }

        @Test(expected = IllegalArgumentException::class)
        fun `Throws exception if pass negative count`() {
            mutableListOf(1, 2, 3).limitElementsCount(-1)
        }

        @Test
        fun `Removes nothing when count is greater than list size`() {
            assertThat(mutableListOf(1, 2, 3).limitElementsCount(4)).contains(1, 2, 3)
        }
    }
}