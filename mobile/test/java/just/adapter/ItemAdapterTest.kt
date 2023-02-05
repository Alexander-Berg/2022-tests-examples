package just.adapter

import android.view.ViewGroup
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import just.adapter.item.Item
import java.lang.RuntimeException

class ItemAdapterTest {

    @Test
    fun `Default implementation of 'viewType' returns same view type each time`() {
        class TestItemAdapter : ItemAdapter<Item, AnyViewHolder>() {
            override fun createViewHolder(parent: ViewGroup): AnyViewHolder {
                throw RuntimeException()
            }

            override fun bindViewHolder(holder: AnyViewHolder, item: Item) {
                throw RuntimeException()
            }

            override fun unbindViewHolder(holder: AnyViewHolder) {
                throw RuntimeException()
            }
        }

        val itemAdapter = TestItemAdapter()
        assertThat(itemAdapter.viewType).isEqualTo(itemAdapter.viewType)
    }

    @Test
    fun `Default implementation of 'viewType' generates new value for each instance`() {
        class TestItemAdapter : ItemAdapter<Item, AnyViewHolder>() {
            override fun createViewHolder(parent: ViewGroup): AnyViewHolder {
                throw RuntimeException()
            }

            override fun bindViewHolder(holder: AnyViewHolder, item: Item) {
                throw RuntimeException()
            }

            override fun unbindViewHolder(holder: AnyViewHolder) {
                throw RuntimeException()
            }
        }

        val itemAdapter1 = TestItemAdapter()
        val itemAdapter2 = TestItemAdapter()
        assertThat(itemAdapter1.viewType).isNotEqualTo(itemAdapter2.viewType)
    }

    @Test
    fun `Default implementation of 'viewType' generates values out ouf IdRes`() {
        class TestItemAdapter : ItemAdapter<Item, AnyViewHolder>() {
            override fun createViewHolder(parent: ViewGroup): AnyViewHolder {
                throw RuntimeException()
            }

            override fun bindViewHolder(holder: AnyViewHolder, item: Item) {
                throw RuntimeException()
            }

            override fun unbindViewHolder(holder: AnyViewHolder) {
                throw RuntimeException()
            }
        }

        val itemAdapter = TestItemAdapter()
        assertThat(itemAdapter.viewType).isGreaterThan(0x000000)
        assertThat(itemAdapter.viewType).isLessThan(0x00FFFFFF)
    }
}
