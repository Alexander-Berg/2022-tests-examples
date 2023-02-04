@file:Suppress("MagicNumber")

package ru.auto.feature.cartinder

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.CarInfo
import ru.auto.data.model.data.offer.MarkInfo
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.util.LoadableData
import ru.auto.feature.cartinder.presentation.Cartinder
import ru.auto.test.tea.TeaTestFeature

private const val TEST_OFFER_ID_FOR_EXCHANGE = "TEST_OFFER_ID_FOR_EXCHANGE"
private const val DEFALULT_PORTION_SIZE = 10

@RunWith(AllureRunner::class)
class CartinderFeatureTest {

    private val sampleOffer = Offer(
        category = VehicleCategory.CARS,
        id = "0",
        sellerType = SellerType.PRIVATE,
        carInfo = CarInfo(
            markInfo = MarkInfo(
                name = "TestOffer"
            )
        )
    )

    private val receivedOfferForExchange = sampleOffer.copy(id = TEST_OFFER_ID_FOR_EXCHANGE)

    private fun createCardsPortion(size: Int): List<Cartinder.Card> {
        val result = mutableListOf<Cartinder.Card>()
        (0 until size).forEach { index ->
            result.add(
                index, Cartinder.Card(
                    sampleOffer.copy(id = index.toString()),
                    engineSpec = "TestEngine $index",
                    diffPrice = index.toLong(),
                    contactSellerType = Cartinder.ContactSellerType.None,
                )
            )
        }
        return result
    }


    private fun createFeature(
        context: Cartinder.Context,
        screenState: LoadableData<Cartinder.CardsResult>,
    ) = TeaTestFeature(
        initialState = Cartinder.State(
            context = context,
            screenState = screenState,
        ),
        reducer = Cartinder::reduce
    )

    private fun createPreUsedFeature() = createFeature(
        context = Cartinder.Context.OfferToExchange(receivedOfferForExchange),
        screenState = LoadableData.Success(
            Cartinder.CardsResult(
                cards = createCardsPortion(DEFALULT_PORTION_SIZE),
                lastCardsPortionSize = DEFALULT_PORTION_SIZE,
                cardPosition = DEFALULT_PORTION_SIZE / 2,
                afterCardsScreen = Cartinder.AfterCardsScreen.Loading,
            )
        ),
    )

    @Test
    fun `should successfully load two portions`() {
        val feature = createFeature(
            context = Cartinder.Context.OfferId(TEST_OFFER_ID_FOR_EXCHANGE),
            screenState = LoadableData.Loading,
        )
        step("received offer model for exchange from network") {
            feature.accept(
                Cartinder.Msg.OnOfferLoaded(receivedOfferForExchange)
            )
        }
        step(
            "context should be changed to offer and loading of new portion should be started"
        ) {
            assertThat(
                (feature.currentState.context as Cartinder.Context.OfferToExchange).offer
            ).isEqualTo(receivedOfferForExchange)
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.LoadNewPortion(receivedOfferForExchange),
                Cartinder.Eff.Onboarding.CheckFirstTimeAvailability,
            )
        }

        val loadedPortion = createCardsPortion(DEFALULT_PORTION_SIZE)
        val firstPortionSuccessData = LoadableData.Success(
            value = Cartinder.CardsResult(
                cards = loadedPortion,
                lastCardsPortionSize = DEFALULT_PORTION_SIZE,
            )
        )
        step("received new portion") {
            feature.accept(
                Cartinder.Msg.OnNewCardsPortionLoaded(loadedPortion)
            )
        }

        step(
            "should successfully place loaded portion to the feature's state"
        ) {
            assertThat(feature.currentState.screenState).isEqualTo(firstPortionSuccessData)
        }

        step(
            "move forward by a half of default portion size num of cards"
        ) {
            repeat(DEFALULT_PORTION_SIZE / 2) {
                feature.accept(Cartinder.Msg.OnMoveForward)
            }
        }

        step(
            "a new portion loading should be called"
        ) {
            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cardPosition
            ).isEqualTo(DEFALULT_PORTION_SIZE / 2)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.afterCardsScreen
            ).isEqualTo(Cartinder.AfterCardsScreen.Loading)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.lastCardsPortionSize
            ).isEqualTo(DEFALULT_PORTION_SIZE)

            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.LoadNewPortion(receivedOfferForExchange)
            )
        }

        step("received new portion again") {
            feature.accept(
                Cartinder.Msg.OnNewCardsPortionLoaded(loadedPortion)
            )
        }


        step(
            "check result of successful loading of second portion"
        ) {
            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cardPosition
            ).isEqualTo(DEFALULT_PORTION_SIZE / 2)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.afterCardsScreen
            ).isEqualTo(Cartinder.AfterCardsScreen.Obscurity)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cards.size
            ).isEqualTo(DEFALULT_PORTION_SIZE * 2)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.lastCardsPortionSize
            ).isEqualTo(DEFALULT_PORTION_SIZE)

        }

        step(
            "move forward by a whole portion, " +
                "so total move will be equals 1.5 of the portion size"
        ) {
            repeat(DEFALULT_PORTION_SIZE) {
                feature.accept(Cartinder.Msg.OnMoveForward)
            }
        }


        step(
            "a new portion loading should be called a third time"
        ) {
            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cardPosition
            ).isEqualTo((DEFALULT_PORTION_SIZE * 1.5).toInt())

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.afterCardsScreen
            ).isEqualTo(Cartinder.AfterCardsScreen.Loading)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cards.size
            ).isEqualTo(DEFALULT_PORTION_SIZE * 2)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.lastCardsPortionSize
            ).isEqualTo(DEFALULT_PORTION_SIZE)

            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.LoadNewPortion(receivedOfferForExchange)
            )
        }
    }


    @Test
    fun `should process portions correctly till the end`() {
        val feature = createPreUsedFeature()

        step("feed end reached") {
            feature.accept(Cartinder.Msg.OnEndReached)
        }

        step("should correctly change after cards screen") {
            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.afterCardsScreen
            ).isEqualTo(Cartinder.AfterCardsScreen.End)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cardPosition
            ).isEqualTo(DEFALULT_PORTION_SIZE / 2)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cards.size
            ).isEqualTo(DEFALULT_PORTION_SIZE)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.lastCardsPortionSize
            ).isEqualTo(DEFALULT_PORTION_SIZE)
        }
    }


    @Test
    fun `should process portions correctly till the error`() {
        val feature = createPreUsedFeature()

        step("error when loading new portion") {
            val testException = IllegalStateException("TestException")
            feature.accept(Cartinder.Msg.OnNewCardsPortionLoadingError(testException))
        }

        step("should correctly change after cards screen") {
            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.afterCardsScreen
            ).isEqualTo(Cartinder.AfterCardsScreen.Error)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cardPosition
            ).isEqualTo(DEFALULT_PORTION_SIZE / 2)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.cards.size
            ).isEqualTo(DEFALULT_PORTION_SIZE)

            assertThat(
                (feature.currentState.screenState as LoadableData.Success).value.lastCardsPortionSize
            ).isEqualTo(DEFALULT_PORTION_SIZE)
        }
    }

    @Test
    fun `should successfully like cards`() {
        val feature = createPreUsedFeature()

        step("card moved by swipe or by side effect") {
            feature.accept(Cartinder.Msg.OnLike)
        }

        step("should move card on ui in right direction") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.CommitLike(
                    offerId = (DEFALULT_PORTION_SIZE / 2).toString(),
                    selfOfferId = receivedOfferForExchange.id,
                )
            )
        }

        step("like offer by button") {
            feature.accept(Cartinder.Msg.OnLikeClick)
        }

        step("should move card on ui in right direction") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.MoveLike
            )
        }
    }

    @Test
    fun `should successfully dislike cards`() {
        val feature = createPreUsedFeature()

        step("card moved by swipe or by side effect") {
            feature.accept(Cartinder.Msg.OnDislike)
        }

        step("should move card on ui in right direction") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.CommitDislike(
                    offerId = (DEFALULT_PORTION_SIZE / 2).toString(),
                    selfOfferId = receivedOfferForExchange.id,
                ),
                Cartinder.Eff.Onboarding.CheckFirstDislikeAvailability
            )
        }

        step("dislike offer by button") {
            feature.accept(Cartinder.Msg.OnDislikeClick)
        }

        step("should move card on ui in right direction") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.MoveDislike
            )
        }
    }

    @Test
    fun `should retry offer loading when was error`() {
        val feature = createFeature(
            context = Cartinder.Context.OfferId(TEST_OFFER_ID_FOR_EXCHANGE),
            screenState = LoadableData.Loading,
        )

        val testException = IllegalStateException("TestException")
        step("error when offer loading") {
            feature.accept(Cartinder.Msg.OnOfferLoadingError(testException))
        }

        step("should be in error state") {
            assertThat(feature.currentState.screenState).isEqualTo(
                LoadableData.Failure(testException)
            )
        }

        step("click on retry button") {
            feature.accept(Cartinder.Msg.OnRetryLoadClick)
        }

        step("should start loading offer again") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.LoadOffer(TEST_OFFER_ID_FOR_EXCHANGE)
            )
        }
    }

    @Test
    fun `should retry portion loading when was error`() {
        val feature = createFeature(
            context = Cartinder.Context.OfferId(TEST_OFFER_ID_FOR_EXCHANGE),
            screenState = LoadableData.Loading,
        )

        step("successfully loaded offer") {
            feature.accept(Cartinder.Msg.OnOfferLoaded(receivedOfferForExchange))
        }

        step("should start loading new portion") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.LoadNewPortion(receivedOfferForExchange),
                Cartinder.Eff.Onboarding.CheckFirstTimeAvailability,
            )
        }

        val testException = IllegalStateException("TestException")
        step("error to load new portion") {
            feature.accept(Cartinder.Msg.OnNewCardsPortionLoadingError(testException))
        }

        step("should start loading new portion") {
            assertThat(feature.currentState.screenState).isEqualTo(
                LoadableData.Failure(testException)
            )
        }

        step("click on retry button") {
            feature.accept(Cartinder.Msg.OnRetryLoadClick)
        }

        step("should start loading new portion again") {
            assertThat(feature.latestEffects).containsExactly(
                Cartinder.Eff.LoadNewPortion(receivedOfferForExchange)
            )
        }
    }
}
