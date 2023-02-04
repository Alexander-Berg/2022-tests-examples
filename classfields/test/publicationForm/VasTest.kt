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
            isVasDescriptionShown("459 ₽ на 7 дней")
            isVasFirstBadgeShown("×5 просмотров")
            isVasSwitchShown(R.string.product_premium_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_premium_short,
                enabled = true,
                checked = false
            )
            isVasIconShown(R.drawable.ic_product_premium)

            isVasShown(R.string.product_raising_short)
            isVasTitleShown(R.string.product_raising_short)
            isVasDescriptionShown("30 ₽ на 24 часа")
            isVasFirstBadgeShown("×3 звонков")
            isVasSwitchShown(R.string.product_raising_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_raising_short,
                enabled = true,
                checked = true
            )
            isVasIconShown(R.drawable.ic_product_raising)

            isVasShown(R.string.product_promotion_short)
            isVasTitleShown(R.string.product_promotion_short)
            isVasDescriptionShown("139 ₽ на 7 дней")
            isVasFirstBadgeShown("×2 звонков")
            isVasSwitchShown(R.string.product_promotion_short)
            isVasSwitchEnabledAndChecked(
                R.string.product_promotion_short,
                enabled = true,
                checked = true
            )
            isVasIconShown(R.drawable.ic_product_promotion)

            isVasShown(R.string.product_turbo)
            isVasTitleShown(R.string.product_turbo)
            isVasDescriptionShown("799 ₽ на 7 дней")
            isVasFirstBadgeShown("×7 просмотров")
            isVasSecondBadgeShown("×3 звонков")
            isVasSwitchShown(R.string.product_turbo)
            isVasSwitchEnabledAndChecked(R.string.product_turbo, enabled = true, checked = false)
            isVasIconShown(R.drawable.ic_product_turbo)

            isSubmitButtonShown("Опубликовать за 219 ₽")
            tapOn(lookup.matchesVasView(R.string.product_turbo))
            performOnVasDescriptionScreen {
                isVasTitleShown(R.string.product_turbo)
                isVasFirstBadgeShown("×7 просмотров")
                isVasSecondBadgeShown("×3 звонков")
                isVasDurationShown("на 7 дней")
                isVasDescriptionShown("Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков! Будет автоматически продлеваться за 799 ₽ каждые 7 дней")
                isPurchaseButtonShown("Применить")
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

            isSubmitButtonShown("Опубликовать за 849 ₽")

            tapOn(lookup.matchesVasView(R.string.product_turbo))

            performOnVasDescriptionScreen {
                isPurchaseButtonDisable()
                pressBack()
            }

            tapOn(lookup.matchesVasSwitch(R.string.product_turbo))
            isSubmitButtonShown("Опубликовать за 50 ₽")

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
                isVasShown("Премиум на 7 дней")
                isVasTitleShown("Премиум на 7 дней")
                isVasIconShown(R.drawable.ic_product_premium)
                isVasPriceShown("459 ₽")

                isVasShown("Поднятие на 24 часа")
                isVasTitleShown("Поднятие на 24 часа")
                isVasIconShown(R.drawable.ic_product_raising)
                isVasPriceShown("30 ₽")

                isVasShown("Продвижение на 7 дней")
                isVasTitleShown("Продвижение на 7 дней")
                isVasIconShown(R.drawable.ic_product_promotion)
                isVasPriceShown("139 ₽")

                isVasShown("Публикация на 30 дней")
                isVasTitleShown("Публикация на 30 дней")
                isVasIconShown(R.drawable.ic_product_placement)
                isVasBasePriceShown("57 ₽")
                isVasPriceShown("50 ₽")

                isPurchaseButtonShown("Оплатить 678 ₽")

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
            isOptionDescriptionShown("Опция «Поднятие» будет автоматически продлеваться за 30 ₽ каждые 24 часа")
            isOptionTitleShown(R.string.product_promotion_payment_complete)
            isOptionDescriptionShown("Опция «Продвижение» будет автоматически продлеваться за 139 ₽ каждые 7 дней")
            isOptionTitleShown(R.string.product_premium_payment_complete)
            isOptionDescriptionShown("Опция «Премиум» будет автоматически продлеваться за 459 ₽ каждые 7 дней")
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

                isPurchaseButtonShown("Оплатить 678 ₽")

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

            isSubmitButtonShown("Продвинуть за 169 ₽")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }

                isVasShown("Поднятие на 24 часа")
                isVasTitleShown("Поднятие на 24 часа")
                isVasIconShown(R.drawable.ic_product_raising)
                isVasPriceShown("30 ₽")

                isVasShown("Продвижение на 7 дней")
                isVasTitleShown("Продвижение на 7 дней")
                isVasIconShown(R.drawable.ic_product_promotion)
                isVasPriceShown("139 ₽")

                isPurchaseButtonShown("Оплатить 169 ₽")

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
            isOptionDescriptionShown("Опция «Поднятие» будет автоматически продлеваться за 30 ₽ каждые 24 часа")
            isOptionTitleShown(R.string.product_promotion_payment_complete)
            isOptionDescriptionShown("Опция «Продвижение» будет автоматически продлеваться за 139 ₽ каждые 7 дней")
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

            isSubmitButtonShown("Продвинуть за 799 ₽")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                    isPurchaseButtonShown("Оплатить 799 ₽")
                }

                isVasShown("Пакет «Турбо» на 7 дней")
                isVasTitleShown("Пакет «Турбо» на 7 дней")
                isVasIconShown(R.drawable.ic_product_turbo)
                isVasPriceShown("799 ₽")

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
                "Пакет «Турбо» будет автоматически продлеваться за 799 ₽ каждые 7 дней"
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
            isPlacementAnnotationShown("30 дней")
            isPublicationAnnotationShown()
            isProceedButtonShown()
            val submitButtonText = "Опубликовать за 50 ₽"
            isSubmitButtonShown(submitButtonText)
            tapOn(lookup.matchesSubmitButton(submitButtonText))
        }
        performOnPaymentDialog {
            waitUntil {
                isTitleShown()
            }

            isVasShown("Публикация на 30 дней")
            isVasTitleShown("Публикация на 30 дней")
            isVasIconShown(R.drawable.ic_product_placement)
            isVasBasePriceShown("57 ₽")
            isVasPriceShown("50 ₽")

            isPurchaseButtonShown("Оплатить 50 ₽")

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

            isSubmitButtonShown("Продвинуть за 169 ₽")

            tapOn(lookup.matchesSubmitButton())

            performOnPaymentDialog {
                waitUntil {
                    isTitleShown()
                }

                isPurchaseButtonShown("Оплатить")
                isPurchaseButtonNotEnabled()
                isErrorItemShown()
                tapOn(lookup.matchesErrorItem())

                waitUntil {
                    isPurchaseButtonShown("Оплатить 169 ₽")
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

            isSubmitButtonShown("Продвинуть за 169 ₽")

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
