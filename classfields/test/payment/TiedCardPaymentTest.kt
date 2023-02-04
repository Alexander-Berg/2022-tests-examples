package ru.auto.ara.test.payment

import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.billing.vas.VasEventSource
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.paymentProcessError
import ru.auto.ara.core.dispatchers.payment.paymentStatusError
import ru.auto.ara.core.dispatchers.payment.paymentSuccess
import ru.auto.ara.core.dispatchers.payment.postInitPaymentNoPayableProductFound
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mocks_and_stubbs.trust.TestTrustPaymentController
import ru.auto.ara.core.mocks_and_stubbs.trust.TestTrustPaymentController.Companion.TRUST_ERROR
import ru.auto.ara.core.robot.payment.checkPayment
import ru.auto.ara.core.robot.payment.performPayment
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTestEnvironmentRule
import ru.auto.ara.core.rules.TrustPaymentControllerFactoryRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.TestObjectsRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.router.PopupScreenBuilder
import ru.auto.ara.ui.activity.MultiSelectActivity
import ru.auto.ara.ui.fragment.payment.PaymentMethodsFragment
import ru.auto.ara.ui.fragment.picker.DialogListener
import ru.auto.ara.util.statistics.event.WalletAddMoneyStatEvent.Context.ADVERTS_LIST
import ru.auto.feature.payment.api.IPaymentStatusListenerProvider
import ru.auto.feature.payment.api.PaymentMethodsContext
import ru.auto.feature.payment.context.PaymentStatusContext
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.payment.PaymentProduct
import ru.auto.data.model.payment.Section
import ru.auto.data.util.TEST_IS_PAYMENT_METHODS_GO_BACK_ALLOWED
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class TiedCardPaymentTest(private val paymentSystem: PaymentSystem) {

    private val webServerRule = WebServerRule {
        userSetup()
    }

    private val activityTestRule = lazyActivityScenarioRule<MultiSelectActivity>()

    private var trustPaymentActivityResult = TestTrustPaymentController.TRUST_SUCCESS

    private val paymentStatusListener: PaymentStatusListener = PaymentStatusListener()
    private val paymentMethodContext = PaymentMethodsContext(
        type = PaymentMethodsContext.Type.Products(
            products = listOf(
                PaymentProduct(
                    id = "turbo",
                    name = "Турбо-продажа",
                    days = 33,
                    priceRur = 999,
                    count = 1,
                    isProlongationAllowed = true,
                    aliases = emptyList()
                )
            ),
            category = VehicleCategory.CARS,
            section = Section.USED,
            offerId = "1088833358-cbf10ccb"
        ),
        from = VasEventSource.LK,
        metrikaContext = ADVERTS_LIST,
        checkStatusTimeoutSec = 1,
        listenerProvider = PaymentStatusListenerProvider(paymentStatusListener)
    )

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        TestObjectsRule { config -> config.paymentStatusListener = paymentStatusListener },
        activityTestRule,
        SetupAuthRule(),
        SetupTestEnvironmentRule { test -> TEST_IS_PAYMENT_METHODS_GO_BACK_ALLOWED = !test },
        TrustPaymentControllerFactoryRule { trustPaymentActivityResult },
    )

    @Test
    fun check_successful_payment_status_after_purchasing_vas() {
        webServerRule.routing { paymentSuccess(paymentSystem) }
        launchPaymentScreen()
        performPayment {
            clickOnPayButton()
        }.checkResult {
            isListenerCalledWithSuccessState(paymentStatusListener)
        }
    }

    @Test
    fun check_successful_payment_status_after_no_payable_product_found_error() {
        webServerRule.routing { postInitPaymentNoPayableProductFound(paymentSystem) }
        launchPaymentScreen()
        performPayment { waitPaymentRootViewIsDisplayed() }
        checkPayment {
            isListenerCalledWithSuccessState(paymentStatusListener)
        }
    }

    @Test
    fun check_error_waiting_for_status_after_purchasing_vas() {
        webServerRule.routing { paymentStatusError(paymentSystem) }
        launchPaymentScreen()
        performPayment {
            clickOnPayButton()
        }.checkResult {
            isListenerCalledWithErrorState(paymentStatusListener)
        }
    }

    @Test
    fun check_error_when_purchasing_vas() {
        setErrorPaymentActivityResult()
        webServerRule.routing { paymentProcessError(paymentSystem) }
        launchPaymentScreen()
        performPayment {
            waitSomething(1, TimeUnit.SECONDS) // wait button initialized
            clickOnPayButton()
        }.checkResult {
            isErrorPopupShownWithMessage("Неизвестная ошибка")
        }
    }

    private fun launchPaymentScreen() {
        val screen = PaymentMethodsFragment.screen(context = paymentMethodContext)
        activityTestRule.launchFragment<PaymentMethodsFragment>(
            args = screen.args,
            isDialog = (screen as? PopupScreenBuilder.PopupScreen)?.isAsDialog == true
        )
    }

    private fun setErrorPaymentActivityResult() {
        trustPaymentActivityResult = TRUST_ERROR
    }

    @Parcelize
    class PaymentStatusListenerProvider(private val listener: PaymentStatusListener) : IPaymentStatusListenerProvider {
        override fun getListener(): DialogListener<PaymentStatusContext> = object : DialogListener<PaymentStatusContext> {
            override fun onChosen(context: PaymentStatusContext?) {
                context ?: return
                listener.onChosen(context)
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<PaymentSystem> = PaymentSystem.values().toList()
    }

}
