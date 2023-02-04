package ru.auto.ara.test.main

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.stories.getFullStory
import ru.auto.ara.core.dispatchers.stories.getPreviewStory
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.interaction.stories.FullStoryInteractions
import ru.auto.ara.core.robot.stories.checkFullStory
import ru.auto.ara.core.robot.stories.performFullStory
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

const val ADD_TAB_WITH_DOT_SCREENSHOT_NAME = "main/tabs/add_tab_with_dot.png"
const val ADD_TAB_WITHOUT_DOT_SCREENSHOT_NAME = "main/tabs/add_tab_without_dot.png"
const val ADD_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME = "main/tabs/add_tab_without_dot_selected.png"

@RunWith(AndroidJUnit4::class)
class CarOnlinePresentationAuthorizedTabTest {
    private val webServerRule = WebServerRule {
        userSetup()
        getFullStory(STORY_ID)
        getPreviewStory(STORY_ID)
    }

    private val activityRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        SetupAuthRule()
    )

    @Test
    fun shouldSeeDotOnAddTab() {
        checkMain {
            isLowTabTheSame(ADD_TAB, ADD_TAB_WITH_DOT_SCREENSHOT_NAME)
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun shouldOpenStoryWhenOpenTab() {
        performMain {
            openLowTab(
                R.string.add_offer,
                FullStoryInteractions.onDurationBar(),
                ViewMatchers.isCompletelyDisplayed()
            )
        }
        checkFullStory {
            isStoryControlsAndTextsDisplayed(
                listOf(
                    "Показывайте машину онлайн",
                    "Отметьте в объявлении пункт «Готов показывать онлайн»  и проводите осмотры по видеосвязи в любом мессенджере.",
                    "Я готов"
                )
            )
        }
        performFullStory { closeStory() }
        checkMain {
            isLowTabTheSame(ADD_TAB, ADD_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    companion object {
        private const val STORY_ID = "18f0be4c-77fb-4bfc-b82a-35989d53e895"
        private const val ADD_TAB = R.string.add_offer
    }
}
