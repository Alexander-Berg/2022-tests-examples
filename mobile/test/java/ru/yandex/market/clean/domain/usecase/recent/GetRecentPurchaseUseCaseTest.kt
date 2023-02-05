package ru.yandex.market.clean.domain.usecase.recent

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.RecentPurchaseRepository
import ru.yandex.market.clean.domain.model.offerAffectingInformationTestInstance
import ru.yandex.market.clean.domain.model.sku.DetailedSku
import ru.yandex.market.clean.domain.model.sku.detailedSkuTestInstance
import ru.yandex.market.clean.domain.usecase.OfferAffectingInformationUseCase
import ru.yandex.market.clean.presentation.feature.recent.GetRecentPurchaseUseCase
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.domain.product.model.SkuId

class GetRecentPurchaseUseCaseTest {

    private val testOfferAffectingInformation = offerAffectingInformationTestInstance()

    private val hid = 1111L
    private val pageSize = 10
    private val page = 1
    private val status = emptyList<OrderStatus>()

    private val repositorySubject = SingleSubject.create<List<DetailedSku>>()

    private val recentPurchaseRepository = mock<RecentPurchaseRepository> {
        on {
            getRecentPurchase(
                hid,
                pageSize,
                page,
                status,
                testOfferAffectingInformation,
            )
        } doReturn repositorySubject
    }

    private val getOfferAffectingInformationUseCase = mock<OfferAffectingInformationUseCase> {
        on { getOfferAffectingInformation() } doReturn Single.just(testOfferAffectingInformation)
    }

    private val useCase = GetRecentPurchaseUseCase(
        recentPurchaseRepository = recentPurchaseRepository,
        getOfferAffectingInformationUseCase = getOfferAffectingInformationUseCase
    )

    @Test
    fun `Gets recent purchase when repository result is empty`() {
        repositorySubject.onSuccess(emptyList())

        useCase.getRecentPurchase(
            hid = hid,
            pageSize = pageSize,
            page = page,
            status = status
        )
            .test()
            .assertNoErrors()
            .assertValue(emptyList())
    }

    @Test
    fun `Gets recent purchase when repository result is not empty`() {
        val id = "skuId"
        val skuId = SkuId(id, null, null)
        val sku = detailedSkuTestInstance(skuId = skuId)

        repositorySubject.onSuccess(listOf(sku))
        useCase.getRecentPurchase(
            hid = hid,
            pageSize = pageSize,
            page = page,
            status = status
        )
            .test()
            .assertNoErrors()
            .assertValue(listOf(sku))
    }

    companion object {
        private const val TEST_AUTH_TOKEN = "123abc123"
        private const val TEST_MARKET_UID = "456rty456"
    }
}
