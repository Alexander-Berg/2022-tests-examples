package just.adapter.manager.composite

import android.view.ViewGroup
import just.adapter.ItemAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import just.adapter.AnyItem
import just.adapter.AnyViewHolder
import just.adapter.item.HasModel
import just.adapter.item.Item

class CompositeItemAdapterManagerTest {

    @Test
    fun `Manager without adapters returns fallback view type for any item`() {
        val manager = CompositeItemAdapterManager.builder().build()

        val viewType = manager.getViewType(mock())

        assertThat(manager.isFallbackViewType(viewType))
    }

    @Test
    fun `Manager returns non-null fallback view type if fallback is not specified`() {
        val manager = CompositeItemAdapterManager.builder().build()

        val fallbackViewType = manager.getViewType(mock())

        assertThat(manager.isFallbackViewType(fallbackViewType))
        assertThat(manager.getItemAdapter(fallbackViewType)).isNotNull
    }

    @Test
    fun `Manager returns fallback adapter for fallback view type`() {
        val fallbackAdapter = createEmptyAdapter<AnyItem>(viewType = 1)
        val manager = CompositeItemAdapterManager.builder()
            .fallback(fallbackAdapter)
            .build()

        val fallbackViewType = manager.getViewType(mock())

        assertThat(manager.isFallbackViewType(fallbackViewType))
        assertThat(manager.getItemAdapter(fallbackViewType)).isEqualTo(fallbackAdapter)
    }

    @Test
    fun `Matcher returns fallback adapter if there is no matched adapter`() {
        class FooItem : Item()
        class BarItem : Item()

        val fallbackAdapter = createEmptyAdapter<AnyItem>(viewType = 1)
        val manager = CompositeItemAdapterManager.builder()
            .add(createEmptyAdapter<FooItem>(viewType = 2) forType FooItem::class)
            .fallback(fallbackAdapter)
            .build()

        val viewType = manager.getViewType(BarItem())

        assertThat(manager.getItemAdapter(viewType)).isEqualTo(fallbackAdapter)
    }

    @Test
    fun `Matcher returns adapter matched by type`() {
        class FooItem : Item()
        class BarItem : Item()

        val fooItemAdapter = createEmptyAdapter<FooItem>(viewType = 1)
        val barItemAdapter = createEmptyAdapter<BarItem>(viewType = 2)
        val manager = CompositeItemAdapterManager.builder()
            .add(fooItemAdapter forType FooItem::class)
            .add(barItemAdapter forType BarItem::class)
            .build()

        val fooViewType = manager.getViewType(FooItem())
        val adapterForFooItem = manager.getItemAdapter(fooViewType)
        val barViewType = manager.getViewType(BarItem())
        val adapterForBarItem = manager.getItemAdapter(barViewType)

        assertThat(adapterForFooItem).isEqualTo(fooItemAdapter)
        assertThat(adapterForBarItem).isEqualTo(barItemAdapter)
    }

    @Test
    fun `Matcher returns adapter matched by type and condition`() {
        class NumberItem(override val model: Int) : Item(), HasModel<Int>

        val evenItemAdapter = createEmptyAdapter<NumberItem>(viewType = 1)
        val oddItemAdapter = createEmptyAdapter<NumberItem>(viewType = 2)

        val manager = CompositeItemAdapterManager.builder()
            .add(evenItemAdapter forMatching { item -> item.model % 2 == 0 })
            .add(oddItemAdapter forMatching { item -> item.model % 2 != 0 })
            .build()

        val evenItemViewType = manager.getViewType(NumberItem(24))
        val adapterForEvenItem = manager.getItemAdapter(evenItemViewType)
        val oddItemViewType = manager.getViewType(NumberItem(19))
        val adapterForOddItem = manager.getItemAdapter(oddItemViewType)

        assertThat(adapterForEvenItem).isEqualTo(evenItemAdapter)
        assertThat(adapterForOddItem).isEqualTo(oddItemAdapter)
    }

    @Test
    fun `View type for item does not change`() {
        class StringItem(override val model: String) : Item(), HasModel<String>

        val manager = CompositeItemAdapterManager.builder()
            .add(createEmptyAdapter<StringItem>(viewType = 1) forType StringItem::class)
            .build()

        val viewTypes = listOf("foo", "bar", "baz")
            .map { string -> StringItem(string) }
            .map { item -> manager.getViewType(item) }

        val firstViewType = viewTypes.first()
        assertThat(viewTypes).allMatch { viewType -> viewType == firstViewType }
    }

    @Test
    fun `View types for different matchers are not equal`() {
        class FooItem : Item()
        class BarItem : Item()

        val manager = CompositeItemAdapterManager.builder()
            .add(createEmptyAdapter<FooItem>(viewType = 1) forType FooItem::class)
            .add(createEmptyAdapter<BarItem>(viewType = 2) forType BarItem::class)
            .build()

        val fooViewType = manager.getViewType(FooItem())
        val barViewType = manager.getViewType(BarItem())

        assertThat(fooViewType).isNotEqualTo(barViewType)
    }

    @Test
    fun `Manager do not crashes if view types are the same for different adapters`() {
        class FooItem : Item()
        class BarItem : Item()

        CompositeItemAdapterManager.builder()
            .add(createEmptyAdapter<FooItem>(viewType = 1) forType FooItem::class)
            .add(createEmptyAdapter<BarItem>(viewType = 1) forType BarItem::class)
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
