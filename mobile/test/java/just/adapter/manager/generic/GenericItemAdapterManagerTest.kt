package just.adapter.manager.generic

import android.view.ViewGroup
import just.adapter.ItemAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import just.adapter.AnyItem
import just.adapter.AnyViewHolder
import just.adapter.item.HasModel
import just.adapter.item.Item
import org.mockito.kotlin.doReturn

class GenericItemAdapterManagerTest {

    @Test
    fun `Manager without factory returns fallback view type for any item`() {
        val manager = GenericItemAdapterManager.builder().build()

        val viewType = manager.getViewType(mock())

        assertThat(manager.isFallbackViewType(viewType))
    }

    @Test
    fun `Manager returns non-null fallback view type if fallback is not specified`() {
        val manager = GenericItemAdapterManager.builder().build()

        val fallbackViewType = manager.getViewType(mock())

        assertThat(manager.isFallbackViewType(fallbackViewType))
        assertThat(manager.getItemAdapter(fallbackViewType)).isNotNull
    }

    @Test
    fun `Manager returns fallback adapter for fallback view type`() {
        val fallbackAdapter = createEmptyAdapter<AnyItem>(viewType = 1)
        val manager = GenericItemAdapterManager.builder()
            .fallback(fallbackAdapter)
            .build()

        val fallbackViewType = manager.getViewType(mock())

        assertThat(manager.isFallbackViewType(fallbackViewType))
        assertThat(manager.getItemAdapter(fallbackViewType)).isEqualTo(fallbackAdapter)
    }

    @Test
    fun `Manager calls factory once per item type`() {
        class StringItem(override val model: String) : Item(), HasModel<String>

        val factory = mock<GenericItemAdapterFactory> {
            on { createItemAdapter(any()) } doReturn createEmptyAdapter<StringItem>(viewType = 1)
        }
        val manager = GenericItemAdapterManager.builder()
            .factory(factory)
            .build()

        val items = listOf("foo", "bar", "baz").map { string -> StringItem(string) }
        val adapters = mutableListOf<ItemAdapter<*, *>>()
        for (item in items) {
            adapters += manager.getItemAdapter(item)
        }
        assertThat(adapters).allMatch { adapter -> adapter == adapters.first() }
        verify(factory, times(1)).createItemAdapter(any())
    }

    @Test
    fun `View type for item does not change`() {
        class StringItem(override val model: String) : Item(), HasModel<String>

        val factory = GenericItemAdapterFactory { itemType ->
            when (itemType) {
                StringItem::class -> createEmptyAdapter<StringItem>(viewType = 1)
                else -> null
            }
        }
        val manager = GenericItemAdapterManager.builder()
            .factory(factory)
            .build()

        val viewTypes = listOf("foo", "bar", "baz")
            .map { string -> StringItem(string) }
            .map { item -> manager.getViewType(item) }

        val firstViewType = viewTypes.first()
        assertThat(viewTypes).allMatch { viewType -> viewType == firstViewType }
    }

    @Test
    fun `Manager do not crashes if view types are the same for different adapters`() {
        class FooItem : Item()
        class BarItem : Item()

        val factory = GenericItemAdapterFactory { itemType ->
            when (itemType) {
                FooItem::class -> createEmptyAdapter<FooItem>(viewType = 1)
                BarItem::class -> createEmptyAdapter<BarItem>(viewType = 1)
                else -> null
            }
        }
        GenericItemAdapterManager.builder()
            .factory(factory)
            .build()
    }

    /**
     * Если сломается эта проверка, все остальные тесты станут невалидными.
     */
    @Test
    fun `Check empty adapter factory method`() {
        val firstEmptyAdapterInstance = createEmptyAdapter<AnyItem>(viewType = 1)
        val secondEmptyAdapterInstance = createEmptyAdapter<AnyItem>(viewType = 2)
        assertThat(firstEmptyAdapterInstance).isNotEqualTo(secondEmptyAdapterInstance)
    }

    private fun <T : AnyItem> createEmptyAdapter(
        viewType: Int
    ): ItemAdapter<T, AnyViewHolder> {
        return object : ItemAdapter<T, AnyViewHolder>() {

            override val viewType = viewType

            override fun createViewHolder(parent: ViewGroup): AnyViewHolder {
                throw NullPointerException()
            }

            override fun bindViewHolder(holder: AnyViewHolder, item: T) {
                // no-op
            }

            override fun unbindViewHolder(holder: AnyViewHolder) {
                // no-op
            }
        }
    }
}
