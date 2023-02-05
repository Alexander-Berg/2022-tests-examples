package just.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import just.adapter.item.HasModel
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import just.adapter.item.Item
import org.assertj.core.api.Assertions.assertThat

class StatefulItemAdapterTest {

    @Test
    fun `Adapter creates state for new item and calls 'bindViewHolder' with new state`() {
        val item = TestItem("42")
        val holder = mock<RecyclerView.ViewHolder>()
        val state = TestItemState()
        val adapter = spy(TestStatefulItemAdapter()) {
            on { createItemState(item) } doReturn state
        }
        adapter.bindViewHolder(holder, item)
        verify(adapter).createItemState(item)
        verify(adapter).bindViewHolder(holder, item, state)
    }

    @Test
    fun `Adapter provides existing state for item if state matched by key`() {
        val item1 = TestItem("42")
        val item2 = TestItem("42")
        val holder1 = mock<RecyclerView.ViewHolder>()
        val holder2 = mock<RecyclerView.ViewHolder>()
        val state = TestItemState()
        val adapter = spy(TestStatefulItemAdapter()) {
            on { createItemState(item1) } doReturn state
        }
        adapter.bindViewHolder(holder1, item1)
        verify(adapter).createItemState(item1)
        verify(adapter).bindViewHolder(holder1, item1, state)
        adapter.bindViewHolder(holder2, item2)
        verify(adapter, never()).createItemState(item2)
        verify(adapter).bindViewHolder(holder2, item2, state)
    }

    @Test
    fun `Adapter creates new state for item if state not matched by key`() {
        val item1 = TestItem("42")
        val item2 = TestItem("43")
        val holder1 = mock<RecyclerView.ViewHolder>()
        val holder2 = mock<RecyclerView.ViewHolder>()
        val state1 = TestItemState()
        val state2 = TestItemState()
        val adapter = spy(TestStatefulItemAdapter()) {
            on { createItemState(item1) } doReturn state1
            on { createItemState(item2) } doReturn state2
        }
        adapter.bindViewHolder(holder1, item1)
        verify(adapter).createItemState(item1)
        verify(adapter).bindViewHolder(holder1, item1, state1)
        adapter.bindViewHolder(holder2, item2)
        verify(adapter).createItemState(item2)
        verify(adapter).bindViewHolder(holder2, item2, state2)
    }

    @Test
    fun `Adapter creates new state for item if 'unbindViewHolder' called before 'bindViewHolder' and cache disabled`() {
        val item1 = TestItem("42")
        val item2 = TestItem("42")
        val holder1 = mock<RecyclerView.ViewHolder>()
        val holder2 = mock<RecyclerView.ViewHolder>()
        val state1 = TestItemState()
        val state2 = TestItemState()
        val adapter = spy(TestStatefulItemAdapter(StatefulItemAdapter.DetachedStateCache.disabled())) {
            on { createItemState(item1) } doReturn state1
            on { createItemState(item2) } doReturn state2
        }
        adapter.bindViewHolder(holder1, item1)
        verify(adapter).createItemState(item1)
        verify(adapter).bindViewHolder(holder1, item1, state1)
        adapter.unbindViewHolder(holder1)
        adapter.bindViewHolder(holder2, item2)
        verify(adapter).createItemState(item2)
        verify(adapter).bindViewHolder(holder2, item2, state2)
    }

    @Test
    fun `Adapter provides existing state if 'unbindViewHolder' called before 'bindViewHolder' but cache is enabled`() {
        val item1 = TestItem("42")
        val item2 = TestItem("42")
        val holder1 = mock<RecyclerView.ViewHolder>()
        val holder2 = mock<RecyclerView.ViewHolder>()
        val state = TestItemState()
        val adapter = spy(TestStatefulItemAdapter(StatefulItemAdapter.DetachedStateCache.limited(1))) {
            on { createItemState(item1) } doReturn state
        }
        adapter.bindViewHolder(holder1, item1)
        verify(adapter).createItemState(item1)
        verify(adapter).bindViewHolder(holder1, item1, state)
        adapter.unbindViewHolder(holder1)
        adapter.bindViewHolder(holder2, item2)
        verify(adapter).bindViewHolder(holder2, item2, state)
    }

    @Test
    fun `Adapter calls 'onDestroyItemState' method when last holder associated with state unbounded`() {
        val item = TestItem("42")
        val holder = mock<RecyclerView.ViewHolder>()
        val state = TestItemState()
        val adapter = spy(TestStatefulItemAdapter()) {
            on { createItemState(item) } doReturn state
        }
        adapter.bindViewHolder(holder, item)
        adapter.unbindViewHolder(holder)
        verify(adapter).onDestroyItemState(state)
    }

    @Test
    fun `Adapter calls 'onDestroyItemState' method after repeated binding to same holder`() {
        val item = TestItem("42")
        val holder = mock<RecyclerView.ViewHolder>()
        val state = TestItemState()
        val adapter = spy(TestStatefulItemAdapter()) {
            on { createItemState(item) } doReturn state
        }
        adapter.bindViewHolder(holder, item)
        adapter.bindViewHolder(holder, item)
        adapter.unbindViewHolder(holder)
        verify(adapter).onDestroyItemState(state)
    }

    @Test
    fun `Detached state cache clears eldest state on push if there is lack of capacity`() {
        val cache = StatefulItemAdapter.DetachedStateCache.limited<String>(5)
        cache.push("first-key", "first-state")
        cache.push("second-key", "second-state")
        cache.push("third-key", "third-state")
        cache.push("fourth-key", "fourth-state")
        cache.push("fifth-key", "fifth-state")
        cache.push("sixth-key", "sixth-state")
        cache.push("seventh-key", "seventh-state")
        assertThat(cache.pop("first-key")).isEqualTo(null)
        assertThat(cache.pop("third-key")).isEqualTo("third-state")
        assertThat(cache.pop("fourth-key")).isEqualTo("fourth-state")
        assertThat(cache.pop("fifth-key")).isEqualTo("fifth-state")
        assertThat(cache.pop("second-key")).isEqualTo(null)
        assertThat(cache.pop("sixth-key")).isEqualTo("sixth-state")
        assertThat(cache.pop("seventh-key")).isEqualTo("seventh-state")
    }

    @Test
    fun `Detached state cache updates previous state by key`() {
        val cache = StatefulItemAdapter.DetachedStateCache.limited<String>(1)
        cache.push("first-key", "first-state")
        cache.push("first-key", "first-state-2")
        assertThat(cache.pop("first-key")).isEqualTo("first-state-2")
    }

    @Test
    fun `Adapter correctly scrolls with small cache`() {
        val range = (0..9)
        val items = range.map { index -> TestItem("$index") }
        val states = range.map { index -> TestItemState() }
        val holders = range.map { index -> mock<RecyclerView.ViewHolder>() }
        val adapter = spy(TestStatefulItemAdapter(StatefulItemAdapter.DetachedStateCache.limited(1))) {
            for (i in range) {
                on { createItemState(items[i]) } doReturn states[i]
            }
        }

        // Количество элементов на экране
        val visible = 2

        // Пролистываем от начала до конца
        for (i in range) {
            adapter.bindViewHolder(holders[i], items[i])
            if (i - visible > 0) {
                adapter.unbindViewHolder(holders[i - visible])
            }
        }

        // Пролистываем из конца в начало
        for (i in range.reversed()) {
            adapter.bindViewHolder(holders[i], items[i])
            if (i + visible < items.size) {
                adapter.unbindViewHolder(holders[i + visible])
            }
        }
    }

    private class TestItem(override val model: String) : Item(), HasModel<String>

    private class TestItemState

    private class TestStatefulItemAdapter(
        stateCache: DetachedStateCache<TestItemState> = DetachedStateCache.disabled()
    ) : StatefulItemAdapter<TestItem, TestItemState, RecyclerView.ViewHolder>(stateCache) {

        override val viewType = 0

        override fun createItemState(item: TestItem): TestItemState {
            return TestItemState()
        }

        override fun generateStateKey(item: TestItem): Any {
            return item.model
        }

        override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            return mock()
        }

        override fun bindViewHolder(
            holder: RecyclerView.ViewHolder,
            item: TestItem,
            state: TestItemState,
        ) {
            // no-op
        }

        override fun unbindViewHolder(holder: RecyclerView.ViewHolder, state: TestItemState) {
            // no-op
        }
    }
}
