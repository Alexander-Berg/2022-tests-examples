package ru.auto.feature.cartinder

import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.feature.cartinder_uploader.data.CartinderReaction
import ru.auto.feature.cartinder_uploader.feature.CartinderUploader
import ru.auto.test.tea.TeaTestFeature

private const val TEST_SELF_OFFER_ID = "TEST_SELF_OFFER_ID"
private const val TEST_TARGET_OFFER_ID = "TEST_TARGET_OFFER_ID"

@RunWith(AllureRunner::class)
class CartinderUploaderTest {

    private fun createFeature() = TeaTestFeature(
        initialState = CartinderUploader.State.Idle,
        reducer = CartinderUploader::reduce,
    )

    @Test
    fun `should retry when reconnected to the network`() {
        val feature = createFeature()

        Allure.step("connected to the internet") {
            feature.accept(CartinderUploader.Msg.OnConnectedToTheInternet)
        }

        Allure.step("should try to resend all saved") {
            Assertions.assertThat(feature.latestEffects).containsExactly(
                CartinderUploader.Eff.TryToResendAllSaved
            )
        }
    }

    @Test
    fun `should send or save like`() {
        val feature = createFeature()

        val likeReaction = CartinderReaction.Like(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("liked offer") {
            feature.accept(CartinderUploader.Msg.OnUserReaction(likeReaction))
        }

        Allure.step("should send or save like") {
            Assertions.assertThat(feature.latestEffects).containsExactly(
                CartinderUploader.Eff.SendOrSaveUserReaction(likeReaction)
            )
        }
    }

    @Test
    fun `should send or save dislike`() {
        val feature = createFeature()

        val dislikeReaction = CartinderReaction.Dislike(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        Allure.step("disliked offer") {
            feature.accept(CartinderUploader.Msg.OnUserReaction(dislikeReaction))
        }

        Allure.step("should send or save dislike") {
            Assertions.assertThat(feature.latestEffects).containsExactly(
                CartinderUploader.Eff.SendOrSaveUserReaction(dislikeReaction)
            )
        }
    }

    @Test
    fun `should clear on deauthorization`() {
        val feature = createFeature()

        Allure.step("user deauthorized") {
            feature.accept(CartinderUploader.Msg.OnUserDeauthorized)
        }

        Allure.step("should clear all saved") {
            Assertions.assertThat(feature.latestEffects).containsExactly(
                CartinderUploader.Eff.ClearAllSaved
            )
        }
    }
}
