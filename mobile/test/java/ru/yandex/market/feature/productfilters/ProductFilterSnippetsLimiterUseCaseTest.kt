package ru.yandex.market.feature.productfilters

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.domain.cms.model.content.filter.ProductFilterLimit
import ru.yandex.market.domain.cms.model.content.filter.ProductFilterSnippet
import ru.yandex.market.domain.cms.model.content.filter.ProductFilterSnippetSelectionParams
import ru.yandex.market.domain.cms.model.content.filter.ProductFilterSnippetState
import ru.yandex.market.domain.cms.model.widget.WidgetStyle

class ProductFilterSnippetsLimiterUseCaseTest {

    private val useCase = ProductFilterSnippetsLimiterUseCase()

    @Test
    fun `Do not truncate snippets if visible limit is not violated`() {

        val snippets = listOf(
            buildProductFilterSnippet(id = "1", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "2", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "3", state = ProductFilterSnippetState.DISABLED),
            buildProductFilterSnippet(id = "4", state = ProductFilterSnippetState.SELECTED),
            buildProductFilterSnippet(id = "5", state = ProductFilterSnippetState.ENABLED),
        )
        val limit = ProductFilterLimit(visibleBorder = 5, text = "")

        val truncatedSnippets = useCase.execute(snippets = snippets, limit = limit)

        assertThat(truncatedSnippets).hasSize(snippets.size)
        assertThat(truncatedSnippets).containsAll(snippets)
    }

    /**
     * Этот тест нужно будет выплить после того, как бэкенд научится  присылать объект limit
     */
    @Test
    fun `Apply default limit if it does not come from backend`() {

        val snippets = listOf(
            buildProductFilterSnippet(id = "1", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "2", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "3", state = ProductFilterSnippetState.DISABLED),
            buildProductFilterSnippet(id = "4", state = ProductFilterSnippetState.SELECTED),
            buildProductFilterSnippet(id = "5", state = ProductFilterSnippetState.SELECTED),
            buildProductFilterSnippet(id = "6", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "7", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "8", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "9", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "10", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "11", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "12", state = ProductFilterSnippetState.ENABLED),
        )

        val truncatedSnippets = useCase.execute(snippets = snippets, limit = null)

        assertThat(truncatedSnippets).hasSize(snippets.size)
        assertThat(truncatedSnippets).containsAll(snippets)
    }

    @Test
    fun `Apply limit and always display selected snippet`() {

        val limit = ProductFilterLimit(visibleBorder = 10, text = "Еще 1")

        val selectedSnippet = buildProductFilterSnippet(id = "13", state = ProductFilterSnippetState.SELECTED)

        val snippets = listOf(
            buildProductFilterSnippet(id = "1", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "2", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "3", state = ProductFilterSnippetState.DISABLED),
            buildProductFilterSnippet(id = "4", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "5", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "6", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "7", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "8", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "9", state = ProductFilterSnippetState.DISABLED),
            buildProductFilterSnippet(id = "10", state = ProductFilterSnippetState.ENABLED),
            buildProductFilterSnippet(id = "11", state = ProductFilterSnippetState.DISABLED),
            buildProductFilterSnippet(id = "12", state = ProductFilterSnippetState.ENABLED),
            selectedSnippet,
        )

        val truncatedSnippets = useCase.execute(snippets = snippets, limit = limit)

        assertThat(truncatedSnippets).hasSize(limit.visibleBorder + 1)
        assertThat(truncatedSnippets).containsAll(snippets.subList(0, limit.visibleBorder))
        assertThat(truncatedSnippets).contains(selectedSnippet)
    }

    private fun buildProductFilterSnippet(id: String, state: ProductFilterSnippetState): ProductFilterSnippet {
        return ProductFilterSnippet.TextFilterSnippet(
            id = id,
            filterId = "filterId",
            style = WidgetStyle.DEFAULT,
            title = "be",
            filterTitle = "filter: title",
            state = state,
            selectionParams = ProductFilterSnippetSelectionParams(
                id = id,
                filterId = "filterId",
                skuId = "skuId$id",
                productId = "productId",
            ),
            subtitle = "happy"
        )
    }
}
