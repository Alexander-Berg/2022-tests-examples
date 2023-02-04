package ru.auto.ara.test.sale

import androidx.annotation.DrawableRes
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
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(Parameterized::class)
class SaleForOneOfferOneVasTest(private val vas: VasTest) {

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetUserOffersDispatcher.prolongationEnabledActive(),
            vas.saleDispatcher,
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
    private val activityRule = lazyActivityScenarioRule<SplashActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        activityRule,
        SetupTimeRule(date = "01.01.2020", localTime = "12:00"),
    )

    @Test
    fun shouldOpenCorrectSaleItemAndCheckPurchaseLog() {
        activityRule.launchActivity()
        checkSale {
            isCorrectTitleDisplayed(SALE_TITLE)
            isCorrectDescriptionDisplayed(SALE_DESCRIPTION)
            isCloseIconDisplayed()
        }
        performSale { scrollToVasItem(vas.title) }
        checkSale {
            vas.drawableResourceId.forEach { isCorrectSaleDetailsItemVasIcon(it) }
            isCorrectLottieIllustration()
            isCorrectDetailsVasTitleDisplayed(vas.title)
            vas.viewsCount?.let { isCorrectDetailsVasCount(vas.viewsCount) } ?: isDetailsVasCountGone()
            isCorrectDetailsVasDescription(vas.description)
            isCorrectDetailsVasOldPriceDecription(VAS_OLD_PRICE_DESCRIPTION)
        }
        performSale { scrollToBottom() }
        checkSale {
            isCorrectDetailsVasOldPrice(vas.oldPrice)
            isCorrectDetailsVasPercent(vas.oldPriceDiscount)
            isCorrectDetailsVasButton(vas.buyButtonText)
        }
        performSale {
            clickOnBuyVasButton(vas.buyButtonText)
        }

        checkPayment { isPayButtonCompletelyDisplayed() }
    }

    companion object {
        private const val DISCOUNT = 70
        private const val SALE_TITLE = "Скидка до $DISCOUNT%"
        private const val SALE_DESCRIPTION = "До окончания акции"
        private const val VAS_OLD_PRICE_DESCRIPTION = "Обычная цена"

        private fun getDataForPaymentSystem(paymentSystem: PaymentSystem) = listOf(
            VasTest(
                title = "VIP",
                description = "Это супер-комбо — все мощности Авто.ру будут использованы для продажи вашего автомобиля. " +
                    "Объявление будет размещено в специальном блоке вверху страниц, выделено цветом и самое главное " +
                    "— каждый день будет автоматически подниматься на первое место до конца размещения.\n60 дней до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "1 625 \u20BD",
                oldPriceDiscount = "-40%",
                buyButtonText = "Подключить за 975 ₽",
                viewsCount = null,
                saleDispatcher = SaleDispatcher.oneOfferOneVasVip(),
                drawableResourceId = listOf(
                    R.drawable.ic_service_color,
                    R.drawable.ic_service_top,
                    R.drawable.ic_service_spec,
                    R.drawable.ic_service_up2
                ),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "package_vip",
                ),
            ),
            VasTest(
                title = "Турбо–продажа",
                description = "Ваше предложение увидит максимум посетителей — это увеличит шансы на быструю и выгодную продажу." +
                    " Объявление будет выделено цветом, поднято в топ, размещено в специальном блоке на главной странице," +
                    " на странице марки и в выдаче объявлений.\n3 дня до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "498 \u20BD",
                oldPriceDiscount = "-40%",
                buyButtonText = "Подключить за 299 ₽",
                viewsCount = "20 просмотров",
                saleDispatcher = SaleDispatcher.oneOfferOneVasTurbo(),
                drawableResourceId = listOf(
                    R.drawable.ic_service_color,
                    R.drawable.ic_service_top,
                    R.drawable.ic_service_spec
                ),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "package_turbo",
                ),
            ),
            VasTest(
                title = "Экспресс-продажа",
                description = "Объявление будет размещено в специальном блоке на карточках похожих автомобилей," +
                    " в выдаче объявлений и на главной странице (для легковых), а также выделено цветом." +
                    " Это существенно увеличит количество просмотров.\n6 дней до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "378 \u20BD",
                oldPriceDiscount = "-40%",
                buyButtonText = "Подключить за 227 ₽",
                viewsCount = "5 просмотров",
                saleDispatcher = SaleDispatcher.oneOfferOneVasExpress(),
                drawableResourceId = listOf(R.drawable.ic_service_color, R.drawable.ic_service_spec),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "package_express",
                ),
            ),
            VasTest(
                title = "Поднять в поиске",
                description = "Самый недорогой способ продвижения, который позволит вам в любой момент оказаться" +
                    " наверху списка объявлений, отсортированного по актуальности или по дате." +
                    " Это поможет быстрее найти покупателя — ведь предложения в начале списка" +
                    " просматривают гораздо чаще.\n1 день до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "97 \u20BD",
                oldPriceDiscount = "-40%",
                buyButtonText = "Подключить за 58 ₽",
                viewsCount = null,
                saleDispatcher = SaleDispatcher.oneOfferOneVasAllSaleFresh(),
                drawableResourceId = listOf(R.drawable.ic_service_up2),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_fresh",
                ),
            ),
            VasTest(
                title = "Выделение цветом",
                description = "Отличная возможность выделить своё предложение среди других — " +
                    "в результатах поиска оно будет привлекать больше внимания.\n3 дня до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "97 \u20BD",
                oldPriceDiscount = "-51%",
                buyButtonText = "Подключить за 48 ₽",
                viewsCount = null,
                saleDispatcher = SaleDispatcher.oneOfferOneVasAllSaleColor(),
                drawableResourceId = listOf(R.drawable.ic_service_color),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_color",
                ),
            ),
            VasTest(
                title = "Поднятие в ТОП",
                description = "Ваше объявление окажется в специальном блоке на самом верху списка " +
                    "при сортировке по актуальности или по дате. Покупатели вас точно не пропустят.\n3 дня до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "497 \u20BD",
                oldPriceDiscount = "-70%",
                buyButtonText = "Подключить за 149 ₽",
                viewsCount = null,
                saleDispatcher = SaleDispatcher.oneOfferOneVasAllSaleTop(),
                drawableResourceId = listOf(R.drawable.ic_service_top),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_toplist",
                ),
            ),
            VasTest(
                title = "Спецпредложение",
                description = "Ваше объявление будет отображаться в специальном блоке в результатах поиска и на " +
                    "карточках объявлений о продаже аналогичных авто. А для легковых — также на главной странице " +
                    "и в Каталоге.\n3 дня до окончания",
                paymentSystem = paymentSystem,
                oldPrice = "597 \u20BD",
                oldPriceDiscount = "-67%",
                buyButtonText = "Подключить за 197 \u20BD",
                viewsCount = null,
                saleDispatcher = SaleDispatcher.oneOfferOneVasAllSaleSpecial(),
                drawableResourceId = listOf(R.drawable.ic_service_spec),
                purchase = Purchase(
                    offerId = "1085755394-eb5bb9b7",
                    prolongable = false,
                    name = "all_sale_special",
                ),
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data() = getDataForPaymentSystem(PaymentSystem.TRUST) + getDataForPaymentSystem(PaymentSystem.YANDEXKASSA)
    }

    data class VasTest(
        val title: String,
        val description: String,
        val paymentSystem: PaymentSystem,
        val oldPrice: String,
        val oldPriceDiscount: String,
        val buyButtonText: String,
        val viewsCount: String?,
        val saleDispatcher: SaleDispatcher,
        @DrawableRes val drawableResourceId: List<Int>,
        val purchase: Purchase
    )

    data class Purchase(
        val offerId: String,
        val prolongable: Boolean,
        val name: String,
        val from: String = "api_android_popup_discount",
        val platform: String = "PLATFORM_ANDROID"
    )

}
