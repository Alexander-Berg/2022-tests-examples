package ru.auto.ara.test.user_offers

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.getPaymentClosed
import ru.auto.ara.core.dispatchers.payment.postInitPayment
import ru.auto.ara.core.dispatchers.payment.postProcessPayment
import ru.auto.ara.core.dispatchers.payment.postStartPayment
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.DeleteProductProlongDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PutProductProlongDispatcher
import ru.auto.ara.core.dispatchers.user_offers.getActiveUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getUserOffer
import ru.auto.ara.core.robot.payment.performPayment
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.checkVasDetails
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.robot.useroffers.performVasDetails
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.TrustPaymentControllerFactoryRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.data.util.RUB_UNICODE
import ru.auto.data.util.VAS_ALIAS_ALL_SALE_COLOR
import ru.auto.data.util.VAS_ALIAS_ALL_SALE_SPECIAL
import ru.auto.data.util.VAS_ALIAS_ALL_SALE_TOPLIST

@RunWith(Parameterized::class)
class PurchaseVasAutoProlongationTest(private val paymentSystem: PaymentSystem) {

    private val webServerRule = WebServerRule {
        postInitPayment(paymentSystem)
        postStartPayment(paymentSystem)
        postProcessPayment(paymentSystem)
        getPaymentClosed(paymentSystem)
        getActiveUserOffers()
        getUserOffer(OFFER_ID)
        userSetup()
    }

    val activityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        TrustPaymentControllerFactoryRule(),
    )

    @Test
    fun shouldSeeCorrectControlsOnProlongScreen() {
        webServerRule.routing {
            delegateDispatcher(PutProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_TOPLIST))
                .watch { checkRequestWasNotCalled() }

            delegateDispatcher(DeleteProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_TOPLIST))
                .watch { checkRequestWasNotCalled() }
        }

        buyVasFromUserOffersList(VAS_ALIAS_ALL_SALE_TOPLIST)
        checkVasDetails {
            isAutoprolongationBlockTitleDisplayed("Поднятие в ТОП активировано")
            isAutoprolongationBlockSubTitleDisplayed("2 дня до окончания")
            isAutoprolongationBlockImageDisplayed()
            isAutoprolongationSwitchTitleDisplayed()
            isAutoprolongationSwitchDisplayed()
            isAutorpolongationDescriptionDisplayed(
                "Поднятие в ТОП будет автоматически продлеваться за 497 $RUB_UNICODE каждые 3 дня"
            )
            isAutoprolongationDoneButtonDisplayed()
        }
    }

    @Test
    fun shouldSeeNotCheckedSwitchForNotForcedProlongation() {
        webServerRule.routing {
            delegateDispatcher(PutProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_SPECIAL))
                .watch { checkRequestWasNotCalled() }

            delegateDispatcher(DeleteProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_SPECIAL))
                .watch { checkRequestWasNotCalled() }
        }

        buyVasFromUserOffersList(VAS_ALIAS_ALL_SALE_SPECIAL)
        checkVasDetails {
            isAutoprolongationSwitchChecked(false)
        }
    }

    @Test
    fun shouldNotEnableAutoProlongationWhenUnCheckItDuringPurchase() {
        webServerRule.routing {
            delegateDispatcher(PutProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_TOPLIST))
                .watch { checkRequestWasNotCalled() }

            delegateDispatcher(DeleteProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_TOPLIST))
                .watch { checkRequestWasNotCalled() }
        }

        buyVasFromUserOffersList(VAS_ALIAS_ALL_SALE_TOPLIST)
        checkVasDetails { isAutoprolongationBlockDisplayed() }
        performVasDetails {
            clickAutorpolongSwitch()
        }.checkResult {
            isAutoprolongationSwitchChecked(checked = false)
        }
        performVasDetails { clickDoneInAutoprolong() }
        performOffers { waitForUserOffersCompletelyDisplayed() }
    }

    @Test
    fun shouldEnableAutoProlongationWhenCheckItDuringPurchase() {
        webServerRule.routing {
            delegateDispatcher(PutProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_TOPLIST))
                .watch { checkRequestWasCalled() }

            delegateDispatcher(DeleteProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_TOPLIST))
                .watch { checkRequestWasNotCalled() }
        }

        buyVasFromUserOffersList(VAS_ALIAS_ALL_SALE_TOPLIST)
        checkVasDetails {
            isAutoprolongationBlockDisplayed()
            isAutoprolongationSwitchChecked()
        }
        performVasDetails {
            clickDoneInAutoprolong()
        }
        performOffers { waitForUserOffersCompletelyDisplayed() }
    }

    @Test
    fun shouldNotSeeProlongScreenForNotProlongableVas() {
        webServerRule.routing {
            delegateDispatcher(PutProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_COLOR))
                .watch { checkRequestWasNotCalled() }

            delegateDispatcher(DeleteProductProlongDispatcher("cars", OFFER_ID, VAS_ALIAS_ALL_SALE_COLOR))
                .watch { checkRequestWasNotCalled() }
        }

        buyVasFromUserOffersList(VAS_ALIAS_ALL_SALE_COLOR)
        checkVasDetails { isAutoprolongationBlockNotExists() }
        performOffers { waitForOfferSnippets(1) }
    }

    private fun buyVasFromUserOffersList(vasAlias: String) {
        activityRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForUserOffersCompletelyDisplayed()
            collapseToolbar()
            scrollToVasBlock(vasAlias)
            clickSimpleVasBuyButton(vasAlias)
        }
        performPayment {
            clickOnPayButton()
        }
    }

    companion object {
        private const val OFFER_ID = "1092688300-a5a5cc01"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<PaymentSystem> = PaymentSystem.values().toList()
    }
}
