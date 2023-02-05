package ru.yandex.market.clean.domain.usecase.smartshopping

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.searchresult.SearchResultFragment
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.domain.model.smartCoinInformationTestInstance
import ru.yandex.market.clean.presentation.feature.catalog.CatalogParams
import ru.yandex.market.clean.presentation.feature.catalog.CatalogTargetScreen
import ru.yandex.market.clean.presentation.feature.catalog.RootCatalogTargetScreen
import ru.yandex.market.data.deeplinks.links.Deeplink

class ResolveSmartCoinApplicableGoodsUseCaseTest {

    private val smartCoin = spy(SmartCoin.testBuilder().build())
    private val validCoinDeepLink = mock<Deeplink> {
        on { targetScreen } doReturn VALID_LINK_TARGET
    }
    private val smartCoinInfoDeepLinkUseCase = mock<SmartCoinInfoDeepLinkUseCase> {
        on {
            getDeepLinkByCoinUrl(VALID_COIN_LINK)
        } doReturn Single.just(Optional.of(validCoinDeepLink))
        on {
            getDeepLinkByCoinUrl(NOT_VALID_COIN_LINK)
        } doReturn Single.just(Optional.empty())
    }
    private val useCase = ResolveSmartCoinApplicableGoodsUseCase(smartCoinInfoDeepLinkUseCase)

    @Test
    fun `When coin have valid link open deep link target screen`() {
        doReturn(VALID_COIN_LINK).whenever(smartCoin).outgoingLink()

        useCase.resolveSmartCoinApplicableGoods(smartCoin)
            .test()
            .assertResult(VALID_LINK_TARGET)
    }

    @Test
    fun `When coin does not have valid link and it isn't category bonus - open root catalog screen`() {
        doReturn(
            smartCoinInformationTestInstance(restrictions = null)
        ).whenever(smartCoin).information()

        doReturn(false).whenever(smartCoin).isCategoryBonus

        useCase.resolveSmartCoinApplicableGoods(smartCoin)
            .test()
            .assertResult(RootCatalogTargetScreen())
    }

    @Test
    fun `When coin does not have valid link but it is category bonus - open search`() {
        val bonusId = "12345"
        doReturn(true).whenever(smartCoin).isCategoryBonus
        doReturn(bonusId).whenever(smartCoin).id()

        val expected = SearchResultFragment.builder()
            .bonusId(bonusId)
            .buildTarget()

        useCase.resolveSmartCoinApplicableGoods(smartCoin)
            .test()
            .assertResult(expected)
    }

    companion object {
        private const val VALID_COIN_LINK = "https://beru.ru"
        private const val NOT_VALID_COIN_LINK = "https://example.com"
        private val VALID_LINK_TARGET = CatalogTargetScreen(CatalogParams.EMPTY)
    }
}
