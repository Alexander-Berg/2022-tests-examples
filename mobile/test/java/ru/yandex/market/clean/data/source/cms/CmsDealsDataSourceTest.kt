package ru.yandex.market.clean.data.source.cms

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.source.cms.DealsFapiClient
import ru.yandex.market.clean.data.mapper.cms.CmsProductMapper
import ru.yandex.market.clean.domain.usecase.GetOfferConfigUseCase
import ru.yandex.market.common.featureconfigs.models.OfferMapperConfig
import ru.yandex.market.domain.auth.repository.AuthRepository

class CmsDealsDataSourceTest {

    private val dealsFapiClient = mock<DealsFapiClient>()
    private val productMapper = mock<CmsProductMapper>()
    private val authRepository = mock<AuthRepository>()
    private val getOfferConfigUseCase = mock<GetOfferConfigUseCase>()

    @Test
    fun `Should return empty list if fapi responds with empty`() {
        val source = CmsDealsDataSource(
            productMapper = productMapper,
            dealsFapiClient = dealsFapiClient,
            authRepository = authRepository,
            getOfferConfigUseCase = getOfferConfigUseCase,
        )
        whenever(dealsFapiClient.resolveDeals(any(), any())) doReturn Single.just(emptyList())
        whenever(authRepository.getIsLoggedInSingle()) doReturn Single.just(false)
        whenever(getOfferConfigUseCase.execute()) doReturn Single.just(
            OfferMapperConfig(isResaleGoodsAvailable = false)
        )

        source.getDeals()
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }
}
