package ru.auto.ara.test.offer.communicate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.appmetrica.checkAppMetrica
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.getResourceStringWithoutNonbreakingSpace
import ru.auto.ara.core.utils.intendingNotInternal
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.respondWithOk
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.experiments.Experiments
import ru.auto.experiments.letsCallDealer
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MakeCallTest {

    private val webServerRule = WebServerRule {
        postSearchOffers()
    }

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        DisableAdsRule(),
        SetPreferencesRule(),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf { Experiments::letsCallDealer then true }
        )
    )

    @Test
    fun shouldCallWithOnePhone() {
        webServerRule.routing {
            getOffer("1083280948-dc2c56")
            getPhones("1083280948-dc2c56", PhonesResponse.ONE)
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1083280948-dc2c56")
        checkOfferCard {
            isMakeCallButtonTitleDisplayed()
            isMakeCallButtonSubtitleDisplayed("с 9:00 до 21:00")
        }
        withIntents {
            performOfferCard { clickCallButton() }
            checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
        }
        checkAppMetrica {
            checkAppMetricaEvent(
                eventName = "Тап по кнопке \"Позвонить\" (авто)",
                eventParams = mapOf(
                    "Источник" to "Карточка объявления",
                    "Новый" to mapOf("Компания" to "Санкт-Петербург"),
                    "Продавец" to "Дилер",
                )
            )
        }
    }

    @Test
    fun shouldSeeCallTimeWholeDay() {
        webServerRule.routing {
            getOffer("1084148879-75b01c96")
            getPhones("1084148879-75b01c96", PhonesResponse.ONE)
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1084148879-75b01c96")
        checkOfferCard {
            isMakeCallButtonTitleDisplayed()
            isMakeCallButtonSubtitleDisplayed(getResourceString(R.string.round_the_clock))
        }
    }

    @Test
    fun shouldSeeCallSellerOnlineCallToAction() {
        webServerRule.routing {
            getOffer("1096920532-7b934805")
            getPhones("1096920532-7b934805", PhonesResponse.ONE)
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1096920532-7b934805")
        checkOfferCard {
            isMakeCallButtonTitleDisplayed()
            isMakeCallButtonSubtitleDisplayed(getResourceString(R.string.seller_online_call_them))
        }
    }

    @Test
    fun shouldSeeMultiplePhones() {
        webServerRule.routing {
            getOffer("1082957054-8d55bf9a")
            getPhones("1082957054-8d55bf9a", PhonesResponse.MULTIPLE)
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")
        checkOfferCard {
            isMakeCallButtonTitleDisplayed()
            isMakeCallButtonSubtitleGone()
        }
        performOfferCard { clickCallButton() }.checkResult {
            isPhonesBottomsheetDisplayed(getResourceStringWithoutNonbreakingSpace(R.string.call_to_seller), "Сергей")
            isPhoneCellDisplayed("+7 934 777-97-77", "с 1:00 до 23:00")
            isPhoneCellDisplayed("+7 950 287-35-55", "с 5:00 до 23:00")
        }
    }

    @Test
    fun shouldCallActionDialIntentFromBottomsheet() {
        webServerRule.routing {
            getOffer("1082957054-8d55bf9a")
            getPhones("1082957054-8d55bf9a", PhonesResponse.MULTIPLE)
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")
        performOfferCard { clickCallButton() }
        withIntents {
            performOfferCard { clickOnPhoneNumber("+7 950 287-35-55") }
            checkCommon { isActionDialIntentCalled("+7 950 287-35-55") }
        }
    }

    @Test
    fun shouldBackToOfferCardByCloseIcon() {
        webServerRule.routing {
            getOffer("1082957054-8d55bf9a")
            getPhones("1082957054-8d55bf9a", PhonesResponse.MULTIPLE)
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")
        performOfferCard {
            clickCallButton()
            waitSomething(300, TimeUnit.MILLISECONDS)
            clickPhonesCloseIcon()
        }.checkResult {
            isOfferCard()
        }
    }

    @Test
    fun shouldCallWithOnePhoneMoto() {
        webServerRule.routing {
            postSearchOffers(category = "moto")
            getOffer("1894128-d229", "moto")
            getPhones("1894128-d229", PhonesResponse.ONE, "moto")
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/moto/used/sale/1894128-d229")
        withIntents {
            intendingNotInternal().respondWithOk()
            performOfferCard { clickCallButton() }
            checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
        }
    }

    @Test
    fun shouldCallWithOnePhoneTrucks() {
        webServerRule.routing {
            postSearchOffers(category = "trucks")
            getOffer("10448426-ce654669", "trucks")
            getPhones("10448426-ce654669", PhonesResponse.ONE, "trucks")
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/trucks/used/sale/10448426-ce654669")
        withIntents {
            intendingNotInternal().respondWithOk()
            performOfferCard { clickCallButton() }
            checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
        }
    }

}
