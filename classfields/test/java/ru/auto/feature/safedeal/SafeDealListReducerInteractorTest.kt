package ru.auto.feature.safedeal

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.Mockito
import org.mockito.Mockito.anyMap
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import ru.auto.data.model.data.offer.BuyerStep
import ru.auto.data.model.data.offer.Deal
import ru.auto.data.model.data.offer.DealStep
import ru.auto.data.model.data.offer.ParticipantType
import ru.auto.data.model.data.offer.SafeDealOfferMetaInfo
import ru.auto.data.model.data.offer.SellerStep
import ru.auto.data.repository.ISafeDealPromoOfferOverlayRepository
import ru.auto.data.repository.ISafeDealSellerOnboardingRepository
import ru.auto.data.repository.user.IUserRepository
import ru.auto.feature.safedeal.interactor.SafeDealInteractor
import ru.auto.feature.safedeal.repository.ISafeDealRepository
import ru.auto.feature.safedeal.storage.ISafeDealLocalRepository
import ru.auto.test.core.RxTestAndroid
import rx.Observable
import rx.Single


@RunWith(AllureRunner::class)
 @Suppress("UnstableApiUsage")
class SafeDealListReducerInteractorTest : RxTestAndroid() {

    private val mockUserRepo = mock(IUserRepository::class.java)
    private val mockStorage = mock(ISafeDealLocalRepository::class.java)
    private val mockStorageForOffersSource = mock(ISafeDealLocalRepository::class.java)
    private val mockRepository = mock(ISafeDealRepository::class.java)
    private val mockSellerOnboardingRepository = mock(ISafeDealSellerOnboardingRepository::class.java)
    private val promoOfferOverlayRepository = mock(ISafeDealPromoOfferOverlayRepository::class.java)

    @Test
    fun emptyList() {
        val controller = createSafeDealInteractor()
        val result = controller.observeDealList().getLastValue()
        assert(result.isEmpty())
        verify(mockRepository, never()).getDeals()
    }

    @Test
    fun updateListTest() {
        Mockito.`when`(mockRepository.getDeals()).thenReturn(Single.just(createSimpleListDeals()))
        val controller = createSafeDealInteractor()
        controller.updateDealList().await()
        val result = controller.observeDealList().toBlocking().first()
        assert(result.isNotEmpty())
        assert(result.size == 2)
        verify(mockRepository).getDeals()
    }

    @Test
    fun updateWithException() {
        val exception = Exception("something went wrong")
        Mockito.`when`(mockRepository.getDeals())
            .thenReturn(Single.just(createSimpleListDeals()))
            .thenReturn(Single.error(exception))
        val controller = createSafeDealInteractor()
        controller.updateDealList().await()
        var result = controller.observeDealList().getLastValue()
        assert(result.size == 2)
        controller.updateDealList().onErrorComplete { true }
        result = controller.observeDealList().getLastValue()
        assert(result.size == 2)
    }

    @Test
    fun clearTest() {
        Mockito.`when`(mockRepository.getDeals()).thenReturn(Single.just(createSimpleListDeals()))
        val controller = createSafeDealInteractor()
        controller.updateDealList().await()
        var result = controller.observeDealList().getLastValue()
        assert(result.isNotEmpty())
        controller.clear().await()
        result = controller.observeDealList().getLastValue()
        assert(result.isEmpty())
        verify(mockStorage).clear()
        verify(mockStorageForOffersSource).clear()
    }

    @Test
    fun getNewDeals() {
        Mockito.`when`(mockRepository.getDeals())
            .thenReturn(Single.just(createSimpleListDeals().map { it.copy(participantType = ParticipantType.SELLER) }))
        Mockito.`when`(mockStorage.getDealIds())
            .thenReturn(emptyMap())
            .thenReturn(mapOf("0" to DealStep.DEAL_CREATED))
        val controller = createSafeDealInteractor()
        controller.updateDealList().await()
        val allDealResult = controller.observeDealList().getLastValue()
        assert(allDealResult.isNotEmpty())
        var newDealResult = controller.observeNewDeals().getLastValue()
        assertEquals(allDealResult, newDealResult)
        verify(mockStorage).getDealIds()
        verify(mockStorage).saveDealIds(anyMap())
        newDealResult = controller.observeNewDeals().getLastValue()
        assert(newDealResult.size == 1)
    }

    private fun createSafeDealInteractor() = SafeDealInteractor(
        userRepository = mockUserRepo,
        safeDealRepository = mockRepository,
        localRepository = mockStorage,
        localRepositoryForOffersSource = mockStorageForOffersSource,
        sellerOnboardingRepository = mockSellerOnboardingRepository,
        promoOfferOverlayRepository = promoOfferOverlayRepository,
    )

    private fun createSimpleListDeals() = listOf(createDeal("0"), createDeal("1"))

    private fun createDeal(id: String): Deal =
        Deal(
            dealId = id,
            step = DealStep.DEAL_CREATED,
            participantType = ParticipantType.BUYER,
            sellerStep = SellerStep.SELLER_APPROVED_DEAL,
            buyerId = "0",
            buyerStep = BuyerStep.BUYER_APPROVED_DEAL,
            url = "url",
            cancelledBy = null,
            meta = SafeDealOfferMetaInfo(
                offerId = "0",
                subjectShortTitle = "subject name",
                price = 100,
                sellerName = null,
                buyerName = null,
                imageUrl = "",
                color = "",
                engineType = "",
                vin = "",
                offer = null,
            ),
        )

    private fun <T> Observable<T>.getLastValue() = toBlocking().first()
}
