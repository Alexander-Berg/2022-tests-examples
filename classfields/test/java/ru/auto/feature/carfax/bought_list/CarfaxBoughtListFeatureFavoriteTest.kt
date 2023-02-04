package ru.auto.feature.carfax.bought_list

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.autocode.HistoryBoughtReport
import ru.auto.data.model.autocode.yoga.ReportOfferInfo
import ru.auto.data.model.carfax.CarfaxServerGenerateModel
import ru.auto.data.model.subscription.UserSubscription
import ru.auto.data.util.LoadableUpdData
import ru.auto.feature.carfax.viewmodel.CarfaxPreviewPayload
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testextension.checkResult
import ru.auto.testextension.kotlinFixtureDefault
import ru.auto.testextension.perform
import ru.auto.testextension.prepareParameter

@RunWith(AllureRunner::class)
class CarfaxBoughtListFeatureFavoriteTest {
    private val fixture = kotlinFixtureDefault()

    @Test
    fun `should ignore favorite messages if reportOfferInfo is null`() {
        val offerId = prepareParameter("offer id", fixture<String>())
        prepareFeature(null).perform("send toggle favorite message") { feature ->
            feature.accept(clickFavoriteMsg(offerId))
        }.checkResult("no effects sent") { feature ->
            Assertions.assertThat(feature.latestEffects).isEmpty()
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
            feature.accept(clickFavoriteMsg(offerId))
        }.checkResult("send effect with favorite request and set like") { feature ->
            Assertions.assertThat(feature.latestEffects)
                .containsExactlyInAnyOrder(
                    CarfaxBoughtList.Eff.Data.UpdateFavorite(offerId, favorite = true),
                    CarfaxBoughtList.Eff.Logger.LogFavoriteClick(favorite = true),
                )
            assertForFavoriteStatusInOfferInfo(feature, offerId, true)
        }.perform("send success response message") { feature ->
            feature.accept(CarfaxBoughtList.Msg.Data.OnFavoriteResponse(offerId, null))
        }.checkResult("should make no effects and dont change state") { feature ->
            Assertions.assertThat(feature.latestEffects).isEmpty()
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
            feature.accept(clickFavoriteMsg(offerId))
        }.checkResult("send effect with favorite request and remove favorite") { feature ->
            Assertions.assertThat(feature.latestEffects)
                .containsExactlyInAnyOrder(
                    CarfaxBoughtList.Eff.Data.UpdateFavorite(offerId, favorite = false),
                    CarfaxBoughtList.Eff.Logger.LogFavoriteClick(favorite = false),
                )
            assertForFavoriteStatusInOfferInfo(feature, offerId, false)
        }
    }

    private fun clickFavoriteMsg(offerId: String) =
        CarfaxBoughtList.Msg.Events.OnFavoriteClick(CarfaxPreviewPayload(fixture(), fixture(), offerId))

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
            feature.accept(clickFavoriteMsg(offerId))
        }.perform("send favorite response with error status") { feature ->
            feature.accept(CarfaxBoughtList.Msg.Data.OnFavoriteResponse(offerId, resetFavoriteTo = false))
        }.checkResult("send effect with error snack request and set favorite status back") { feature ->
            Assertions.assertThat(feature.latestEffects).hasOnlyOneElementSatisfying { eff ->
                Assertions.assertThat(eff).isInstanceOf(CarfaxBoughtList.Eff.View.Snack::class.java)
            }
            assertForFavoriteStatusInOfferInfo(feature, offerId, false)
        }
    }

    companion object {
        private fun assertForFavoriteStatusInOfferInfo(
            feature: TeaTestFeature<CarfaxBoughtList.Msg, CarfaxBoughtList.State, CarfaxBoughtList.Eff>,
            offerId: String,
            isFavorite: Boolean
        ) {
            Assertions.assertThat((feature.currentState).reports.value).hasOnlyOneElementSatisfying { report ->
                Assertions.assertThat(report.reportOfferInfo).isEqualTo(
                    ReportOfferInfo(
                        offerId,
                        noLicensePlatePhoto = false,
                        isFavorite = isFavorite
                    )
                )
            }
        }

        fun prepareFeature(
            reportOfferInfo: ReportOfferInfo?
        ): TeaTestFeature<CarfaxBoughtList.Msg, CarfaxBoughtList.State, CarfaxBoughtList.Eff> {
            val fixture = kotlinFixtureDefault {
                factory<LoadableUpdData<List<HistoryBoughtReport>>> {
                    LoadableUpdData(listOf(fixture()), LoadableUpdData.Status.Void)
                }
                property(HistoryBoughtReport::offerId) {
                    reportOfferInfo?.offerId
                }
                factory<LoadableUpdData<UserSubscription>> { LoadableUpdData(null, LoadableUpdData.Status.Void) }
                factory<ReportOfferInfo?> { reportOfferInfo }
                factory<CarfaxServerGenerateModel?> { null }
                factory<CarfaxBoughtListFragment.OnErrorStateListener> {
                    object : CarfaxBoughtListFragment.OnErrorStateListener {
                        override fun onErrorStateSelected() {
                            TODO("not implemented")
                        }
                    }
                }
            }
            return TeaTestFeature(
                fixture(),
                CarfaxBoughtList::reducer
            )
        }
    }
}
