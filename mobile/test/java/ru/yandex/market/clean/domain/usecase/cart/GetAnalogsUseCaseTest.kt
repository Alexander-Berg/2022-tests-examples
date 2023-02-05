package ru.yandex.market.clean.domain.usecase.cart

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import ru.yandex.market.Constants
import ru.yandex.market.clean.data.repository.cart.AnalogRepository
import ru.yandex.market.clean.domain.model.cms.CmsProduct
import ru.yandex.market.mockResult
import ru.yandex.market.net.BillingZone

class GetAnalogsUseCaseTest {

    private val analogRepository = mock<AnalogRepository>()

    private val useCase = GetAnalogsUseCase(analogRepository)

    @Test
    fun `check with empty analogs`() {

        analogRepository.getAnalogs(
            skuId = anyOrNull(),
            modelId = any(),
            cpa = anyOrNull(),
            billingZone = any(),
            cartSnapshot = any(),
            reportState = anyOrNull(),
            isLoggedIn = any(),
            pageAnalogsParams = anyOrNull(),
        ).mockResult(Single.just(emptyList()))

        useCase.execute(
            skuId = "1",
            modelId = "1",
            isLoggedIn = true,
            cpa = Constants.CPA_OR_CPC_OFFER_FLAG,
            billingZone = BillingZone.DEFAULT,
            cartSnapshot = emptyList(),
            reportState = null,
            pageAnalogsParams = null,
        )
            .test()
            .assertNoErrors()
            .assertValueSequence(listOf(emptyList()))

    }

    @Test
    fun `check with analog`() {

        val cmsProduct = CmsProduct.testInstance()

        analogRepository.getAnalogs(
            skuId = anyOrNull(),
            modelId = any(),
            cpa = anyOrNull(),
            billingZone = any(),
            cartSnapshot = any(),
            reportState = anyOrNull(),
            isLoggedIn = any(),
            pageAnalogsParams = anyOrNull()
        ).mockResult(Single.just(listOf(cmsProduct)))

        useCase.execute(
            skuId = "1",
            modelId = "1",
            isLoggedIn = true,
            cpa = Constants.CPA_OR_CPC_OFFER_FLAG,
            billingZone = BillingZone.DEFAULT,
            cartSnapshot = emptyList(),
            reportState = null,
            pageAnalogsParams = null,
        )
            .test()
            .assertNoErrors()
            .assertValueSequence(listOf(listOf(cmsProduct)))
    }
}
