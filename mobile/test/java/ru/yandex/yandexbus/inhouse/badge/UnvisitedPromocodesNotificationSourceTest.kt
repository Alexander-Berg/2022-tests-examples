package ru.yandex.yandexbus.inhouse.badge

import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.promocode.PromoCodeData
import ru.yandex.yandexbus.inhouse.promocode.PromoCodeInfo
import ru.yandex.yandexbus.inhouse.promocode.PromoCodeTestData
import ru.yandex.yandexbus.inhouse.promocode.PromoCodesEvent
import ru.yandex.yandexbus.inhouse.promocode.PromoCodesFacade
import ru.yandex.yandexbus.inhouse.promocode.PromocodeSource
import ru.yandex.yandexbus.inhouse.promocode.State
import ru.yandex.yandexbus.inhouse.promocode.repo.PromoCode
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable

class UnvisitedPromocodesNotificationSourceTest : BaseTest() {

    @Mock
    private lateinit var promoCodesFacade: PromoCodesFacade

    @Test
    fun `notification when there is new promocode`() {
        assertNotification(
            generateEvent(
                generatePromoCodesWithStates(State.NEW, State.VISITED),
                generatePromoCodesWithStates(State.NEW, State.VISITED)
            ),
            notificationVisible = true
        )
    }

    @Test
    fun `no notification when all promocodes visited`() {
        assertNotification(
            generateEvent(
                generatePromoCodesWithStates(State.VISITED, State.VISITED),
                generatePromoCodesWithStates(State.VISITED, State.VISITED)
            ),
            notificationVisible = false
        )
    }

    @Test
    fun `no notification when cache is not empty but network answer is`() {
        assertNotification(
            generateEvent(
                generatePromoCodesWithStates(State.NEW, State.VISITED),
                generatePromoCodesWithStates()
            ),
            notificationVisible = false
        )
    }

    @Test
    fun `no notification when loading`() {
        whenever(promoCodesFacade.promoCodes()).thenReturn(Observable.just(PromoCodesEvent.Loading))

        val notificationSource = UnvisitedPromocodesNotificationSource(promoCodesFacade)
        notificationSource.notificationAvailability()
            .test()
            .assertNoErrors()
            .assertNoValues()
    }

    private fun assertNotification(event: PromoCodesEvent, notificationVisible: Boolean) {
        whenever(promoCodesFacade.promoCodes()).thenReturn(Observable.just(event))

        val notificationSource = UnvisitedPromocodesNotificationSource(promoCodesFacade)
        notificationSource.notificationAvailability()
            .test()
            .assertNoErrors()
            .assertValue(notificationVisible)
    }

    private fun generateEvent(
        fromCache: List<PromoCodeInfo>,
        fromNetwork: List<PromoCodeInfo>
    ): PromoCodesEvent {
        return PromoCodesEvent.NetworkData(
            PromoCodeData.from(fromCache, PromocodeSource.CACHE),
            PromoCodeData.from(fromNetwork, PromocodeSource.NETWORK)
        )
    }

    private fun generatePromoCodesWithStates(vararg states: State): List<PromoCodeInfo> {
        return states.mapIndexed { i, state -> generatePromocodeInfo(i, state) }
    }

    private fun generatePromocodeInfo(id: Int, state: State): PromoCodeInfo {
        return PromoCodeInfo(generatePromoCode(id), state)
    }

    private fun generatePromoCode(id: Int): PromoCode {
        return PromoCodeTestData.emptyPromoCode.copy(id = id.toString())
    }
}
