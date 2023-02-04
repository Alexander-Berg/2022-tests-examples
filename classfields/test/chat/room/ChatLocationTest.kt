package ru.auto.ara.test.chat.room

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetTechSupportChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.postChatMessage
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.chat.performChatPicker
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.locationpicker.checkLocationPicker
import ru.auto.ara.core.robot.locationpicker.checkLocationViewer
import ru.auto.ara.core.robot.locationpicker.performLocationPicker
import ru.auto.ara.core.robot.locationpicker.performLocationViewer
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.AccessFineLocationRule
import ru.auto.ara.core.rules.MockLocationRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.LocationPoint

@RunWith(AndroidJUnit4::class)
class ChatLocationTest {

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetRoomMessagesDispatcher.getLocationResponse(),
            GetRoomSpamMessagesDispatcher.getEmptyResponse(),
            GetChatRoomDispatcher("from_customer_to_seller"),
            GetTechSupportChatRoomDispatcher()
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val mockLocationRule = MockLocationRule()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        ImmediateImageLoaderRule { url ->
            url.contains("static-maps.yandex.ru")
        },
        AccessFineLocationRule(),
        activityTestRule,
        mockLocationRule,
        SetupAuthRule(),
    )

    @Before
    fun setup() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
    }

    @Test
    fun shouldOpenChatWithTwoMiniMaps() {
        checkChatRoom {
            isMapDisplayed(
                index = 1,
                link = "https://static-maps.yandex.ru/1.x%2F?" +
                    "ll=30.31217721390641%2C60.02457231535383&" +
                    "z=16&l=map&pt=30.31217721390641%2C60.02457231535383%2Ccomma"
            )
            isMapDisplayed(
                index = 2,
                link = "https://static-maps.yandex.ru/1.x%2F?" +
                    "ll=35.31217721390641%2C40.02457231535383&" +
                    "z=16&l=map&pt=35.31217721390641%2C40.02457231535383%2Ccomma"
            )
        }
    }

    @Test
    fun shouldOpenAndCloseLocationViewer() {
        performChatRoom {
            clickMapMessage(1)
        }
        checkLocationViewer {
            isLocationViewer()
        }
        performLocationViewer {
            closeViewer()
        }
        checkChatRoom {
            isChatSubjectDisplayed("Audi A3 III (8V) Рестайлинг, 2019", "100 000 \u20BD")
        }
    }

    @Test
    fun shouldOpenLocationViewerWithCertainCoordinate() {
        performChatRoom {
            clickMapMessage(1)
        }
        checkLocationViewer {
            isLocationViewer()
            isMapCenteredAtPoint(TEST_LOCATION_POINT)
        }
    }

    @Test
    fun shouldShareLocationFromViewer() {
        performChatRoom {
            clickMapMessage(1)
        }
        withIntents {
            performLocationViewer {
                waitUntilAddressLoaded()
                clickShareButton()
            }
            checkCommon {
                isSendPlainTextIntentCalled(
                    "Удельный проспект, 53\n" +
                        "https://yandex.ru/maps/" +
                        "?mode=whatshere&whatshere%5Bpoint%5D=30.31217721390641%2C60.02457231535383&whatshere"
                )
            }
        }
    }

    @Test
    fun shouldOpenGeoDialog() {
        performChatRoom {
            clickMapMessage(1)
        }
        withIntents {
            performLocationViewer {
                waitUntilAddressLoaded()
                clickMakeRouteButton()
                clickOnGeoApp()
            }
            checkCommon {
                isOpenMapIntentCalled("geo:0,0?q=60.02457231535383,30.31217721390641(Удельный проспект, 53)")
            }
        }
    }

    @Test
    fun shouldOpenAndCloseLocationPicker() {
        openLocationPicker()
        checkLocationPicker {
            isLocationPicker()
        }
        performLocationPicker {
            closeViewer()
        }
        checkChatRoom {
            isChatSubjectDisplayed("Audi A3 III (8V) Рестайлинг, 2019", "100 000 \u20BD")
        }
    }

    @Test
    fun shouldPickLocation() {
        webServerRule.routing { postChatMessage("map") }
        openLocationPicker()
        performLocationPicker {
            scrollMapTo(TEST_LOCATION_POINT)
            clickSendLocation()
        }
        checkChatRoom {
            isMapDisplayed(
                index = 3,
                link = "https://static-maps.yandex.ru/1.x%2F?" +
                    "ll=30.31217721390641%2C60.02457231535383&" +
                    "z=16&l=map&pt=30.31217721390641%2C60.02457231535383%2Ccomma"
            )
        }
    }

    private fun openLocationPicker() {
        performChatRoom {
            openImagePicker()
        }
        performChatPicker {
            openLocation()
        }
    }

    companion object {
        private val TEST_LOCATION_POINT = LocationPoint(60.02457231535383, 30.31217721390641)
        private const val DEFAULT_CHAT_DEEPLINK = "autoru://app/chat/room/6822dc60e71440f35f012d0b35b5b234"
    }
}
