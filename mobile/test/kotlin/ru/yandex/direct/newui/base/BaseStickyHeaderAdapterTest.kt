package ru.yandex.direct.newui.base

import android.app.Application
import android.os.Build
import android.view.ViewGroup
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [Build.VERSION_CODES.P])
class BaseStickyHeaderAdapterTest {

    private lateinit var testAdapter: BaseStickyHeaderAdapter<Int>

    @Before
    fun runBeforeEachTest() {
        testAdapter = StickyHeaderTestAdapter()
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
    fun isLastInSectionForLastElement() {
        testAdapter.add(1)
        assertThat(testAdapter.isLastInSection(0)).isEqualTo(true)
    }

    @Test
    fun isLastInSectionForSameElements() {
        testAdapter.add(1)
        testAdapter.add(1)
        assertThat(testAdapter.isLastInSection(0)).isEqualTo(false)
        assertThat(testAdapter.isLastInSection(1)).isEqualTo(true)
    }

    @Test
    fun isLastInSectionForDifferentElements() {
        testAdapter.add(1)
        testAdapter.add(2)
        assertThat(testAdapter.isLastInSection(0)).isEqualTo(true)
    }

    inner class StickyHeaderTestAdapter : BaseStickyHeaderAdapter<Int>() {

        override fun onCreateHeaderViewHolder(parent: ViewGroup?): BaseViewHolder<Int> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Int> {
            return super.createViewHolder(parent, viewType)
        }

        override fun getSectionCriteria(item: Int): Long {
            return item.toLong()
        }
    }
}