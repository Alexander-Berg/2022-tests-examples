package ru.yandex.market.clean.presentation.formatter

import android.content.res.AssetManager
import android.graphics.Typeface
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers.not
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.beru.android.R
import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.clean.domain.model.OfferDeliveryOption
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.offerDeliveryOptionTestInstance
import ru.yandex.market.clean.domain.model.offerPricesTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.feature.cms.item.offer.formatter.ProductOfferDeliveryOptionFormatter
import ru.yandex.market.clean.presentation.feature.cms.item.offerexpress.formatter.ShopAvailableChecker
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.common.featureconfigs.managers.CheapestCourierDeliveryOnSkuToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.PharmaBookingConfig
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.domain.delivery.model.deliveryConditions_WithPriceTestInstance
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.money.model.amountTestInstance
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.feature.manager.CheckoutDynamicDeliveryPriceFeatureManager
import ru.yandex.market.feature.manager.PurchaseByListFeatureManager
import ru.yandex.market.uikit.text.TypefaceUtils
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class ProductOfferDeliveryOptionFormatterTest {

    private val assets = mock<AssetManager>()
    private val resourcesManager = mock<ResourcesManager> {
        on { getString(R.string.free) }.thenReturn("бесплатно")
        on {
            getFormattedString(
                eq(R.string.sku_delivery_price_from),
                any<String>()
            )
        }.thenAnswer { invocation ->
            val price = invocation.arguments[1]
            " от $price"
        }
        on {
            getFormattedString(
                eq(R.string.shop_schedule_from_or_on_time),
                any<String>()
            )
        }.thenAnswer { invocation ->
            val time = invocation.arguments[1]
            " с $time или ко времени"
        }
        on { getFormattedString(R.string.sku_express_delivery_in_one_two_hours) }.thenReturn(" за 1–2 часа")
        on { getFormattedString(R.string.sku_express_delivery_in_one_two_hours_upper_case) }.thenReturn("За 1–2 часа")
        on { getString(not(eq(R.string.free))) }.thenReturn("Тестовая доставка")
        on { assets }.thenReturn(assets)
    }
    private val dateFormatter = mock<DateFormatter>()
    private val moneyFormatter = mock<MoneyFormatter> {
        on { formatPrice(any()) }.thenAnswer { invocation ->
            val money = invocation.arguments[0] as Money
            money.amount.value.toString()
        }
    }
    private val timeFormatter = mock<TimeFormatter>()
    private val shopAvailableChecker = mock<ShopAvailableChecker>()
    private val cheapestCourierDeliveryOnSkuToggleManager = mock<CheapestCourierDeliveryOnSkuToggleManager> {
        on { getFromCacheOrDefault() }.thenReturn(FeatureToggle(true))
    }
    private val purchaseByListFeatureManager = mock<PurchaseByListFeatureManager> {
        on { getPurchaseByListBookingConfig() }.thenReturn(PharmaBookingConfig(false, null))
    }
    private val checkoutDynamicDeliveryPriceFeatureManager = mock<CheckoutDynamicDeliveryPriceFeatureManager> {
        on { isEnabled() }.thenReturn(false)
    }

    private val formatter = ProductOfferDeliveryOptionFormatter(
        resourcesManager = resourcesManager,
        dateFormatter = dateFormatter,
        moneyFormatter = moneyFormatter,
        timeFormatter = timeFormatter,
        shopAvailableChecker = shopAvailableChecker,
        cheapestCourierDeliveryOnSkuToggleManager = cheapestCourierDeliveryOnSkuToggleManager,
        purchaseByListFeatureManager = purchaseByListFeatureManager,
        checkoutDynamicDeliveryPriceFeatureManager = checkoutDynamicDeliveryPriceFeatureManager,
    )

    private val offerPrices = offerPricesTestInstance(
        purchasePrice = moneyTestInstance(
            amount = amountTestInstance(BigDecimal.valueOf(100L)),
            currency = Currency.RUR
        )
    )

    @Test
    fun `Return free on pick up string when only Market's points available`() {
        val freeDeliveryOption = createDeliveryOption(DeliveryType.PICKUP, BigDecimal.valueOf(0L))
        val offer = createOffer(freeDeliveryOption)

        val viewObject = formatter.format(productOffer = offer)

        assertThat(viewObject.size).isEqualTo(1)

        val productOfferDeliveryOptionVo = viewObject[0]

        assertThat(productOfferDeliveryOptionVo.priceInfo?.value.toString()).isEqualTo("бесплатно")
    }

    @Test
    fun `Return from 0 on pick up string when Market's and contract points available`() {
        val paidDeliveryOption = createDeliveryOption(DeliveryType.PICKUP, BigDecimal.valueOf(49L))
        val freeDeliveryOption = createDeliveryOption(DeliveryType.PICKUP, BigDecimal.valueOf(0L))
        val offer = createOffer(paidDeliveryOption, freeDeliveryOption)

        val viewObject = formatter.format(productOffer = offer)

        assertThat(viewObject.size).isEqualTo(1)

        val productOfferDeliveryOptionVo = viewObject[0]

        assertThat(productOfferDeliveryOptionVo.priceInfo?.value.toString()).isEqualTo(" от 0")
    }

    @Test
    fun `Return exact price on pick up string when only contract points available`() {
        val paidDeliveryOption = createDeliveryOption(DeliveryType.PICKUP, BigDecimal.valueOf(49L))
        val offer = createOffer(paidDeliveryOption)

        val viewObject = formatter.format(productOffer = offer)

        assertThat(viewObject.size).isEqualTo(1)

        val productOfferDeliveryOptionVo = viewObject[0]

        assertThat(productOfferDeliveryOptionVo.priceInfo?.value.toString()).isEqualTo("49")
    }

    private fun createOffer(vararg offerDeliveryOptions: OfferDeliveryOption): ProductOffer {
        return productOfferTestInstance(
            offer = offerTestInstance(
                deliveryOptions = offerDeliveryOptions.toList(),
                prices = offerPrices,
                isEdaDelivery = false,
                features = emptySet()
            )
        )
    }

    @Suppress("SameParameterValue")
    private fun createDeliveryOption(type: DeliveryType, price: BigDecimal): OfferDeliveryOption {
        return offerDeliveryOptionTestInstance(
            type,
            conditions = deliveryConditions_WithPriceTestInstance(
                price = moneyTestInstance(
                    amount = amountTestInstance(price),
                    currency = Currency.RUR
                )
            )
        )
    }

    companion object {
        private lateinit var typeFaceUtils: MockedStatic<TypefaceUtils>

        @JvmStatic
        @BeforeClass
        fun setUp() {
            typeFaceUtils = Mockito.mockStatic(TypefaceUtils::class.java)
            typeFaceUtils.`when`<Any> { TypefaceUtils.getBoldSpan(any<AssetManager>()) }
                .thenReturn(CalligraphyTypefaceSpan(mock<Typeface>()))
        }

        @AfterClass
        fun close() {
            typeFaceUtils.close()
        }
    }
}
