package just.adapter.manager.composite

import just.adapter.ItemAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import just.adapter.AnyItem
import just.adapter.AnyViewHolder
import just.adapter.item.HasModel
import just.adapter.item.Item

class ItemAdapterBindingTest {

    @Test
    fun `'forType' allows matching by Class`() {
        class TestItem : AnyItem()
        mock<ItemAdapter<TestItem, AnyViewHolder>>() forType TestItem::class.java
    }

    @Test
    fun `'forType' allows matching by KClass`() {
        class TestItem : AnyItem()
        mock<ItemAdapter<TestItem, AnyViewHolder>>() forType TestItem::class
    }

    @Test
    fun `'forType' allows matching by subtype of adapter parameter`() {
        class TestItem : AnyItem()
        mock<ItemAdapter<AnyItem, AnyViewHolder>>() forType TestItem::class
    }

    @Test
    fun `'forType' matches exact by class`() {
        open class BaseItem : AnyItem()
        class FooItem : BaseItem()
        class BarItem : BaseItem()
        class BazItem : AnyItem()

        val binding = mock<ItemAdapter<BaseItem, AnyViewHolder>>() forType BaseItem::class

        assertThat(binding.matcher.matches(BaseItem())).isEqualTo(true)
        assertThat(binding.matcher.matches(FooItem())).isEqualTo(false)
        assertThat(binding.matcher.matches(BarItem())).isEqualTo(false)
        assertThat(binding.matcher.matches(BazItem())).isEqualTo(false)
    }

    @Test
    fun `'forSubtype' allows matching by Class`() {
        class TestItem : AnyItem()
        mock<ItemAdapter<TestItem, AnyViewHolder>>() forSubtype TestItem::class.java
    }

    @Test
    fun `'forSubtype' allows matching by KClass`() {
        class TestItem : AnyItem()
        mock<ItemAdapter<TestItem, AnyViewHolder>>() forSubtype TestItem::class
    }

    @Test
    fun `'forSubtype' allows matching by subtype of adapter parameter`() {
        class TestItem : AnyItem()
        mock<ItemAdapter<AnyItem, AnyViewHolder>>() forSubtype TestItem::class
    }

    @Test
    fun `'forSubtype' matches by superclass`() {
        open class BaseItem : AnyItem()
        class FooItem : BaseItem()
        class BarItem : BaseItem()
        class BazItem : AnyItem()

        val binding = mock<ItemAdapter<BaseItem, AnyViewHolder>>() forSubtype BaseItem::class

        assertThat(binding.matcher.matches(BaseItem())).isEqualTo(true)
        assertThat(binding.matcher.matches(FooItem())).isEqualTo(true)
        assertThat(binding.matcher.matches(BarItem())).isEqualTo(true)
        assertThat(binding.matcher.matches(BazItem())).isEqualTo(false)
    }

    @Test
    fun `'forMatching' suggests type of item from adapter parameter`() {
        class NumberItem(override val model: Int) : AnyItem(), HasModel<Int>
        mock<ItemAdapter<NumberItem, AnyViewHolder>>() forMatching { item ->
            // Студия подсказывает, что параметр item имеет тип NumberItem
            item.model % 2 == 0
        }
    }

    @Test
    fun `'forMatching' matches by condition`() {
        class NumberItem(override val model: Int) : Item(), HasModel<Int>

        val evenBinding = mock<ItemAdapter<NumberItem, AnyViewHolder>>() forMatching { item -> item.model % 2 == 0 }
        val oddBinding = mock<ItemAdapter<NumberItem, AnyViewHolder>>() forMatching { item -> item.model % 2 != 0 }

        assertThat(evenBinding.matcher.matches(NumberItem(4))).isEqualTo(true)
        assertThat(evenBinding.matcher.matches(NumberItem(5))).isEqualTo(false)
        assertThat(oddBinding.matcher.matches(NumberItem(4))).isEqualTo(false)
        assertThat(oddBinding.matcher.matches(NumberItem(5))).isEqualTo(true)
    }
}
