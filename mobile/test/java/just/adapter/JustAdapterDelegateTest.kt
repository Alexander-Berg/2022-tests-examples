package just.adapter

import android.view.ViewGroup
import just.adapter.diffing.CompositeDiffItemCallback
import just.adapter.manager.ItemAdapterManager
import just.adapter.plugin.AdapterPlugin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class JustAdapterDelegateTest {

    @Test
    fun `Returns view type from delegate manager`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getViewType(item) } doReturn viewType
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        assertThat(adapterDelegate.getItemViewType(item)).isEqualTo(viewType)
    }

    @Test
    fun `Calls 'bindViewHolder' and 'bindViewListeners' if payloads is empty`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>>()
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        adapterDelegate.onBindViewHolder(holder, item, 0, emptyList())
        verify(itemAdapter, times(1)).bindViewHolder(holder, item)
        verify(itemAdapter, times(1)).bindViewListeners(holder, item)
    }

    @Test
    fun `Do not call 'bindViewHolder' if payload is RebindDispatcherOnly and 'bindViewListeners' returns true`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>> {
            on { bindViewListeners(holder, item) } doReturn true
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        val payloads = listOf(CompositeDiffItemCallback.Payload.RebindCallbacksOnly)
        adapterDelegate.onBindViewHolder(holder, item, 0, payloads)
        verify(itemAdapter, never()).bindViewHolder(holder, item)
        verify(itemAdapter, times(1)).bindViewListeners(holder, item)
    }

    @Test
    fun `Calls 'bindViewHolder' if payload is RebindDispatcherOnly and 'bindViewListeners' returns false`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>> {
            on { bindViewListeners(holder, item) } doReturn false
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        val payloads = listOf(CompositeDiffItemCallback.Payload.RebindCallbacksOnly)
        adapterDelegate.onBindViewHolder(holder, item, 0, payloads)
        verify(itemAdapter, times(1)).bindViewHolder(holder, item)
        verify(itemAdapter, times(1)).bindViewListeners(holder, item)
    }

    @Test
    fun `Calls 'applyChangePayload' if payload is not only RebindDispatcherOnly`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>>()
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        val payloads = listOf(CompositeDiffItemCallback.Payload.RebindCallbacksOnly, Any())
        adapterDelegate.onBindViewHolder(holder, item, 0, payloads)
        verify(itemAdapter, times(1)).applyChangePayloads(holder, item, payloads)
    }

    @Test
    fun `Do not call 'bindViewHolder' if 'applyChangePayload' and 'bindViewListeners' returns true`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val payloads = listOf(Any())
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>> {
            on { applyChangePayloads(holder, item, payloads) } doReturn true
            on { bindViewListeners(holder, item) } doReturn true
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        adapterDelegate.onBindViewHolder(holder, item, 0, payloads)
        verify(itemAdapter, times(1)).applyChangePayloads(holder, item, payloads)
        verify(itemAdapter, never()).bindViewHolder(holder, item)
    }

    @Test
    fun `Calls 'bindViewHolder' if 'applyChangePayload' returns false`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val payloads = listOf(Any())
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>> {
            on { applyChangePayloads(holder, item, payloads) } doReturn false
            on { bindViewListeners(holder, item) } doReturn true
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        adapterDelegate.onBindViewHolder(holder, item, 0, payloads)
        verify(itemAdapter, times(1)).applyChangePayloads(holder, item, payloads)
        verify(itemAdapter, times(1)).bindViewHolder(holder, item)
    }

    @Test
    fun `Do not call 'bindViewHolder' if 'applyChangePayload' returns true`() {
        val item = mock<AnyItem>()
        val viewType = 42
        val holder = mock<AnyViewHolder> {
            on { itemViewType } doReturn viewType
        }
        val payloads = listOf(Any())
        val itemAdapter = mock<ItemAdapter<AnyItem, AnyViewHolder>> {
            on { applyChangePayloads(holder, item, payloads) } doReturn true
            on { bindViewListeners(holder, item) } doReturn false
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(viewType) } doReturn itemAdapter
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, emptyList())
        adapterDelegate.onBindViewHolder(holder, item, 0, payloads)
        verify(itemAdapter, times(1)).applyChangePayloads(holder, item, payloads)
        verify(itemAdapter, never()).bindViewHolder(holder, item)
    }

    @Test
    fun `Calls plugin method 'onBeforeCreateViewHolder' with correct params`() {
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(any<Int>()) } doReturn mock()
        }
        val adapterPlugin = mock<AdapterPlugin>()
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, listOf(adapterPlugin))
        val parent = mock<ViewGroup>()
        val viewType = 42
        adapterDelegate.onCreateViewHolder(
            parent = parent,
            viewType = viewType
        )
        verify(adapterPlugin).onBeforeCreateViewHolder(
            parent = parent,
            viewType = viewType
        )
    }

    @Test
    fun `Calls plugin method 'onAfterCreateViewHolder' with correct params`() {
        val beforePluginPayload = "Hello, world!"
        val holder = mock<AnyViewHolder>()
        val itemDelegate = mock<AnyItemAdapter> {
            on { createViewHolder(any()) } doReturn holder
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(any<Int>()) } doReturn itemDelegate
        }
        val adapterPlugin = mock<AdapterPlugin> {
            on { onBeforeCreateViewHolder(any(), any()) } doReturn beforePluginPayload
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, listOf(adapterPlugin))
        val parent = mock<ViewGroup>()
        val viewType = 42
        adapterDelegate.onCreateViewHolder(
            parent = parent,
            viewType = viewType
        )
        verify(adapterPlugin).onAfterCreateViewHolder(
            parent = parent,
            viewType = viewType,
            holder = holder,
            itemAdapter = itemDelegate,
            beforePayload = beforePluginPayload
        )
    }

    @Test
    fun `Calls plugin method 'onBeforeBindViewHolder' with correct params`() {
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(any<Int>()) } doReturn mock()
        }
        val adapterPlugin = mock<AdapterPlugin>()
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, listOf(adapterPlugin))
        val position = 42
        val item = mock<AnyItem>()
        val holder = mock<AnyViewHolder>()
        adapterDelegate.onBindViewHolder(
            position = position,
            item = item,
            holder = holder,
            payloads = emptyList()
        )
        verify(adapterPlugin).onBeforeBindViewHolder(
            position = position,
            item = item,
            holder = holder
        )
    }

    @Test
    fun `Calls plugin method 'onAfterBindViewHolder' with correct params`() {
        val beforePluginPayload = "Hello, world!"
        val holder = mock<AnyViewHolder>()
        val itemDelegate = mock<AnyItemAdapter> {
            on { createViewHolder(any()) } doReturn holder
        }
        val itemAdapterManager = mock<ItemAdapterManager> {
            on { getItemAdapter(any<Int>()) } doReturn itemDelegate
        }
        val adapterPlugin = mock<AdapterPlugin> {
            on { onBeforeBindViewHolder(any(), any(), any()) } doReturn beforePluginPayload
        }
        val adapterDelegate = JustAdapterDelegate(itemAdapterManager, listOf(adapterPlugin))
        val position = 42
        val item = mock<AnyItem>()
        adapterDelegate.onBindViewHolder(
            holder = holder,
            item = item,
            position = position,
            payloads = emptyList()
        )
        verify(adapterPlugin).onAfterBindViewHolder(
            holder = holder,
            item = item,
            position = position,
            itemAdapter = itemDelegate,
            beforePayload = beforePluginPayload
        )
    }
}
