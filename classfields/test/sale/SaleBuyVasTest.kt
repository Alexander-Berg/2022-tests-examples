package ru.auto.ara.test.sale

import androidx.test.core.app.launchActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.SplashActivity
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.paymentStatusError
import ru.auto.ara.core.dispatchers.payment.getPaymentError
import ru.auto.ara.core.dispatchers.payment.paymentProcessError
import ru.auto.ara.core.dispatchers.payment.postInitPayment
import ru.auto.ara.core.dispatchers.payment.postProcessPaymentErrorWithoutDescription
import ru.auto.ara.core.dispatchers.payment.postStartPayment
import ru.auto.ara.core.dispatchers.payment.paymentSuccess
import ru.auto.ara.core.dispatchers.prolongation.putProlongation
import ru.auto.ara.core.dispatchers.sale.Sale.ONE_OFFER_MANY_VAS
import ru.auto.ara.core.dispatchers.sale.getSale
import ru.auto.ara.core.dispatchers.session.getSession
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.getCustomizableUserOffer
import ru.auto.ara.core.dispatchers.user_offers.getForSaleUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getUserOffer
import ru.auto.ara.core.mocks_and_stubbs.trust.TestTrustPaymentController.Companion.TRUST_ERROR
import ru.auto.ara.core.mocks_and_stubbs.trust.TestTrustPaymentController.Companion.TRUST_SUCCESS
import ru.auto.ara.core.robot.payment.checkPaymentStatus
import ru.auto.ara.core.robot.payment.performPayment
import ru.auto.ara.core.robot.sale.performSale
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performVasDetails
import ru.auto.ara.core.rules.IncreasePaymentPopupDismissTimeRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.TrustPaymentControllerFactoryRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.TestObjectsRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.test.payment.PaymentStatusListener

@RunWith(Parameterized::class)
class SaleBuyVasTest(private val vas: SaleBuyVas) {

    private val webServerRule = WebServerRule {
        getSession(banned = false)
        getSale(ONE_OFFER_MANY_VAS)
        getForSaleUserOffers()
        userSetup()
        putProlongation(
            offerId = "1095669442-b3989724",
            vasName = "package_turbo",
            assetPath = "status_unknown_error.json",
            responseCode = 401,
        )
    }

    private val paymentStatusListener: PaymentStatusListener = PaymentStatusListener()
    private var trustPaymentActivityResult = TRUST_SUCCESS

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        TestObjectsRule { config -> config.paymentStatusListener = paymentStatusListener },
        SetupAuthRule(),
        IncreasePaymentPopupDismissTimeRule(),
        SetupTimeRule(date = "01.01.2020", localTime = "12:00", timeZoneId = "Europe/Moscow"),
        TrustPaymentControllerFactoryRule { trustPaymentActivityResult },
    )

    @Test
    fun shouldBuyVasSuccessfullyCorrect() {
        webServerRule.routing {
            paymentSuccess(vas.paymentSystem)
            putProlongation(offerId = OFFER_ID, vasName = vas.id)
        }
        launchActivity<SplashActivity>().use {
            performSale {
                scrollToVasItemPosition(vas.position)
                clickOnVasItem(vas.title)
                scrollToVasItemPosition(vas.position)
                clickOnBuyVasButton(vas.buyButton)
            }

            performPayment {
                clickOnPayButton()
            }.checkResult {
                isListenerCalledWithSuccessState(paymentStatusListener)
            }

            if (vas.isProlongationAllowed) {
                performVasDetails { clickDoneInAutoprolong() }
            } else {
                checkPaymentStatus {
                    isPaymentStatusScreenshotSame("success_popup.png")
                }
            }

            checkMain { isLowTabSelected(USER_OFFERS) }
        }
    }

    @Test
    fun shouldBuyVasFailCorrect() {
        webServerRule.routing {
            paymentStatusError(vas.paymentSystem)
            putProlongation(offerId = OFFER_ID, vasName = vas.id)
        }
        launchActivity<SplashActivity>().use {
            performSale {
                scrollToVasItemPosition(vas.position)
                clickOnVasItem(vas.title)
                scrollToVasItemPosition(vas.position)
                clickOnBuyVasButton(vas.buyButton)
            }
            performPayment {
                clickOnPayButton()
            }.checkResult {
                isListenerCalledWithErrorState(paymentStatusListener)
            }

            checkPaymentStatus {
                isPaymentStatusScreenshotSame("failed_popup.png")
            }

            checkMain { isLowTabSelected(USER_OFFERS) }
        }
    }

    @Test
    fun shouldFailWhenBuyingVasCorrect() {
        setErrorPaymentActivityResult()

        webServerRule.routing {
            paymentProcessError(vas.paymentSystem)
            putProlongation(offerId = OFFER_ID, vasName = vas.id)
        }
        launchActivity<SplashActivity>().use {
            performSale {
                scrollToVasItemPosition(vas.position)
                clickOnVasItem(vas.title)
                scrollToVasItemPosition(vas.position)
                clickOnBuyVasButton(vas.buyButton)
            }
            performPayment {
                clickOnPayButton()
            }
            checkPaymentStatus {
                isPaymentStatusScreenshotSame("failed_custom_text_popup.png")
            }
        }
    }

    @Test
    fun shouldBuyVasOnCollapseItemSuccessfullyCorrect() {
        webServerRule.routing {
            getUserOffer("1095669442-b3989724")
            paymentSuccess(vas.paymentSystem)
            putProlongation(offerId = OFFER_ID, vasName = vas.id)
            getCustomizableUserOffer(OFFER_ID)
        }
        launchActivity<MainActivity>().use {
            performMain {
                openLowTab(USER_OFFERS)
            }
            performSale {
                clickOnSalePreviewItem()
                scrollToVasItemPosition(vas.position)
                clickOnPriceOnCollapseItem(vas.price)
            }
            performPayment {
                clickOnPayButton()
            }.checkResult {
                isListenerCalledWithSuccessState(paymentStatusListener)
            }

            if (vas.isProlongationAllowed) {
                performVasDetails { clickDoneInAutoprolong() }
            } else {
                checkPaymentStatus {
                    isPaymentStatusScreenshotSame("success_popup.png")
                }
            }
            checkMain {
                isLowTabSelected(USER_OFFERS)
            }
        }
    }

    @Test
    fun shouldBuyVasOnCollapseItemFailCorrect() {
        webServerRule.routing {
            paymentStatusError(vas.paymentSystem)
            putProlongation(offerId = OFFER_ID, vasName = vas.id)
        }
        launchActivity<MainActivity>().use {
            performMain {
                openLowTab(USER_OFFERS)
            }
            performSale {
                clickOnSalePreviewItem()
                scrollToVasItemPosition(vas.position)
                clickOnPriceOnCollapseItem(vas.price)
            }
            performPayment {
                clickOnPayButton()
            }.checkResult {
                isListenerCalledWithErrorState(paymentStatusListener)
            }

            checkPaymentStatus {
                isPaymentStatusScreenshotSame("failed_popup.png")
            }

            checkMain { isLowTabSelected(USER_OFFERS) }
        }
    }

    @Test
    fun shouldFailWhenBuyingVasOnCollapseItemCorrect() {
        setErrorPaymentActivityResult()

        webServerRule.routing {
            postInitPayment(vas.paymentSystem)
            postStartPayment(vas.paymentSystem)
            postProcessPaymentErrorWithoutDescription(vas.paymentSystem)
            getPaymentError(vas.paymentSystem)
            putProlongation(offerId = OFFER_ID, vasName = vas.id)
        }
        launchActivity<MainActivity>().use {
            performMain {
                openLowTab(R.string.offers)
            }.checkResult {
                isLowTabSelected(R.string.offers)
            }

            performSale {
                clickOnSalePreviewItem()
                scrollToVasItemPosition(vas.position)
                clickOnPriceOnCollapseItem(vas.price)
            }
            performPayment {
                clickOnPayButton()
            }
            checkPaymentStatus {
                isPaymentStatusScreenshotSame("failed_popup.png")
            }
        }
    }

    private fun setErrorPaymentActivityResult() {
        trustPaymentActivityResult = TRUST_ERROR
    }

    companion object {
        private const val USER_OFFERS = R.string.offers
        private const val OFFER_ID = "1085755394-eb5bb9b7"

        private fun getDataForPaymentSystem(paymentSystem: PaymentSystem) = listOf(
            SaleBuyVas(
                id = "package_vip",
                paymentSystem = paymentSystem,
                title = "VIP",
                price = "975 \u20BD",
                buyButton = "Подключить за 975 \u20BD",
                position = 2,
                isProlongationAllowed = false,
            ),
            SaleBuyVas(
                id = "package_turbo",
                paymentSystem = paymentSystem,
                title = "Турбо-продажа",
                price = "299 \u20BD",
                buyButton = "Подключить за 299 \u20BD",
                position = 4,
                isProlongationAllowed = true,
            ),
        )

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): List<SaleBuyVas> =
            getDataForPaymentSystem(PaymentSystem.TRUST) + getDataForPaymentSystem(PaymentSystem.YANDEXKASSA)
    }

    data class SaleBuyVas(
        val id: String,
        val paymentSystem: PaymentSystem,
        val title: String,
        val price: String,
        val buyButton: String,
        val position: Int,
        val isProlongationAllowed: Boolean,
    )
}
