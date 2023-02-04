package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.doesNotExist
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.PublicationFormRobot
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.configureWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 24.09.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RoomsDependencyTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun checkRoomsDependencySellApartment() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            doesNotContainRoom1AreaField()
            doesNotContainRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorStudio()).tapOn()
            doesNotContainRoom1AreaField()
            doesNotContainRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorOne()).tapOn()
            containsRoom1AreaField()
            doesNotContainRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorTwo()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorThree()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorFour()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorFive()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            containsRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            containsRoom5AreaField()
            containsRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            containsRoom5AreaField()
            containsRoom6AreaField()
            containsRoom7AreaField()
        }
    }

    @Test
    fun checkRoomsDependencyRentRoom() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentLongRoom()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }

            doesNotContainRoom1AreaField()
            doesNotContainRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorTwo()).tapOn()
            containsRoomsOfferedField(
                maxEnabledValue = lookup.matchesRoomsOfferedSelectorOne(),
                selectedValue = lookup.matchesRoomsOfferedSelectorOne()
            )
            containsRoom1AreaField()
            doesNotContainRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorThree()).tapOn()
            containsRoomsOfferedField(
                maxEnabledValue = lookup.matchesRoomsOfferedSelectorTwo(),
                selectedValue = lookup.matchesRoomsSelectorOne()
            )
            onView(lookup.matchesRoomsOfferedSelectorTwo()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            doesNotContainRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorFour()).tapOn()
            containsRoomsOfferedField(
                maxEnabledValue = lookup.matchesRoomsOfferedSelectorThree(),
                selectedValue = lookup.matchesRoomsSelectorTwo()
            )
            onView(lookup.matchesRoomsOfferedSelectorThree()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            doesNotContainRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorFive()).tapOn()
            containsRoomsOfferedField(
                maxEnabledValue = lookup.matchesRoomsOfferedSelectorFour(),
                selectedValue = lookup.matchesRoomsSelectorThree()
            )
            onView(lookup.matchesRoomsOfferedSelectorFour()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            doesNotContainRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            containsRoomsOfferedField(
                maxEnabledValue = lookup.matchesRoomsOfferedSelectorFive(),
                selectedValue = lookup.matchesRoomsSelectorFour()
            )
            onView(lookup.matchesRoomsOfferedSelectorFive()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            containsRoom5AreaField()
            doesNotContainRoom6AreaField()
            doesNotContainRoom7AreaField()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            containsRoomsOfferedField(
                maxEnabledValue = lookup.matchesRoomsOfferedSelectorSix(),
                selectedValue = lookup.matchesRoomsSelectorFive()
            )
            onView(lookup.matchesRoomsOfferedSelectorSix()).tapOn()
            containsRoom1AreaField()
            containsRoom2AreaField()
            containsRoom3AreaField()
            containsRoom4AreaField()
            containsRoom5AreaField()
            containsRoom6AreaField()
            doesNotContainRoom7AreaField()
        }
    }

    private fun PublicationFormRobot.doesNotContainRoom1AreaField() {
        onView(lookup.matchesRoom1AreaField()).check(doesNotExist())
    }

    private fun PublicationFormRobot.doesNotContainRoom2AreaField() {
        onView(lookup.matchesRoom2AreaField()).check(doesNotExist())
    }

    private fun PublicationFormRobot.doesNotContainRoom3AreaField() {
        onView(lookup.matchesRoom3AreaField()).check(doesNotExist())
    }

    private fun PublicationFormRobot.doesNotContainRoom4AreaField() {
        onView(lookup.matchesRoom4AreaField()).check(doesNotExist())
    }

    private fun PublicationFormRobot.doesNotContainRoom5AreaField() {
        onView(lookup.matchesRoom5AreaField()).check(doesNotExist())
    }

    private fun PublicationFormRobot.doesNotContainRoom6AreaField() {
        onView(lookup.matchesRoom6AreaField()).check(doesNotExist())
    }

    private fun PublicationFormRobot.doesNotContainRoom7AreaField() {
        onView(lookup.matchesRoom7AreaField()).check(doesNotExist())
    }
}
