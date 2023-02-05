package ru.yandex.market.clean.domain.usecase.search

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.activity.searchresult.SearchTitleItem
import ru.yandex.market.clean.domain.model.SearchProductItem
import ru.yandex.market.clean.domain.model.SponsoredCarouselSearchItem
import ru.yandex.market.data.searchitem.model.SearchItemType

class MergeWithSearchTitleItemUseCaseTest {

    private val carouselItem = mock<SponsoredCarouselSearchItem> {
        on { itemType } doReturn SearchItemType.CAROUSEL
    }

    private val offerItem = mock<SearchProductItem.Offer> {
        on { itemType } doReturn SearchItemType.PRODUCT
    }

    private val useCase = MergeWithSearchTitleItemUseCase()

    @Test
    fun testEmptyItemsList() {
        val result = useCase.execute(emptyList()).test().assertNoErrors().assertComplete()
        result.assertValue { newItems ->
            !newItems.any { it is SearchTitleItem }
        }
    }

    @Test
    fun testSearchItemsWithoutIncuts() {
        val items = listOf(offerItem, offerItem, offerItem, offerItem)
        val result = useCase.execute(items).test().assertNoErrors().assertComplete()
        result.assertValue { newItems ->
            !newItems.any { it is SearchTitleItem }
        }
    }

    @Test
    fun testSearchItemsWithMiddleIncut() {
        val items = listOf(offerItem, offerItem, carouselItem, offerItem, offerItem)
        val result = useCase.execute(items).test().assertNoErrors().assertComplete()
        result.assertValue { newItems ->
            !newItems.any { it is SearchTitleItem }
        }
    }

    @Test
    fun testSearchItemsWithFirstIncut() {
        val items = listOf(carouselItem, offerItem, offerItem, offerItem, offerItem)
        val result = useCase.execute(items).test().assertNoErrors().assertComplete()
        result.assertValue { newItems ->
            newItems[1] is SearchTitleItem && newItems.filterIsInstance<SearchTitleItem>().size == 1
        }
    }

    @Test
    fun testSearchItemsWithMoreFirstIncut() {
        val items = listOf(carouselItem, carouselItem, carouselItem, offerItem, offerItem)
        val result = useCase.execute(items).test().assertNoErrors().assertComplete()
        result.assertValue { newItems ->
            newItems[3] is SearchTitleItem && newItems.filterIsInstance<SearchTitleItem>().size == 1
        }
    }

    @Test
    fun testSearchItemsWithMoreFirstAndMiddleIncut() {
        val items = listOf(carouselItem, carouselItem, offerItem, offerItem, carouselItem, offerItem)
        val result = useCase.execute(items).test().assertNoErrors().assertComplete()
        result.assertValue { newItems ->
            newItems[2] is SearchTitleItem && newItems.filterIsInstance<SearchTitleItem>().size == 1
        }
    }
}
