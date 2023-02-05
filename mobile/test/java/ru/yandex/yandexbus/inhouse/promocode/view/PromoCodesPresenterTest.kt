package ru.yandex.yandexbus.inhouse.promocode.view

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.promocode.PromoCodeData
import ru.yandex.yandexbus.inhouse.promocode.PromoCodeInfo
import ru.yandex.yandexbus.inhouse.promocode.PromoCodeTestData
import ru.yandex.yandexbus.inhouse.promocode.PromoCodesEvent
import ru.yandex.yandexbus.inhouse.promocode.PromoCodesFacade
import ru.yandex.yandexbus.inhouse.promocode.PromocodeSource.CACHE
import ru.yandex.yandexbus.inhouse.promocode.PromocodeSource.NETWORK
import ru.yandex.yandexbus.inhouse.promocode.State.NEW
import ru.yandex.yandexbus.inhouse.promocode.State.VISITED
import ru.yandex.yandexbus.inhouse.promocode.repo.PromoCode
import ru.yandex.yandexbus.inhouse.utils.argumentCaptor
import ru.yandex.yandexbus.inhouse.whenever
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.io.IOException

class PromoCodesPresenterTest : BaseTest() {

    @Mock
    lateinit var promoCodesFacade: PromoCodesFacade
    @Mock
    lateinit var navigator: PromoCodesNavigator
    @Mock
    lateinit var analyticsSender: PromoCodesAnalyticsSender
    @Mock
    lateinit var view: PromoCodesView

    private val showPromoClicksSubject = PublishSubject.create<PromoCodeInfo>()
    private val promocodeAppearanceSubject = PublishSubject.create<PromoCodeInfo>()

    private val promocodesSubject = BehaviorSubject.create<PromoCodesEvent>()

    private val cachedPromoCodes = listOf(
        PromoCodeInfo(promoCode("1", "title1"), NEW),
        PromoCodeInfo(promoCode("2", "title2"), NEW)
    )

    private val networkPromoCodes = listOf(
        PromoCodeInfo(promoCode("3", "title3"), NEW),
        PromoCodeInfo(promoCode("4", "title4"), NEW)
    )

    private val oneMorePromocode = PromoCodeInfo(promoCode("5", "title5"), NEW)

    private val promoCodesFromCache = PromoCodesEvent.CachedData(PromoCodeData.from(cachedPromoCodes, CACHE))
    private val promoCodesFromNetwork = PromoCodesEvent.NetworkData(
        PromoCodeData.from(cachedPromoCodes, CACHE),
        PromoCodeData.from(networkPromoCodes, NETWORK)
    )

    private val promoCodesFromNetworkAfterFirstVisited = PromoCodesEvent.NetworkData(
        PromoCodeData.from(cachedPromoCodes, CACHE),
        PromoCodeData.from(
            listOf(
                PromoCodeInfo(promoCode("4", "title4"), NEW),
                PromoCodeInfo(promoCode("3", "title3"), VISITED)
            ), NETWORK
        )
    )
    private val morePromoCodesFromNetwork = PromoCodesEvent.NetworkData(
        PromoCodeData.from(cachedPromoCodes, CACHE),
        PromoCodeData.from(networkPromoCodes + oneMorePromocode, NETWORK)
    )

    private val networkError = PromoCodeData.from(IOException(), NETWORK)

    @Before
    override fun setUp() {
        super.setUp()

        whenever(view.showPromoClicks).thenReturn(showPromoClicksSubject)
        whenever(view.promoAppearanceOnScreen).thenReturn(promocodeAppearanceSubject)

        whenever(promoCodesFacade.promoCodes()).thenReturn(promocodesSubject)
    }

    @Test
    fun `shows proper state updates`() {

        createAttachStart(view)
        verify(view, never()).showViewState(any())

        promoCodesLoaded(PromoCodesEvent.Loading)
        assertStateShown(1, loading = true)

        promoCodesLoaded(promoCodesFromCache)
        assertStateShown(2, cachedPromoCodes)

        promoCodesLoaded(promoCodesFromNetwork)
        assertStateShown(3, networkPromoCodes)
    }

    @Test
    fun `shows promocodes from cache when network request failed`() {

        createAttachStart(view)
        verify(view, never()).showViewState(any())

        promoCodesLoaded(promoCodesFromCache)
        assertStateShown(1, cachedPromoCodes)

        promoCodesLoaded(PromoCodesEvent.NetworkData(PromoCodeData.from(cachedPromoCodes, CACHE), networkError))
        assertStateShown(1, cachedPromoCodes) //list is the same, same ViewState won't be shown again
    }

    @Test
    fun `shows empty list when when network request failed and cache is empty`() {

        createAttachStart(view)
        verify(view, never()).showViewState(any())

        promoCodesLoaded(PromoCodesEvent.CachedData(PromoCodeData.from(emptyList(), CACHE)))
        assertStateShown(1, emptyList())

        promoCodesLoaded(PromoCodesEvent.NetworkData(PromoCodeData.from(emptyList(), CACHE), networkError))
        assertStateShown(1, emptyList()) //list is the same, same ViewState won't be shown again
    }

    @Test
    fun `doesn't show same ViewState after reopening screen`() {

        val presenter = createAttachStart(view)
        verify(view, never()).showViewState(any())

        promoCodesLoaded(promoCodesFromNetwork)
        assertStateShown(1, networkPromoCodes)

        presenter.onViewStop()
        presenter.onViewStart()
        assertStateShown(1, networkPromoCodes) //list is the same, same ViewState won't be shown again
    }

    @Test
    fun `sends promocodes metrics after changing screen to another and going back`() {

        val presenter = createAttachStart(view)

        whenever(view.visiblePromocodes()).thenReturn(listOf(networkPromoCodes.first()))
        promoCodesLoaded(promoCodesFromNetwork)
        assertStateShown(1, networkPromoCodes)
        verifyPromoCodesListEventSent(1, promoCodesFromNetwork)
        verifyPromoCodeEventSent(1, networkPromoCodes.first().promoCode, position = 1)

        presenter.onViewStop()
        presenter.onDetach()

        presenter.onAttach(view)
        presenter.onViewStart()
        assertStateShown(2, networkPromoCodes)
        verifyPromoCodesListEventSent(2, promoCodesFromNetwork)
        verifyPromoCodeEventSent(2, networkPromoCodes.first().promoCode, position = 1)
    }

    @Test
    fun `send only new promocodes list metrics after minimizing and reopening app on this screen`() {

        val presenter = createAttachStart(view)

        whenever(view.visiblePromocodes()).thenReturn(listOf(networkPromoCodes.first()))
        promoCodesLoaded(promoCodesFromNetwork)
        verify(analyticsSender).promoCodesShown(promoCodesFromNetwork)
        verifyPromoCodeEventSent(1, networkPromoCodes.first().promoCode, position = 1)

        presenter.onViewStop()
        whenever(navigator.isGoingFromBackground()).thenReturn(true)
        presenter.onViewStart()
        verifyNoMoreInteractions(analyticsSender)

        whenever(view.visiblePromocodes()).thenReturn(listOf(cachedPromoCodes.first()))
        promoCodesLoaded(promoCodesFromCache)
        verifyPromoCodesListEventSent(2, promoCodesFromCache)
        verifyPromoCodeEventSent(2, cachedPromoCodes.first().promoCode, position = 1)

        whenever(view.visiblePromocodes()).thenReturn(listOf(networkPromoCodes.first()))
        promoCodesLoaded(morePromoCodesFromNetwork)
        assertStateShown(3, networkPromoCodes + oneMorePromocode)
        verifyPromoCodesListEventSent(3, morePromoCodesFromNetwork)
        verifyPromoCodeEventSent(3, networkPromoCodes.first().promoCode, position = 1)
    }

    @Test
    fun `send promocode appearance events`() {

        createAttachStart(view)

        promoCodesLoaded(morePromoCodesFromNetwork)
        val (first, second, third) = morePromoCodesFromNetwork.networkData.promoCodes

        promocodeAppearanceSubject.onNext(first)
        verifyPromoCodeEventSent(1, first.promoCode, position = 1)

        promocodeAppearanceSubject.onNext(second)
        verifyPromoCodeEventSent(2, second.promoCode, position = 2)

        promocodeAppearanceSubject.onNext(third)
        verifyPromoCodeEventSent(3, third.promoCode, position = 3)

        promocodeAppearanceSubject.onNext(second)
        verify(analyticsSender, times(3)).promoCodeShown(any(), anyInt()) //already sent
    }

    @Test
    fun `processes promo code click`() {

        createAttachStart(view)

        promoCodesLoaded(promoCodesFromNetwork)
        assertStateShown(1, networkPromoCodes)

        val clickedPromocode = networkPromoCodes.first()
        whenever(promoCodesFacade.setPromoCodeVisited(clickedPromocode.promoCode)).thenAnswer {
            promoCodesLoaded(promoCodesFromNetworkAfterFirstVisited) //send update immediately, like real PromoCodesFacade
        }

        showPromoClicksSubject.onNext(clickedPromocode)
        verify(analyticsSender).promoCodeClicked(clickedPromocode.promoCode, 1)
        verify(navigator).toPromoDetailsView(clickedPromocode)
        //promoCodesFromNetworkAfterFirstVisited should not be reported
        verifyPromoCodesListEventSent(1, promoCodesFromNetwork)
    }

    private fun verifyPromoCodeEventSent(invocationNumber: Int, promoCode: PromoCode, position: Int) {
        val promoCodeCaptor = argumentCaptor<PromoCode>()
        val positionCaptor = argumentCaptor<Int>()
        verify(analyticsSender, times(invocationNumber)).promoCodeShown(promoCodeCaptor.capture(), positionCaptor.capture())
        Assert.assertEquals(promoCode, promoCodeCaptor.allValues[invocationNumber - 1])
        Assert.assertEquals(position, positionCaptor.allValues[invocationNumber - 1])
    }

    private fun verifyPromoCodesListEventSent(invocationNumber: Int, event: PromoCodesEvent) {
        val captor = argumentCaptor<PromoCodesEvent>()
        verify(analyticsSender, times(invocationNumber)).promoCodesShown(captor.capture())
        Assert.assertEquals(event, captor.allValues[invocationNumber - 1])
    }

    private fun assertStateShown(
        invocationNumber: Int,
        promoCodes: List<PromoCodeInfo> = emptyList(),
        loading: Boolean = false
    ) {
        val stateCaptor = argumentCaptor<PromoCodesView.ViewState>()
        verify(view, Mockito.times(invocationNumber)).showViewState(stateCaptor.capture())
        Assert.assertEquals(
            PromoCodesView.ViewState(loading, promoCodes),
            stateCaptor.allValues[invocationNumber - 1]
        )
    }

    private fun promoCodesLoaded(event: PromoCodesEvent) = promocodesSubject.onNext(event)

    private fun createAttachStart(view: PromoCodesView) = createPresenter().apply {
        onCreate()
        onAttach(view)
        onViewStart()
    }

    private fun createPresenter() = PromoCodesPresenter(
        promoCodesFacade,
        navigator,
        analyticsSender
    )

    private fun promoCode(id: String, title: String): PromoCode {
        return PromoCodeTestData.emptyPromoCode.copy(
            id = id,
            title = title
        )
    }
}
