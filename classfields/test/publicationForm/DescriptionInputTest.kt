package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnDescriptionInputScreen
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
 * @author matek3022 on 2020-07-07.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DescriptionInputTest : BasePublishFormTest() {

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
    fun checkFields() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        prepareDescriptionPickerScreen()

        performOnDescriptionInputScreen {
            waitUntil {
                isExpandedToolbarShown()
            }
            isDoneButtonShown()
            isInputFieldShown("")

            collapseAppBar()

            waitUntil {
                isCollapsedToolbarShown()
            }
            isDoneButtonShown()
            isInputFieldShown("")
        }
    }

    @Test
    fun typeTextConfirmAndReenter() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        prepareDescriptionPickerScreen()

        performOnDescriptionInputScreen {
            typeText(lookup.matchesInputView(), TEST_TEXT)
            isInputFieldShown(TEST_TEXT)
            tapOn(lookup.matchesDoneButton())
        }

        performOnPublicationFormScreen {
            scrollToPosition(lookup.matchesDescriptionField())
            containsDescriptionField(TEST_TEXT)
            tapOn(lookup.matchesDescriptionField())
        }

        performOnDescriptionInputScreen {
            isInputFieldShown(TEST_TEXT)
        }
    }

    @Test
    fun typeTextAndClose() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        prepareDescriptionPickerScreen()

        performOnDescriptionInputScreen {
            typeText(lookup.matchesInputView(), TEST_TEXT)
            isInputFieldShown(TEST_TEXT)
            pressBack()
        }

        performOnPublicationFormScreen {
            scrollToPosition(lookup.matchesDescriptionField())
            containsDescriptionField()
        }
    }

    private fun prepareDescriptionPickerScreen() {
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesDescriptionField())
            tapOn(lookup.matchesDescriptionField())
        }
    }

    companion object {
        private const val TEST_TEXT = "test text"
    }
}
