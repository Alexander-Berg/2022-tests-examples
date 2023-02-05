package ru.yandex.market.clean.presentation.feature.smartshopping

import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.atMost
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.SmartCoinsAnalytics
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.presentationSchedulersMock

class SmartCoinsPresenterTest {

    private val cmsWidgetsPublisher: PublishSubject<List<CmsWidget>> = PublishSubject.create()
    private val userCoinsPublisher: PublishSubject<List<SmartCoin>> = PublishSubject.create()
    private val schedulers = presentationSchedulersMock()
    private val nearlyExpiredSmartCoin = SmartCoin.testBuilder().id("12345").build()
    private val commonSmartCoin = SmartCoin.testInstance()
    private val smartCoinsArguments = smartCoinsArgumentsTestInstance()
    private val smartCoinStateFormatter: SmartCoinStateFormatter = mock {
        on { format(eq(nearlyExpiredSmartCoin)) } doReturn SmartCoinState.NEARLY_EXPIRED
        on { format(eq(commonSmartCoin)) } doReturn SmartCoinState.UNKNOWN
    }
    private val useCases = mock<SmartCoinsUseCases> {
        on { smartCoinsCmsSteam } doReturn cmsWidgetsPublisher
        on { userCoinsStream } doReturn userCoinsPublisher
    }
    private val router = mock<Router>()
    private val view = mock<SmartCoinsView>()
    private val metricaSender = mock<MetricaSender>()
    private val smartCoinsAnalytics = mock<SmartCoinsAnalytics>()

    private val presenter = SmartCoinsPresenter(
        schedulers,
        useCases,
        router,
        metricaSender,
        smartCoinStateFormatter,
        smartCoinsArguments,
        smartCoinsAnalytics
    )

    @Test
    fun `Observe coins cms widgets changes`() {
        presenter.attachView(view)
        val firstWidgets = mock<List<CmsWidget>>()
        val secondWidgets = mock<List<CmsWidget>>()
        cmsWidgetsPublisher.onNext(firstWidgets)
        cmsWidgetsPublisher.onNext(secondWidgets)
        view.inOrder {
            verify().showCoinsCms(firstWidgets)
            verify().showCoinsCms(secondWidgets)
        }
    }

    @Test
    fun `Show error when failed to load coins cms`() {
        presenter.attachView(view)
        val error = RuntimeException()
        cmsWidgetsPublisher.onError(error)

        verify(view).showError(error)
    }

    @Test
    fun `Show initial loading`() {
        presenter.attachView(view)

        verify(view, atLeastOnce()).showLoading()
    }

    @Test
    fun `Reloads coins after error`() {
        presenter.attachView(view)
        val error = RuntimeException()
        val widgets = mock<List<CmsWidget>>()
        cmsWidgetsPublisher.onError(error)
        whenever(useCases.smartCoinsCmsSteam) doReturn Observable.just(widgets)
        presenter.reloadCoins()

        view.inOrder {
            verify().showLoading()
            verify().showError(error)
            verify().showLoading()
            verify().showCoinsCms(widgets)
        }
    }

    @Test
    fun `Show coin info on coin click`() {
        presenter.attachView(view)

        userCoinsPublisher.onNext(listOf(commonSmartCoin))

        presenter.showUserSmartCoinInfo(commonSmartCoin.id())
        verify(view).showCoinInformation(commonSmartCoin)
    }

    @Test
    fun `Show delayed coin info on coin click after coins loaded`() {
        presenter.attachView(view)

        presenter.showUserSmartCoinInfo(commonSmartCoin.id())

        userCoinsPublisher.onNext(listOf(commonSmartCoin))
        userCoinsPublisher.onNext(listOf(commonSmartCoin))
        verify(view, atMost(1)).showCoinInformation(commonSmartCoin)
    }

    @Test
    fun `Show nearly expired warning`() {
        presenter.attachView(view)

        userCoinsPublisher.onNext(listOf(nearlyExpiredSmartCoin, commonSmartCoin))

        verify(view).showNearlyExpiredWarning()
    }

    @Test
    fun `Hide nearly expired warning`() {
        presenter.attachView(view)

        userCoinsPublisher.onNext(listOf(commonSmartCoin))

        verify(view).hideNearlyExpiredWarning()
    }

}