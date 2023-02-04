package com.yandex.mobile.realty.test.publicationForm

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

/**
 * @author andrey-bgm on 07/07/2021.
 */
class GarageDependencyTest : BasePublishFormTest() {

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
    fun checkGarageParkingTypeDependency() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellGarage()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellGarageCollapsedToolbarTitle() }

            doesNotContainGarageParkingTypeField()

            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorGarage()).tapOn()
            doesNotContainGarageParkingTypeField()

            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorParkingPlace()).tapOn()
            containsGarageParkingTypeField()

            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorBox()).tapOn()
            containsGarageParkingTypeField()
        }
    }

    private fun PublicationFormRobot.doesNotContainGarageParkingTypeField() {
        onView(lookup.matchesGarageParkingTypeField()).check(doesNotExist())
    }
}
