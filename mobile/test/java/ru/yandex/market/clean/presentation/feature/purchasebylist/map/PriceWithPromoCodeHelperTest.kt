package ru.yandex.market.clean.presentation.feature.purchasebylist.map

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.checkout.domain.model.CheckoutPromo
import ru.yandex.market.clean.domain.model.purchasebylist.MedicineOffer
import ru.yandex.market.clean.domain.model.purchasebylist.delivery.MedicinePurchaseByListBucket
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.PriceWithPromoCodeHelper
import ru.yandex.market.clean.presentation.formatter.MedicineOfferFormatter
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.parcelable.money.MoneyParcelable
import ru.yandex.market.clean.presentation.parcelable.promo.SimplePromoParcelable
import ru.yandex.market.clean.presentation.vo.MedicineOfferVo
import ru.yandex.market.clean.presentation.vo.ParentScreen
import ru.yandex.market.data.order.options.PromoType
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.utils.Characters
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

/**
 * 1. Проверить форматирование цены на сниппете. Не пишется двойная цены, промокод два раза не считается
 * 2. Проверить форматирование цены на сниппете с 2шт. Не пишется двойная цены, промокод два раза не считается
 * 3. Два товара, оба с 1шт. Цена кнопки курьера на карте == цене кнопки курьера на enrich
 * 4. Два товара, один с 1шт, другой 2шт. Цена кнопки курьера на карте == цене кнопки курьера на enrich
 * 5. Несколько бакетов
 * 6. Проверить форматирование кнопки курьера на карте с промокодами
 * 7. Проверить форматирование кнопки курьера на карте без промокодов
 * 8. Проверить форматирование кнопки курьера на enrich с промокодами
 * 9. Проверить форматирование кнопки курьера на enrich без промокодов
 */
class PriceWithPromoCodeHelperTest {

    private val moneyFormatter = mock<MoneyFormatter> {
        on {
            formatAsMoneyVo(
                amount = FIRST_OFFER_BASE_PRICE - FIRST_OFFER_PROMO_CODE_PRICE,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn FIRST_OFFER_TOTAL_PRICE_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = SECOND_OFFER_BASE_PRICE - SECOND_OFFER_PROMO_CODE_PRICE,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn SECOND_OFFER_TOTAL_PRICE_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = TOTAL_PRICE_WITH_PROMO_CODES,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn TOTAL_PRICE_WITH_PROMO_CODES_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = TOTAL_PRICE_WITH_PROMO_CODES_DOUBLED,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn TOTAL_PRICE_WITH_PROMO_CODES_DOUBLED_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = TWO_BUCKETS_TOTAL_PRICE_WITH_PROMO_CODES,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn TWO_BUCKETS_TOTAL_PRICE_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = TOTAL_PRICE,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn BASE_MONEY_VO.copy(amount = TOTAL_PRICE.toString())
    }
    private val medicineOfferFormatter = mock<MedicineOfferFormatter> {
        on {
            format(
                offer = FIRST_BUCKET_OFFER,
                promos = FIRST_OFFER_CHECKOUT_PROMOS,
                cartCount = FIRST_OFFER.itemsFromCartInStock,
                isPrescriptionOffer = false,
                parentScreen = ParentScreen.COURIER,
                cashBackInfo = null,
            )
        } doReturn FIRST_OFFER

        on {
            format(
                offer = FIRST_BUCKET_OFFER.copy(count = 2),
                promos = FIRST_OFFER_CHECKOUT_PROMOS,
                cartCount = 2,
                isPrescriptionOffer = false,
                parentScreen = ParentScreen.COURIER,
                cashBackInfo = null,
            )
        } doReturn FIRST_OFFER.copy(itemsFromCartInStock = 2, count = "2шт", cartCount = 2)

        on {
            format(
                offer = SECOND_BUCKET_OFFER,
                promos = SECOND_OFFER_CHECKOUT_PROMOS,
                cartCount = SECOND_OFFER.itemsFromCartInStock,
                isPrescriptionOffer = false,
                parentScreen = ParentScreen.COURIER,
                cashBackInfo = null,
            )
        } doReturn SECOND_OFFER

        on {
            format(
                offer = THIRD_BUCKET_OFFER,
                promos = THIRD_OFFER_CHECKOUT_PROMOS,
                cartCount = THIRD_OFFER.itemsFromCartInStock,
                isPrescriptionOffer = false,
                parentScreen = ParentScreen.COURIER,
                cashBackInfo = null,
            )
        } doReturn THIRD_OFFER
    }

    private val formatter = PriceWithPromoCodeHelper(moneyFormatter, medicineOfferFormatter)

    /**
     * Проверить форматирование цены на сниппете. Не пишется двойная цены, промокод два раза не считается
     */
    @Test
    fun `Check snippet price formatting`() {
        val result = formatter.calculateMedicineOffersPriceWithPromoCode(
            listOf(FIRST_OFFER)
        )
        assertThat(result.amount).isEqualTo((FIRST_OFFER_BASE_PRICE - FIRST_OFFER_PROMO_CODE_PRICE).toString())
    }

    /**
     * Проверить форматирование цены на сниппете с 2шт. Не пишется двойная цены, промокод два раза не считается
     */
    @Test
    fun `Check snippet price with few quantity offer - do not change result amount`() {
        val result = formatter.calculateMedicineOffersPriceWithPromoCode(
            listOf(
                FIRST_OFFER.copy(
                    count = "2шт",
                    cartCount = 2,
                    itemsFromCartInStock = 2,
                )
            )
        )
        assertThat(result.amount).isEqualTo((FIRST_OFFER_BASE_PRICE - FIRST_OFFER_PROMO_CODE_PRICE).toString())
    }

    /**
     * Два товара, оба с 1шт. Цена кнопки курьера на карте == цене кнопки курьера на enrich
     */
    @Test
    fun `Check map courier button price equals enrich courier button price - single quantity offers`() {
        val mapCourierButtonPrice = formatter.calculateCourierButtonPrice(
            isPromoEnabled = true,
            buckets = BUCKETS_WITH_TWO_OFFERS
        )
        val offersTotalPrice = formatter.calculateOffersTotalPrice(listOf(FIRST_OFFER, SECOND_OFFER))
        val enrichCourierButtonPrice = formatter.calculateMedicineOffersPriceWithPromoCodeAndAmount(
            offersTotalPrice = offersTotalPrice,
            medicineOffersVo = listOf(FIRST_OFFER, SECOND_OFFER)
        )

        assertThat(mapCourierButtonPrice).isEqualTo(enrichCourierButtonPrice.amount.toBigDecimal())
    }

    /**
     * Два товара, один с 1шт, другой 2шт. Цена кнопки курьера на карте == цене кнопки курьера на enrich
     */
    @Test
    fun `Check map courier button price equals enrich courier button price - few quantity offers`() {
        val buckets = BUCKETS_WITH_TWO_OFFERS.first().copy(
            offers = listOf(
                FIRST_BUCKET_OFFER.copy(count = 2),
                SECOND_BUCKET_OFFER
            ),
            offersTotalPrice = FIRST_OFFER_BASE_PRICE.times(BigDecimal(2)) + SECOND_OFFER_BASE_PRICE
        )
        val mapCourierButtonPrice = formatter.calculateCourierButtonPrice(
            isPromoEnabled = true,
            buckets = listOf(buckets)
        )
        val offersTotalPrice = formatter.calculateOffersTotalPrice(
            listOf(
                FIRST_OFFER.copy(itemsFromCartInStock = 2),
                SECOND_OFFER
            )
        )
        val enrichCourierButtonPrice = formatter.calculateMedicineOffersPriceWithPromoCodeAndAmount(
            offersTotalPrice = offersTotalPrice,
            medicineOffersVo = listOf(
                FIRST_OFFER.copy(itemsFromCartInStock = 2),
                SECOND_OFFER
            )
        )

        assertThat(mapCourierButtonPrice).isEqualTo(enrichCourierButtonPrice.amount.toBigDecimal())
    }

    /**
     * Несколько бакетов
     */
    @Test
    fun `Check few buckets button price`() {
        val mapCourierButtonPrice = formatter.calculateCourierButtonPrice(
            isPromoEnabled = true,
            buckets = TWO_BUCKETS
        )
        val offersTotalPrice = formatter.calculateOffersTotalPrice(
            listOf(
                FIRST_OFFER,
                SECOND_OFFER,
                THIRD_OFFER,
            )
        )
        val enrichCourierButtonPrice = formatter.calculateMedicineOffersPriceWithPromoCodeAndAmount(
            offersTotalPrice = offersTotalPrice,
            medicineOffersVo = listOf(
                FIRST_OFFER,
                SECOND_OFFER,
                THIRD_OFFER,
            )
        )

        assertThat(mapCourierButtonPrice).isEqualTo(enrichCourierButtonPrice.amount.toBigDecimal())
    }

    /**
     * Проверить форматирование кнопки курьера на карте с промокодами
     */
    @Test
    fun `Check map courier button price formatting with promocodes`() {
        val mapCourierButtonPrice = formatter.calculateCourierButtonPrice(
            isPromoEnabled = true,
            buckets = BUCKETS_WITH_TWO_OFFERS
        )
        assertThat(mapCourierButtonPrice).isEqualTo(TOTAL_PRICE_WITH_PROMO_CODES)
    }

    /**
     * Проверить форматирование кнопки курьера на карте без промокодов
     * с включенной фичой промомеханики в покупки списком
     */
    @Test
    fun `Check map courier button price formatting without promocodes`() {
        val mapCourierButtonPrice = formatter.calculateCourierButtonPrice(
            isPromoEnabled = true,
            buckets = listOf(BUCKETS_WITH_TWO_OFFERS.first().copy(promoCodeByMarketSku = emptyMap()))
        )
        assertThat(mapCourierButtonPrice).isEqualTo(FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE)
    }

    /**
     * Проверить форматирование кнопки курьера на enrich с промокодами
     */
    @Test
    fun `Check enrich courier button price formatting with promocodes`() {
        val offersTotalPrice = formatter.calculateOffersTotalPrice(
            listOf(FIRST_OFFER, SECOND_OFFER)
        )
        val enrichCourierButtonPrice = formatter.calculateMedicineOffersPriceWithPromoCodeAndAmount(
            offersTotalPrice = offersTotalPrice,
            medicineOffersVo = listOf(FIRST_OFFER, SECOND_OFFER)
        )
        assertThat(enrichCourierButtonPrice.amount).isEqualTo(TOTAL_PRICE_WITH_PROMO_CODES.toString())
    }

    /**
     * Проверить форматирование кнопки курьера на enrich без промокодов
     */
    @Test
    fun `Check enrich courier button price formatting without promocodes`() {
        val offers = listOf(
            FIRST_OFFER.copy(promos = emptyList()),
            SECOND_OFFER.copy(promos = emptyList())
        )
        val offersTotalPrice = formatter.calculateOffersTotalPrice(offers)
        val enrichCourierButtonPrice = formatter.calculateMedicineOffersPriceWithPromoCodeAndAmount(
            offersTotalPrice = offersTotalPrice,
            medicineOffersVo = offers,
        )
        assertThat(enrichCourierButtonPrice.amount).isEqualTo((FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE).toString())
    }

    private companion object {
        val BASE_MONEY_VO = MoneyVo.ONE_RUBLE
        val BASE_MEDICINE_OFFER = MedicineOfferVo(
            wareId = "",
            title = "",
            image = "",
            count = "1шт",
            cartCount = 1,
            price = MoneyVo.ONE_RUBLE,
            isAbsentOnStore = false,
            hasLessItemsThenExpected = false,
            itemsFromCartInStock = 1,
            marketSku = "",
            shopName = "",
            informer = null,
            parentScreen = ParentScreen.COURIER,
            atcCode = "",
            vendorId = null,
            supplierId = null,
            warehouseId = null,
            persistentOfferId = "",
            promos = listOf(),
            cashBackInfo = null,
        )

        val FIRST_OFFER_BASE_PRICE = BigDecimal(1000)
        val FIRST_OFFER_PROMO_CODE_PRICE = BigDecimal(30)
        val FIRST_OFFER_BASE_PRICE_MONEY_VO = BASE_MONEY_VO.copy(amount = FIRST_OFFER_BASE_PRICE.toString())
        val FIRST_OFFER_TOTAL_PRICE_MONEY_VO = BASE_MONEY_VO.copy(
            amount = (FIRST_OFFER_BASE_PRICE - FIRST_OFFER_PROMO_CODE_PRICE).toString()
        )
        const val FIRST_OFFER_MARKET_SKU = "marketSku1"
        val FIRST_OFFER_PROMOS = listOf(
            SimplePromoParcelable(
                buyerDiscount = MoneyParcelable(amount = FIRST_OFFER_PROMO_CODE_PRICE, currency = Currency.RUR),
                deliveryDiscount = MoneyParcelable(amount = BigDecimal(1), currency = Currency.RUR),
                type = PromoType.MARKET_PROMOCODE,
                marketPromoId = "",
                isPickupPromoCode = false,
                promoCodeName = "promoCodeNameTest",
            )
        )
        val FIRST_OFFER_CHECKOUT_PROMOS = listOf(
            CheckoutPromo.testInstanceSimplePromo().copy(
                buyerDiscount = Money(
                    amount = FIRST_OFFER_PROMO_CODE_PRICE,
                    currency = Currency.RUR
                )
            )
        )
        val FIRST_OFFER = BASE_MEDICINE_OFFER.copy(
            price = FIRST_OFFER_BASE_PRICE_MONEY_VO,
            promos = FIRST_OFFER_PROMOS,
            marketSku = FIRST_OFFER_MARKET_SKU
        )
        val FIRST_BUCKET_OFFER = MedicineOffer(
            wareId = "",
            marketSku = FIRST_OFFER_MARKET_SKU,
            count = FIRST_OFFER.itemsFromCartInStock,
            price = Money.Companion.createRub(FIRST_OFFER_BASE_PRICE),
            title = "",
            image = "",
            shopName = "",
            atcCode = "",
            vendorId = null,
            supplierId = null,
            warehouseId = null,
            persistentOfferId = ""
        )

        val SECOND_OFFER_BASE_PRICE = BigDecimal(500)
        val SECOND_OFFER_PROMO_CODE_PRICE = BigDecimal(15)
        val SECOND_OFFER_BASE_PRICE_MONEY_VO = BASE_MONEY_VO.copy(amount = SECOND_OFFER_BASE_PRICE.toString())
        val SECOND_OFFER_TOTAL_PRICE_MONEY_VO = BASE_MONEY_VO.copy(
            amount = (SECOND_OFFER_BASE_PRICE - SECOND_OFFER_PROMO_CODE_PRICE).toString()
        )
        const val SECOND_OFFER_MARKET_SKU = "marketSku2"
        val SECOND_OFFER_PROMOS = listOf(
            SimplePromoParcelable(
                buyerDiscount = MoneyParcelable(amount = SECOND_OFFER_PROMO_CODE_PRICE, currency = Currency.RUR),
                deliveryDiscount = MoneyParcelable(amount = BigDecimal(1), currency = Currency.RUR),
                type = PromoType.MARKET_PROMOCODE,
                marketPromoId = "",
                isPickupPromoCode = false,
                promoCodeName = "promoCodeNameTest",
            )
        )
        val SECOND_OFFER_CHECKOUT_PROMOS = listOf(
            CheckoutPromo.testInstanceSimplePromo().copy(
                buyerDiscount = Money(
                    amount = SECOND_OFFER_PROMO_CODE_PRICE,
                    currency = Currency.RUR
                )
            )
        )
        val SECOND_OFFER = BASE_MEDICINE_OFFER.copy(
            price = SECOND_OFFER_BASE_PRICE_MONEY_VO,
            promos = SECOND_OFFER_PROMOS,
            marketSku = SECOND_OFFER_MARKET_SKU
        )
        val SECOND_BUCKET_OFFER = MedicineOffer(
            wareId = "",
            marketSku = SECOND_OFFER_MARKET_SKU,
            count = SECOND_OFFER.itemsFromCartInStock,
            price = Money.Companion.createRub(SECOND_OFFER_BASE_PRICE),
            title = "",
            image = "",
            shopName = "",
            atcCode = "",
            vendorId = null,
            supplierId = null,
            warehouseId = null,
            persistentOfferId = ""
        )

        val THIRD_OFFER_BASE_PRICE = BigDecimal(250)
        val THIRD_OFFER_PROMO_CODE_PRICE = BigDecimal(70)
        val THIRD_OFFER_BASE_PRICE_MONEY_VO = BASE_MONEY_VO.copy(amount = THIRD_OFFER_BASE_PRICE.toString())
        const val THIRD_OFFER_MARKET_SKU = "marketSku3"
        val THIRD_OFFER_PROMOS = listOf(
            SimplePromoParcelable(
                buyerDiscount = MoneyParcelable(amount = THIRD_OFFER_PROMO_CODE_PRICE, currency = Currency.RUR),
                deliveryDiscount = MoneyParcelable(amount = BigDecimal(1), currency = Currency.RUR),
                type = PromoType.MARKET_PROMOCODE,
                marketPromoId = "",
                isPickupPromoCode = false,
                promoCodeName = "promoCodeNameTest",
            )
        )
        val THIRD_OFFER_CHECKOUT_PROMOS = listOf(
            CheckoutPromo.testInstanceSimplePromo().copy(
                buyerDiscount = Money(
                    amount = THIRD_OFFER_PROMO_CODE_PRICE,
                    currency = Currency.RUR
                )
            )
        )
        val THIRD_OFFER = BASE_MEDICINE_OFFER.copy(
            price = THIRD_OFFER_BASE_PRICE_MONEY_VO,
            promos = THIRD_OFFER_PROMOS,
            marketSku = THIRD_OFFER_MARKET_SKU
        )
        val THIRD_BUCKET_OFFER = MedicineOffer(
            wareId = "",
            marketSku = THIRD_OFFER_MARKET_SKU,
            count = THIRD_OFFER.itemsFromCartInStock,
            price = Money.Companion.createRub(THIRD_OFFER_BASE_PRICE),
            title = "",
            image = "",
            shopName = "",
            atcCode = "",
            vendorId = null,
            supplierId = null,
            warehouseId = null,
            persistentOfferId = ""
        )

        val TOTAL_PRICE = FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE
        val TOTAL_PROMO_CODES_PRICE = FIRST_OFFER_PROMO_CODE_PRICE + SECOND_OFFER_PROMO_CODE_PRICE
        val TOTAL_PRICE_WITH_PROMO_CODES = TOTAL_PRICE - (TOTAL_PROMO_CODES_PRICE)
        val TOTAL_PRICE_WITH_PROMO_CODES_MONEY_VO = BASE_MONEY_VO.copy(
            amount = TOTAL_PRICE_WITH_PROMO_CODES.toString()
        )

        val TOTAL_PROMO_CODES_FIRST_DOUBLED_PRICE =
            FIRST_OFFER_PROMO_CODE_PRICE + FIRST_OFFER_PROMO_CODE_PRICE + SECOND_OFFER_PROMO_CODE_PRICE
        val TOTAL_PRICE_WITH_PROMO_CODES_DOUBLED =
            TOTAL_PRICE + FIRST_OFFER_BASE_PRICE - TOTAL_PROMO_CODES_FIRST_DOUBLED_PRICE
        val TOTAL_PRICE_WITH_PROMO_CODES_DOUBLED_MONEY_VO = BASE_MONEY_VO.copy(
            amount = TOTAL_PRICE_WITH_PROMO_CODES_DOUBLED.toString()
        )

        val TWO_BUCKETS_TOTAL_PROMO_CODES_PRICE =
            FIRST_OFFER_PROMO_CODE_PRICE + SECOND_OFFER_PROMO_CODE_PRICE + THIRD_OFFER_PROMO_CODE_PRICE
        val TWO_BUCKETS_TOTAL_PRICE = FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE + THIRD_OFFER_BASE_PRICE
        val TWO_BUCKETS_TOTAL_PRICE_WITH_PROMO_CODES = TWO_BUCKETS_TOTAL_PRICE - TWO_BUCKETS_TOTAL_PROMO_CODES_PRICE
        val TWO_BUCKETS_TOTAL_PRICE_MONEY_VO = BASE_MONEY_VO.copy(
            amount = TWO_BUCKETS_TOTAL_PRICE_WITH_PROMO_CODES.toString()
        )

        val BUCKETS_WITH_TWO_OFFERS = listOf(
            MedicinePurchaseByListBucket(
                deliveryTotalPrice = BigDecimal(99),
                chipsDeliveryTotalPrice = BigDecimal(99),
                offers = listOf(FIRST_BUCKET_OFFER, SECOND_BUCKET_OFFER),
                offersTotalPrice = FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE,
                shopId = 0,
                promoCodeByMarketSku = mapOf(
                    FIRST_OFFER_MARKET_SKU to FIRST_OFFER_CHECKOUT_PROMOS,
                    SECOND_OFFER_MARKET_SKU to SECOND_OFFER_CHECKOUT_PROMOS,
                ),
                totalCashBack = BigDecimal.ZERO,
                cashBackByMarketSku = emptyMap(),
                deliveryIntervals = emptyList(),
                deliveryBeginDate = Date.from(Instant.now()),
            )
        )

        val TWO_BUCKETS = listOf(
            MedicinePurchaseByListBucket(
                deliveryTotalPrice = BigDecimal(99),
                chipsDeliveryTotalPrice = BigDecimal(99),
                offers = listOf(FIRST_BUCKET_OFFER, SECOND_BUCKET_OFFER),
                offersTotalPrice = FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE,
                shopId = 1,
                promoCodeByMarketSku = mapOf(
                    FIRST_OFFER_MARKET_SKU to FIRST_OFFER_CHECKOUT_PROMOS,
                    SECOND_OFFER_MARKET_SKU to SECOND_OFFER_CHECKOUT_PROMOS,
                ),
                totalCashBack = BigDecimal.ZERO,
                cashBackByMarketSku = emptyMap(),
                deliveryIntervals = emptyList(),
                deliveryBeginDate = Date.from(Instant.now()),
            ),
            MedicinePurchaseByListBucket(
                deliveryTotalPrice = BigDecimal(99),
                chipsDeliveryTotalPrice = BigDecimal(99),
                offers = listOf(THIRD_BUCKET_OFFER),
                offersTotalPrice = THIRD_OFFER_BASE_PRICE,
                shopId = 2,
                promoCodeByMarketSku = mapOf(
                    THIRD_OFFER_MARKET_SKU to THIRD_OFFER_CHECKOUT_PROMOS,
                ),
                totalCashBack = BigDecimal.ZERO,
                cashBackByMarketSku = emptyMap(),
                deliveryIntervals = emptyList(),
                deliveryBeginDate = Date.from(Instant.now()),
            )
        )
    }
}
