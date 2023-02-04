package ru.auto.ara.test.favorites

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.behaviour.checkInitialApp2AppOutgoingCallIsDisplayingCorrectly
import ru.auto.ara.core.behaviour.disableApp2AppInstantCalling
import ru.auto.ara.core.behaviour.enableApp2AppInstantCalling
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
import ru.auto.ara.core.dispatchers.carfax.report.getRawReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.chat.postChatRoom
import ru.auto.ara.core.dispatchers.favorites.getDifferentFavorites
import ru.auto.ara.core.dispatchers.frontlog.checkFrontlogCommonParams
import ru.auto.ara.core.dispatchers.frontlog.postFrontLog
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.favorites.checkFavorites
import ru.auto.ara.core.robot.favorites.performFavorites
import ru.auto.ara.core.routing.Route
import ru.auto.ara.core.routing.Routing
import ru.auto.ara.core.routing.watch

@RunWith(AndroidJUnit4::class)
class FavoritesCallButtonTest : FavoritesListSetup() {

    override fun Routing.getFavoritesRoute(): Route = getDifferentFavorites()

    @Before
    fun setUp() {
        experiments.disableApp2AppInstantCalling()
    }

    @Test
    fun shouldShowDialerFromCallButtonOnActiveOffer() {
        val offerId = "1097094462-d5f4b83c"
        webServerRule.routing {
            getPhones(offerId, PhonesResponse.ONE)
            postFrontLog().watch { checkFrontlogCommonParams("phone_call_event") }
        }
        openFavorites()

        Intents.init()
        performFavorites { scrollToFavoriteSnippet(0, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, true)
            isReportButtonVisible(offerId, true)
        }
        performFavorites { clickOnCallOrChatButton(offerId) }
        checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
        Intents.release()
    }

    @Test
    fun shouldCellCallNotApp2AppFromCallButtonOnActiveOffer() {
        val offerId = "1097094462-d5f4b83c"
        webServerRule.routing {
            getPhones(offerId, PhonesResponse.ONE_WITH_APP2APP)
        }
        openFavorites()

        performFavorites { scrollToFavoriteSnippet(0, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, true)
            isReportButtonVisible(offerId, true)
        }
        Intents.init()
        performFavorites { clickOnCallOrChatButton(offerId) }
        checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
    }

    @Test
    fun shouldCallByApp2AppInstantlyFromCallButtonOnActiveOffer() {
        val offerId = "1097094462-d5f4b83c"
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            getPhones(offerId, PhonesResponse.ONE_WITH_APP2APP)
        }
        openFavorites()

        performFavorites { scrollToFavoriteSnippet(0, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, true)
            isReportButtonVisible(offerId, true)
        }
        performFavorites { clickOnCallOrChatButton(offerId) }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldShowChatFromChatButtonOnOnlyChatOffer() {
        val offerId = "1090549870-048914de"
        webServerRule.routing {
            webServerRule.routing {
                postChatRoom("room_after_post").watch { checkPostChatRoom(offerId) }
            }
        }
        openFavorites()
        performFavorites { scrollToFavoriteSnippet(3, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, true)
            isReportButtonVisible(offerId, true)
        }
        performFavorites { clickOnCallOrChatButton(offerId) }
        checkChatRoom {
            isChatToolbarTitleAndSubtitleDisplayed(title = "Денис", subtitle = null)
        }
    }

    @Test
    fun shouldShowChatFromChatButtonOnOnlyChatOfferWithoutReportButton() {
        val offerId = "1100458828-341fcc4b"
        webServerRule.routing {
            postChatRoom("room_after_post").watch { checkPostChatRoom(offerId) }
        }
        openFavorites()
        performFavorites { scrollToFavoriteSnippet(8, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, true)
            isReportButtonVisible(offerId, false)
        }
        performFavorites { clickOnCallOrChatButton(offerId) }
        checkChatRoom {
            isChatToolbarTitleAndSubtitleDisplayed(title = "Денис", subtitle = null)
        }
    }

    @Test
    fun shouldOpenReportFromReportButtonOnSnippet() {
        val offerId = "1090549870-048914de"
        webServerRule.routing {
            getRawReport(requestOfferId = offerId, fileOfferId = "1093024666-aa502a2a", dirType = NOT_BOUGHT)
            makeXmlForReportByOfferId(
                offerId = offerId,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
        }
        openFavorites()
        performFavorites { scrollToFavoriteSnippet(3, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, true)
            isReportButtonVisible(offerId, true)
        }
        performFavorites { clickOnReportButton(offerId) }
        performCarfaxReport { waitCarfaxReport() }
    }

    @Test
    fun shouldNotShowCallButtonAndReportButtonOnSoldOffer() {
        openFavorites()
        val offerId = "1100323504-dfb25b94"

        performFavorites { scrollToFavoriteSnippet(4, offerId) }
        checkFavorites {
            isCallOrChatButtonVisible(offerId, false)
            isReportButtonVisible(offerId, false)
        }
    }

    private fun RequestWatcher.checkPostChatRoom(offerId: String) {
        checkRequestsCount(1)
        checkRequestBodyParameters(
            "subject.offer.category" to "cars",
            "subject.offer.id" to offerId
        )
    }
}
