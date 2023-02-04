package ru.auto.feature.cartinder

import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import ru.auto.data.repository.ICartinderRepository
import ru.auto.data.repository.INetworkInfoRepository
import ru.auto.feature.cartinder_uploader.data.CartinderReaction
import ru.auto.feature.cartinder_uploader.data.CartinderUploadStorage
import ru.auto.feature.cartinder_uploader.feature.CartinderUploader
import ru.auto.feature.cartinder_uploader.feature.CartinderUploaderEffHandler
import rx.Completable
import rx.Single

private const val TEST_SELF_OFFER_ID = "TEST_SELF_OFFER_ID"
private const val TEST_TARGET_OFFER_ID = "TEST_TARGET_OFFER_ID"

@RunWith(AllureRunner::class)
class CartinderUploaderEffHandlerTest {
    private val storage: CartinderUploadStorage = mock()
    private val cartinderRepo: ICartinderRepository = mock()
    private val networkRepo: INetworkInfoRepository = mock()
    private val instance = CartinderUploaderEffHandler(
        storage = storage,
        cartinderRepo = cartinderRepo,
        networkRepo = networkRepo,
    )

    @Test
    fun `should try to send a like to the network`() {
        whenever(cartinderRepo.like(any(), any())).thenReturn(Completable.complete())

        val likeReaction = CartinderReaction.Like(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("send like") {
            instance.invoke(
                CartinderUploader.Eff.SendOrSaveUserReaction(likeReaction)
            ) {}
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(1))
                .like(likeReaction.targetOfferId, likeReaction.selfOfferId)
            verifyNoMoreInteractions(cartinderRepo)
        }
    }

    @Test
    fun `should try to send a dislike to the network`() {
        whenever(cartinderRepo.dislike(any(), any())).thenReturn(Completable.complete())

        val dislikeReaction = CartinderReaction.Dislike(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("send dislike") {
            instance.invoke(
                CartinderUploader.Eff.SendOrSaveUserReaction(dislikeReaction)
            ) {}
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(1))
                .dislike(dislikeReaction.targetOfferId, dislikeReaction.selfOfferId)
            verifyNoMoreInteractions(cartinderRepo)
        }
    }

    @Test
    fun `should save like on network error`() {
        whenever(networkRepo.isConnectedToInternet()).thenReturn(false)
        whenever(cartinderRepo.like(any(), any())).thenReturn(
            Completable.error(
                IllegalStateException("Test exception")
            )
        )
        whenever(storage.saveReaction(any())).thenReturn(Completable.complete())

        val likeReaction = CartinderReaction.Like(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("send like") {
            instance.invoke(
                CartinderUploader.Eff.SendOrSaveUserReaction(likeReaction)
            ) {}
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(1))
                .like(likeReaction.targetOfferId, likeReaction.selfOfferId)
            verifyNoMoreInteractions(cartinderRepo)
        }

        Allure.step("assert that saving happened") {
            verify(storage, times(1))
                .saveReaction(likeReaction)
            verifyNoMoreInteractions(storage)
        }
    }

    @Test
    fun `should save dislike on network error`() {
        whenever(networkRepo.isConnectedToInternet()).thenReturn(false)
        whenever(cartinderRepo.dislike(any(), any())).thenReturn(
            Completable.error(
                IllegalStateException("Test exception")
            )
        )
        whenever(storage.saveReaction(any())).thenReturn(Completable.complete())

        val dislike = CartinderReaction.Dislike(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("send dislike") {
            instance.invoke(
                CartinderUploader.Eff.SendOrSaveUserReaction(dislike)
            ) {}
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(1))
                .dislike(dislike.targetOfferId, dislike.selfOfferId)
            verifyNoMoreInteractions(cartinderRepo)
        }

        Allure.step("assert that saving happened") {
            verify(storage, times(1))
                .saveReaction(dislike)
            verifyNoMoreInteractions(storage)
        }
    }

    @Test
    fun `should not save like if is connected to a network`() {
        whenever(networkRepo.isConnectedToInternet()).thenReturn(true)
        whenever(cartinderRepo.like(any(), any())).thenReturn(
            Completable.error(
                IllegalStateException("Test exception")
            )
        )
        whenever(storage.saveReaction(any())).thenReturn(Completable.complete())

        val likeReaction = CartinderReaction.Like(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("send like") {
            instance.invoke(
                CartinderUploader.Eff.SendOrSaveUserReaction(likeReaction)
            ) {}
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(1))
                .like(likeReaction.targetOfferId, likeReaction.selfOfferId)
            verifyNoMoreInteractions(cartinderRepo)
        }

        Allure.step("assert that saving happened") {
            verify(storage, never())
                .saveReaction(likeReaction)
            verifyNoMoreInteractions(storage)
        }
    }

    @Test
    fun `should not save dislike if is connected to a network`() {
        whenever(networkRepo.isConnectedToInternet()).thenReturn(true)
        whenever(cartinderRepo.dislike(any(), any())).thenReturn(
            Completable.error(
                IllegalStateException("Test exception")
            )
        )
        whenever(storage.saveReaction(any())).thenReturn(Completable.complete())

        val dislike = CartinderReaction.Dislike(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("send dislike") {
            instance.invoke(
                CartinderUploader.Eff.SendOrSaveUserReaction(dislike)
            ) {}
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(1))
                .dislike(dislike.targetOfferId, dislike.selfOfferId)
            verifyNoMoreInteractions(cartinderRepo)
        }

        Allure.step("assert that saving happened") {
            verify(storage, never())
                .saveReaction(dislike)
            verifyNoMoreInteractions(storage)
        }
    }

    @Test
    fun `should clear storage`() {
        whenever(storage.clear()).thenReturn(Completable.complete())

        Allure.step("send clear eff") {
            instance.invoke(
                CartinderUploader.Eff.ClearAllSaved
            ) {}
        }

        Allure.step("assert that saving happened") {
            verify(storage, times(1))
                .clear()
            verifyNoMoreInteractions(storage)
        }
    }

    @Test
    fun `should resend all saved`() {
        whenever(cartinderRepo.dislike(any(), any())).thenReturn(Completable.complete())
        whenever(cartinderRepo.like(any(), any())).thenReturn(Completable.complete())
        whenever(storage.removeReaction(any())).thenReturn(Completable.complete())

        val reactions = setOf(
            CartinderReaction.Dislike(
                selfOfferId = TEST_SELF_OFFER_ID,
                targetOfferId = "${TEST_TARGET_OFFER_ID}_1",
            ),
            CartinderReaction.Like(
                selfOfferId = TEST_SELF_OFFER_ID,
                targetOfferId = "${TEST_TARGET_OFFER_ID}_2",
            ),
            CartinderReaction.Dislike(
                selfOfferId = TEST_SELF_OFFER_ID,
                targetOfferId = "${TEST_TARGET_OFFER_ID}_3",
            )
        )

        whenever(storage.getReactions()).thenReturn(Single.just(reactions))

        Allure.step("try to resend saved") {
            instance.invoke(
                CartinderUploader.Eff.TryToResendAllSaved
            ) {}
        }

        Allure.step("assert that storage called") {
            verify(storage, times(1)).getReactions()
            verify(storage, times(3)).removeReaction(any())
            verifyNoMoreInteractions(storage)
        }

        Allure.step("assert that cartinder repo called") {
            verify(cartinderRepo, times(2))
                .dislike(any(), any())
            verify(cartinderRepo, times(1))
                .like(any(), any())
            verifyNoMoreInteractions(cartinderRepo)
        }
    }
}
