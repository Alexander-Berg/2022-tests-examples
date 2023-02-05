package ru.yandex.direct.newui.base

import android.view.ViewGroup
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test

class BaseAdapterTest {

    private lateinit var testAdapter: BaseAdapter<Int>

    @Before
    fun runBeforeEachTest() {
        testAdapter = BaseTestAdapter()
    }

    @Test
    fun isLastInSectionWithEmpty() {
        assertThatThrownBy { testAdapter.isLastInSection(0) }.isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun isLastInSectionWithNoExistIndex() {
        testAdapter.add(1)
        testAdapter.add(2)
        assertThatThrownBy { testAdapter.isLastInSection(3) }.isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun isLastInSectionInSingleListForLastElement() {
        testAdapter.add(1)
        assertThat(testAdapter.isLastInSection(0)).isEqualTo(true)
    }

    @Test
    fun isLastInSectionForMiddleElement() {
        testAdapter.add(1)
        testAdapter.add(2)
        testAdapter.add(3)
        testAdapter.add(4)
        assertThat(testAdapter.isLastInSection(2)).isEqualTo(false)
    }

    @Test
    fun isLastInSectionForLastElement() {
        testAdapter.add(1)
        testAdapter.add(2)
        testAdapter.add(3)
        assertThat(testAdapter.isLastInSection(2)).isEqualTo(true)
    }

    inner class BaseTestAdapter : BaseAdapter<Int>() {

        override fun addAll(data: MutableList<Int>) {
            items.addAll(data)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Int> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}