package ru.auto.ara.test.carfax.reload

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.carfax.report.CarfaxReloadDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferParametrizedDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForUserOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.getCustomizableUserOffer
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getReloadResolutionCell
import ru.auto.ara.core.robot.carfax.checkCarfaxReport
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.snack.checkSnack
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.OfferCardBundles
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.ui.activity.OfferDetailsActivity
import ru.auto.ara.ui.fragment.offer.OfferDetailsFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.autocode.yoga.ReloadResolutionItem
import ru.auto.data.util.hoursToMillis

@RunWith(AndroidJUnit4::class)
class CarfaxReloadTest {
    private val activityRule = lazyActivityScenarioRule<OfferDetailsActivity>()

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

    private val dispatchers: List<DelegateDispatcher> = listOf(
        carfaxRawDispatcher,
        carfaxReloadDispatcher
    )

    private val webServerRule = WebServerRule {
        delegateDispatchers(dispatchers)
        getCustomizableUserOffer(OFFER_ID)
        userSetup()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule(),
        SetPreferencesRule()
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
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
        }
        openCard()
        performOfferCard {
            scrollToCarfax()
        }
        checkOfferCard {
            isCarfaxReloadAvailableDisplayed()
        }
    }

    @Test
    fun shouldShowReloadResolutionRequestedBlockWithOneHourAwait() {
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a_hour_block"
            )
        }
        shouldShowReloadResolutionRequestedBlock(
            millisTillReload = 10000,
            hoursString = "час"
        )
    }

    @Test
    fun shouldShowReloadResolutionRequestedBlockWith48HoursAwait() {
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a_48_hour_block"
            )
        }
        shouldShowReloadResolutionRequestedBlock(
            millisTillReload = 1000 * 3600 * 48,
            hoursString = "48 часов"
        )
    }

    @Test
    fun shouldShowReloadResolutionRequestedBlockWith49HoursAwait() {
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a_49_hour_block"
            )
        }
        shouldShowReloadResolutionRequestedBlock(
            millisTillReload = 1000 * 3600 * 48 + 1,
            hoursString = "49 часов"
        )
    }

    private fun shouldShowReloadResolutionRequestedBlock(millisTillReload: Long, hoursString: String) {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = false,
                millisTillReload = millisTillReload
            )
        )
        openCard()
        performOfferCard {
            scrollToCarfax()
        }
        checkOfferCard {
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
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
        }
        openCard()
        performOfferCard {
            scrollToCarfax()
        }
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = false,
                millisTillReload = 10000
            )
        )
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a_hour_block"
            ).watch { checkRequestWasCalled() }
        }
        performOfferCard { clickCarfaxReloadButton() }
        checkOfferCard { isCarfaxReloadSpinnerDisplayed() }
        carfaxReloadWatcher.checkRequestWasCalled()
        checkSnack { isSnackDisplayed(getResourceString(R.string.autocode_update_success)) }
        checkOfferCard {
            isCarfaxReloadRequestedDisplayed(hours = "час")
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
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
        }
        openCard()
        performOfferCard { scrollToCarfax() }
        carfaxReloadDispatcher.success = false
        carfaxRawWatcher.clearRequestWatcher()
        performOfferCard { clickCarfaxReloadButton() }
        checkOfferCard { isCarfaxReloadSpinnerDisplayed() }
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            ).watch { checkRequestWasNotCalled() }
        }
        checkSnack { isSnackDisplayed(getResourceString(R.string.autocode_update_error)) }
        checkOfferCard {
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
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
        }
        openCard()
        performOfferCard {
            scrollToCarfax()
        }
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            success = false
        )
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a",
                responseCode = DelegateDispatcher.ERROR_CODE
            ).watch { checkRequestWasCalled() }
        }
        performOfferCard { clickCarfaxReloadButton() }
        checkOfferCard { isCarfaxReloadSpinnerDisplayed() }
        carfaxReloadWatcher.checkRequestWasCalled()
        checkSnack { isSnackDisplayed(getResourceString(R.string.autocode_update_success)) }
        checkOfferCard {
            isCarfaxReloadRequestedDisplayed(hours = "24 часа")
            isCarfaxReloadSpinnerNotExists()
        }
    }

    @Test
    fun shouldUpdateReloadResolutionOnCardAfterReloadClickOnFullReport() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
            makeXmlForReportByOfferId(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getReloadResolutionCell())) }
            )
        }
        openCard()
        performOfferCard {
            scrollToCarfax()
            clickToOpenFullReport()
        }
        performCarfaxReport {
            waitCarfaxReport()
        }
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = false,
                millisTillReload = 24.hoursToMillis()
            )
        )
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a_hour_block"
            )
        }
        resetWatchers()

        performCarfaxReport { clickCarfaxReloadButton() }

        checkCarfaxReport { isCarfaxReloadRequestedDisplayed(hours = "24 часа") }

        carfaxReloadWatcher.checkRequestsCount(1)

        pressBack()

        checkOfferCard {
            isCarfaxReloadRequestedDisplayed(hours = "24 часа")
            isCarfaxReloadSpinnerNotExists()
        }

        carfaxReloadWatcher.checkRequestsCount(1)
    }

    @Test
    fun shouldUpdateReloadResolutionWith24HoursOnCardAfterReloadErrorOnFullReport() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
            makeXmlForReportByOfferId(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getReloadResolutionCell())) }
            )
        }
        openCard()
        performOfferCard {
            scrollToCarfax()
            clickToOpenFullReport()
        }
        performCarfaxReport {
            waitCarfaxReport()
        }
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            success = false
        )
        resetWatchers()

        performCarfaxReport { clickCarfaxReloadButton() }
        checkCarfaxReport { isCarfaxReloadRequestedDisplayed(hours = "24 часа") }

        carfaxReloadWatcher.checkRequestsCount(1)

        pressBack()

        checkOfferCard {
            isCarfaxReloadRequestedDisplayed(hours = "24 часа")
            isCarfaxReloadSpinnerNotExists()
        }
        carfaxReloadWatcher.checkRequestsCount(1)
    }

    @Test
    fun shouldKeepReloadResolutionOnCardAfterReloadRequestErrorOnFullReport() {
        carfaxRawDispatcher.arguments = RawCarfaxOfferParametrizedDispatcher.Arguments(
            reloadItem = ReloadResolutionItem(
                allowReload = true
            )
        )
        webServerRule.routing {
            makeXmlForUserOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = "1101824989-e01d608a"
            )
            makeXmlForReportByOfferId(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getReloadResolutionCell())) }
            )
        }
        openCard()
        performOfferCard {
            scrollToCarfax()
            clickToOpenFullReport()
        }
        performCarfaxReport {
            waitCarfaxReport()
        }
        carfaxReloadDispatcher.success = false
        resetWatchers()

        performCarfaxReport { clickCarfaxReloadButton() }
        checkCarfaxReport { isCarfaxReloadAvailableDisplayed() }

        carfaxRawWatcher.checkRequestsCount(0)
        carfaxReloadWatcher.checkRequestsCount(1)

        pressBack()

        checkOfferCard {
            isCarfaxReloadAvailableDisplayed()
            isCarfaxReloadSpinnerNotExists()
        }
        carfaxRawWatcher.checkRequestsCount(0)
        carfaxReloadWatcher.checkRequestsCount(1)
    }

    private fun openCard() {
        resetWatchers()
        activityRule.launchFragment<OfferDetailsFragment>(
            OfferCardBundles.userOfferBundle(
                category = VehicleCategory.CARS,
                offerId = OFFER_ID
            )
        )
        performOfferCard { collapseAppBar() }
    }

    private fun resetWatchers() {
        carfaxReloadWatcher.clearRequestWatcher()
        carfaxRawWatcher.clearRequestWatcher()
    }

    companion object {
        private var OFFER_ID = "1095669442-b39897242"
    }
}
