package ru.auto.ara.test.chat.room

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withParentIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.behaviour.checkInitialApp2AppOutgoingCallIsDisplayingCorrectly
import ru.auto.ara.core.behaviour.disableApp2AppInstantCalling
import ru.auto.ara.core.behaviour.enableApp2AppInstantCalling
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.app2app.getApp2AppCallInfo
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.chat.DeleteChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.GENERAL_UPLOAD_SIGN
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.SIGN
import ru.auto.ara.core.dispatchers.chat.GetChatMessagesBootstrapDispatcher.Companion.WITH_TECH_SUPPORT_UPLOAD
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesTemplateDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatBlockRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatMuteRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatUnblockRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.PutChatUnmuteRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.getChatMessagesBootstrapError
import ru.auto.ara.core.dispatchers.chat.getChatMessagesBootstrapTimeout
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesStartedFromMessageId
import ru.auto.ara.core.dispatchers.chat.postChatMessage
import ru.auto.ara.core.dispatchers.chat.postChatMessageError
import ru.auto.ara.core.dispatchers.chat.postChatMessageTimeout
import ru.auto.ara.core.dispatchers.chat.putChatOpenRoom
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.dispatchers.shark.getOneDraftApplicationNoProfile
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOfferDispatcher
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.feature.calls.OFFER_ID_FOR_VOX_TEST
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
import ru.auto.ara.core.matchers.ViewMatchers.withMaterialToolbarTitle
import ru.auto.ara.core.matchers.ViewMatchers.withNoSpannableText
import ru.auto.ara.core.matchers.ViewMatchers.withNotEmptyMaterialToolbarSubtitle
import ru.auto.ara.core.matchers.ViewMatchers.withToolbarIcon
import ru.auto.ara.core.robot.chat.BLOCK
import ru.auto.ara.core.robot.chat.ChatRoomRobotChecker.Companion.closeKeyboard
import ru.auto.ara.core.robot.chat.ChatRoomRobotChecker.Companion.longWaitAnimation
import ru.auto.ara.core.robot.chat.DELETE
import ru.auto.ara.core.robot.chat.GO_TO_OFFER
import ru.auto.ara.core.robot.chat.MUTE
import ru.auto.ara.core.robot.chat.UNBLOCK
import ru.auto.ara.core.robot.chat.UNMUTE
import ru.auto.ara.core.robot.chat.checkChatPicker
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.chat.checkMessages
import ru.auto.ara.core.robot.chat.performChatPicker
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.SetupTimeZoneRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA
import ru.auto.ara.core.testdata.CHAT_ROOM_PRESETS_NOT_EMPTY_ROOM_DATA
import ru.auto.ara.core.testdata.CHAT_ROOM_SELLER_PRESETS_EMPTY_ROOM_DATA
import ru.auto.ara.core.testdata.CHAT_ROOM_SELLER_PRESETS_NOT_EMPTY_ROOM_DATA
import ru.auto.ara.core.utils.clickToolbarMenu
import ru.auto.ara.core.utils.createImageAndGetUriString
import ru.auto.ara.core.utils.getDeepLinkIntent
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.core_ui.ui.viewmodel.Resources
import ru.auto.data.model.VehicleCategory
import ru.auto.feature.chats.messages.ui.CallMessageView.ViewModel.CallType
import ru.auto.feature.chats.model.MessageStatus
import java.util.concurrent.TimeUnit

private const val LONG_PAUSE = 3000L
private const val OFFER_ID = "1082957054-8d55bf9a"

private const val CHAT_ID = "6822dc60e71440f35f012d0b35b5b234"

@RunWith(AndroidJUnit4::class)
class RoomTest {
    private val DEFAULT_CHAT_DEEPLINK = "autoru://app/chat/room/6822dc60e71440f35f012d0b35b5b234"
    private val PHONE_NUMBER = "+7 985 440-66-27"

    private val blockWatcher = RequestWatcher()
    private val unblockWatcher = RequestWatcher()
    private val muteWatcher = RequestWatcher()
    private val unmuteWatcher = RequestWatcher()
    private val deleteWatcher = RequestWatcher()

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule {
        userSetup()
        getOneDraftApplicationNoProfile()
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", OFFER_ID),
            GetUserOfferDispatcher(VehicleCategory.CARS, "1091046926-d31bf482"),
            PutChatBlockRoomDispatcher(CHAT_ID, blockWatcher),
            PutChatUnblockRoomDispatcher(CHAT_ID, unblockWatcher),
            PutChatMuteRoomDispatcher(CHAT_ID, muteWatcher),
            PutChatUnmuteRoomDispatcher(CHAT_ID, unmuteWatcher),
            DeleteChatRoomDispatcher(CHAT_ID, deleteWatcher),
            RawCarfaxOfferDispatcher(
                requestOfferId = "1091046926-d31bf482",
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
            ),
            RawCarfaxOfferDispatcher(
                requestOfferId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
            ),
        )
        stub {
            delegateDispatcher(GetPhonesDispatcher.onePhone(OFFER_ID))
            delegateDispatcher(GetRoomSpamMessagesDispatcher.getEmptyResponse())
        }
    }

    private val experiments = experimentsOf()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        SetupTimeRule(date = "24.01.2020", time = "16:00"),
        SetupTimeZoneRule(),
        activityTestRule,
        GrantPermissionsRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        ),
        arguments = TestMainModuleArguments(
            experiments
        )
    )

    @Before
    fun setUp() {
        experiments.disableApp2AppInstantCalling()
    }

    @Test
    fun shouldSeeEmptyRoomOfCustomerControls() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            putChatOpenRoom(CHAT_ID).watch { checkRequestWasCalled() }
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        checkChatRoom {
            closeKeyboard()
            isChatSubjectDisplayed("Audi A3 III (8V) Рестайлинг, 2019", "100 000 \u20BD")
            interactions.onToolbar()
                .waitUntil(isCompletelyDisplayed(), withMaterialToolbarTitle("Продавец"), withNotEmptyMaterialToolbarSubtitle())
            interactions.onPhoneMenuItem().waitUntil(
                isCompletelyDisplayed(),
                withToolbarIcon(R.drawable.ic_secondary_phone, Resources.Color.COLOR_SECONDARY_EMPHASIS_HIGH)
            )
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA)
            clickToolbarMenu()
            isAvailiableActionsDisplayed(listOf(GO_TO_OFFER, MUTE, BLOCK, DELETE))
        }
    }

    @Test
    fun shouldHideCustomerPresetIfItIsAlreadyInChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_PRESETS)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_NOT_EMPTY_ROOM_DATA - "Ещё продаётся?")
        }
    }

    @Test
    fun shouldHideSellerPresetIfItIsAlreadyInChat() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_PRESETS)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_SELLER_PRESETS_NOT_EMPTY_ROOM_DATA - "Здравствуйте")
        }
    }

    @Test
    fun shouldHidePresetsWhenMoreThanTwoMyMessagesInChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_MY_MESSAGES)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            interactions.onPresetMessagesList().waitUntilIsInvisible()
        }
    }

    @Test
    fun shouldHidePresetsWhenWeSend2ndMessageToChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_ONE_MY_MESSAGES)
            postChatMessage("from_454cf7e6bb8bbaef")
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()
        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_NOT_EMPTY_ROOM_DATA)
        }
        performChatRoom {
            sendMessage("message without preset")
        }
        checkChatRoom {
            arePresetsHidden()
        }
    }

    @Test
    fun shouldHidePresetsWhenMoreThan100MessagesInChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ONE_HUNDRED_MESSAGES)
            getRoomMessagesStartedFromMessageId(
                expectedMessageResponse = RoomMessages.ONE_MESSAGE_BEFORE_ONE_HUNDRED_MESSAGES,
                messageId = "e16874a3-654d-447c-8a80-360af227c30c"
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            interactions.onPresetMessagesList().waitUntilIsInvisible()
        }
    }

    @Test
    fun shouldCheckCorrectPagination() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ONE_HUNDRED_MESSAGES).watch {
                checkQueryParameters(
                    "room_id" to "6822dc60e71440f35f012d0b35b5b234",
                    "count" to "100",
                    "asc" to "false"
                )
            }
            getRoomMessagesStartedFromMessageId(
                expectedMessageResponse = RoomMessages.ONE_MESSAGE_BEFORE_ONE_HUNDRED_MESSAGES,
                messageId = "e16874a3-654d-447c-8a80-360af227c30c"
            ).watch {
                checkQueryParameters(
                    "room_id" to "6822dc60e71440f35f012d0b35b5b234",
                    "count" to "100",
                    "asc" to "false"
                )
            }
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        longWaitAnimation(LONG_PAUSE)
        performChatRoom {
            scrollToMessageWithText("ef559909-dbca-4112-92e3-d94ac90a6606")
            longWaitAnimation()
            swipeDown()
        }.checkResult {
            isChatMessageDisplayed("Ура! Ты пролистал 100 сообщений и загрузил вторую страницу")
        }
    }

    @Test
    fun shouldHidePresetsWhenWeSend100thMessageToChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.NINETY_NINE_MESSAGES)
            postChatMessage("from_454cf7e6bb8bbaef")
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()
        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_NOT_EMPTY_ROOM_DATA)
        }
        performChatRoom {
            sendMessage("message without preset")
        }
        checkChatRoom {
            arePresetsHidden()
        }
    }

    @Test
    fun shouldSendPresetAndChangePresetList() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessage("first_preset_in_empty_room")
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()
        performChatRoom {
            interactions.onPresetMessage(CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA.first()).waitUntilIsCompletelyDisplayed()
                .performClick()
        }.checkResult {
            isChatMessageDisplayed(CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA.first())
        }
        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_NOT_EMPTY_ROOM_DATA.drop(1))
            interactions.onPresetMessage(CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA.first()).checkNotExists()
            interactions.onPresetMessage(CHAT_ROOM_PRESETS_NOT_EMPTY_ROOM_DATA.first()).checkNotExists()
        }
    }

    @Test
    fun shouldSendSellerPresetAndChangePresetList() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessage("first_seller_preset_in_empty_room")
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()
        performChatRoom {
            interactions.onPresetMessage(CHAT_ROOM_SELLER_PRESETS_EMPTY_ROOM_DATA.first()).waitUntilIsCompletelyDisplayed()
                .performClick()
        }.checkResult {
            isChatMessageDisplayed(CHAT_ROOM_SELLER_PRESETS_EMPTY_ROOM_DATA.first())
        }
        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_SELLER_PRESETS_NOT_EMPTY_ROOM_DATA.drop(1))
            interactions.onPresetMessage(CHAT_ROOM_SELLER_PRESETS_EMPTY_ROOM_DATA.first()).checkNotExists()
            interactions.onPresetMessage(CHAT_ROOM_SELLER_PRESETS_NOT_EMPTY_ROOM_DATA.first()).checkNotExists()
        }
    }

    @Test
    fun shouldHidePresetsOnCloseClick() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()
        performChatRoom {
            clickPresetsCloseButton()
        }
        checkChatRoom {
            arePresetsHidden()
        }

        launchActivity<DeeplinkActivity>(getDeepLinkIntent(DEFAULT_CHAT_DEEPLINK)).use {
            checkChatRoom {
                arePresetsHidden()
            }
        }
    }

    @Test
    fun shouldOpenDRIVE2LinkFromChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_CHAT_LINKS)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()

        performChatRoom {
            interactions.onMessageText("Эта ссылка https://www.drive2.ru/r/dodge/1292866 сейчас откроется")
                .waitUntilIsDisplayed()
                .performClickClickableSpan("https://www.drive2.ru/r/dodge/1292866")
        }
        checkWebView { isWebViewToolBarDisplayed("DRIVE2.RU") }
    }

    @Test
    @Ignore
    fun shouldOpenAutoRuLinkFromChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_CHAT_LINKS)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()

        performChatRoom {
            interactions.onMessageText("А эта снова да https://auto.ru/cars/used/sale/mazda/cx_5/1082957054-8d55bf9a")
                .waitUntilIsDisplayed()
                .performClickClickableSpan("https://auto.ru/cars/used/sale/mazda/cx_5/1082957054-8d55bf9a")
        }
        checkOfferCard { isOfferCardTitle("BMW 4 серия 420 F32/F33/F36 420d xDrive, 2016") }
    }

    @Test
    fun shouldNotSpanAnotherUriFromChat() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_CHAT_LINKS)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()

        checkChatRoom {
            interactions.onMessageText("А эта https://www.avito.ru/moskva/audio_i_video/muzykalnye_tsentry_magnitoly нет")
                .waitUntilIsDisplayed().check(withNoSpannableText())
        }
    }

    @Test
    fun shouldOpenOfferFromMenu() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(GO_TO_OFFER).waitUntilIsCompletelyDisplayed().performClick()
        }
        checkOfferCard { isOfferCardTitle("BMW 4 серия 420d xDrive F32/F33/F36, 2016") }
    }

    @Test
    fun shouldOpenOfferFromMenuIfUserIsSeller() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(GO_TO_OFFER).performClick()
        }
        checkOfferCard { isUserOffer() }
    }

    @Test
    fun shouldOpenOfferFromChatSubjectIfUserIsSeller() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom { interactions.onSubjectLayout().waitUntilIsCompletelyDisplayed().performClick() }
        checkOfferCard { isUserOffer() }
    }

    @Test
    fun shouldOpenOfferFromChatSubject() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom { interactions.onSubjectLayout().waitUntilIsCompletelyDisplayed().performClick() }
        checkOfferCard { isOfferCardTitle("BMW 4 серия 420d xDrive F32/F33/F36, 2016") }
    }

    @Test
    fun shouldCallFromChatToolbar() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        withIntents {
            performChatRoom { interactions.onPhoneMenuItem().waitUntilIsCompletelyDisplayed().performClick() }
            checkCommon { isActionDialIntentCalled(PHONE_NUMBER) }
        }
    }

    @Test
    fun shouldCellCallNotApp2AppFromChatToolbar() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        withIntents {
            performChatRoom { interactions.onPhoneMenuItem().waitUntilIsCompletelyDisplayed().performClick() }
            checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
        }
    }

    @Test
    fun shouldCallByApp2AppInstantlyFromChatToolbar() {
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()

        performChatRoom { interactions.onPhoneMenuItem().waitUntilIsCompletelyDisplayed().performClick() }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldSeeRoomWithSellerControls() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        checkChatRoom {
            closeKeyboard()
            isChatSubjectDisplayed("Audi 90 II (B3), 1991", "452 222 \u20BD")
            interactions.onToolbar()
                .waitUntil(isCompletelyDisplayed(), withMaterialToolbarTitle("dimaq_"), withNotEmptyMaterialToolbarSubtitle())
            interactions.onPhoneMenuItem().checkNotExists()
            isPresetMessagesDisplayed(CHAT_ROOM_SELLER_PRESETS_EMPTY_ROOM_DATA)
            clickToolbarMenu()
            isAvailiableActionsDisplayed(listOf(GO_TO_OFFER, MUTE, BLOCK, DELETE))
        }
    }

    @Test
    fun shouldBlockUnblockRoom() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(BLOCK).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            blockWatcher.checkRequestWasCalled()
            arePresetsHidden()
            interactions.onUnblockButtom().waitUntil(isCompletelyDisplayed(), withClearText(UNBLOCK))
            clickToolbarMenu()
            isAvailiableActionsDisplayed(listOf(GO_TO_OFFER, UNBLOCK, DELETE))
        }
        performChatRoom {
            interactions.onMenuAction(UNBLOCK).waitUntilIsCompletelyDisplayed().performClick()
            unblockWatcher.checkRequestWasCalled()
        }.checkResult { interactions.onUnblockButtom().checkIsGone() }
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(BLOCK).waitUntilIsCompletelyDisplayed().performClick()
            interactions.onUnblockButtom().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            interactions.onUnblockButtom().checkIsGone()
            unblockWatcher.checkRequestWasCalled()
        }
    }

    @Test
    fun shouldMuteUnmuteRoom() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(MUTE).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            muteWatcher.checkRequestWasCalled()
            clickToolbarMenu()
            isAvailiableActionsDisplayed(listOf(GO_TO_OFFER, BLOCK, UNMUTE, DELETE))
        }
        performChatRoom {
            interactions.onMenuAction(UNMUTE).waitUntilIsCompletelyDisplayed().performClick()
            unmuteWatcher.checkRequestWasCalled()
        }
    }

    @Test
    fun shouldDeleteRoom() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(DELETE).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isAlertDialogDisplayed()
            interactions.onDismissButton().performClick()
            isChatSubjectDisplayed("Audi A3 III (8V) Рестайлинг, 2019", "100 000 \u20BD")
        }
        performChatRoom {
            clickToolbarMenu()
            interactions.onMenuAction(DELETE).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            interactions.onDeleteButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        longWaitAnimation(LONG_PAUSE)
        checkMessages {
            isBaseSnippetInfoDisplayed(
                snippetIndex = 1,
                title = "Авто.ру",
                subject = "Чат с поддержкой",
            )
        }
    }

    @Test
    fun shouldDisplayCorrectDates() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_DIFFERENT_DATES)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        closeKeyboard()

        checkChatRoom {
            interactions.onTimeHeader("18 июля 2019")
                .check(isCompletelyDisplayed(), withParentIndex(0))
            interactions.onTimeHeader("18 января")
                .check(isCompletelyDisplayed(), withParentIndex(2))
            interactions.onTimeHeader("Вчера")
                .check(isCompletelyDisplayed(), withParentIndex(4))
            interactions.onTimeHeader("Сегодня")
                .check(isCompletelyDisplayed(), withParentIndex(6))
        }
    }

    @Test
    fun shouldDisplayReadStatus() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_OUTCOMING_MESSAGES)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            isMessageStatus("Это сообщение собеседник прочитал", MessageStatus.READ)
            isMessageStatus("Это сообщение собеседник ещё не успел прочесть", MessageStatus.SENT)
        }
    }

    @Test
    fun shouldDisplayReadStatusOnImageMessages() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_OUTCOMING_IMAGE_MESSAGES)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom {
            isMessageStatus(1, MessageStatus.READ)
            isMessageStatus(2, MessageStatus.SENT)
        }
    }

    @Test
    fun shouldHideSpamMessages() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_SPAM)
            delegateDispatcher(GetRoomSpamMessagesDispatcher.getResponseWithOneId())
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            isNoMessagesDisplayed()
        }
    }

    @Test
    fun shouldNotFailMessagesLoadIfMessagesFailed() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_SPAM)
            delegateDispatcher(GetRoomSpamMessagesDispatcher.failedResponse())
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        checkChatRoom {
            isChatMessageDisplayed(
                "Хей, зацени мою магнитолу на авито https://www.avito.ru/moskva/audio_i_video/muzykalnye_tsentry_magnitoly"
            )
        }
    }

    @Test
    fun shouldSeeImagePickerDialog() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            openImagePicker()
        }
        checkChatPicker {
            isImagePickerDisplayed()
        }
    }


    @Test
    fun shouldSeeImageSendingStatus() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getChatMessagesBootstrapTimeout()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            openImagePicker()
        }
        withIntents {
            performCommon {
                interceptActionPickIntent(createImageAndGetUriString())
            }
            performChatPicker {
                openExternalGallery()
            }
        }
        checkChatRoom {
            isMessageStatus(index = 1, status = MessageStatus.SENDING)
        }
    }

    @Test
    fun shouldSeeImageErrorStatus() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getChatMessagesBootstrapError()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()


        performChatRoom {
            openImagePicker()
        }
        withIntents {
            performCommon {
                interceptActionPickIntent(createImageAndGetUriString())
            }
            performChatPicker {
                openExternalGallery()
            }
        }
        checkChatRoom {
            isMessageErrorStatus(index = 1)
        }
    }

    @Test
    fun shouldSendImageMessageFromGallery() {
        val bootstrapWatcher = RequestWatcher()
        val uploadWatcher = RequestWatcher()

        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            delegateDispatcher(
                GetChatMessagesBootstrapDispatcher(
                    authorId = "454cf7e6bb8bbaef",
                    bootstrapWatcher = bootstrapWatcher,
                    uploadWatcher = uploadWatcher
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom { isNoMessagesDisplayed() }
        performChatRoom { openImagePicker() }
        withIntents {
            performCommon { interceptActionPickIntent(createImageAndGetUriString()) }
            performChatPicker { openExternalGallery() }
        }
        checkChatRoom { isMessageStatus(1, MessageStatus.READ) }

        bootstrapWatcher.checkQueryParameter(WITH_TECH_SUPPORT_UPLOAD, false.toString())
        uploadWatcher.checkQueryParameter(SIGN, GENERAL_UPLOAD_SIGN)
    }

    @Test
    fun shouldSendImageMessageFromCamera() {
        val bootstrapWatcher = RequestWatcher()
        val uploadWatcher = RequestWatcher()
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            delegateDispatcher(
                GetChatMessagesBootstrapDispatcher(
                    authorId = "454cf7e6bb8bbaef",
                    bootstrapWatcher = bootstrapWatcher,
                    uploadWatcher = uploadWatcher
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom { isNoMessagesDisplayed() }
        performChatRoom { openImagePicker() }
        withIntents {
            performCommon {
                interceptActionImageCaptureIntentWithTestImage {
                    performChatPicker { openCamera() }
                }
            }
        }
        checkChatRoom { isMessageStatus(1, MessageStatus.READ) }

        bootstrapWatcher.checkQueryParameter(WITH_TECH_SUPPORT_UPLOAD, false.toString())
        uploadWatcher.checkQueryParameter(SIGN, GENERAL_UPLOAD_SIGN)
    }

    @Test
    fun shouldSendMessage() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessage("from_454cf7e6bb8bbaef").watch {
                checkRequestBodyParameters(
                    "room_id" to CHAT_ID,
                    "payload.content_type" to "TEXT_PLAIN",
                    "payload.value" to "message without preset"
                )
            }
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom {
            isNoMessagesDisplayed()
        }
        performChatRoom {
            sendMessage("message without preset")
        }
        checkChatRoom {
            isChatMessageDisplayed("message without preset")
        }
        webServerRule.runWatch()
    }


    @Test
    fun shouldOpenDialogOnSentMessageAndCopy() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_OUTCOMING_MESSAGES)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        performChatRoom {
            interactions.onMessageBubble("Это сообщение собеседник прочитал")
                .waitUntilIsCompletelyDisplayed().perform(longClick())
            interactions.onContextMenuButton("Копировать сообщение").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon {
            isClipboardText("Это сообщение собеседник прочитал")
        }
    }


    @Test
    fun shouldOpenDialogOnReadMessageAndCopy() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.ROOM_WITH_TWO_OUTCOMING_MESSAGES)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)

        closeKeyboard()

        performChatRoom {
            interactions.onMessageBubble("Это сообщение собеседник ещё не успел прочесть")
                .waitUntilIsCompletelyDisplayed().perform(longClick())
            interactions.onContextMenuButton("Копировать сообщение").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon {
            isClipboardText("Это сообщение собеседник ещё не успел прочесть")
        }
    }

    @Test
    fun shouldDisplaySendingMessageStatus() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessageTimeout()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom {
            isNoMessagesDisplayed()
        }
        performChatRoom {
            sendMessage("message without preset")
        }
        checkChatRoom {
            isChatMessageDisplayed("message without preset")
            isMessageStatus("message without preset", MessageStatus.SENDING)
        }
    }

    @Test
    fun shouldOpenDialogOnSendingMessageAndCopy() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessageTimeout()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            sendMessage("message without preset")
            longClickMessage("message without preset")
            interactions.onContextMenuButton("Копировать сообщение").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon {
            isClipboardText("message without preset")
        }
    }

    @Test
    fun shouldDisplayErrorMessageStatus() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessageError()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            sendMessage("message without preset")
        }
        checkChatRoom {
            isChatMessageDisplayed("message without preset")
            isMessageErrorStatus("message without preset")
        }
    }

    @Test
    fun shouldOpenDialogOnErrorMessageLongClickAndCopy() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessageError()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            sendMessage("message without preset")
            longClickMessage("message without preset")
            interactions.onContextMenuButton("Копировать сообщение").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon {
            isClipboardText("message without preset")
        }
    }


    @Test
    fun shouldOpenDialogOnErrorMessageLongClickAndDelete() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            postChatMessageError()
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            sendMessage("message without preset")
            longClickMessage("message without preset")
            interactions.onContextMenuButton("Удалить").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkChatRoom {
            interactions.onMessageBubble("message without preset").checkNotExists()
        }
    }


    @Test
    fun shouldOpenDialogOnErrorMessageLongClickAndResend() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            oneOff { postChatMessageError() }
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            sendMessage("message without preset")
            longClickMessage("message without preset")
            webServerRule.routing {
                oneOff {
                    postChatMessage("from_454cf7e6bb8bbaef").watch {
                        checkRequestBodyParameters(
                            "room_id" to CHAT_ID,
                            "payload.content_type" to "TEXT_PLAIN",
                            "payload.value" to "message without preset"
                        )
                    }
                }
            }
            interactions.onContextMenuButton("Повторить отправку").waitUntilIsCompletelyDisplayed().performClick()
        }
        checkChatRoom {
            isChatMessageDisplayed("message without preset")
        }
        webServerRule.runWatch()
    }

    @Test
    fun shouldShowCallMessages() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "454cf7e6bb8bbaef",
                    partnerId = "c1a762aebe8f3086"
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom {
            isCallMessage(
                callText = "Входящий звонок",
                callDuration = "2 мин 0 с",
                callType = CallType.INCOMING,
                callDate = "12:18"
            )
            isCallMessage(
                callText = "Исходящий звонок",
                callDuration = "2 мин 1 с",
                callType = CallType.OUTCOMING,
                callDate = "12:28"
            )
            isCallMessage(
                callText = "Пропущенный звонок",
                callDuration = null,
                callType = CallType.INCOMING,
                callDate = "12:38"
            )
            isCallMessage(
                callText = "Пропущенный звонок",
                callDuration = null,
                callType = CallType.OUTCOMING,
                callDate = "12:48"
            )
        }
    }


    @Test
    fun shouldShowPresetsWhenOnlyCallMessagesPresent() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "454cf7e6bb8bbaef",
                    partnerId = "c1a762aebe8f3086"
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        checkChatRoom {
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA)
        }
    }


    @Test
    fun shouldCallToSeller() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "454cf7e6bb8bbaef",
                    partnerId = "c1a762aebe8f3086"
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        withIntents {
            performChatRoom {
                clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
            }
            checkCommon { isActionDialIntentCalled(PHONE_NUMBER) }
        }
    }

    @Test
    fun shouldCellCallNotApp2AppToSeller() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "454cf7e6bb8bbaef",
                    partnerId = "c1a762aebe8f3086"
                )
            )
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        withIntents {
            performChatRoom {
                clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
            }
            checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
        }
    }

    @Test
    fun shouldCallByApp2AppInstantlyToSeller() {
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "454cf7e6bb8bbaef",
                    partnerId = "c1a762aebe8f3086"
                )
            )
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    @Ignore("shouldShowToastWhenCallToSellerAndGettingError")
    fun shouldShowToastWhenCallToSellerAndGettingError() {
        webServerRule.routing {
            getChatRoom("from_customer_to_seller")
            delegateDispatcher(GetPhonesDispatcher.error(OFFER_ID))
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "454cf7e6bb8bbaef",
                    partnerId = "c1a762aebe8f3086"
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
        }
        checkCommon { isToastWithText("Не удалось совершить звонок") }
    }

    @Test
    fun shouldCallToCustomer() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessages(
                    myId = "331513600d5642bd",
                    partnerId = "c9357a26248ce81a"
                )
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        withIntents {
            performChatRoom {
                clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
            }
            checkCommon { isActionDialIntentCalled("+79291112233") }
        }
    }

    @Test
    fun shouldCallToCustomerViaApp2AppWithAskingByDialog() {
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessagesByApp2App(
                    myId = "331513600d5642bd",
                    partnerId = "c9357a26248ce81a"
                )
            )
            getApp2AppCallInfo()
            getOffer(OFFER_ID_FOR_VOX_TEST)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldCallToCustomerViaApp2AppInstantly() {
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            getChatRoom("from_seller_to_customer")
            delegateDispatcher(
                GetRoomMessagesTemplateDispatcher.getRoomWithCallMessagesByApp2App(
                    myId = "331513600d5642bd",
                    partnerId = "c9357a26248ce81a"
                )
            )
            getApp2AppCallInfo()
            getOffer(OFFER_ID_FOR_VOX_TEST)
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        waitSomething(1, TimeUnit.SECONDS)
        closeKeyboard()

        performChatRoom {
            clickCallMessage("Входящий звонок", "2 мин 0 с", CallType.INCOMING)
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

}
