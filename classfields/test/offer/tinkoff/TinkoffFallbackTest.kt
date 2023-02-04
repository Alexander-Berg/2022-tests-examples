package ru.auto.ara.test.offer.tinkoff

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.ExperimentsScope
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.device.postHello
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.preliminary.updatePreliminary
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.mapWithLoan
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.bankloan.performCreditPreliminary
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.robot.ratecall.checkRateCallExp
import ru.auto.ara.core.robot.ratecall.performRateCallExp
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.tinkoff.checkLoan
import ru.auto.ara.core.robot.tinkoff.performLoan
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.BlockWebViewLoadUrlRule
import ru.auto.ara.core.rules.DecreaseRateCallTimeRule
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.buildLoanUrl
import ru.auto.ara.core.utils.closeSoftKeyboard
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.intendingNotInternal
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.respondWithOk
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.data.model.credit.Bank
import ru.auto.data.util.toLowerString
import ru.auto.experiments.Experiments
import ru.auto.experiments.commercialBank
import ru.auto.experiments.creditBankCard
import ru.auto.experiments.fallbackBank
import ru.auto.experiments.loanBrokerEnabled
import java.util.concurrent.TimeUnit

private const val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
private const val FILE_OFFER_ID = "1093024666-aa502a2a"
private const val DEFAULT_OFFER_ID = "1082957054-8d55bf9a"

@RunWith(AndroidJUnit4::class)
class TinkoffFallbackSravniTest : TinkoffFallbackSetup() {

    // OFFER CARD TITLE LOAN MONTLY PRICE
    @Test
    fun shouldShowFallbackBankLoanPriceInTitleIfMoreThan51Hours() {
        webServerRule.routing { getTinkoffClaims("client_verification") }
        openCard()
        checkOfferCard { isLoanPriceVisible(getResourceString(R.string.person_profile_loan_monthly_payment_from, "35 100")) }
    }

    // OFFER CARD LOAN BLOCK
    @Test
    fun shouldNotShowFallbackCalcIfTinkoffTerminalState() {
        val urlBank = Bank.TINKOFF.value
        webServerRule.routing {
            getTinkoffClaims("meeting")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
        }

        openCardAndScrollToLoan()
        checkLoan {
            isHeadLoanBlockDisplayed()
            isHoldLoanDisplayed()
        }
    }

    @Test
    fun shouldShowFallbackBankCalculatorIfMoreThan51Hours() {
        val urlBank = Bank.SRAVNI.value
        webServerRule.routing {
            getTinkoffClaims("first_agreement")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
        }

        openCardAndScrollToLoan()
        checkLoan {
            isSravniHeaderDisplayed()
            isSravniCalcDisplayed()
        }
        checkWebViewOpenedWithBank(urlBank)
    }

    // RATE CALL
    @Test
    fun shouldUseFallbackBankInRateCallFromCardIfMoreThan51Hours() {
        webServerRule.routing {
            getTinkoffClaims("client_verification")
            getPhones("1082957054-8d55bf9a", PhonesResponse.ONE)
            updatePreliminary("sravniru")
            getPreliminary("sravniru")
        }
        openCard()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performOfferCard { clickCallButton() }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(35100) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(buildLoanUrl("sravniru"))
        }
    }

    @Test
    fun shouldNotUseFallbackBankInRateCallFromCardIfTinkoffTerminalState() {
        webServerRule.routing {
            getTinkoffClaims("second_agreement")
            getPhones("1082957054-8d55bf9a", PhonesResponse.ONE)
            updatePreliminary("tinkoff")
            getPreliminary("tinkoff")
        }
        openCard()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performOfferCard { clickCallButton() }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(37650) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(buildLoanUrl("tinkoff"))
        }
    }

    @Test
    fun shouldUseFallbackBankInRateCallFromListingIfMoreThan51Hours() {
        webServerRule.routing {
            getTinkoffClaims("client_verification")
            getChatRoom("rarely_reply_time")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getPhones("1087439802-b6940925", PhonesResponse.ONE)
            delegateDispatchers(
                ParseDeeplinkDispatcher.carsAll(),
                PostSearchOffersDispatcher(fileName = "informers_extended_snippet_brand_cert")
            )
            updatePreliminary("sravniru")
            getPreliminary("sravniru")
        }
        openListing()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performListingOffers {
                scrollToFirstSnippet()
                clickVisibleCallButton()
            }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(15950) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=800000&term=5&fee=200000&only_frame=true&bank_name=sravniru"
            )
        }
    }

    @Test
    fun shouldNotUseFallbackBankInRateCallFromListingIfTinkoffTerminalState() {
        webServerRule.routing {
            getTinkoffClaims("second_agreement")
            getPhones("1087439802-b6940925", PhonesResponse.ONE)
            getChatRoom("rarely_reply_time")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            delegateDispatchers(
                ParseDeeplinkDispatcher.carsAll(),
                PostSearchOffersDispatcher(fileName = "informers_extended_snippet_brand_cert")
            )
            updatePreliminary("tinkoff")
            getPreliminary("tinkoff")
        }
        openListing()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performListingOffers {
                scrollToFirstSnippet()
                clickVisibleCallButton()
            }
        }
        checkRateCallExp { checkLoanWithMonthlyPayment(17100) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=800000&term=5&fee=200000&only_frame=true&bank_name=tinkoff"
            )
        }
    }

    @Test
    @Ignore("it leads to looping and blocks another tests on emulator")
    fun shouldUseFallbackBankInRateCallFromChatIfMoreThan51Hours() {
        webServerRule.routing {
            getTinkoffClaims("client_verification")
            getPhones(
                "1086893549-303bb635",
                PhonesResponse.ONE
            )
            updatePreliminary("sravniru")
            getPreliminary("sravniru")
        }
        openRateCallFromChat()

        checkRateCallExp { checkLoanWithMonthlyPayment(59800) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=3000000&term=5&fee=1140000&only_frame=true&bank_name=sravniru"
            )
        }
    }

    @Test
    @Ignore("it leads to looping and blocks another tests on emulator")
    fun shouldNotUseFallbackBankInRateCallFromChatIfTinkoffTerminalState() {
        webServerRule.routing {
            getTinkoffClaims("second_agreement")
            getPhones(
                "1086893549-303bb635",
                PhonesResponse.ONE
            )
            updatePreliminary("tinkoff")
            getPreliminary("tinkoff")
        }
        openRateCallFromChat()

        checkRateCallExp { checkLoanWithMonthlyPayment(64150) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=3000000&term=5&fee=1140000&only_frame=true&bank_name=tinkoff"
            )
        }
    }

}

@RunWith(AndroidJUnit4::class)
class TinkoffFallbackSravniNextDayTest : TinkoffFallbackSetup() {

    override fun getTimeRule() = SetupTimeRule(date = "29.02.2019", timeZoneId = "Europe/Moscow")

    // OFFER CARD TITLE LOAN MONTLY PRICE
    @Test
    fun shouldShowFallbackLoanPriceInTitleIfImmediateFallbackStatus() {
        webServerRule.routing { getTinkoffClaims("reject") }
        openCard()
        checkOfferCard { isLoanPriceVisible(getResourceString(R.string.person_profile_loan_monthly_payment_from, "35 100")) }
    }

    // OFFER CARD LOAN BLOCK
    @Test
    fun shouldNotFallbackBankInLoanBlockIfLessThan51Hours() {
        val urlBank = Bank.SRAVNI.value
        webServerRule.routing {
            getTinkoffClaims("first_agreement")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
        }

        openCardAndScrollToLoan()
        checkLoan { isHoldLoanDisplayed() }
    }

    @Test
    fun shouldShowFallbackBankCalculatorIfImmediateFallbackStatus() {
        val urlBank = Bank.SRAVNI.value
        webServerRule.routing {
            getTinkoffClaims("reject")
            updatePreliminary(urlBank)
            getPreliminary(urlBank)
        }

        openCardAndScrollToLoan()
        checkLoan {
            isSravniHeaderDisplayed()
            isSravniCalcDisplayed()
        }
        checkWebViewOpenedWithBank(urlBank)
    }

    // RATE CALL
    @Test
    fun shouldNotUseFallbackBankInRateCallFromCardIfLessThan51Hours() {
        webServerRule.routing {
            getTinkoffClaims("client_verification")
            getPhones("1082957054-8d55bf9a", PhonesResponse.ONE)
            updatePreliminary("tinkoff")
            getPreliminary("tinkoff")
        }
        openCard()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performOfferCard { clickCallButton() }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(37650) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(buildLoanUrl("tinkoff"))
        }
    }

    @Test
    fun shouldUseFallbackBankInRateCallFromCardIfImmediateFallbackStatus() {
        webServerRule.routing {
            getTinkoffClaims("reject")
            getPhones("1082957054-8d55bf9a", PhonesResponse.ONE)
            updatePreliminary("sravniru")
            getPreliminary("sravniru")
        }

        openCard()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performOfferCard { clickCallButton() }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(35100) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(buildLoanUrl("sravniru"))
        }
    }

    @Test
    fun shouldNotUseFallbackBankInRateCallFromListingIfLessThan51Hours() {
        webServerRule.routing {
            getTinkoffClaims("client_verification")
            getPhones("1087439802-b6940925", PhonesResponse.ONE)
            getChatRoom("rarely_reply_time")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            delegateDispatchers(
                ParseDeeplinkDispatcher.carsAll(),
                PostSearchOffersDispatcher(fileName = "informers_extended_snippet_brand_cert")
            )
            updatePreliminary("tinkoff")
            getPreliminary("tinkoff")
        }
        openListing()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performListingOffers {
                scrollToFirstSnippet()
                clickVisibleCallButton()
            }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(17100) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=800000&term=5&fee=200000&only_frame=true&bank_name=tinkoff"
            )
        }
    }

    @Test
    fun shouldUseFallbackBankInRateCallFromListingIfImmediateFallbackStatus() {
        webServerRule.routing {
            getTinkoffClaims("reject")
            getChatRoom("rarely_reply_time")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getPhones("1087439802-b6940925", PhonesResponse.ONE)
            delegateDispatchers(
                ParseDeeplinkDispatcher.carsAll(),
                PostSearchOffersDispatcher(fileName = "informers_extended_snippet_brand_cert")
            )
            updatePreliminary("sravniru")
            getPreliminary("sravniru")
        }
        openListing()
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performListingOffers {
                scrollToFirstSnippet()
                clickVisibleCallButton()
            }
        }

        checkRateCallExp { checkLoanWithMonthlyPayment(15950) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=800000&term=5&fee=200000&only_frame=true&bank_name=sravniru"
            )
        }
    }

    @Test
    @Ignore("it leads to looping and blocks another tests on emulator")
    fun shouldNotUseFallbackBankInRateCallFromChatIfLessThan51Hours() {
        webServerRule.routing {
            getTinkoffClaims("client_verification")
            getPhones(
                "1086893549-303bb635",
                PhonesResponse.ONE
            )
            updatePreliminary("tinkoff")
            getPreliminary("tinkoff")
        }
        openRateCallFromChat()

        checkRateCallExp { checkLoanWithMonthlyPayment(64150) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=3000000&term=5&fee=1140000&only_frame=true&bank_name=tinkoff"
            )
        }
    }

    @Test
    @Ignore("it leads to looping and blocks another tests on emulator")
    fun shouldUseFallbackBankInRateCallFromChatIfImmediateFallbackStatus() {
        webServerRule.routing {
            getTinkoffClaims("reject")
            getPhones(
                "1086893549-303bb635",
                PhonesResponse.ONE
            )
            updatePreliminary("sravniru")
            getPreliminary("sravniru")
        }
        openRateCallFromChat()

        checkRateCallExp { checkLoanWithMonthlyPayment(59800) }

        watchWebView {
            performRateCallExp { clickLoanPromo() }
            applyPreliminary()
        }.checkResult {
            checkUrlMatches(
                "https://auto.ru/promo/autoru-tcs/" +
                    "?&amount=3000000&term=5&fee=1140000&only_frame=true&bank_name=sravniru"
            )
        }
    }

}

@RunWith(AndroidJUnit4::class)
class TinkoffDefaultFallbackInDayBankTest : TinkoffFallbackSetup() {

    override fun ExperimentsScope.getFallbackBank() = Unit
    override fun getTimeRule() = SetupTimeRule(date = "29.02.2019", timeZoneId = "Europe/Moscow")
    override fun ExperimentsScope.getCommercialBank() {
        Experiments::commercialBank.then(value = Bank.SRAVNI.toLowerString())
    }

    @Test
    fun shouldFallbackToDefaultIfImmediateFallbackStatus() {
        webServerRule.routing { getTinkoffClaims("reject") }
        openCardAndScrollToLoan()
        checkLoan {
            isAlfaHeaderDisplayed()
            isAlfaCalcDisplayed()
        }
    }

    @Test
    fun shouldNotFallbackIfTimeLessThan51Hours() {
        webServerRule.routing { getTinkoffClaims("draft") }
        openCardAndScrollToLoan()
        checkLoan {
            isDraftLoanDisplayed()
        }
    }

}

@RunWith(AndroidJUnit4::class)
class TinkoffDefaultFallbackBankTest : TinkoffFallbackSetup() {

    override fun ExperimentsScope.getFallbackBank() = Unit
    override fun ExperimentsScope.getCommercialBank() {
        Experiments::commercialBank then Bank.SRAVNI.toLowerString()
    }

    @Test
    fun shouldFallbackToDefaultIfPostponeFallbackStatus() {
        webServerRule.routing { getTinkoffClaims("draft") }
        openCardAndScrollToLoan()
        checkLoan {
            isAlfaHeaderDisplayed()
            isAlfaCalcDisplayed()
        }
    }

    @Test
    fun shouldNotFallbackToDefaultIfTerminalStatus() {
        webServerRule.routing { getTinkoffClaims("second_agreement") }
        openCardAndScrollToLoan()
        checkLoan {
            isHeadLoanBlockDisplayed()
            isThisCarWaitApprovalLoanDisplayed()
        }
    }
}

@Suppress("UnnecessaryAbstractClass") // should be abstract to prevent problems when running in sandbox
abstract class TinkoffFallbackSetup {

    protected val webServerRule = WebServerRule {
        stub {
            makeXmlForReportByOfferId(
                offerId = DEFAULT_OFFER_ID,
                dirType = NOT_BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
            makeXmlForOffer(offerId = DEFAULT_OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        }
        userSetup()
        stub { getTinkoffClaims("reject") }
        stub { getOffer(DEFAULT_OFFER_ID) }
        getPreliminary("tinkoff")
        postHello(
            headers = mapOf(
                "X-Session-Id" to "52895595|1648029072971.7776100.UMCWpGKWWedlPSHJhsYcZw.VENviKlySz4ktyf2Nr3QP0kO1aTBYZz8W8Sb6Q",
                "X-Device-Uid" to "g623a22c952e6sg71oiga4ddf9b8okfq.d28689eef0721545e8da6d71b784d6e8",
            )
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        ImmediateImageLoaderRule.mockPhotos(),
        getTimeRule(),
        DecreaseRateCallTimeRule(),
        BlockWebViewLoadUrlRule(),
        SetupAuthRule(),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
                getFallbackBank()
                getCommercialBank()
                Experiments::creditBankCard then Bank.TINKOFF.toLowerString()
            }
        )
    )

    open fun getTimeRule() = SetupTimeRule(date = "01.01.2020", timeZoneId = "Europe/Moscow")
    open fun ExperimentsScope.getFallbackBank() {
        Experiments::fallbackBank then Bank.SRAVNI.toLowerString()
    }

    open fun ExperimentsScope.getCommercialBank() {
        Experiments::commercialBank then Bank.TINKOFF.toLowerString()
    }


    protected fun openCardAndScrollToLoan(offerId: String = DEFAULT_OFFER_ID) {
        openCard(offerId)

        performOfferCardVin { clickShowFreeReport() }
        checkVinReport { isVinReport() }
        waitSomething(300, TimeUnit.MILLISECONDS)
        performVinReport { close() }
        checkOfferCard { isOfferCard() }
        performOfferCard { scrollToLoanBlock() }
    }

    protected fun openCard(offerId: String = DEFAULT_OFFER_ID) {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$offerId")
    }

    protected fun openListing() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
    }

    protected fun openRateCallFromChat() {
        webServerRule.routing {
            getChatRoom("rarely_reply_time", mapper = { mapWithLoan() })
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getRoomSpamMessages(RoomSpamMessage.EMPTY)
        }
        activityTestRule.launchDeepLinkActivity("autoru://app/chat/room/f5d5b794395b82aff25e325d21c987c3")
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performChatRoom { clickReplyTimeCallButton() }
        }
    }

    protected fun applyPreliminary() {
        closeSoftKeyboard()
        waitSomething(2, TimeUnit.SECONDS)
        performCreditPreliminary { clickApplyCredit() }
        waitSomething(2, TimeUnit.SECONDS)
    }

    protected fun checkWebViewOpenedWithBank(urlBank: String) {
        watchWebView {
            performLoan { scrollAndClickApplyLoanButton() }
        }.checkResult {
            checkUrlMatches(buildLoanUrl(urlBank))
            checkWithCookies(listOf("autoru_sid", "autoruuid"), "auto.ru")
        }
    }

}
