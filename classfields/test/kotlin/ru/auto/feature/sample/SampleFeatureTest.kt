package ru.auto.feature.sample

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.search.SearchContext
import ru.auto.data.util.LoadableRequestData
import ru.auto.data.util.Try
import ru.auto.data.util.maybeValue
import ru.auto.feature.sample.Sample.OfferResponse
import ru.auto.test.runner.AllureRobolectricRunner
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testextension.kotlinFixtureDefault
import ru.auto.testextension.prepareParameter
import ru.auto.testextension.withPropertyValue

/**
 * Annotate your test classes with @RunWith annotations and Allure runners
 * This will generate reports. If you don't do this, test will not be included
 * in the allure report.
 *
 * If you need to use robolectric in your tests, use [AllureRobolectricRunner] for that.
 */
@RunWith(AllureRunner::class)
class SampleFeatureTest {

    private val fixture = kotlinFixtureDefault()

    /**
     * KotlinFixture can be used to generate complex objects like offers if you dont care about its contents
     */
    @Test
    fun `should load and display page`() {
        val feature = prepareFeature()

        val page = feature.currentState.offerCards.request
        val offers = prepareParameter("offers", fixture.asSequence<Offer>().take(page.size).toList())
        step("When successful offer response") {
            feature.accept(Sample.Msg.OnOfferResponse(page, Try.Success(OfferResponse(offers))))
        }

        step("It should collect offers as main page") {
            assertThat(feature.currentState.offerCards).isInstanceOf(LoadableRequestData.Success::class.java)
            assertThat(feature.currentState.offerCards.maybeValue?.listing)
                .hasSize(5)
                .allMatch { card ->
                    card.id in offers.map { it.id }
                    card.price in offers.map { it.getRurPrice() }
                }
        }
    }


    /**
     * You can build complex scenarios freely
     */
    @Test
    fun `should fail loading offers and retry`() {
        val feature = prepareFeature()

        val th = Throwable()
        val page = feature.currentState.offerCards.request
        step("When failed offer response") {
            feature.accept(Sample.Msg.OnOfferResponse(page, Try.Error(th)))
        }

        step("It should collect error as part of the state") {
            assertThat(feature.currentState.offerCards).isInstanceOf(LoadableRequestData.Failure::class.java)
            assertThat(feature.currentState.offerCards.maybeValue).isNull()
            assertThat((feature.currentState.offerCards as LoadableRequestData.Failure).error).isEqualTo(th)
        }

        step("When Retry click") {
            feature.accept(Sample.Msg.OnRetryClicked)
        }

        step("It should translate to loading state again and emit new request") {
            assertThat(feature.currentState.offerCards).isInstanceOf(LoadableRequestData.Loading::class.java)
            assertThat(feature.latestEffects).containsExactly(
                Sample.Eff.LoadOffers(
                    page,
                    feature.currentState.request
                )
            )
        }
    }


    @Test
    fun `should emit like effect when like clicked`() {
        val feature = prepareFeature()
        val offer = prepareParameter("offer", fixture<Offer>())
        val page = feature.currentState.offerCards.request
        step("Given feature with one offer") {
            feature.accept(Sample.Msg.OnOfferResponse(page, Try.Success(OfferResponse(listOf(offer)))))
        }
        step("When clicked like") {
            feature.accept(Sample.Msg.OnOfferLikeClicked)
        }
        step("It should load like response") {
            assertThat(feature.currentState.loadingLike).isTrue()
            assertThat(feature.latestEffects).containsExactly(Sample.Eff.FavOffer(offer.id))
        }
    }


    @Test
    fun `should go to next card effect when dislike clicked`() {
        val feature = prepareFeature()
        val topOffer = prepareParameter("heading offer", fixture<Offer>())
        val page = feature.currentState.offerCards.request
        val offers = prepareParameter("tail offers", fixture.asSequence<Offer>().take(page.size - 1).toList())
        step("Given feature with 10 offers") {
            feature.accept(Sample.Msg.OnOfferResponse(page, Try.Success(OfferResponse(listOf(topOffer) + offers))))
        }
        step("When clicked dislike") {
            feature.accept(Sample.Msg.OnOfferDislikeClicked)
        }
        step("It should go to next page and report dislike") {
            assertThat(feature.currentState.offerCards.maybeValue?.position)
                .isEqualTo(1)
            assertThat(feature.currentState.offerCards.maybeValue?.listing)
                .element(feature.currentState.offerCards.maybeValue?.position!!)
                .has(withPropertyValue(Sample.OfferCard::id, offers.first().id))
            assertThat(feature.latestEffects).containsExactly(Sample.Eff.ReportOfferDislike(topOffer.id))
        }
    }

    companion object {
        private val context = Sample.Context(
            SearchContext.LISTING
        )

        private fun prepareFeature() = TeaTestFeature(
            Sample.initialState(context),
            Sample::reducer
        )
    }
}
