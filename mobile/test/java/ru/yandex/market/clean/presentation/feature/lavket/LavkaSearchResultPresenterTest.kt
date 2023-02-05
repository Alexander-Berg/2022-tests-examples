package ru.yandex.market.clean.presentation.feature.lavket

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfoStatus
import ru.yandex.market.clean.domain.model.lavka2.product.LavkaSearchResult
import ru.yandex.market.clean.domain.model.lavka2.product.lavkaSearchResultItemTestInstance
import ru.yandex.market.clean.domain.model.search.searchResultTestInstance
import ru.yandex.market.clean.presentation.feature.cart.formatter.LavkaServiceInfoFormatter
import ru.yandex.market.clean.presentation.feature.lavka.searchresult.LavkaSearchResultFragment
import ru.yandex.market.clean.presentation.feature.lavka.searchresult.LavkaSearchResultPresenter
import ru.yandex.market.clean.presentation.feature.lavka.searchresult.LavkaSearchResultUseCases
import ru.yandex.market.clean.presentation.feature.lavka.searchresult.LavkaSearchResultView
import ru.yandex.market.domain.adult.model.AdultState
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.geoCoordinatesTestInstance
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock
import com.annimon.stream.Optional as AnnimonOptional

class LavkaSearchResultPresenterTest {

    private val argumentsFromMarketSearchResult = LavkaSearchResultFragment.Arguments.ArgumentsFromMarketSearchResult(
        onlyLavkaSearch = false,
        text = SEARCH_TEXT
    )

    private val rewardBlocks = listOf(
        Pair(
            first = DELIVERY_TEXT,
            second = MINIMUM_PRICE
        )
    )

    private val lavkaServiceInfo = LavkaServiceInfo(
        status = LavkaServiceInfoStatus.OPEN,
        availableAt = null,
        deliveryText = DELIVERY_TEXT,
        eta = ETA,
        depotId = null,
        isSurge = true,
        availability = null,
        rewardBlocks = rewardBlocks,
        informers = emptyList(),
        isLavkaNewbie = false,
        juridicalInfo = null,
        lavkaDiscountInfo = null,
    )

    private val lavkaSearchResultItem = lavkaSearchResultItemTestInstance()

    private val lavkaSearchResult = LavkaSearchResult(
        offerId = null,
        items = listOf(lavkaSearchResultItem),
        searchMinimum = 0L
    )

    private val marketSearchResult = searchResultTestInstance()

    private val geoCoordinates = geoCoordinatesTestInstance()

    private val hyperlocalAddress = HyperlocalAddress.Exists.Expired(
        coordinates = geoCoordinates,
        userAddress = userAddressTestInstance()
    )

    private val useCases = mock<LavkaSearchResultUseCases>() {

        on { getActualAdultStateChangesStream() } doReturn Observable.just(AdultState.UNKNOWN)

        on { lavkaSearchByText(SEARCH_TEXT, USE_CACHE) } doReturn Single.just(lavkaSearchResult)

        on { marketSearchByText(any()) } doReturn Single.just(marketSearchResult)

        on { observeLayoutId() } doReturn Observable.just(Optional.empty())

        on { observeHyperlocalAddress() } doReturn Observable.just(hyperlocalAddress)

        on { observeHyperlocalCoordinates() } doReturn Observable.just(AnnimonOptional.of(geoCoordinates))

        on { observeLavkaCart() } doReturn Observable.just(Optional.empty())

        on { isCartInSurgeEnabled() } doReturn Single.just(false)

        on { isLavkaComboEnabled() } doReturn Single.just(false)

    }

    private val lavkaServiceInfoFormatter =
        LavkaServiceInfoFormatter(
            resourcesManager = mock(),
            timeFormatter = mock(),
            juridicalInfoMapper = mock()
        )

    private val presenter = LavkaSearchResultPresenter(
        schedulers = presentationSchedulersMock(),
        router = mock(),
        arguments = argumentsFromMarketSearchResult,
        useCases = useCases,
        lavkaSearchResultVoFormatter = mock(),
        lavkaServiceInfoFormatter = lavkaServiceInfoFormatter,
        lavkaAnalytics = mock(),
        searchResultListFormatter = mock(),
        lavkaCartButtonDelegate = mock(),
        lavkaHealthFacade = mock()
    )

    private val view = mock<LavkaSearchResultView>()

    @Test
    fun `Lavka delivery bottom bar was shown`() {
        whenever(useCases.observeLavkaServiceInfo()) doReturn Observable.just(Optional.of(lavkaServiceInfo))

        presenter.attachView(view)

        verify(view, times(1)).showLavkaDeliveryInformationView(any())
    }

    @Test
    fun `Lavka delivery bottom bar was not shown`() {
        whenever(useCases.observeLavkaServiceInfo()) doReturn Observable.just(Optional.empty())

        presenter.attachView(view)

        verify(view, never()).showLavkaDeliveryInformationView(any())
    }

    companion object {
        private const val SEARCH_TEXT = "some search"
        private const val USE_CACHE = true
        private const val DELIVERY_TEXT = "Доставка 149Р"
        private const val MINIMUM_PRICE = "От 0 Р"
        private const val ETA = "15-25 мин"
    }
}
