package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertSize
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.BodyNode.Companion.checkEventInfo
import ru.auto.ara.core.dispatchers.frontlog.checkFrontlogCommonParams
import ru.auto.ara.core.dispatchers.frontlog.postFrontLog
import ru.auto.ara.core.dispatchers.stories.getFullStory
import ru.auto.ara.core.dispatchers.stories.getPreviewStory
import ru.auto.ara.core.robot.stories.checkFullStory
import ru.auto.ara.core.robot.stories.performFullStory
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class StoryDeeplinkTest {

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldOpenStoryDeeplink() {

        val storyId = "7941829b-8b49-4be0-b0bf-80231cefea74"

        webServerRule.routing {
            getPreviewStory(storyId)
            getFullStory(storyId)
            postFrontLog().watch {
                checkRequestsCount(1)
                checkFrontlogCommonParams(EVENT_NAME)
                checkEventInfo(EVENT_NAME) {
                    get(EVENT_NAME).asObject {
                        assertSize(2)
                        get("story_id").assertValue(storyId)
                        get("slide_id").assertValue("0")
                    }
                }
            }
        }

        checkStory(storyId)
    }

    @Test
    fun shouldOpenOfferStoryDeeplink() {

        val storyId = "7941829b-8b49-4be0-b0bf-80231cefea75"

        webServerRule.routing {
            getPreviewStory(storyId)
            getFullStory(storyId)
            postFrontLog().watch {
                checkRequestsCount(1)
                checkFrontlogCommonParams(EVENT_NAME)
                checkEventInfo(EVENT_NAME) {
                    get(EVENT_NAME).asObject {
                        assertSize(4)
                        get("story_id").assertValue(storyId)
                        get("slide_id").assertValue("0")
                        get("card_id").assertValue("111_test")
                        get("card_category").assertValue("CARS")
                    }
                }
            }
        }

        checkStory("7941829b-8b49-4be0-b0bf-80231cefea75")
    }

    private fun checkStory(storyId: String) {
        activityTestRule.launchDeepLinkActivity("autoru://app/story/$storyId")
        checkFullStory {
            isStoryControlsAndTextsDisplayed(
                listOf(
                    "Дилер подарит полтонны туалетной бумаги и гречки за покупку машины",
                    "Подробнее"
                )
            )
        }

        // wait some time for frontlog event sending
        waitSomething(2L)

        performFullStory {
            closeStory()
        }
    }

    companion object {
        private const val EVENT_NAME = "story_show_event"
    }
}
