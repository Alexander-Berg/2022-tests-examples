package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesShareIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.PaymentCompleteScreen
import com.yandex.mobile.realty.core.screen.PaymentScreen
import com.yandex.mobile.realty.core.screen.RenewalUpdateScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOfferCardScreen
import com.yandex.mobile.realty.core.screen.VasDescriptionScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 2020-03-26.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class UserOfferCardTest {

    private val activityTestRule =
        UserOfferCardActivityTestRule(offerId = OFFER_ID, launchActivity = false)

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun turboPurchase() {
        turboPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/turboPurchase/initial",
            paymentKey = "UserOfferSnippetTest/turboPurchase/payment"
        )
    }

    @Test
    fun turboPurchaseWithPromocode() {
        turboPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/turboPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun turboPurchaseWithOverallPromocode() {
        turboPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/turboPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/turboPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithOverallPromocode"
        )
    }

    private fun turboPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitTurbo(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/turboPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/turboPurchase/userOfferTurboPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches(snippetKey)
                .purchaseButton
                .click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/turboPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboPurchase/purchased")
        }
    }

    @Test
    fun turboPurchasePendingActivation() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsNotPurchased.json")

            registerPurchaseInitTurbo("userOffer/turboPurchase/initPurchase.json")
            registerPurchasePayment()
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/turboPurchase/purchaseStatusPendingActivation.json")
                }
            )

            registerUserOfferCard("userOffer/turboPurchase/userOfferTurboPendingActivation.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboPurchase/initial")
                .purchaseButton
                .click()
        }

        onScreen<PaymentScreen> {
            purchaseButton
                .waitUntil { isEnabled() }
                .click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/turboPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/turboPurchase/pendingActivation",
                turboPendingActivationItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun turboPurchasePendingPayment() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsNotPurchased.json")

            registerPurchaseInitTurbo("userOffer/turboPurchase/initPurchase.json")
            registerPurchasePayment()
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/turboPurchase/purchaseStatusPendingPayment.json")
                }
            )

            registerUserOfferCard("userOffer/turboPurchase/userOfferTurboPendingPayment.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboPurchase/initial")
                .purchaseButton
                .click()
        }

        onScreen<PaymentScreen> {
            purchaseButton
                .waitUntil { isEnabled() }
                .click()

            waitUntil {
                timeoutView.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/turboPurchase/paymentTimeout")
            timeoutCloseButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/turboPurchase/pendingPayment",
                turboPendingPaymentItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun turboPurchaseFromDescription() {
        turboPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/turboPurchase/initial",
            descriptionKey = "UserOfferCardTest/turboPurchase/description",
            paymentKey = "UserOfferSnippetTest/turboPurchase/payment"
        )
    }

    @Test
    fun turboPurchaseFromDescriptionWithPromocode() {
        turboPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/turboPurchase/initialWithPromocode",
            descriptionKey = "UserOfferCardTest/turboPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun turboPurchaseFromDescriptionWithOverallPromocode() {
        turboPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/turboPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/turboPurchase/initialWithOverallPromocode",
            descriptionKey = "UserOfferCardTest/turboPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithOverallPromocode"
        )
    }

    private fun turboPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitTurbo(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/turboPurchase/purchaseStatus.json")
                }
            )
            registerUserOfferCard("userOffer/turboPurchase/userOfferTurboPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches(snippetKey)
                .click()
        }

        onScreen<VasDescriptionScreen> {
            waitUntil {
                imageView.isNotAnimating()
            }
            root.isViewStateMatches(descriptionKey)
            purchaseButton.click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/turboPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboPurchase/purchased")
        }
    }

    @Test
    fun turboRenewal() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsPurchased.json")
            registerRenewalActivation("turboSale", "PACKAGE_TURBO")
            registerRenewalDeactivation("turboSale", "PACKAGE_TURBO")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboRenewal/purchased")
                .renewalSwitch
                .click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/turboRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboRenewal/activated")
                .renewalSwitch
                .click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/turboRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/turboRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboRenewal/purchased")
        }
    }

    @Test
    fun turboPurchasedWithPromocode() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsPurchasedWithPromocode.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches("UserOfferCardTest/turboPurchase/purchasedWithPromocode")
        }
    }

    @Test
    fun turboPurchasedWithOverallPromocode() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsPurchasedWithOverallPromocode.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(turboProductItem)
                .isViewStateMatches(
                    "UserOfferCardTest/turboPurchase/purchasedWithOverallPromocode"
                )
        }
    }

    @Test
    fun premiumPurchase() {
        premiumPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/premiumPurchase/initial",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/payment"
        )
    }

    @Test
    fun premiumPurchaseWithPromocode() {
        premiumPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/premiumPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun premiumPurchaseWithOverallPromocode() {
        premiumPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/premiumPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/premiumPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithOverallPromocode"
        )
    }

    private fun premiumPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitPremium(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/premiumPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/premiumPurchase/userOfferPremiumPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(premiumProductItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .isViewStateMatches(snippetKey)
                .purchaseButton
                .click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/premiumPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/premiumPurchase/purchased",
                premiumProductItem,
                2
            )
        }
    }

    @Test
    fun premiumPurchaseFromDescription() {
        premiumPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/premiumPurchase/initial",
            descriptionKey = "UserOfferCardTest/premiumPurchase/description",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/payment"
        )
    }

    @Test
    fun premiumPurchaseFromDescriptionWithPromocode() {
        premiumPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/premiumPurchase/initialWithPromocode",
            descriptionKey = "UserOfferCardTest/premiumPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun premiumPurchaseFromDescriptionWithOverallPromocode() {
        premiumPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/premiumPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/premiumPurchase/initialWithOverallPromocode",
            descriptionKey =
            "UserOfferCardTest/premiumPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithOverallPromocode"
        )
    }

    private fun premiumPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitPremium(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/premiumPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/premiumPurchase/userOfferPremiumPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(premiumProductItem)
                .also { view ->
                    view.isViewStateMatches(snippetKey)
                    listView.scrollByFloatingButtonHeight()
                }
                .click()
        }

        onScreen<VasDescriptionScreen> {
            waitUntil {
                imageView.isNotAnimating()
            }
            root.isViewStateMatches(descriptionKey)
            purchaseButton.click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/premiumPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/premiumPurchase/purchased",
                premiumProductItem,
                2
            )
        }
    }

    @Test
    fun premiumRenewal() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsPurchased.json")
            registerRenewalActivation("premium", "PREMIUM")
            registerRenewalDeactivation("premium", "PREMIUM")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.isItemsStateMatches(
                "UserOfferCardTest/premiumRenewal/purchased",
                premiumProductItem,
                2
            )
            listView.scrollByFloatingButtonHeight()
            premiumRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/premiumRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/premiumRenewal/activated",
                premiumProductItem,
                2
            )
            premiumRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/premiumRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/premiumRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/premiumRenewal/purchased",
                premiumProductItem,
                2
            )
        }
    }

    @Test
    fun raisingPurchase() {
        raisingPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/raisingPurchase/initial",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/payment"
        )
    }

    @Test
    fun raisingPurchaseWithPromocode() {
        raisingPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/raisingPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun raisingPurchaseWithOverallPromocode() {
        raisingPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/raisingPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/raisingPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithOverallPromocode"
        )
    }

    private fun raisingPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitRaising(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/raisingPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/raisingPurchase/userOfferRaisingPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(raisingProductItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .isViewStateMatches(snippetKey)
                .purchaseButton
                .click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/raisingPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/raisingPurchase/purchased",
                raisingProductItem,
                2
            )
        }
    }

    @Test
    fun raisingPurchaseFromDescription() {
        raisingPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/raisingPurchase/initial",
            descriptionKey = "UserOfferCardTest/raisingPurchase/description",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/payment"
        )
    }

    @Test
    fun raisingPurchaseFromDescriptionWithPromocode() {
        raisingPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/raisingPurchase/initialWithPromocode",
            descriptionKey = "UserOfferCardTest/raisingPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun raisingPurchaseFromDescriptionWithOverallPromocode() {
        raisingPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/raisingPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/raisingPurchase/initialWithOverallPromocode",
            descriptionKey =
            "UserOfferCardTest/raisingPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithOverallPromocode"
        )
    }

    private fun raisingPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitRaising(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/raisingPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/raisingPurchase/userOfferRaisingPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(raisingProductItem)
                .also { view ->
                    view.isViewStateMatches(snippetKey)
                    listView.scrollByFloatingButtonHeight()
                }
                .click()
        }

        onScreen<VasDescriptionScreen> {
            waitUntil {
                imageView.isNotAnimating()
            }
            root.isViewStateMatches(descriptionKey)
            purchaseButton.click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/raisingPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/raisingPurchase/purchased",
                raisingProductItem,
                2
            )
        }
    }

    @Test
    fun raisingRenewal() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsPurchased.json")
            registerRenewalActivation("raising", "RAISING")
            registerRenewalDeactivation("raising", "RAISING")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.isItemsStateMatches(
                "UserOfferCardTest/raisingRenewal/purchased",
                raisingProductItem,
                2
            )
            listView.scrollByFloatingButtonHeight()
            raisingRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/raisingRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/raisingRenewal/activated",
                raisingProductItem,
                2
            )
            raisingRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/raisingRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/raisingRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/raisingRenewal/purchased",
                raisingProductItem,
                2
            )
        }
    }

    @Test
    fun promotionPurchase() {
        promotionPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/promotionPurchase/initial",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/payment"
        )
    }

    @Test
    fun promotionPurchaseWithPromocode() {
        promotionPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/promotionPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun promotionPurchaseWithOverallPromocode() {
        promotionPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/promotionPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/promotionPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithOverallPromocode"
        )
    }

    private fun promotionPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitPromotion(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/promotionPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/promotionPurchase/userOfferPromotionPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(promotionProductItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .isViewStateMatches(snippetKey)
                .purchaseButton
                .click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/promotionPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/promotionPurchase/purchased",
                promotionProductItem,
                2
            )
        }
    }

    @Test
    fun promotionPurchaseFromDescription() {
        promotionPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchase.json",
            snippetKey = "UserOfferCardTest/promotionPurchase/initial",
            descriptionKey = "UserOfferCardTest/promotionPurchase/description",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/payment"
        )
    }

    @Test
    fun promotionPurchaseFromDescriptionWithPromocode() {
        promotionPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferCardTest/promotionPurchase/initialWithPromocode",
            descriptionKey = "UserOfferCardTest/promotionPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun promotionPurchaseFromDescriptionWithOverallPromocode() {
        promotionPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            cardResponse = "userOffer/userOfferProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/promotionPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferCardTest/promotionPurchase/initialWithOverallPromocode",
            descriptionKey =
            "UserOfferCardTest/promotionPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithOverallPromocode"
        )
    }

    private fun promotionPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        cardResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferCard(cardResponse)

            registerPurchaseInitPromotion(purchaseInitResponse)
            apply(registerPurchasePayment)
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/promotionPurchase/purchaseStatus.json")
                }
            )

            registerUserOfferCard("userOffer/promotionPurchase/userOfferPromotionPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.scrollTo(promotionProductItem)
                .also { view ->
                    view.isViewStateMatches(snippetKey)
                    listView.scrollByFloatingButtonHeight()
                }
                .click()
        }

        onScreen<VasDescriptionScreen> {
            waitUntil {
                imageView.isNotAnimating()
            }
            root.isViewStateMatches(descriptionKey)
            purchaseButton.click()
        }

        onScreen<PaymentScreen> {
            purchaseButton.waitUntil { isEnabled() }
            root.isViewStateMatches(paymentKey)
            purchaseButton.click()
        }

        onScreen<PaymentCompleteScreen> {
            waitUntil {
                bottomSheet.isCompletelyDisplayed()
            }
            root.isViewStateMatches("UserOfferCardTest/promotionPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/promotionPurchase/purchased",
                promotionProductItem,
                2
            )
        }
    }

    @Test
    fun promotionRenewal() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferProductsPurchased.json")
            registerRenewalActivation("promotion", "PROMOTION")
            registerRenewalDeactivation("promotion", "PROMOTION")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            appBar.collapse()

            listView.isItemsStateMatches(
                "UserOfferCardTest/promotionRenewal/purchased",
                promotionProductItem,
                2
            )
            listView.scrollByFloatingButtonHeight()
            promotionRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/promotionRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/promotionRenewal/activated",
                promotionProductItem,
                2
            )
            promotionRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/promotionRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferCardTest/promotionRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOfferCardScreen> {
            listView.isItemsStateMatches(
                "UserOfferCardTest/promotionRenewal/purchased",
                promotionProductItem,
                2
            )
        }
    }

    @Test
    fun publishedFreeOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferPublishedFree.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/publishedFreeOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/publishedFreeOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun publishedUnpaidOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferPublishedUnpaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/publishedUnpaidOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/publishedUnpaidOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun publishedPaymentInProcessOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferPublishedPaymentInProcess.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/publishedPaymentInProcessOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/publishedPaymentInProcessOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun publishedPaidOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferPublishedPaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/publishedPaidOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/publishedPaidOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun moderationFreeOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferModerationFree.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isNotClickable()

            root.isViewStateMatches("UserOfferCardTest/moderationFreeOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/moderationFreeOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun moderationUnpaidOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferModerationUnpaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isNotClickable()

            root.isViewStateMatches("UserOfferCardTest/moderationUnpaidOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/moderationUnpaidOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun moderationPaymentInProcessOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferModerationPaymentInProcess.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isNotClickable()

            root.isViewStateMatches(
                "UserOfferCardTest/moderationPaymentInProcessOffer/initialState"
            )

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/moderationPaymentInProcessOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun moderationPaidOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferModerationPaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isNotClickable()

            root.isViewStateMatches("UserOfferCardTest/moderationPaidOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/moderationPaidOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun unpublishedFreeOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferUnpublishedFree.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/unpublishedFreeOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/unpublishedFreeOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun unpublishedUnpaidOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferUnpublishedUnpaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/unpublishedUnpaidOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/unpublishedUnpaidOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun unpublishedPaymentInProcessOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferUnpublishedPaymentInProcess.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches(
                "UserOfferCardTest/unpublishedPaymentInProcessOffer/initialState"
            )

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/unpublishedPaymentInProcessOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun unpublishedPaidOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferUnpublishedPaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/unpublishedPaidOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/unpublishedPaidOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun bannedRecoverableOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferBannedRecoverable.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isClickable()

            root.isViewStateMatches("UserOfferCardTest/bannedRecoverableOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/bannedRecoverableOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun bannedNonRecoverableOffer() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferBannedNonRecoverable.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
            priceView.isNotClickable()

            root.isViewStateMatches("UserOfferCardTest/bannedNonRecoverableOffer/initialState")

            appBar.collapse()

            listView.isContentStateMatches(
                key = "UserOfferCardTest/bannedNonRecoverableOffer/content",
                scrollExtra = -floatingButtonHeight
            )
        }
    }

    @Test
    fun shouldShowSupportButtonWhenBannedAndFromFeed() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferBannedFromFeed.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }

            appBar.collapse()

            listView.contains(supportButtonItem)
        }
    }

    @Test
    fun openSupportChat() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferBannedRecoverable.json")
            registerTechSupportChat()
        }

        activityTestRule.launchActivity()

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }

            appBar.collapse()

            listView.scrollTo(supportButtonItem).click()
        }

        performOnChatMessagesScreen {
            waitUntil { isSupportChatTitleShown() }
            pressBack()
        }

        onScreen<UserOfferCardScreen> {
            waitUntil {
                priceView.isCompletelyDisplayed()
            }
        }
    }

    @Test
    fun shouldShareUserOffer() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferWithShareUrl(SHARE_URL)
        }

        activityTestRule.launchActivity()
        registerResultOkIntent(matchesShareIntent(SHARE_URL), null)

        onScreen<UserOfferCardScreen> {
            shareButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            intended(matchesShareIntent(SHARE_URL))
        }
    }

    private fun DispatcherRegistry.registerUserOfferCard(responseFileName: String) {
        registerUserProfile()
        registerUserProfile()
        registerUserOffer(responseFileName)
    }

    private fun DispatcherRegistry.registerUserOffer(responseFileName: String) {
        register(
            request {
                path("2.0/user/me/offers/$OFFER_ID/card")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerUserOfferWithShareUrl(shareUrl: String) {
        register(
            request {
                path("2.0/user/me/offers/$OFFER_ID/card")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "content" to jsonObject {
                            "id" to OFFER_ID
                            "publishingInfo" to jsonObject {
                                "creationDate" to "2020-02-12T12:10:00.000Z"
                                "status" to "PUBLISHED"
                            }
                            "placement" to jsonObject {
                                "free" to jsonObject { }
                            }
                            "sell" to jsonObject {
                                "price" to jsonObject {
                                    "value" to 4_500_000
                                    "currency" to "RUB"
                                    "priceType" to "PER_OFFER"
                                    "pricingPeriod" to "WHOLE_LIFE"
                                }
                            }
                            "apartment" to jsonObject { }
                            "shareUrl" to shareUrl
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userOwner.json")
            }
        )
    }

    private fun DispatcherRegistry.registerTechSupportChat() {
        register(
            request {
                path("2.0/chat/room/tech-support")
            },
            response {
                assetBody("techSupportChatCommon.json")
            }
        )
    }

    private fun DispatcherRegistry.registerPurchaseInitTurbo(
        responseFileName: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/init")
                body(
                    "{\"item\":[{" +
                        "\"productType\":\"PRODUCT_TYPE_PACKAGE_TURBO\"," +
                        "\"target\":{\"offer\":{\"offerId\":\"$OFFER_ID\"}}}]," +
                        "\"renewalByDefault\":true" +
                        "}"
                )
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerPurchaseInitPremium(
        responseFileName: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/init")
                body(
                    "{\"item\":[{" +
                        "\"productType\":\"PRODUCT_TYPE_PREMIUM\"," +
                        "\"target\":{\"offer\":{\"offerId\":\"$OFFER_ID\"}}}]," +
                        "\"renewalByDefault\":true" +
                        "}"
                )
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerPurchaseInitRaising(
        responseFileName: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/init")
                body(
                    "{\"item\":[{" +
                        "\"productType\":\"PRODUCT_TYPE_RAISING\"," +
                        "\"target\":{\"offer\":{\"offerId\":\"$OFFER_ID\"}}}]," +
                        "\"renewalByDefault\":true" +
                        "}"
                )
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerPurchaseInitPromotion(
        responseFileName: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/init")
                body(
                    "{\"item\":[{" +
                        "\"productType\":\"PRODUCT_TYPE_PROMOTION\"," +
                        "\"target\":{\"offer\":{\"offerId\":\"$OFFER_ID\"}}}]," +
                        "\"renewalByDefault\":true" +
                        "}"
                )
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerPurchasePayment() {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/payment")
                body(
                    "{\"externalSystem\":{" +
                        "\"context\":{" +
                        "\"bankCard\":{}," +
                        "\"email\":\"some@domain.com\"" +
                        "}," +
                        "\"method\":{" +
                        "\"id\":\"bank_card\"," +
                        "\"name\":\"Банковская карта\"," +
                        "\"needEmail\":true," +
                        "\"preferred\":true," +
                        "\"properties\":{" +
                        "\"card\":{" +
                        "\"brand\":\"MASTERCARD\"," +
                        "\"cddPanMask\":\"555555|4444\"," +
                        "\"expireMonth\":\"2\"," +
                        "\"expireYear\":\"2022\"" +
                        "}}," +
                        "\"psId\":\"YANDEXKASSA_V3\"" +
                        "}}," +
                        "\"purchaseId\":\"1bacbe9f572343db8af961c069664005\"" +
                        "}"
                )
            },
            response {
                setBody(
                    "{\"response\": {" +
                        "\"noConfirmation\": {}," +
                        "\"paymentRequestId\": \"9599752874211521490465fd2ca0ff37\"" +
                        "}}"
                )
            }
        )
    }

    private fun DispatcherRegistry.registerPromocodeOnlyPurchasePayment() {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/payment")
                body("{\"promocodeOnlyPayment\":{}}")
                body(
                    "{\"promocodeOnlyPayment\":{}," +
                        "\"purchaseId\":\"1bacbe9f572343db8af961c069664005\"" +
                        "}"
                )
            },
            response {
                setBody(
                    "{\"response\": {" +
                        "\"noConfirmation\": {}," +
                        "\"paymentRequestId\": \"9599752874211521490465fd2ca0ff37\"" +
                        "}}"
                )
            }
        )
    }

    private fun DispatcherRegistry.registerRenewalActivation(
        pathSuffix: String,
        type: String
    ) {
        register(
            request {
                path("2.0/user/me/renewals/offers/$OFFER_ID/$pathSuffix")
                body("{ \"turnedOn\": true }")
            },
            response {
                setBody(
                    "{ \"response\": " +
                        "{ \"renewals\": [ " +
                        "{ \"productType\": \"$type\"," +
                        "\"renewal\": { \"status\": \"ACTIVE\" } } ] } }"
                )
            }
        )
    }

    private fun DispatcherRegistry.registerRenewalDeactivation(
        pathSuffix: String,
        type: String
    ) {
        register(
            request {
                path("2.0/user/me/renewals/offers/$OFFER_ID/$pathSuffix")
                body("{ \"turnedOn\": false }")
            },
            response {
                setBody(
                    "{ \"response\": " +
                        "{ \"renewals\": [ " +
                        "{ \"productType\": \"$type\"," +
                        "\"renewal\": { \"status\": \"INACTIVE\" } } ] } }"
                )
            }
        )
    }

    private companion object {

        private const val OFFER_ID = "1"
        private const val SHARE_URL = "https://realty.yandex.ru/offer/6124286403445165313"
    }
}
