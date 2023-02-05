package ru.yandex.market.clean.domain.usecase.sku

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.clean.data.repository.WebLinksRepository

class GetSkuShareLinkUseCaseTest {

    private val webLinksRepository = mock<WebLinksRepository>()

    private val useCase = GetSkuShareLinkUseCase(webLinksRepository)

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when sku id is empty`() {
        useCase.getPokupkiSkuShareLink("").subscribe()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when model id is empty`() {
        useCase.getModelShareLink("", null, null, "").subscribe()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when offer id is empty`() {
        useCase.getOfferShareLink("").subscribe()
    }

    @Test
    fun `Return generated link from url provider`() {
        val link = "url"
        whenever(webLinksRepository.getShareProductBeruDesktopLink(any()))
            .thenReturn(Single.just(link))

        useCase.getPokupkiSkuShareLink("definitely-not-an-empty-string")
            .test()
            .assertValue(link)
    }
}