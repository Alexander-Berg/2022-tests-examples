package ru.auto.feature.carfax.ui.presenter

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.core_ui.ui.viewmodel.Resources
import ru.auto.data.model.autocode.yoga.AdditionalYogaLayoutData
import ru.auto.data.model.autocode.yoga.ReportOfferInfo
import ru.auto.data.model.carfax.PageElement
import ru.auto.data.model.carfax.Text
import ru.auto.data.model.common.IComparableItem
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testextension.checkResult
import ru.auto.testextension.kotlinFixtureDefault
import ru.auto.testextension.perform
import ru.auto.testextension.prepareParameter
import ru.auto.testextension.withPropertyValue

@RunWith(AllureRunner::class)
class CarfaxReportFeatureFavoritesTest {
    private val fixture = kotlinFixtureDefault()

    @Test
    fun `should ignore favorite messages if reportOfferInfo is null`() {
        prepareFeature(null).perform("send toggle favorite message") { feature ->
            feature.accept(CarfaxReport.Msg.FavoriteClicked)
        }.checkResult("no effects sent") { feature ->
            assertThat(feature.latestEffects).isEmpty()
            assertThat((feature.currentState.content as CarfaxReport.Content.Loaded).modelSsr.additionalData).has(
                withPropertyValue(AdditionalYogaLayoutData::reportOfferInfo, null)
            )
        }
    }

    @Test
    fun `should add to favorites when clicking favorite`() {
        val offerId = fixture<String>()
        val reportOfferInfo = prepareParameter(
            "report info not favorite", ReportOfferInfo(
                offerId,
                noLicensePlatePhoto = false,
                isFavorite = false
            )
        )

        prepareFeature(reportOfferInfo).perform("send toggle favorite message") { feature ->
            feature.accept(CarfaxReport.Msg.FavoriteClicked)
        }.checkResult("send effect with favorite request and set like") { feature ->
            assertThat(feature.latestEffects).containsExactlyInAnyOrder(
                CarfaxReport.Effect.LogFavoriteEffect(favorite = true),
                CarfaxReport.Effect.SetOfferFavorite(offerId, favorite = true),
            )
            assertForFavoriteStatusInOfferInfo(feature, offerId, true)
        }.perform("send success response message") { feature ->
            feature.accept(CarfaxReport.Msg.FavoriteResponse(null))
        }.checkResult("should make no effects and dont change state") { feature ->
            assertThat(feature.latestEffects).isEmpty()
            assertForFavoriteStatusInOfferInfo(feature, offerId, true)
        }
    }

    @Test
    fun `should remove from favorites when clicking favorite if offer already favorite`() {
        val offerId = fixture<String>()
        val reportOfferInfo = prepareParameter(
            "report info favorite", ReportOfferInfo(
                offerId,
                noLicensePlatePhoto = false,
                isFavorite = true
            )
        )

        prepareFeature(reportOfferInfo).perform("send toggle favorite message") { feature ->
            feature.accept(CarfaxReport.Msg.FavoriteClicked)
        }.checkResult("send effect with favorite request and remove favorite") { feature ->
            assertThat(feature.latestEffects).containsExactlyInAnyOrder(
                CarfaxReport.Effect.LogFavoriteEffect(favorite = false),
                CarfaxReport.Effect.SetOfferFavorite(offerId, favorite = false),
            )
            assertForFavoriteStatusInOfferInfo(feature, offerId, false)
        }
    }

    @Test
    fun `should reset favorite status if failed`() {
        val offerId = fixture<String>()
        val reportOfferInfo = prepareParameter(
            "report info not favorite", ReportOfferInfo(
                offerId,
                noLicensePlatePhoto = false,
                isFavorite = false
            )
        )

        prepareFeature(reportOfferInfo).perform("send toggle favorite message") { feature ->
            feature.accept(CarfaxReport.Msg.FavoriteClicked)
        }.perform("send favorite response with error status") { feature ->
            feature.accept(CarfaxReport.Msg.FavoriteResponse(favorite = false))
        }.checkResult("send effect with error snack request and set favorite status back") { feature ->
            assertThat(feature.latestEffects).containsExactly(
                CarfaxReport.Effect.ShowSnack(Resources.Text.ResId(R.string.carfax_favorite_error))
            )
            assertForFavoriteStatusInOfferInfo(feature, offerId, false)
        }
    }

    companion object {
        private fun assertForFavoriteStatusInOfferInfo(
            feature: TeaTestFeature<CarfaxReport.Msg, CarfaxReport.State, CarfaxReport.Effect>,
            offerId: String,
            isFavorite: Boolean
        ) {
            assertThat(feature.currentState.content).isInstanceOf(CarfaxReport.Content.Loaded::class.java)
            assertThat((feature.currentState.content as CarfaxReport.Content.Loaded).modelSsr.additionalData).has(
                withPropertyValue(
                    AdditionalYogaLayoutData::reportOfferInfo, ReportOfferInfo(
                        offerId,
                        noLicensePlatePhoto = false,
                        isFavorite = isFavorite
                    )
                )
            )
        }

        fun prepareFeature(
            reportOfferInfo: ReportOfferInfo?
        ): TeaTestFeature<CarfaxReport.Msg, CarfaxReport.State, CarfaxReport.Effect> {
            val fixture = kotlinFixtureDefault {
                factory<PageElement> { fixture<Text>() }
                factory<Map<String, IComparableItem>> { emptyMap() }
                factory<Map<String, List<IComparableItem>>> { emptyMap() }
                factory<ReportOfferInfo?> { reportOfferInfo }
                factory<CarfaxReport.Content> { CarfaxReport.Content.Loaded(fixture(), fixture()) }
            }
            return TeaTestFeature(
                fixture(),
                CarfaxReport::reducer
            )
        }
    }
}
