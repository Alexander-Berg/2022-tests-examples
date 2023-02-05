package just.adapter.diffing

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import just.adapter.AnyItem
import just.adapter.item.HasIdentifier
import just.adapter.item.HasModel
import just.adapter.item.Item

class DefaultItemDifferTest {

    @Test
    fun `Items are the same if there is same instance`() {
        class TestItem(override val model: Unit = Unit) : Item(), HasModel<Unit>

        val item = TestItem()
        val differ = DefaultItemDiffer<AnyItem>()

        val areItemsTheSame = differ.areItemsTheSame(oldItem = item, newItem = item)
        assertThat(areItemsTheSame).isEqualTo(true)
    }

    @Test
    fun `Items are the same if models are equal`() {
        class TestItem(override val model: Int) : Item(), HasModel<Int>

        val oldItem = TestItem(42)
        val newItem = TestItem(42)
        val differ = DefaultItemDiffer<AnyItem>()

        val areItemsTheSame = differ.areItemsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areItemsTheSame).isEqualTo(true)
    }

    @Test
    fun `Items are the same if ids are equal`() {
        class TestItem(
            override val itemId: Int,
            override val model: Int
        ) : Item(), HasModel<Int>, HasIdentifier

        val oldItem = TestItem(itemId = 1, model = 2)
        val newItem = TestItem(itemId = 1, model = 3)
        val differ = DefaultItemDiffer<AnyItem>()

        val areItemsTheSame = differ.areItemsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areItemsTheSame).isEqualTo(true)
    }

    @Test
    fun `Items are not same if ids are not equal`() {
        class TestItem(
            override val itemId: Int,
            override val model: Int
        ) : Item(), HasModel<Int>, HasIdentifier

        val oldItem = TestItem(itemId = 1, model = 3)
        val newItem = TestItem(itemId = 2, model = 3)
        val differ = DefaultItemDiffer<AnyItem>()

        val areItemsTheSame = differ.areItemsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areItemsTheSame).isEqualTo(false)
    }

    @Test
    fun `Items are not same if models are equal but classes are different`() {
        class FooItem(override val model: Int) : Item(), HasModel<Int>
        class BarItem(override val model: Int) : Item(), HasModel<Int>

        val oldItem = FooItem(42)
        val newItem = BarItem(42)
        val differ = DefaultItemDiffer<AnyItem>()

        val areItemsTheSame = differ.areItemsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areItemsTheSame).isEqualTo(false)
    }

    @Test
    fun `Items are not same if ids are equal but classes are different`() {
        class FooItem(
            override val itemId: Int,
            override val model: Int
        ) : Item(), HasModel<Int>, HasIdentifier

        class BarItem(
            override val itemId: Int,
            override val model: Int
        ) : Item(), HasModel<Int>, HasIdentifier

        val oldItem = FooItem(itemId = 42, model = 42)
        val newItem = BarItem(itemId = 42, model = 42)
        val differ = DefaultItemDiffer<AnyItem>()

        val areItemsTheSame = differ.areItemsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areItemsTheSame).isEqualTo(false)
    }

    @Test
    fun `Contents are the same if there is same instance`() {
        class TestItem(
            override val model: Unit = Unit
        ) : Item(), HasModel<Unit>

        val item = TestItem()
        val differ = DefaultItemDiffer<AnyItem>()

        val areContentsTheSame = differ.areContentsTheSame(oldItem = item, newItem = item)
        assertThat(areContentsTheSame).isEqualTo(true)
    }

    @Test
    fun `Content are the same if models are equal`() {
        class TestItem(override val model: Int) : Item(), HasModel<Int>

        val oldItem = TestItem(42)
        val newItem = TestItem(42)
        val differ = DefaultItemDiffer<AnyItem>()

        val areContentsTheSame = differ.areContentsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areContentsTheSame).isEqualTo(true)
    }

    @Test
    fun `Content are not same if models are not equal`() {
        class TestItem(override val model: Int) : Item(), HasModel<Int>

        val oldItem = TestItem(42)
        val newItem = TestItem(43)
        val differ = DefaultItemDiffer<AnyItem>()

        val areContentsTheSame = differ.areContentsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areContentsTheSame).isEqualTo(false)
    }

    @Test
    fun `Content are the same if models are equal but ids are not equal`() {
        class TestItem(
            override val itemId: Int,
            override val model: Int
        ) : Item(), HasModel<Int>, HasIdentifier

        val oldItem = TestItem(itemId = 1, model = 42)
        val newItem = TestItem(itemId = 2, model = 42)
        val differ = DefaultItemDiffer<AnyItem>()

        val areContentsTheSame = differ.areContentsTheSame(oldItem = oldItem, newItem = newItem)
        assertThat(areContentsTheSame).isEqualTo(true)
    }
}
