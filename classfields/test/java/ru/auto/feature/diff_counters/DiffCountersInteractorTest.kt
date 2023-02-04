package ru.auto.feature.diff_counters

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.ACTIVE
import ru.auto.data.model.data.offer.Counters
import ru.auto.data.model.data.offer.INACTIVE
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.repository.IUserOffersRepository
import ru.auto.feature.diff_counters.data.IOfferCountersRepository
import ru.auto.feature.diff_counters.data.OfferCountersRepository
import ru.auto.feature.diff_counters.data.model.OfferCounters
import ru.auto.feature.diff_counters.data.model.OfferDiff
import ru.auto.test.runner.AllureRobolectricRunner
import rx.Observable
import rx.Single
import kotlin.test.assertEquals

@RunWith(AllureRobolectricRunner::class)
class DiffCountersInteractorTest {

    private val offerCountersRepository: IOfferCountersRepository = mock()
    private val userOffersRepository: IUserOffersRepository = mock()

    @Test
    fun `should get correct diff if counters exist and offer is active`() {
        whenever(offerCountersRepository.getCounters()).thenReturn(Single.just(mockCounters()))
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(mockOffers()))

        val interactor = DiffCountersInteractor(offerCountersRepository, userOffersRepository)
        val expected = OfferDiff(5, 15)
        val actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
    }

    @Test
    fun `should get correct diff if counters exist and offer is inactive`() {
        whenever(offerCountersRepository.getCounters()).thenReturn(Single.just(mockCounters()))
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(
            mockOffers(
                status = INACTIVE
            )
        ))

        val interactor = DiffCountersInteractor(offerCountersRepository, userOffersRepository)
        val expected = OfferDiff(0, 0)
        val actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
    }

    @Test
    fun `should get correct diff if counters does not exist`() {
        whenever(offerCountersRepository.getCounters()).thenReturn(Single.just(null))
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(mockOffers()))

        val interactor = DiffCountersInteractor(offerCountersRepository, userOffersRepository)
        val expected = OfferDiff(0, 0)
        val actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
    }

    @Test
    fun `should get correct diff if counters exist and offer has lost its counters`() {
        whenever(offerCountersRepository.getCounters()).thenReturn(Single.just(mockCounters()))
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(
            mockOffers(
                views = null
            )
        ))

        val interactor = DiffCountersInteractor(offerCountersRepository, userOffersRepository)
        val expected = OfferDiff(0, 15)
        val actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
    }

    @Test
    fun `should get empty diff if counters exist and there are several active offers`() {
        whenever(offerCountersRepository.getCounters()).thenReturn(Single.just(mockCounters()))
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(listOf(
            mockOffer(),
            mockOffer()
        )))

        val interactor = DiffCountersInteractor(offerCountersRepository, userOffersRepository)
        val expected = OfferDiff(0, 0)
        val actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
    }

    @Test
    fun `should update diff if counters does not exist and offers contains active one`() {
        val prefs = getInstrumentation().context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val store = OfferCountersRepository(prefs)
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(
            mockOffers(
                views = 10,
                searchPos = 20
            )
        ))
        val interactor = DiffCountersInteractor(store, userOffersRepository)
        interactor.updateCounters().await()
        var expected = OfferDiff(0, 0)
        var actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(
            mockOffers(
                views = 20,
                searchPos = 10
            )
        ))
        expected = OfferDiff(10, -10)
        actual = interactor.getDiff().toBlocking().value()
        assertEquals(expected, actual)
    }

    companion object {
        private const val OFFER_ID = "abcd"

        private fun mockCounters(views: Int = 10, searchPos: Int = 10) = OfferCounters(
            offerId = OFFER_ID,
            viewsCount = views,
            searchPosition = searchPos
        )

        private fun mockOffers(views: Int? = 15, searchPos: Int? = 25, status: String = ACTIVE): List<Offer> =
            listOf(
                mockOffer(
                    views,
                    searchPos,
                    status
                )
            )

        private fun mockOffer(views: Int? = 15, searchPos: Int? = 25, status: String = ACTIVE): Offer {
            val counters = if (views != null) {
                Counters(all = views, daily = 0, callsAll = 0, callsDaily = 0)
            } else {
                null
            }
            return Offer(
                id = OFFER_ID,
                category = VehicleCategory.CARS,
                counters = counters,
                sellerType = SellerType.PRIVATE,
                commonFeedSearchPosition = searchPos,
                searchPos = searchPos,
                status = status
            )
        }
    }
}
