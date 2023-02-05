package ru.yandex.market.clean.data.repository

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.clean.data.MarketWebLinksFactory

class WebLinksRepositoryTest {

    private val beruWebLinksFactory = mock<MarketWebLinksFactory>()

    private val repository = WebLinksRepository(beruWebLinksFactory)

    @Test
    fun `Create product share link using beru links factory`() {
        val expected = "link"
        val skuId = "skuId"
        whenever(beruWebLinksFactory.createShareProductLink(any()))
            .thenReturn(Single.just(expected))

        repository.getShareProductBeruDesktopLink(skuId)
            .test()
            .assertResult(expected)

        verify(beruWebLinksFactory).createShareProductLink(skuId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when sku id is empty`() {
        repository.getShareProductBeruDesktopLink("").subscribe()
    }

    @Test
    fun `Return error if generated share link is empty`() {
        whenever(beruWebLinksFactory.createShareProductLink(any()))
            .thenReturn(Single.just(""))

        repository.getShareProductBeruDesktopLink("definitely-not-an-empty-string")
            .test()
            .assertError(IllegalArgumentException::class.java)
    }
}