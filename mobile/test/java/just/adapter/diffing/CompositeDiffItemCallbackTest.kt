package just.adapter.diffing

import just.adapter.ItemAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import just.adapter.AnyItem
import just.adapter.AnyViewHolder
import just.adapter.item.Callbacks
import just.adapter.item.HasCallbacks
import just.adapter.item.HasModel
import just.adapter.item.Item
import just.adapter.item.callAlways
import just.adapter.item.callNever
import just.adapter.manager.composite.CompositeItemAdapterManager
import just.adapter.manager.composite.forType

class CompositeDiffItemCallbackTest {

    @Test
    fun `Items are not same if there is no adapters passed and default is null`() {
        class NumberItem(override val model: Int) : Item(), HasModel<Int>

        val manager = CompositeItemAdapterManager.builder().build()
        val callback = CompositeDiffItemCallback(manager, defaultDiffer = null)

        val areItemsTheSame = callback.areItemsTheSame(NumberItem(42), NumberItem(42))

        assertThat(areItemsTheSame).isEqualTo(false)
    }

    @Test
    fun `Items are same if differ returns true`() {
        class TestItem(override val model: Unit = Unit) : Item(), HasModel<Unit>

        val oldItem = TestItem()
        val newItem = TestItem()
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areItemsTheSame(oldItem, newItem) } doReturn true
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val areItemsTheSame = callback.areItemsTheSame(oldItem, newItem)
        assertThat(areItemsTheSame).isEqualTo(true)
    }

    @Test
    fun `Items are not same if differ returns false`() {
        class TestItem(override val model: Unit = Unit) : Item(), HasModel<Unit>

        val oldItem = TestItem()
        val newItem = TestItem()
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areItemsTheSame(oldItem, newItem) } doReturn false
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val areItemsTheSame = callback.areItemsTheSame(oldItem, newItem)
        assertThat(areItemsTheSame).isEqualTo(false)
    }

    @Test
    fun `Contents are the same if differ returns true`() {
        class TestItem(override val model: Unit = Unit) : Item(), HasModel<Unit>

        val oldItem = TestItem()
        val newItem = TestItem()
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn true
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val areContentsTheSame = callback.areContentsTheSame(oldItem, newItem)
        assertThat(areContentsTheSame).isEqualTo(true)
    }

    @Test
    fun `Contents are not same if differ returns false`() {
        class TestItem(override val model: Unit = Unit) : Item(), HasModel<Unit>

        val oldItem = TestItem()
        val newItem = TestItem()
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn false
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val areContentsTheSame = callback.areContentsTheSame(oldItem, newItem)
        assertThat(areContentsTheSame).isEqualTo(false)
    }

    @Test
    fun `Contents are the same if differ returns true and dispatchers are the same`() {
        class TestItem(
            override val model: Unit = Unit,
            override val callbacks: Callbacks<Unit>
        ) : Item(), HasModel<Unit>, HasCallbacks<Unit>

        val callbacks = callNever<Unit>()
        val oldItem = TestItem(callbacks = callbacks)
        val newItem = TestItem(callbacks = callbacks)
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn true
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val areContentsTheSame = callback.areContentsTheSame(oldItem, newItem)
        assertThat(areContentsTheSame).isEqualTo(true)
    }

    @Test
    fun `Contents are not same if differ returns true but dispatchers are different`() {
        class TestItem(
            override val model: Unit = Unit,
            override val callbacks: Callbacks<String>
        ) : Item(), HasModel<Unit>, HasCallbacks<String>

        val oldItemCallbacks = callAlways("old")
        val newItemCallbacks = callAlways("new")
        val oldItem = TestItem(callbacks = oldItemCallbacks)
        val newItem = TestItem(callbacks = newItemCallbacks)
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn true
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val areContentsTheSame = callback.areContentsTheSame(oldItem, newItem)
        assertThat(areContentsTheSame).isEqualTo(false)
    }

    @Test
    fun `Payload is null if differ returns true and dispatchers are the same`() {
        class TestItem(
            override val model: Unit = Unit,
            override val callbacks: Callbacks<Unit>
        ) : Item(), HasModel<Unit>, HasCallbacks<Unit>

        val callbacks = callNever<Unit>()
        val oldItem = TestItem(callbacks = callbacks)
        val newItem = TestItem(callbacks = callbacks)
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn true
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val payload = callback.getChangePayload(oldItem, newItem)
        assertThat(payload).isEqualTo(null)
    }

    @Test
    fun `Payload is RebindDispatcherOnly if differ returns true and dispatchers are different`() {
        class TestItem(
            override val model: Unit = Unit,
            override val callbacks: Callbacks<String>
        ) : Item(), HasModel<Unit>, HasCallbacks<String>

        val oldItemCallbacks = callAlways("old")
        val newItemCallbacks = callAlways("new")
        val oldItem = TestItem(callbacks = oldItemCallbacks)
        val newItem = TestItem(callbacks = newItemCallbacks)
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn true
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val payload = callback.getChangePayload(oldItem, newItem)
        assertThat(payload).isEqualTo(CompositeDiffItemCallback.Payload.RebindCallbacksOnly)
    }

    @Test
    fun `Payload is null if differ returns false and dispatchers are the same`() {
        class TestItem(
            override val model: Unit = Unit,
            override val callbacks: Callbacks<Unit>
        ) : Item(), HasModel<Unit>, HasCallbacks<Unit>

        val callbacks = callNever<Unit>()
        val oldItem = TestItem(callbacks = callbacks)
        val newItem = TestItem(callbacks = callbacks)
        val differ = mock<ItemDiffer<AnyItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn false
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val payload = callback.getChangePayload(oldItem, newItem)
        assertThat(payload).isEqualTo(null)
    }

    @Test
    fun `Payload is null if differ returns false and dispatchers are different`() {
        class TestItem(
            override val model: Unit = Unit,
            override val callbacks: Callbacks<Unit>
        ) : Item(), HasModel<Unit>, HasCallbacks<Unit>

        val callbacks = callNever<Unit>()
        val oldItem = TestItem(callbacks = callbacks)
        val newItem = TestItem(callbacks = callbacks)
        val differ = mock<ItemDiffer<TestItem>> {
            on { areContentsTheSame(oldItem, newItem) } doReturn false
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        val payload = callback.getChangePayload(oldItem, newItem)
        assertThat(payload).isEqualTo(null)
    }

    @Test
    fun `Payload is equal to payload from differ`() {
        class TestItem(override val model: String) : Item(), HasModel<String>

        val oldItem = TestItem(model = "foo")
        val newItem = TestItem(model = "bar")
        val payload = Any()
        val differ = mock<ItemDiffer<TestItem>> {
            on { getChangePayload(oldItem, newItem) } doReturn payload
        }
        val itemAdapter = mock<ItemAdapter<TestItem, AnyViewHolder>> {
            on { this.differ } doReturn differ
        }
        val manager = CompositeItemAdapterManager.builder()
            .add(itemAdapter forType TestItem::class)
            .build()
        val callback = CompositeDiffItemCallback(manager)

        assertThat(callback.getChangePayload(oldItem, newItem)).isEqualTo(payload)
    }
}
