package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.*
import com.yandex.mobile.realty.core.rule.*
import com.yandex.mobile.realty.core.webserver.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author matek3022 on 2020-07-20.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VasTest : BasePublishFormTest() {

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
    @Suppress("MaxLineLength")
    fun shouldPublishWhenPaidPlacementWithRaisingPromotionPremium() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedUnpaid.json")
            registerNoRequiredFeatures()
            registerInitPurchase("purchase/initPurchase.json", "purchase/initPurchaseBody.json")
            registerInitPurchase("purchase/initPurchase.json", "purchase/initPurchaseBody.json")
            registerNoConfirmationPayment()
            registerPurchaseClosed("purchase/purchaseClosed.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnVasListScreen {
            waitUntil {
                isToolbarTitleShown()
                isVasShown(R.string.product_premium_short)
            }
            isVasTitleShown(R.string.product_premium_short)
            isVasDescriptionShown("459????? ???? 7 ????????")
            isVasFirstBadgeShown("??5 ????????????????????")
            isVasSwitchShown(R.string.product_premium_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_premium_short,
                enabled = true,
                checked = false
            )
            isVasIconShown(R.drawable.ic_product_premium)

            isVasShown(R.string.product_raising_short)
            isVasTitleShown(R.string.product_raising_short)
            isVasDescriptionShown("30????? ???? 24 ????????")
            isVasFirstBadgeShown("??3 ??????????????")
            isVasSwitchShown(R.string.product_raising_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_raising_short,
                enabled = true,
                checked = true
            )
            isVasIconShown(R.drawable.ic_product_raising)

            isVasShown(R.string.product_promotion_short)
            isVasTitleShown(R.string.product_promotion_short)
            isVasDescriptionShown("139????? ???? 7 ????????")
            isVasFirstBadgeShown("??2 ??????????????")
            isVasSwitchShown(R.string.product_promotion_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_promotion_short,
                enabled = true,
                checked = true
            )
            isVasIconShown(R.drawable.ic_product_promotion)

            isVasShown(R.string.product_turbo)
            isVasTitleShown(R.string.product_turbo)
            isVasDescriptionShown("799????? ???? 7 ????????")
            isVasFirstBadgeShown("??7 ????????????????????")
            isVasSecondBadgeShown("??3 ??????????????")
            isVasSwitchShown(R.string.product_turbo)
            isVasSwitchEnabledAndChecked(R.string.product_turbo, enabled = true, checked = false)
            isVasIconShown(R.drawable.ic_product_turbo)

            isSubmitButtonShown("???????????????????????? ???? 219?????")
            tapOn(lookup.matchesVasView(R.string.product_turbo))
            performOnVasDescriptionScreen {
                isVasTitleShown(R.string.product_turbo)
                isVasFirstBadgeShown("??7 ????????????????????")
                isVasSecondBadgeShown("??3 ??????????????")
                isVasDurationShown("???? 7 ????????")
                isVasDescriptionShown("???????????????? ?? ???????? ?????????? ??????????????????, ??????????????????????????, ???????????????????? ???????????????????? ?? ?????????????? ????????????. ???????????????? ?? 7 ?????? ???????????? ???????????????????? ?? ?? 3 ???????? ???????????? ??????????????! ?????????? ?????????????????????????? ???????????????????????? ???? 799????? ???????????? 7 ????????")
                isPurchaseButtonShown("??????????????????")
                tapOn(lookup.matchesPurchaseButton())
            }

            isVasTitleShown(R.string.product_premium_short)
            isVasSwitchShown(R.string.product_premium_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_premium_short,
                enabled = false,
                checked = false
            )

            isVasShown(R.string.product_raising_short)
            isVasSwitchShown(R.string.product_raising_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_raising_short,
                enabled = false,
                checked = false
            )

            isVasShown(R.string.product_promotion_short)
            isVasSwitchShown(R.string.product_promotion_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_promotion_short,
                enabled = false,
                checked = false
            )

            isVasShown(R.string.product_turbo)
            isVasSwitchShown(R.string.product_turbo)
            isVasSwitchEnabledAndChecked(R.string.product_turbo, enabled = true, checked = true)

            isSubmitButtonShown("???????????????????????? ???? 849?????")

            tapOn(lookup.matchesVasView(R.string.product_turbo))

            performOnVasDescriptionScreen {
                isPurchaseButtonDisable()
                pressBack()
            }

            tapOn(lookup.matchesVasSwitch(R.string.product_turbo))
            isSubmitButtonShown("???????????????????????? ???? 50?????")

            isVasShown(R.string.product_turbo)
            isVasSwitchShown(R.string.product_turbo)
            isVasSwitchEnabledAndChecked(R.string.product_turbo, enabled = true, checked = false)

            isVasShown(R.string.product_premium_short)
            isVasSwitchShown(R.string.product_premium_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_premium_short,
                enabled = true,
                checked = false
            )
            tapOn(lookup.matchesVasSwitch(R.string.product_premium_short))

            isVasShown(R.string.product_raising_short)
            isVasSwitchShown(R.string.product_raising_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_raising_short,
                enabled = true,
                checked = false
            )
            tapOn(lookup.matchesVasSwitch(R.string.product_raising_short))

            isVasShown(R.string.product_promotion_short)
            isVasSwitchShown(R.string.product_promotion_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_promotion_short,
                enabled = true,
                checked = false
            )
            tapOn(lookup.matchesVasSwitch(R.string.product_promotion_short))

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }
                isCloseButtonShown()

                tapOn(lookup.matchesCloseButton())
            }

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }
                isVasShown("?????????????? ???? 7 ????????")
                isVasTitleShown("?????????????? ???? 7 ????????")
                isVasIconShown(R.drawable.ic_product_premium)
                isVasPriceShown("459?????")

                isVasShown("???????????????? ???? 24 ????????")
                isVasTitleShown("???????????????? ???? 24 ????????")
                isVasIconShown(R.drawable.ic_product_raising)
                isVasPriceShown("30?????")

                isVasShown("?????????????????????? ???? 7 ????????")
                isVasTitleShown("?????????????????????? ???? 7 ????????")
                isVasIconShown(R.drawable.ic_product_promotion)
                isVasPriceShown("139?????")

                isVasShown("???????????????????? ???? 30 ????????")
                isVasTitleShown("???????????????????? ???? 30 ????????")
                isVasIconShown(R.drawable.ic_product_placement)
                isVasBasePriceShown("57?????")
                isVasPriceShown("50?????")

                isPurchaseButtonShown("???????????????? 678?????")

                tapOn(lookup.matchesPurchaseButton())
            }
        }

        performOnPaymentCompleteScreen {
            waitUntil {
                isProcessButtonShown()
            }
            isTitleShown(R.string.payment_passed)
            isDescriptionShown(R.string.product_placement_and_vas_payment_complete_desc)
            isOptionTitleShown(R.string.product_raising_payment_complete)
            isOptionDescriptionShown("?????????? ???????????????????? ?????????? ?????????????????????????? ???????????????????????? ???? 30????? ???????????? 24 ????????")
            isOptionTitleShown(R.string.product_promotion_payment_complete)
            isOptionDescriptionShown("?????????? ?????????????????????????? ?????????? ?????????????????????????? ???????????????????????? ???? 139????? ???????????? 7 ????????")
            isOptionTitleShown(R.string.product_premium_payment_complete)
            isOptionDescriptionShown("?????????? ?????????????????? ?????????? ?????????????????????????? ???????????????????????? ???? 459????? ???????????? 7 ????????")
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldShowPaymentInProcessWhenPurchaseStatusTimedOut() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedUnpaid.json")
            registerNoRequiredFeatures()
            registerInitPurchase("purchase/initPurchase.json", "purchase/initPurchaseBody.json")
            registerNoConfirmationPayment()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnVasListScreen {
            waitUntil {
                isToolbarTitleShown()
                isVasShown(R.string.product_premium_short)
            }
            tapOn(lookup.matchesVasSwitch(R.string.product_premium_short))

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }

                isPurchaseButtonShown("???????????????? 678?????")

                tapOn(lookup.matchesPurchaseButton())

                waitUntil {
                    isTitlePaymentOutTimeShown()
                }

                isDescriptionPaymentOutTimeShown()
                isClearButtonPaymentOutTimeShown()
                isGoToUserOffersButtonShown()

                tapOn(lookup.matchesClearButtonPaymentOutTime())
            }
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun shouldPublishWhenFreePlacementWithVases() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedFree.json")
            registerNoRequiredFeatures()
            registerInitPurchase(
                "purchase/initPurchaseFree.json",
                "purchase/initPurchaseFreeBody.json"
            )
            registerNoConfirmationPayment()
            registerPurchaseClosed("purchase/purchaseClosedFree.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnVasListScreen {
            waitUntil {
                isToolbarTitleShown()
                isVasShown(R.string.product_premium_short)
            }

            isVasShown(R.string.product_raising_short)
            isVasSwitchShown(R.string.product_raising_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_raising_short,
                enabled = true,
                checked = true
            )

            isVasShown(R.string.product_promotion_short)
            isVasSwitchShown(R.string.product_promotion_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_promotion_short,
                enabled = true,
                checked = true
            )

            isSubmitButtonShown("???????????????????? ???? 169?????")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }

                isVasShown("???????????????? ???? 24 ????????")
                isVasTitleShown("???????????????? ???? 24 ????????")
                isVasIconShown(R.drawable.ic_product_raising)
                isVasPriceShown("30?????")

                isVasShown("?????????????????????? ???? 7 ????????")
                isVasTitleShown("?????????????????????? ???? 7 ????????")
                isVasIconShown(R.drawable.ic_product_promotion)
                isVasPriceShown("139?????")

                isPurchaseButtonShown("???????????????? 169?????")

                tapOn(lookup.matchesPurchaseButton())
            }
        }

        performOnPaymentCompleteScreen {
            waitUntil {
                isProcessButtonShown()
            }
            isTitleShown(R.string.payment_passed)
            isDescriptionShown(R.string.product_placement_and_vas_payment_complete_desc)
            isOptionTitleShown(R.string.product_raising_payment_complete)
            isOptionDescriptionShown("?????????? ???????????????????? ?????????? ?????????????????????????? ???????????????????????? ???? 30????? ???????????? 24 ????????")
            isOptionTitleShown(R.string.product_promotion_payment_complete)
            isOptionDescriptionShown("?????????? ?????????????????????????? ?????????? ?????????????????????????? ???????????????????????? ???? 139????? ???????????? 7 ????????")
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldPublishWithTurbo() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedFree.json")
            registerNoRequiredFeatures()
            registerInitPurchase(
                "purchase/initPurchaseFreeTurbo.json",
                "purchase/initPurchaseFreeTurboBody.json"
            )
            registerNoConfirmationPayment()
            registerPurchaseClosed("purchase/purchaseClosedFreeTurbo.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnVasListScreen {
            waitUntil {
                isToolbarTitleShown()
                isVasShown(R.string.product_turbo)
            }
            tapOn(lookup.matchesVasSwitch(R.string.product_turbo))

            isSubmitButtonShown("???????????????????? ???? 799?????")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                    isPurchaseButtonShown("???????????????? 799?????")
                }

                isVasShown("?????????? ?????????????? ???? 7 ????????")
                isVasTitleShown("?????????? ?????????????? ???? 7 ????????")
                isVasIconShown(R.drawable.ic_product_turbo)
                isVasPriceShown("799?????")

                tapOn(lookup.matchesPurchaseButton())
            }
        }

        performOnPaymentCompleteScreen {
            waitUntil {
                isProcessButtonShown()
            }
            isTitleShown(R.string.payment_passed)
            isDescriptionShown(R.string.product_placement_payment_complete_desc)
            isOptionTitleShown(R.string.product_turbo_payment_complete)
            isOptionDescriptionShown(
                "?????????? ?????????????? ?????????? ?????????????????????????? ???????????????????????? ???? 799????? ???????????? 7 ????????"
            )
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldPublishWhenPaidPlacementAndNoVases() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedUnpaidNoVas.json")
            registerNoRequiredFeatures()
            registerInitPurchase(
                "purchase/initPurchaseUnpaidNoVas.json",
                "purchase/initPurchaseUnpaidNoVasBody.json"
            )
            registerNoConfirmationPayment()
            registerPurchaseClosed("purchase/purchaseClosedUnpaidNoVas.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPaidPlacementScreen {
            waitUntil {
                isDescriptionShown()
            }
            isPlacementAnnotationShown("30 ????????")
            isPublicationAnnotationShown()
            isProceedButtonShown()
            val submitButtonText = "???????????????????????? ???? 50?????"
            isSubmitButtonShown(submitButtonText)
            tapOn(lookup.matchesSubmitButton(submitButtonText))
        }
        performOnPaymentDialog {
            waitUntil {
                isTitleShown()
            }

            isVasShown("???????????????????? ???? 30 ????????")
            isVasTitleShown("???????????????????? ???? 30 ????????")
            isVasIconShown(R.drawable.ic_product_placement)
            isVasBasePriceShown("57?????")
            isVasPriceShown("50?????")

            isPurchaseButtonShown("???????????????? 50?????")

            tapOn(lookup.matchesPurchaseButton())
        }

        performOnPaymentCompleteScreen {
            waitUntil {
                isProcessButtonShown()
            }
            isTitleShown(R.string.payment_passed)
            isDescriptionShown(R.string.product_placement_payment_complete_desc)
            isDescriptionShown(R.string.uo_placement_complete_promo)
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldPublishWhenFreePlacementAndNoVases() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedNoVasFree.json")
            registerNoRequiredFeatures()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil {
                isTitleShown()
            }
            isDescriptionShown()
            isPromoShown()
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldPublishWithSkippedVasesWhenFailedLoadOfferCard() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        performOnPublicationCompleteScreen {
            waitUntil {
                isTitleShown()
            }
            isDescriptionHidden()
            isPromoShown()
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldShowInitPurchaseError() {
        configureWebServer {
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfile(true)
            registerUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedFree.json")
            registerNoRequiredFeatures()
            registerInitPurchaseError("purchase/initPurchaseFreeBody.json")
            registerInitPurchase(
                "purchase/initPurchaseFree.json",
                "purchase/initPurchaseFreeBody.json"
            )
            registerNoConfirmationPayment()
            registerPurchaseClosed("purchase/purchaseClosedFree.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnVasListScreen {
            waitUntil {
                isToolbarTitleShown()
                isVasShown(R.string.product_premium_short)
            }

            isSubmitButtonShown("???????????????????? ???? 169?????")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }

                isPurchaseButtonShown("????????????????")
                isPurchaseButtonNotEnabled()
                isErrorItemShown()
                tapOn(lookup.matchesErrorItem())

                waitUntil {
                    isPurchaseButtonShown("???????????????? 169?????")
                }
                isPurchaseButtonEnabled()

                tapOn(lookup.matchesPurchaseButton())
            }
        }

        performOnPaymentCompleteScreen {
            waitUntil {
                isProcessButtonShown()
            }
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldShowVasWithNewUser() {
        configureWebServer {
            registerUnknownUserProfile()
            registerUserProfile(true)
            registerUnknownUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedFree.json")
            registerNoRequiredFeatures()
            registerInitPurchase(
                "purchase/initPurchaseFree.json",
                "purchase/initPurchaseFreeBody.json"
            )
            registerNoConfirmationPayment()
            registerPurchaseClosed("purchase/purchaseClosedFree.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnVasListScreen {
            waitUntil {
                isToolbarTitleShown()
                isVasShown(R.string.product_premium_short)
            }

            isSubmitButtonShown("???????????????????? ???? 169?????")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }

                tapOn(lookup.matchesPurchaseButton())
            }
        }

        performOnPaymentCompleteScreen {
            waitUntil {
                isProcessButtonShown()
            }
            tapOn(lookup.matchesProceedButton())
        }
    }

    @Test
    fun shouldNotShowVasWithJuridic() {
        configureWebServer {
            registerUserProfile(false)
            registerUserProfile(false)
            registerUserProfile(false)
            registerJuridicUserProfilePatch()
            registerNoRequiredFeatures()
            registerEmptyUserOffers()
            registerValidation()
            registerDraft()
            registerPublishForm()
            registerUserOfferCard("userOffer/userOfferPublishedFree.json")
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        performOnPublicationCompleteScreen {
            waitUntil {
                isProceedButtonShown()
            }
            isTitleShown()
            isDescriptionShown()
            isPromoHidden()
            tapOn(lookup.matchesProceedButton())
        }
    }

    private fun DispatcherRegistry.registerValidation() {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerPublishForm() {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/draft/1234")
                queryParam("publish", "true")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerUserOfferCard(fileName: String) {
        register(
            request {
                method("GET")
                path("2.0/user/me/offers/1234/card")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    private fun DispatcherRegistry.registerInitPurchase(
        fileName: String,
        bodyFileName: String
    ) {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/init")
                assetBody(bodyFileName)
            },
            response {
                assetBody(fileName)
            }
        )
    }

    private fun DispatcherRegistry.registerInitPurchaseError(bodyFileName: String) {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/init")
                assetBody(bodyFileName)
            },
            response {
                setResponseCode(400)
            }
        )
    }

    private fun DispatcherRegistry.registerNoConfirmationPayment() {
        register(
            request {
                method("POST")
                path("2.0/products/user/me/purchase/payment")
                assetBody("purchase/noConfirmationPaymentBody.json")
            },
            response {
                assetBody("purchase/noConfirmationPayment.json")
            }
        )
    }

    private fun DispatcherRegistry.registerPurchaseClosed(fileName: String) {
        register(
            request {
                method("GET")
                path("2.0/products/user/me/purchase/a396ccb376d344b98c9cfb76d9b4a10e")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    private fun DispatcherRegistry.registerUnknownUserProfile() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("user/newUser.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUnknownUserProfilePatch() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                body(
                    """{
                                    "user": {
                                        "name": "John",
                                        "email": "",
                                        "phones": [],
                                        "redirectPhones": true,
                                        "allowedCommunicationChannels":["COM_CALLS","COM_CHATS"]
                                    }
                                }"""
                )
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfile(isNaturalPerson: Boolean) {
        val paymentType = if (isNaturalPerson) {
            "\"paymentType\": \"NATURAL_PERSON\","
        } else {
            "\"paymentType\": \"JURIDICAL_PERSON\","
        }
        val type = if (isNaturalPerson) {
            "\"type\": \"OWNER\","
        } else {
            "\"type\": \"AGENCY\","
        }
        val body = """{
                                "response": {
                                    "valid": true,
                                    "user": {
                                        "name": "John",
                                        "status": "active",
                                        "phones": [
                                            {
                                                "id": "1",
                                                "phone": "+7111*****44",
                                                "select": true,
                                                "fullPhone": "+71112223344"
                                            }
                                        ],
                                        "email" : "john@gmail.com",
                                        $type
                                        "redirectPhones": true,
                                        $paymentType
                                        "capaUser": false
                                    }
                                }
                            }"""
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                setBody(body)
            }
        )
    }
}
