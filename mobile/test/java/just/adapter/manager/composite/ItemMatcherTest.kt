package just.adapter.manager.composite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import just.adapter.AnyItem
import just.adapter.item.HasModel
import just.adapter.item.Item

class ItemMatcherTest {

    @Test
    fun `'subtypeOf()' matches any subclasses of specified`() {
        open class BaseItem : AnyItem()
        class FooItem : BaseItem()
        class BarItem : BaseItem()
        class BazItem : AnyItem()

        val matcher = ItemMatcher.subtypeOf<BaseItem>()

        assertThat(matcher.matches(BaseItem())).isEqualTo(true)
        assertThat(matcher.matches(FooItem())).isEqualTo(true)
        assertThat(matcher.matches(BarItem())).isEqualTo(true)
        assertThat(matcher.matches(BazItem())).isEqualTo(false)
    }

    @Test
    fun `'subtypeOf(condition)' applies condition to item`() {
        class NumberItem(override val model: Int) : Item(), HasModel<Int>

        val evenMatcher = ItemMatcher.subtypeOf<NumberItem> { item -> item.model % 2 == 0 }
        val oddMatcher = ItemMatcher.subtypeOf<NumberItem> { item -> item.model % 2 != 0 }

        assertThat(evenMatcher.matches(NumberItem(4))).isEqualTo(true)
        assertThat(evenMatcher.matches(NumberItem(5))).isEqualTo(false)
        assertThat(oddMatcher.matches(NumberItem(4))).isEqualTo(false)
        assertThat(oddMatcher.matches(NumberItem(5))).isEqualTo(true)
    }

    @Test
    fun `'exactInstanceOf()' matches specified class only`() {
        open class BaseItem : AnyItem()
        class FooItem : BaseItem()
        class BarItem : BaseItem()
        class BazItem : AnyItem()

        val matcher = ItemMatcher.exactInstanceOf<BaseItem>()

        assertThat(matcher.matches(BaseItem())).isEqualTo(true)
        assertThat(matcher.matches(FooItem())).isEqualTo(false)
        assertThat(matcher.matches(BarItem())).isEqualTo(false)
        assertThat(matcher.matches(BazItem())).isEqualTo(false)
    }

    @Test
    fun `'exactInstanceOf()' applies condition to item`() {
        class NumberItem(override val model: Int) : Item(), HasModel<Int>

        val evenMatcher = ItemMatcher.exactInstanceOf<NumberItem> { item -> item.model % 2 == 0 }
        val oddMatcher = ItemMatcher.exactInstanceOf<NumberItem> { item -> item.model % 2 != 0 }

        assertThat(evenMatcher.matches(NumberItem(4))).isEqualTo(true)
        assertThat(evenMatcher.matches(NumberItem(5))).isEqualTo(false)
        assertThat(oddMatcher.matches(NumberItem(4))).isEqualTo(false)
        assertThat(oddMatcher.matches(NumberItem(5))).isEqualTo(true)
    }

    @Test
    fun `'anyMatching()' applies condition to item`() {
        class NumberItem(override val model: Int) : Item(), HasModel<Int>

        val evenMatcher = ItemMatcher.anyMatching { item -> item is NumberItem && item.model % 2 == 0 }
        val oddMatcher = ItemMatcher.anyMatching { item -> item is NumberItem && item.model % 2 != 0 }

        assertThat(evenMatcher.matches(NumberItem(4))).isEqualTo(true)
        assertThat(evenMatcher.matches(NumberItem(5))).isEqualTo(false)
        assertThat(oddMatcher.matches(NumberItem(4))).isEqualTo(false)
        assertThat(oddMatcher.matches(NumberItem(5))).isEqualTo(true)
    }
}
