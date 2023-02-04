package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnAuthWebView
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnPaymentCompleteScreen
import com.yandex.mobile.realty.core.robot.performOnPaymentDialog
import com.yandex.mobile.realty.core.robot.performOnReportsScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 30/10/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ExcerptReportTest {

    private val activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldShowReportReference() {
        configureWebServer {
            registerOffer()
            register(
                request {
                    path("2.0/paid-report/user/me")
                    queryParam("paymentStatus", "paid")
                    queryParam("offerId", "0")
                },
                response {
                    assetBody("excerpt/paid-report_single.json")
                }
            )
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptReportReference("1a") }
            isExcerptReportReferenceMatches(
                "1a",
                "/ExcerptReportTest/shouldShowReportReference"
            )

            tapOn(lookup.matchesExcerptReportReference("1a"))
        }

        performOnAuthWebView {
            waitUntil {
                isPageUrlEquals("https://m.realty.yandex.ru/egrn-report/1a/?only-content=true")
            }
        }
    }

    @Test
    fun shouldShowReportProduct() {
        configureWebServer {
            registerOffer()
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptReportProduct() }
            performOnExcerptReportProduct {
                isViewStateMatches("/ExcerptReportTest/shouldShowReportProduct/block")
            }
        }
    }

    @Test
    fun shouldPurchaseReportProduct() {
        configureWebServer {
            registerOffer()
            register(
                request {
                    path("2.0/paid-report/user/me")
                    queryParam("paymentStatus", "paid")
                    queryParam("offerId", "0")
                },
                response {
                    setBody("{\"response\":{\"paidReports\": [],\"paging\":{\"total\":0}}}")
                }
            )
            registerUser("user/userOwner.json")
            register(
                request {
                    method("POST")
                    path("2.0/paid-report/address-info")
                    body("{\"offerId\":\"0\"}")
                },
                response {
                    setBody("{\"response\":{\"addressInfoId\":\"c5\",\"status\":\"NEW\"}}")
                }
            )
            register(
                request {
                    path("2.0/paid-report/address-info/c5")
                },
                response {
                    setBody("{\"response\":{\"addressInfoId\":\"c5\",\"status\":\"DONE\"}}")
                }
            )
            register(
                request {
                    method("POST")
                    path("2.0/paid-report/user/me/init")
                    body("{\"addressInfoId\":\"c5\"}")
                },
                response {
                    setBody(
                        "{\"response\":{" +
                            "\"paidReport\":{" +
                            "\"paidReportId\":\"7a\"," +
                            "\"reportDate\":\"2019-10-10T10:28:24Z\"" +
                            "}}}"
                    )
                }
            )
            register(
                request {
                    method("POST")
                    path("2.0/products/user/me/purchase/init")
                    body(
                        "{\"item\":[{" +
                            "\"productType\":\"PRODUCT_TYPE_PAID_REPORT\"," +
                            "\"target\":{" +
                            "\"paidReport\":{" +
                            "\"paidReportId\":\"7a\"" +
                            "}}}]," +
                            "\"renewalByDefault\":true" +
                            "}"
                    )
                },
                response {
                    assetBody("excerpt/purchase/purchaseInit.json")
                }
            )
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
                            "\"purchaseId\":\"0c\"" +
                            "}"
                    )
                },
                response {
                    setBody(
                        "{\"response\": {" +
                            "\"noConfirmation\": {}," +
                            "\"paymentRequestId\": \"d5\"" +
                            "}}"
                    )
                }
            )
            register(
                request {
                    path("2.0/products/user/me/purchase/0c")
                },
                response {
                    assetBody("excerpt/purchase/purchaseStatus.json")
                }
            )
            register(
                request {
                    path("2.0/paid-report/user/me")
                    queryParam("paymentStatus", "paid")
                    queryParam("offerId", "0")
                },
                response {
                    setBody(
                        "{\"response\":{" +
                            "\"paidReports\": [{" +
                            "\"paidReportId\":\"7a\"," +
                            "\"reportDate\":\"2019-10-10T10:28:24Z\"" +
                            "}]," +
                            "\"paging\":{\"total\":0}}}"
                    )
                }
            )
        }
        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptReportProduct() }
            scrollByFloatingButtonHeight()
            performOnExcerptReportProduct {
                tapOn(lookup.matchesPurchaseButton())
            }
        }
        performOnPaymentDialog {
            waitUntil { isPurchaseButtonEnabled() }
            isViewStateMatches("/ExcerptReportTest/shouldPurchaseReportProduct/payment")
            tapOn(lookup.matchesPurchaseButton())
        }
        performOnPaymentCompleteScreen {
            waitUntil {
                isBottomSheetShown()
                isImageViewShown()
            }
            isViewStateMatches("/ExcerptReportTest/shouldPurchaseReportProduct/complete")
            tapOn(lookup.matchesProceedButton())
        }
        performOnOfferCardScreen {
            performOnExcerptReportProduct {
                isViewStateMatches("/ExcerptReportTest/shouldPurchaseReportProduct/blockPurchased")
                tapOn(lookup.matchesShowReportsButton())
            }
        }
        performOnReportsScreen {
            isReportsToolbarTitleShown()
            pressBack()
        }
        performOnOfferCardScreen {
            containsExcerptReportReference("7a")
            isExcerptReportReferenceMatches(
                "7a",
                "/ExcerptReportTest/shouldPurchaseReportProduct/referencePurchased"
            )
        }
    }

    @Test
    fun shouldShowErrorForLegalPerson() {
        configureWebServer {
            registerOffer()
            register(
                request {
                    path("2.0/paid-report/user/me")
                    queryParam("paymentStatus", "paid")
                    queryParam("offerId", "0")
                },
                response {
                    setBody("{\"response\":{\"paidReports\": [],\"paging\":{\"total\":0}}}")
                }
            )
            registerUser("user/userAgency.json")
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptReportProduct() }
            scrollByFloatingButtonHeight()
            performOnExcerptReportProduct {
                tapOn(lookup.matchesPurchaseButton())
            }

            waitUntil {
                isToastShown("Для\u00a0юрлиц оплата пока не\u00a0работает")
            }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("excerpt/cardWithViewsSellApartment.json")
            }
        )
    }

    /**
     *  GET 1.0/user регистрируем два раза, потому что профиль нужен в блоке обратного звонка
     *  при загрузке экрана и для запроса отчета
     *  @see com.yandex.mobile.realty.ui.commoncallback.CommonCallbackFeature
     */
    private fun DispatcherRegistry.registerUser(responseFileName: String) {
        repeat(2) {
            register(
                request {
                    path("1.0/user")
                },
                response {
                    assetBody(responseFileName)
                }
            )
        }
    }
}
