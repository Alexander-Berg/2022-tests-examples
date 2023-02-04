package ru.auto.data.interactor

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.data.model.User
import ru.auto.data.model.catalog.Suggest
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.draft.PublishInfo
import ru.auto.data.repository.IDraftRepository
import ru.auto.data.repository.IPhotoUploadRepository
import ru.auto.data.repository.ISafeDealSellerOnboardingRepository
import ru.auto.data.repository.ISuggestRepository
import ru.auto.data.repository.user.IUserRepository
import ru.auto.testextension.testWithSubscriber
import rx.Observable
import rx.Single

@RunWith(AllureRunner::class)
class DraftInteractorTest {

    private lateinit var draftRepo: IDraftRepository

    private val suggestRepo: ISuggestRepository = mock()
    private val currentOffer: Offer = mock()
    private val updatedOffer: Offer = mock()
    private val photoUploadRepository: IPhotoUploadRepository = mock()
    private val sellerOnboardingRepository: ISafeDealSellerOnboardingRepository = mock()
    private val publishedOffer: Offer = mock()
    private val publishInfo: PublishInfo = PublishInfo(publishedOffer, true)
    private val userRepository: IUserRepository = mock()

    private val suggest: Suggest = mock()
    private val someOffer: Offer = mock()
    private val offerId = "1234"
    private val offerCategory = "15"

    @Before
    fun setUp() {
        draftRepo = mock()

        whenever(draftRepo.getDraft()).thenReturn(Single.just(currentOffer))
        whenever(draftRepo.getDraft(forceNetwork = any(), equipments = anyOrNull())).thenReturn(Single.just(currentOffer))
        whenever(draftRepo.publishOffer(any(), any(), anyOrNull())).thenReturn(Single.just(publishedOffer))
        whenever(draftRepo.updateDraft(any(), anyOrNull())).thenReturn(Single.just(updatedOffer))
        whenever(suggestRepo.getSuggest(any())).thenReturn(Single.just(suggest))
        whenever(updatedOffer.id).thenReturn(offerId)
        whenever(userRepository.observeUser()).thenReturn(Observable.just(User.Unauthorized))
        whenever(userRepository.user).thenReturn(User.Unauthorized)

        whenever(publishedOffer.servicePrices).thenReturn(emptyList())
    }

    @Test
    fun `get, change and save current draft`() {
        val interactor = getDraftInteractor(0)
        val changedOffer: Offer = mock()

        // get, change and save
        testWithSubscriber(interactor.getDraft()) { it.assertValue(currentOffer) }
        testWithSubscriber(interactor.onDraftChanged(changedOffer)) { it.assertValue(updatedOffer) }
        testWithSubscriber(interactor.saveDraftAndPublish(currentOffer)) { it.assertValue(publishInfo) }

        verify(draftRepo, times(1)).updateDraft(changedOffer)
        verify(draftRepo, times(1)).publishOffer(any(), any(), anyOrNull())
    }

    @Test
    fun `should update draft after successfully publishing`(){
        val interactor = getDraftInteractor(0)

        testWithSubscriber(interactor.saveDraftAndPublish(currentOffer))

        verify(draftRepo, times(1)).updateDraft(currentOffer)

        testWithSubscriber(interactor.saveDraft(currentOffer))

        verify(draftRepo, times(2)).updateDraft(currentOffer)
    }

    @Test
    fun `get, change and save draft from saveDraftAndPublish offer`() {
        val interactor = getDraftInteractor(0)

        testWithSubscriber(interactor.getDraft()) { it.assertValue(currentOffer) }

        testWithSubscriber(interactor.onDraftChanged(currentOffer))
        testWithSubscriber(interactor.saveDraftAndPublish(currentOffer)) { it.assertValue(publishInfo) }

        verify(draftRepo, times(2)).updateDraft(currentOffer)
        verify(draftRepo, times(1)).publishOffer(any(), any(), anyOrNull())
    }

    @Test
    fun `do not update too often, only if updateDelay time passed`() {
        val interactor = getDraftInteractor(1000)
        testWithSubscriber(interactor.onDraftChanged(someOffer))
        testWithSubscriber(interactor.onDraftChanged(someOffer))
        testWithSubscriber(interactor.onDraftChanged(someOffer))
        verify(draftRepo, times(1)).updateDraft(someOffer)
    }

    private fun getDraftInteractor(updateDelay: Long) = step("Setup DraftInteractor") {
        parameter("updateDelay", updateDelay)
        DraftInteractor(
            draftRepository = draftRepo,
            suggestRepository = suggestRepo,
            photoUploadRepository = photoUploadRepository,
            sellerOnboardingRepository = sellerOnboardingRepository,
            updateDelay = updateDelay,
            category = offerCategory,
            userRepository = userRepository,
        )
    }

}
