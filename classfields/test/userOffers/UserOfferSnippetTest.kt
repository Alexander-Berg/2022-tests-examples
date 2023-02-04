package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.PaymentCompleteScreen
import com.yandex.mobile.realty.core.screen.PaymentScreen
import com.yandex.mobile.realty.core.screen.RenewalUpdateScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.screen.VasDescriptionScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 2020-03-27.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class UserOfferSnippetTest : BaseTest() {

    private val activityTestRule = UserOffersActivityTestRule(launchActivity = false)
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
    fun checkWarnings() {
        configureWebServer {
            registerUserOfferList("userOffers/userOfferSnippetAllWarnings.json")
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("allWarnings"))
        }
    }

    @Test
    fun turboPurchase() {
        turboPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/turboPurchase/initial",
            paymentKey = "UserOfferSnippetTest/turboPurchase/payment"
        )
    }

    @Test
    fun turboPurchaseWithPromocode() {
        turboPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/turboPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun turboPurchaseWithOverallPromocode() {
        turboPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/turboPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/turboPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithOverallPromocode"
        )
    }

    private fun turboPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

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

            registerUserOffersById("userOffer/turboPurchase/userOffersTurboPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
            listView.scrollTo(turboProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/turboPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboPurchase/purchased")
        }
    }

    @Test
    fun turboPurchasePendingActivation() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsNotPurchased.json")

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

            registerUserOffersById("userOffer/turboPurchase/userOffersTurboPendingActivation.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboPurchase/initial")
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
            root.isViewStateMatches("UserOfferSnippetTest/turboPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/turboPurchase/pendingActivation",
                turboPendingActivationItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun turboPurchasePendingPayment() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsNotPurchased.json")

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

            registerUserOffersById("userOffer/turboPurchase/userOffersTurboPendingPayment.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboPurchase/initial")
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
            root.isViewStateMatches("UserOfferSnippetTest/turboPurchase/paymentTimeout")
            timeoutCloseButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/turboPurchase/pendingPayment",
                turboPendingPaymentItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun turboPurchaseFromDescription() {
        turboPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/turboPurchase/initial",
            descriptionKey = "UserOfferSnippetTest/turboPurchase/description",
            paymentKey = "UserOfferSnippetTest/turboPurchase/payment"
        )
    }

    @Test
    fun turboPurchaseFromDescriptionWithPromocode() {
        turboPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/turboPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/turboPurchase/initialWithPromocode",
            descriptionKey = "UserOfferSnippetTest/turboPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun turboPurchaseFromDescriptionWithOverallPromocode() {
        turboPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/turboPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/turboPurchase/initialWithOverallPromocode",
            descriptionKey =
            "UserOfferSnippetTest/turboPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/turboPurchase/paymentWithOverallPromocode"
        )
    }

    private fun turboPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

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
            registerUserOffersById("userOffer/turboPurchase/userOffersTurboPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(turboProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/turboPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboPurchase/purchased")
        }
    }

    @Test
    fun turboRenewal() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsPurchased.json")
            registerRenewalActivation("turboSale", "PACKAGE_TURBO")
            registerRenewalDeactivation("turboSale", "PACKAGE_TURBO")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboRenewal/purchased")
                .renewalSwitch
                .click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/turboRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboRenewal/activated")
                .renewalSwitch
                .click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/turboRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/turboRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboRenewal/purchased")
        }
    }

    @Test
    fun turboPurchasedWithPromocode() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsPurchasedWithPromocode.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches("UserOfferSnippetTest/turboPurchase/purchasedWithPromocode")
        }
    }

    @Test
    fun turboPurchasedWithOverallPromocode() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsPurchasedWithOverallPromocode.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
            listView.scrollTo(turboProductItem(OFFER_ID))
                .isViewStateMatches(
                    "UserOfferSnippetTest/turboPurchase/purchasedWithOverallPromocode"
                )
        }
    }

    @Test
    fun premiumPurchase() {
        premiumPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/premiumPurchase/initial",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/payment"
        )
    }

    @Test
    fun premiumPurchaseWithPromocode() {
        premiumPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/premiumPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun premiumPurchaseWithOverallPromocode() {
        premiumPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/premiumPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/premiumPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithOverallPromocode"
        )
    }

    private fun premiumPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

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

            registerUserOffersById("userOffer/premiumPurchase/userOffersPremiumPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(premiumProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/premiumPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/premiumPurchase/purchased",
                premiumProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun premiumPurchaseFromDescription() {
        premiumPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/premiumPurchase/initial",
            descriptionKey = "UserOfferSnippetTest/premiumPurchase/description",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/payment"
        )
    }

    @Test
    fun premiumPurchaseFromDescriptionWithPromocode() {
        premiumPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/premiumPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/premiumPurchase/initialWithPromocode",
            descriptionKey = "UserOfferSnippetTest/premiumPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun premiumPurchaseFromDescriptionWithOverallPromocode() {
        premiumPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/premiumPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/premiumPurchase/initialWithOverallPromocode",
            descriptionKey =
            "UserOfferSnippetTest/premiumPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/premiumPurchase/paymentWithOverallPromocode"
        )
    }

    private fun premiumPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

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

            registerUserOffersById("userOffer/premiumPurchase/userOffersPremiumPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(premiumProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/premiumPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/premiumPurchase/purchased",
                premiumProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun premiumRenewal() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsPurchased.json")
            registerRenewalActivation("premium", "PREMIUM")
            registerRenewalDeactivation("premium", "PREMIUM")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.isItemsStateMatches(
                "UserOfferSnippetTest/premiumRenewal/purchased",
                premiumProductItem(OFFER_ID),
                2
            )
            premiumRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/premiumRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/premiumRenewal/activated",
                premiumProductItem(OFFER_ID),
                2
            )
            premiumRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/premiumRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/premiumRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/premiumRenewal/purchased",
                premiumProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun raisingPurchase() {
        raisingPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/raisingPurchase/initial",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/payment"
        )
    }

    @Test
    fun raisingPurchaseWithPromocode() {
        raisingPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/raisingPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun raisingPurchaseWithOverallPromocode() {
        raisingPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/raisingPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/raisingPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithOverallPromocode"
        )
    }

    private fun raisingPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

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

            registerUserOffersById("userOffer/raisingPurchase/userOffersRaisingPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(raisingProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/raisingPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/raisingPurchase/purchased",
                raisingProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun raisingPurchaseFromDescription() {
        raisingPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/raisingPurchase/initial",
            descriptionKey = "UserOfferSnippetTest/raisingPurchase/description",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/payment"
        )
    }

    @Test
    fun raisingPurchaseFromDescriptionWithPromocode() {
        raisingPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/raisingPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/raisingPurchase/initialWithPromocode",
            descriptionKey = "UserOfferSnippetTest/raisingPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun raisingPurchaseFromDescriptionWithOverallPromocode() {
        raisingPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/raisingPurchase/initialWithOverallPromocode",
            purchaseInitResponse =
            "userOffer/raisingPurchase/initPurchaseWithOverallPromocode.json",
            descriptionKey =
            "UserOfferSnippetTest/raisingPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/raisingPurchase/paymentWithOverallPromocode"
        )
    }

    private fun raisingPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

            registerPurchaseInitRaising(purchaseInitResponse)
            registerPurchasePayment()
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/raisingPurchase/purchaseStatus.json")
                }
            )

            registerUserOffersById("userOffer/raisingPurchase/userOffersRaisingPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(raisingProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/raisingPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/raisingPurchase/purchased",
                raisingProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun raisingRenewal() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsPurchased.json")
            registerRenewalActivation("raising", "RAISING")
            registerRenewalDeactivation("raising", "RAISING")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.isItemsStateMatches(
                "UserOfferSnippetTest/raisingRenewal/purchased",
                raisingProductItem(OFFER_ID),
                2
            )
            raisingRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/raisingRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/raisingRenewal/activated",
                raisingProductItem(OFFER_ID),
                2
            )
            raisingRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/raisingRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/raisingRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/raisingRenewal/purchased",
                raisingProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun promotionPurchase() {
        promotionPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/promotionPurchase/initial",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/payment"
        )
    }

    @Test
    fun promotionPurchaseWithPromocode() {
        promotionPurchase(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/promotionPurchase/initialWithPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun promotionPurchaseWithOverallPromocode() {
        promotionPurchase(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/promotionPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/promotionPurchase/initialWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithOverallPromocode"
        )
    }

    private fun promotionPurchase(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

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

            registerUserOffersById("userOffer/promotionPurchase/userOffersPromotionPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(promotionProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/promotionPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/promotionPurchase/purchased",
                promotionProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun promotionPurchaseFromDescription() {
        promotionPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchased.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchase.json",
            snippetKey = "UserOfferSnippetTest/promotionPurchase/initial",
            descriptionKey = "UserOfferSnippetTest/promotionPurchase/description",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/payment"
        )
    }

    @Test
    fun promotionPurchaseFromDescriptionWithPromocode() {
        promotionPurchaseFromDescription(
            registerPurchasePayment = { registerPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithPromocode.json",
            purchaseInitResponse = "userOffer/promotionPurchase/initPurchaseWithPromocode.json",
            snippetKey = "UserOfferSnippetTest/promotionPurchase/initialWithPromocode",
            descriptionKey = "UserOfferSnippetTest/promotionPurchase/descriptionWithPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithPromocode"
        )
    }

    @Test
    fun promotionPurchaseFromDescriptionWithOverallPromocode() {
        promotionPurchaseFromDescription(
            registerPurchasePayment = { registerPromocodeOnlyPurchasePayment() },
            listResponse = "userOffer/userOffersProductsNotPurchasedWithOverallPromocode.json",
            purchaseInitResponse =
            "userOffer/promotionPurchase/initPurchaseWithOverallPromocode.json",
            snippetKey = "UserOfferSnippetTest/promotionPurchase/initialWithOverallPromocode",
            descriptionKey =
            "UserOfferSnippetTest/promotionPurchase/descriptionWithOverallPromocode",
            paymentKey = "UserOfferSnippetTest/promotionPurchase/paymentWithOverallPromocode"
        )
    }

    private fun promotionPurchaseFromDescription(
        registerPurchasePayment: DispatcherRegistry.() -> Unit,
        listResponse: String,
        purchaseInitResponse: String,
        snippetKey: String,
        descriptionKey: String,
        paymentKey: String
    ) {
        configureWebServer {
            registerUserOfferList(listResponse)

            registerPurchaseInitPromotion(purchaseInitResponse)
            registerPurchasePayment()
            register(
                request {
                    path("2.0/products/user/me/purchase/1bacbe9f572343db8af961c069664005")
                },
                response {
                    assetBody("userOffer/promotionPurchase/purchaseStatus.json")
                }
            )

            registerUserOffersById("userOffer/promotionPurchase/userOffersPromotionPurchased.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(promotionProductItem(OFFER_ID))
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
            root.isViewStateMatches("UserOfferSnippetTest/promotionPurchase/success")
            proceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/promotionPurchase/purchased",
                promotionProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun promotionRenewal() {
        configureWebServer {
            registerUserOfferList("userOffer/userOffersProductsPurchased.json")
            registerRenewalActivation("promotion", "PROMOTION")
            registerRenewalDeactivation("promotion", "PROMOTION")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.isItemsStateMatches(
                "UserOfferSnippetTest/promotionRenewal/purchased",
                promotionProductItem(OFFER_ID),
                2
            )
            promotionRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                activatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/promotionRenewal/activation")
            activatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/promotionRenewal/activated",
                promotionProductItem(OFFER_ID),
                2
            )
            promotionRenewalSwitch(OFFER_ID).click()
        }

        onScreen<RenewalUpdateScreen> {
            waitUntil {
                confirmationView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/promotionRenewal/confirmation")
            confirmButton.click()

            waitUntil {
                deactivatedView.isCompletelyDisplayed()
            }

            root.isViewStateMatches("UserOfferSnippetTest/promotionRenewal/deactivation")
            deactivatedProceedButton.click()
        }

        onScreen<UserOffersScreen> {
            listView.isItemsStateMatches(
                "UserOfferSnippetTest/promotionRenewal/purchased",
                promotionProductItem(OFFER_ID),
                2
            )
        }
    }

    @Test
    fun publishedFreeOffer() {
        val id = "1"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersPublishedFree.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedFreeOffer/content"
            )
        }
    }

    @Test
    fun publishedUnpaidOffer() {
        val id = "2"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersPublishedUnpaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedUnpaidOffer/content"
            )
        }
    }

    @Test
    fun publishedPaymentInProcessOffer() {
        val id = "3"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersPublishedPaymentInProcess.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedPaymentInProcessOffer/content"
            )
        }
    }

    @Test
    fun publishedPaidOffer() {
        val id = "4"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersPublishedPaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedPaidOffer/content"
            )
        }
    }

    @Test
    fun moderationFreeOffer() {
        val id = "5"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersModerationFree.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isNotClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/moderationFreeOffer/content"
            )
        }
    }

    @Test
    fun moderationUnpaidOffer() {
        val id = "6"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersModerationUnpaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isNotClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/moderationUnpaidOffer/content"
            )
        }
    }

    @Test
    fun moderationPaymentInProcessOffer() {
        val id = "7"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersModerationPaymentInProcess.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isNotClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/moderationPaymentInProcessOffer/content"
            )
        }
    }

    @Test
    fun moderationPaidOffer() {
        val id = "8"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersModerationPaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isNotClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/moderationPaidOffer/content"
            )
        }
    }

    @Test
    fun unpublishedFreeOffer() {
        val id = "9"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersUnpublishedFree.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/unpublishedFreeOffer/content"
            )
        }
    }

    @Test
    fun unpublishedUnpaidOffer() {
        val id = "10"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersUnpublishedUnpaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/unpublishedUnpaidOffer/content"
            )
        }
    }

    @Test
    fun unpublishedPaymentInProcessOffer() {
        val id = "11"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersUnpublishedPaymentInProcess.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/unpublishedPaymentInProcessOffer/content"
            )
        }
    }

    @Test
    fun unpublishedPaidOffer() {
        val id = "12"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersUnpublishedPaid.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/unpublishedPaidOffer/content"
            )
        }
    }

    @Test
    fun bannedRecoverableOffer() {
        val id = "13"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersBannedRecoverable.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/bannedRecoverableOffer/content"
            )
        }
    }

    @Test
    fun bannedNonRecoverableOffer() {
        val id = "14"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersBannedNonRecoverable.json")
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }
                .priceView.isNotClickable()

            listView.isContentStateMatches(
                "UserOfferSnippetTest/bannedNonRecoverableOffer/content"
            )
        }
    }

    @Test
    fun publishedPaidAgencyOffer() {
        val id = "18"

        configureWebServer {
            registerUserOfferList(
                "userOffers/userOffersAgencyPublishedPaid.json",
                false
            )
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedPaidAgencyOffer/content"
            )
        }
    }

    @Test
    fun publishedPaymentInProcessAgencyOffer() {
        val id = "19"

        configureWebServer {
            registerUserOfferList(
                "userOffers/userOffersAgencyPublishedPaymentInProcess.json",
                false
            )
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedPaymentInProcessAgencyOffer/content"
            )
        }
    }

    @Test
    fun publishedUnpaidAgencyOffer() {
        val id = "20"

        configureWebServer {
            registerUserOfferList(
                "userOffers/userOffersAgencyPublishedUnpaid.json",
                false
            )
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedUnpaidAgencyOffer/content"
            )
        }
    }

    @Test
    fun publishedUnpaidAgencyOfferNotEnoughFunds() {
        val id = "15"

        configureWebServer {
            registerUserOfferList(
                "userOffers/userOffersAgencyPublishedUnpaidNotEnoughFunds.json",
                false
            )
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(
                "UserOfferSnippetTest/publishedUnpaidAgencyOfferNotEnoughFunds/content"
            )
        }
    }

    @Test
    fun moderationUnpaidAgencyOfferNotEnoughFunds() {
        val id = "16"

        configureWebServer {
            registerUserOfferList(
                "userOffers/userOffersAgencyModerationUnpaidNotEnoughFunds.json",
                false
            )
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(
                "UserOfferSnippetTest/moderationUnpaidAgencyOfferNotEnoughFunds/content"
            )
        }
    }

    @Test
    fun unpublishedUnpaidAgencyOfferNotEnoughFunds() {
        val id = "17"

        configureWebServer {
            registerUserOfferList(
                "userOffers/userOffersAgencyUnpublishedUnpaidNotEnoughFunds.json",
                false
            )
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            offerSnippet(id)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(
                "UserOfferSnippetTest/unpublishedUnpaidAgencyOfferNotEnoughFunds/content"
            )
        }
    }

    private fun DispatcherRegistry.registerUserOfferList(
        responseFileName: String,
        owner: Boolean = true
    ) {
        registerNoRequiredFeatures()

        if (owner) {
            registerUserOwnerProfile()
            registerUserOwnerProfile()
        } else {
            registerUserAgencyProfile()
            registerUserAgencyProfile()
        }

        registerUserOffers(responseFileName)
    }

    private fun DispatcherRegistry.registerNoRequiredFeatures() {
        register(
            request {
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": []}}")
            }
        )
    }

    private fun DispatcherRegistry.registerUserOffers(responseFileName: String) {
        register(
            request {
                path("2.0/user/me/offers")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerUserOwnerProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userOwner.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUserAgencyProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userAgency.json")
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

    private fun DispatcherRegistry.registerUserOffersById(responseFileName: String) {
        register(
            request {
                path("2.0/user/me/offers/byIds")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {

        private const val OFFER_ID = "1"
    }
}
