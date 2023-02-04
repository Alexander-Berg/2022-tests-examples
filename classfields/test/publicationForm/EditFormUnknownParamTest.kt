package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormEditActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.configureWebServer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author solovevai on 18.10.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class EditFormUnknownParamTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormEditActivityTestRule(
        offerId = "1234",
        createTime = Date(),
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun checkEditUnknownParam() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellApartmentUnknownParam.json")
        }

        activityTestRule.launchActivity()
        registerMarketIntent()

        performOnConfirmationDialog {
            waitUntil { isUpdateRequiredDialogShown() }
            tapOn(lookup.matchesPositiveButton())
            NamedIntents.intended(matchesMarketIntent())
        }
    }
}
