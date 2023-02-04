package ru.auto.ara.test.listing.snippet

import android.Manifest
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.auto.ara.core.rules.GrantPermissionsRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.behaviour.checkInitialApp2AppOutgoingCallIsDisplayingCorrectly
import ru.auto.ara.core.behaviour.disableApp2AppInstantCalling
import ru.auto.ara.core.behaviour.enableApp2AppInstantCalling
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.chat.PostChatRoomDispatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.dispatchers.salon.getCustomizableSalonById
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.dealer.checkDealerContacts
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getAutoRuYears
import ru.auto.ara.core.utils.getResourceStringWithoutNonbreakingSpace
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class GalleryTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val dealerOfferDispatcher = PostSearchOffersDispatcher("common_used_not_loyal_not_metro")
    private val privateSellerOfferDispatcher = PostSearchOffersDispatcher("informers_common_snippet_vin_untrusted_no_history")

    private val privateSellerChatOnlyOfferDispatcher = PostSearchOffersDispatcher("common_private_chat_only")

    private val dispatcherHolder = DispatcherHolder()
    private val phonesHolder = DispatcherHolder()
    private val createRoomWatcher = RequestWatcher()

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        getCustomizableSalonById(
            dealerId = 20134444,
            dealerCode = "mercury_auto_moskva_ferrari"
        )
        userSetup()
        delegateDispatchers(
            CountDispatcher("cars"),
            dispatcherHolder,
            phonesHolder,
            PostChatRoomDispatcher("1087439802-b6940925", createRoomWatcher),
            ParseDeeplinkDispatcher.carsAll()
        )
    }

    private val experiments =
        experimentsOf()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        SetupAuthRule(),
        activityTestRule,
        GrantPermissionsRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.SYSTEM_ALERT_WINDOW),
        arguments = TestMainModuleArguments(
            experiments
        )
    )

    @Before
    fun setUp() {
        experiments.disableApp2AppInstantCalling()
    }

    @Test
    fun shouldMakeCallFromCommercialCommonSnippet() {
        performCommon { logout() }
        dispatcherHolder.innerDispatcher = dealerOfferDispatcher
        phonesHolder.innerDispatcher = GetPhonesDispatcher.onePhone("1085467540-a0fc302a")
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        Intents.init()
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToCallButton()
        }.checkResult {
            isGallerySellerButtonDisplayed(
                name = "Ferrari Москва Третьяковский",
                type = "На Авто.ру ${getAutoRuYears(2013)} лет"
            )
            isGalleryCallButtonDisplayed("10:00 — 22:00")
        }
        performListingOffers {
            interactions.onGalleryCallLayout().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
    }

    @Test
    fun shouldCellCallNotApp2AppFromCommercialCommonSnippet() {
        dispatcherHolder.innerDispatcher = dealerOfferDispatcher
        performCommon { login() }
        webServerRule.routing {
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToCallButton()
        }.checkResult {
            isGallerySellerButtonDisplayed(
                name = "Ferrari Москва Третьяковский",
                type = "На Авто.ру ${getAutoRuYears(2013)} лет"
            )
            isGalleryCallButtonDisplayed("10:00 — 22:00")
        }
        Intents.init()
        performListingOffers {
            interactions.onGalleryCallLayout().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
    }

    @Test
    fun shouldCallByApp2AppInstantlyFromCommercialCommonSnippet() {
        dispatcherHolder.innerDispatcher = dealerOfferDispatcher
        experiments.enableApp2AppInstantCalling()
        performCommon { login() }
        webServerRule.routing {
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToCallButton()
        }.checkResult {
            isGallerySellerButtonDisplayed(
                name = "Ferrari Москва Третьяковский",
                type = "На Авто.ру ${getAutoRuYears(2013)} лет"
            )
            isGalleryCallButtonDisplayed("10:00 — 22:00")
        }
        performListingOffers {
            interactions.onGalleryCallLayout().performClick()
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldMakeCallFromPrivateSellerCommonSnippet() {
        performCommon { logout() }
        dispatcherHolder.innerDispatcher = privateSellerOfferDispatcher
        phonesHolder.innerDispatcher = GetPhonesDispatcher.onePhone("1087439802-b6940925")
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        Intents.init()
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
        }.checkResult {
            isGallerySellerButtonDisplayed("Юрий", "Частное лицо")
            isGalleryCallButtonDisplayed("9:00 — 21:00")
            isGalleryMessageButtonDisplayed()
        }
        performListingOffers {
            interactions.onGalleryCallLayout().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
    }

    @Test
    fun shouldCellCallNotApp2AppFromPrivateSellerCommonSnippet() {
        performCommon { login() }
        dispatcherHolder.innerDispatcher = privateSellerOfferDispatcher

        webServerRule.routing {
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }

        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
        }.checkResult {
            isGallerySellerButtonDisplayed("Юрий", "Частное лицо")
            isGalleryCallButtonDisplayed("9:00 — 21:00")
            isGalleryMessageButtonDisplayed()
        }
        Intents.init()
        performListingOffers {
            interactions.onGalleryCallLayout().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
    }

    @Test
    fun shouldCallByApp2AppInstantlyFromPrivateSellerCommonSnippet() {
        experiments.enableApp2AppInstantCalling()
        performCommon { login() }
        dispatcherHolder.innerDispatcher = privateSellerOfferDispatcher

        webServerRule.routing {
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }

        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
        }.checkResult {
            isGallerySellerButtonDisplayed("Юрий", "Частное лицо")
            isGalleryCallButtonDisplayed("9:00 — 21:00")
            isGalleryMessageButtonDisplayed()
        }
        performListingOffers {
            interactions.onGalleryCallLayout().performClick()
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldSeeMultiplyPhonesAfterCallFromSnippet() {
        performCommon { logout() }
        dispatcherHolder.innerDispatcher = privateSellerOfferDispatcher
        phonesHolder.innerDispatcher = GetPhonesDispatcher.multiplePhones("1087439802-b6940925")
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        Intents.init()
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
            interactions.onGalleryCallLayout().performClick()
        }
        checkOfferCard {
            isPhonesBottomsheetDisplayed(getResourceStringWithoutNonbreakingSpace(R.string.call_to_seller), "Сергей")
            isPhoneCellDisplayed("+7 934 777-97-77", "с 1:00 до 23:00")
            isPhoneCellDisplayed("+7 950 287-35-55", "с 5:00 до 23:00")
        }
        performOfferCard {
            interactions.onPhoneNumber("+7 950 287-35-55").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 950 287-35-55") }
    }

    @Test
    fun shouldOpenOfferFromPrivateSellerCommonSnippet() {
        dispatcherHolder.innerDispatcher = privateSellerOfferDispatcher
        phonesHolder.innerDispatcher = GetPhonesDispatcher.onePhone("1087439802-b6940925")
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        Intents.init()
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
            interactions.onGallerySellerName("Юрий").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkOfferCard { isOfferCardTitle("BMW X5 35d II (E70) Рестайлинг, 2010") }
    }

    @Test
    fun shouldOpenDealerListingFromDealerCommonSnippet() {
        dispatcherHolder.innerDispatcher = dealerOfferDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        Intents.init()
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToCallButton()
            interactions.onGallerySellerName("Ferrari Москва Третьяковский").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkDealerContacts { checkDealerContactsDialogIsDisplayed() }
    }

    @Test
    fun shouldCreateChatRoomFromSnippet() {
        performCommon { logout() }
        dispatcherHolder.innerDispatcher = privateSellerOfferDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
            interactions.onGalleryMessageLayout().waitUntilIsCompletelyDisplayed().performClick()
        }

        checkChatRoom {
            isChatSubjectDisplayed("BMW X5 II (E70) Рестайлинг, 2010", "1 000 000 \u20BD")
            createRoomWatcher.checkRequestBodyParameters(
                "subject.offer.category" to "cars",
                "subject.offer.id" to "1087439802-b6940925"
            )
        }
    }

    @Test
    fun shouldNotSeeGalleryCallButtonOnChatOnlyOffer() {
        dispatcherHolder.innerDispatcher = privateSellerChatOnlyOfferDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        Intents.init()
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFirstSnippet()
            scrollGalleryToMessageButton()
        }.checkResult {
            isGalleryCallButtonGone()
        }
    }
}
