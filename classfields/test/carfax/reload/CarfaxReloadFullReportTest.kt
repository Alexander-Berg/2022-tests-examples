package ru.auto.ara.test.carfax.reload

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.carfax.report.CarfaxReloadDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferParametrizedDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOfferDispatcher
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getReloadResolutionCell
import ru.auto.ara.core.mapping.ssr.getReloadResolutionCellWithAwait
import ru.auto.ara.core.robot.carfax.checkCarfaxReport
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.autocode.yoga.ReloadResolutionItem
import ru.auto.feature.carfax.api.CarfaxAnalyst
import ru.auto.feature.carfax.ui.fragment.ReCarfaxReportFragment
import ru.auto.feature.carfax.ui.presenter.CarfaxReport

@RunWith(AndroidJUnit4::class)
class CarfaxReloadFullReportTest {
    private val activityRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()

    private val carfaxRawWatcher = RequestWatcher()
    private val carfaxReloadWatcher = RequestWatcher()

    private val carfaxRawDispatcher = RawCarfaxOfferParametrizedDispatcher(
        requestOfferId = OFFER_ID,
        requestWatcher = carfaxRawWatcher
    )
    private val carfaxReloadDispatcher = CarfaxReloadDispatcher(
        requestOfferId = OFFER_ID,
        requestWatcher = carfaxReloadWatcher,
        wait = true
    )

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            carfaxRawDispatcher,
            carfaxReloadDispatcher,
            GetUserOfferDispatcher(
                VehicleCategory.CARS,
                OFFER_ID
            ),
        )
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getReloadResolutionCell())) }
        )
        userSetup()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule()
    )

    @Before
    fun setUp() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments()
        carfaxReloadDispatcher.success = true
        carfaxRawWatcher.clearRequestWatcher()

    }

    @Test
    fun shouldShowReloadResolutionAvailableBlock() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        openFullReport()
        checkCarfaxReport {
            isCarfaxReloadAvailableDisplayed()
        }
    }

    @Test
    fun shouldShowReloadResolutionRequestedBlockWithOneHourAwait() =
        shouldShowReloadResolutionRequestedBlock(
            millisTillReload = 10000,
            hoursString = "час"
        )

    @Test
    fun shouldShowReloadResolutionRequestedBlockWith48HoursAwait() =
        shouldShowReloadResolutionRequestedBlock(
            millisTillReload = 1000 * 3600 * 48,
            hoursString = "48 часов"
        )

    @Test
    fun shouldShowReloadResolutionRequestedBlockWith49HoursAwait() =
        shouldShowReloadResolutionRequestedBlock(
            millisTillReload = 1000 * 3600 * 48 + 1,
            hoursString = "49 часов"
        )

    private fun shouldShowReloadResolutionRequestedBlock(millisTillReload: Long, hoursString: String) {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = false,
                millisTillReload = millisTillReload
            )
        )
        webServerRule.routing {
            makeXmlForReportByOfferId(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getReloadResolutionCellWithAwait(hoursString))) }
            )
        }
        openFullReport()
        checkCarfaxReport {
            isCarfaxReloadRequestedDisplayed(hours = hoursString)
        }
    }

    @Test
    fun shouldShowReloadResolutionRequestedAfterClick() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        openFullReport()
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = false,
                millisTillReload = 10000
            )
        )
        performOfferCard { clickCarfaxReloadButton() }
        checkCarfaxReport { isCarfaxReloadSpinnerDisplayed() }
        carfaxReloadWatcher.checkRequestWasCalled()
        checkCarfaxReport {
            isSnackDisplayed(getResourceString(R.string.autocode_update_success))
            isCarfaxReloadRequestedDisplayed(hours = "24 часа")
            isCarfaxReloadSpinnerNotExists()
        }
    }

    @Test
    fun shouldShowReloadResolutionAvailableWithSnackAfterRequestReloadError() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        openFullReport()
        carfaxReloadDispatcher.success = false
        carfaxRawWatcher.clearRequestWatcher()
        performCarfaxReport { clickCarfaxReloadButton() }
        checkCarfaxReport { isCarfaxReloadSpinnerDisplayed() }
        carfaxReloadWatcher.checkRequestWasCalled()
        carfaxRawWatcher.checkRequestWasNotCalled()
        checkCarfaxReport {
            isSnackDisplayed(getResourceString(R.string.autocode_update_error))
            isCarfaxReloadAvailableDisplayed()
            isCarfaxReloadSpinnerNotExists()
        }
    }

    @Test
    fun shouldShowReloadResolutionRequested24HoursAfterReloadSuccessAndUpdateError() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        openFullReport()
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            success = false
        )
        performCarfaxReport { clickCarfaxReloadButton() }
        checkCarfaxReport { isCarfaxReloadSpinnerDisplayed() }
        carfaxReloadWatcher.checkRequestWasCalled()
        checkCarfaxReport {
            isSnackDisplayed(getResourceString(R.string.autocode_update_success))
            isCarfaxReloadRequestedDisplayed(hours = "24 часа")
            isCarfaxReloadSpinnerNotExists()
        }
    }

    private fun openFullReport() {
        activityRule.launchFragment<ReCarfaxReportFragment>(
            ReCarfaxReportFragment.screen(
                CarfaxReport.Args(
                    source = CarfaxReport.Source.Offer(
                        OFFER_ID,
                        metricaSource = CarfaxAnalyst.BuySource.SOURCE_CARD_FREE_REPORT
                    )
                )
            ).args
        )
        performCarfaxReport { waitCarfaxReport() }
    }

    companion object {
        private var OFFER_ID = "1095669442-b39897242"
    }
}
