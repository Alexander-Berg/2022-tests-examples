package ru.auto.ara.test.sale

import androidx.annotation.DrawableRes
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.SplashActivity
import ru.auto.ara.core.dispatchers.BodyNode
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.postInitPayment
import ru.auto.ara.core.dispatchers.sale.SaleDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.payment.checkPayment
import ru.auto.ara.core.robot.sale.checkSale
import ru.auto.ara.core.robot.sale.performSale
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(Parameterized::class)
class SaleForOneOfferManyVas(private val vas: VasCollapse) {

    private val dispatcherHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetUserOffersDispatcher.prolongationEnabledActive(),
            SaleDispatcher.oneOfferManyVas(),
            dispatcherHolder,
        )
        postInitPayment(vas.paymentSystem).watch {
            checkRequestBodyParameters(
                "autoru_purchase.offer_id" to vas.purchase.offerId,
                "autoru_purchase.prolongable" to vas.purchase.prolongable.toString(),
                "statistics_parameters.from" to vas.purchase.from,
                "statistics_parameters.platform" to vas.purchase.platform,
            )
            checkBody {
                asObject { get("product").asArray {
                    single { e -> e is BodyNode.Object }.asObject { get("name").assertValue(vas.purchase.name) } }
                }
            }
        }
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        SetupTimeRule(date = "01.01.2020", localTime = "12:00"),
    )

    val activityRule = ActivityTestRule(SplashActivity::class.java, false, false)

    @Before
    fun setup() {
        activityRule.launchActivity(null)
        checkSale {
            isCorrectTitleDisplayed(SALE_TITLE)
            isCorrectDescriptionDisplayed(SALE_DESCRIPTION)
            isCloseIconDisplayed()
        }
    }

    @Test
    fun shouldOpenSaleOnStartApp() {
        performSale { scrollToVasItem(vas.title) }
        checkSale {
            isCorrectCollapseVasTitle(vas.title)
            isCorrectCollapseVasIcon(vas.drawableResourceId)
            isCorrectCollapseVasPrice(vas.price)

            vas.oldPrice?.let { isCorrectCollapseVasOldPrice(it) } ?: isCollapseVasOldPriceIsGone()
            vas.days?.let { isCorrectCollapseVasDays(it, vas.position) } ?: isCollapseVasDaysIsGone(vas.position)
            vas.views?.let { isCorrectCollapseVasViews(it, vas.position) } ?: isCollapseVasViewsIsGone(vas.position)
        }

        performSale {
            clickOnPriceOnCollapseItem(vas.price)
        }

        checkPayment { isPayButtonCompletelyDisplayed() }
    }

    companion object {
        private const val DISCOUNT = 70
        private const val SALE_TITLE = "Скидка до $DISCOUNT%"
        private const val SALE_DESCRIPTION = "До окончания акции"

        private fun getDataForPaymentSystem(paymentSystem: PaymentSystem) = listOf(
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "VIP",
                oldPrice = "1 625 \u20BD",
                price = "975 \u20BD",
                days = "60 дней",
                views = null,
                drawableResourceId = R.drawable.ic_service_vip,
                position = 2,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "package_vip",
                ),
            ),
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "Турбо-продажа",
                price = "299 \u20BD",
                oldPrice = "498 \u20BD",
                days = "3 дня",
                views = "20 просмотров",
                drawableResourceId = R.drawable.ic_service_turbo,
                position = 4,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "package_turbo",
                ),
            ),
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "Экспресс-продажа",
                price = "227 \u20BD",
                oldPrice = "378 \u20BD",
                days = "6 дней",
                views = null,
                drawableResourceId = R.drawable.ic_service_express,
                position = 6,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "package_express",
                ),
            ),
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "Поднятие в поиске",
                price = "67 \u20BD",
                oldPrice = "99 \u20BD",
                days = "1 день",
                views = null,
                drawableResourceId = R.drawable.ic_service_up2,
                position = 8,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_fresh",
                ),
            ),
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "Выделение цветом",
                price = "48 \u20BD",
                oldPrice = "97 \u20BD",
                days = "3 дня",
                views = null,
                drawableResourceId = R.drawable.ic_service_color,
                position = 10,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_color",
                ),
            ),
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "Спецпредложение",
                price = "279 \u20BD",
                oldPrice = null,
                days = "3 дня",
                views = null,
                drawableResourceId = R.drawable.ic_service_spec,
                position = 12,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_special",
                ),
            ),
            VasCollapse(
                paymentSystem = paymentSystem,
                title = "Поднятие в ТОП",
                price = "149 \u20BD",
                oldPrice = "150 \u20BD",
                days = null,
                views = null,
                drawableResourceId = R.drawable.ic_service_top,
                position = 14,
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_toplist",
                ),
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data() = getDataForPaymentSystem(PaymentSystem.TRUST) + getDataForPaymentSystem(PaymentSystem.YANDEXKASSA)
    }

    data class VasCollapse(
        val paymentSystem: PaymentSystem,
        val title: String,
        val days: String?,
        val views: String?,
        val price: String,
        val oldPrice: String?,
        @DrawableRes val drawableResourceId: Int,
        val position: Int,
        val purchase: Purchase,
    )

    data class Purchase(
        val offerId: String,
        val prolongable: Boolean,
        val name: String,
        val from: String = "api_android_popup_discount",
        val platform: String = "PLATFORM_ANDROID",
    )
}
