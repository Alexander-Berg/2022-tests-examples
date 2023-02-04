package ru.auto.ara.test.offer.proven_owner

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetTechSupportChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.chat.postChatMessage
import ru.auto.ara.core.dispatchers.draft.DEFAULT_UPLOAD_PHOTO_PATH
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.upload.getDocumentUploadUrls
import ru.auto.ara.core.dispatchers.upload.uploadPhoto
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.getActiveUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getEmptyUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getUserOffer
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.offercard.checkScreenshotAdvantages
import ru.auto.ara.core.robot.offercard.performAdvantageDescription
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.proven_owner_camera.checkProvenOwnerCamera
import ru.auto.ara.core.robot.proven_owner_camera.performProvenOwnerCamera
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTestEnvironmentRule
import ru.auto.ara.core.rules.ShouldShowWizardRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.WizardStepRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.data.offer.details.Advantage
import ru.auto.data.model.wizard.ProvenOwnerStep
import ru.auto.data.util.TEST_DRAFT_AND_WIZARD_WITH_MOCKS
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfferProvenOwnerTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        getOffer(OFFER_WITH_PROVEN_OWNER)
        getUserOffer(USER_OFFER_WITHOUT_PROVEN_OWNER)
        uploadPhoto(DEFAULT_UPLOAD_PHOTO_PATH)
        delegateDispatcher(GetTechSupportChatRoomDispatcher())
        postChatMessage("proven_owner")
        getRoomSpamMessages(RoomSpamMessage.EMPTY)
        delegateDispatcher(GetRoomMessagesDispatcher.getEmptyResponse())
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        GrantPermissionsRule(),
        ShouldShowWizardRule(true),
        SetPreferencesRule(),
        SetupTestEnvironmentRule { test ->
            TEST_DRAFT_AND_WIZARD_WITH_MOCKS = test
        },
        DisableAdsRule(),
        WizardStepRule(ProvenOwnerStep)
    )

    @Test
    fun shouldOpenCameraTakeAndSendPhotos() {
        webServerRule.routing {
            userSetup()
            getActiveUserOffers("cars", 1)
            getDocumentUploadUrls()
        }
        performCommon { login() }
        openOffer()
        performOfferCard {
            collapseAppBar()
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
        performAdvantageDescription { clickOnActionButton() }
        checkProvenOwnerCamera { checkFullScreenCameraDisplayed("sts_front.png") }
        performProvenOwnerCamera { goThroughCameraFlow() }

        waitSomething(2) // Wait for photos to upload

        checkScreenshotAdvantages {
            areAdvantagesBottomsheetScreenshotsSame("${Advantage.ProvenOwnerInactive.InProgress.tag}.png")
        }
    }

    @Test
    fun shouldOpenLoginScreenAndCameraAfter() {
        webServerRule.routing {
            userSetup()
            postLoginOrRegisterSuccess()
            getActiveUserOffers("cars")
            getDocumentUploadUrls()
        }
        openOffer()
        performOfferCard {
            collapseAppBar()
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
        performAdvantageDescription { clickOnActionButton() }
        performLogin { loginWithPhoneAndCode() }
        waitSomething(2L, TimeUnit.SECONDS) //wait for image in camera
        checkProvenOwnerCamera { checkFullScreenCameraDisplayed("sts_front.png") }
    }

    @Test
    fun shouldOpenChatWhenNoOffers() {
        webServerRule.routing {
            userSetup()
            getEmptyUserOffers("cars")
            getDocumentUploadUrls()
        }
        performCommon { login() }

        openOffer()
        performOfferCard {
            collapseAppBar()
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
        performAdvantageDescription { clickOnActionButton() }
        checkChatRoom { isChatMessageDisplayed(getResourceString(R.string.offer_advantage_proven_owner_question)) }
    }

    private fun openOffer() {
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$OFFER_WITH_PROVEN_OWNER")
    }

    companion object {
        private const val OFFER_WITH_PROVEN_OWNER = "1098490914-ca2ba503"
        private const val USER_OFFER_WITHOUT_PROVEN_OWNER = "1092688300-a5a5cc01"
    }
}
