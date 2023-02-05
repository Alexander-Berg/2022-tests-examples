package ru.yandex.market.clean.presentation.feature.purchasebylist.map.enrich

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.clean.domain.model.purchasebylist.DeliveryPeriod
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.PriceWithPromoCodeHelper
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.PurchaseByListButtonWithTextSpannableFormatter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.courier.DeliveryPeriodToReadableNameFormatter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.enrich.MedicineEnrichAddressVoFormatter
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.parcelable.money.MoneyParcelable
import ru.yandex.market.clean.presentation.parcelable.promo.SimplePromoParcelable
import ru.yandex.market.clean.presentation.vo.MedicineOfferVo
import ru.yandex.market.clean.presentation.vo.ParentScreen
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.data.order.options.PromoType
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.feature.money.formatter.MoneyFormatterResult
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.optional.Optional
import ru.yandex.market.utils.Characters
import ru.yandex.market.utils.orNull
import java.math.BigDecimal

class MedicineEnrichAddressVoFormatterTest {

    private val resourcesManager = mock<ResourcesManager> {
        on {
            getFormattedString(
                R.string.item_pack_delivery_with_date,
                DELIVERY_BY_COURIER,
                IN_ONE_TWO_HOURS.lowercase(),
                DELIVERY_OPTION_DATE_FREE
            )
        } doReturn EXPRESS_FREE_TITLE

        on {
            getFormattedString(
                R.string.item_pack_delivery_with_date,
                DELIVERY_BY_COURIER,
                IN_ONE_TWO_HOURS.lowercase(),
                "за $DELIVERY_OPTION_PAID"
            )
        } doReturn EXPRESS_PAID_TITLE

        on {
            getFormattedString(
                R.string.item_pack_delivery_with_date,
                DELIVERY_BY_COURIER,
                ARRIVE_TODAY.lowercase(),
                DELIVERY_OPTION_DATE_FREE
            )
        } doReturn TODAY_FREE_TITLE

        on {
            getFormattedString(
                R.string.item_pack_delivery_with_date,
                DELIVERY_BY_COURIER,
                ARRIVE_TODAY.lowercase(),
                "за $DELIVERY_OPTION_PAID"
            )
        } doReturn TODAY_PAID_TITLE

        on {
            getFormattedString(
                R.string.purchase_by_list_delivery_period_price_text,
                FIRST_OFFER_BASE_PRICE_MONEY_VO.getFormatted(),
                BASE_MONEY_VO_ZERO.getFormatted(),
            )
        } doReturn BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY

        on {
            getFormattedString(
                R.string.purchase_by_list_delivery_period_price_text,
                FIRST_OFFER_BASE_PRICE_MONEY_VO.getFormatted(),
                DELIVERY_OPTION_PAID,
            )
        } doReturn BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY

        on {
            getFormattedString(
                R.string.purchase_by_list_delivery_period_price_text,
                FIRST_SECOND_OFFER_BASE_PRICE_MONEY_VO.getFormatted(),
                BASE_MONEY_VO_ZERO.getFormatted(),
            )
        } doReturn BUTTON_PRICE_TEXT_FEW_OFFERS_FREE_DELIVERY

        on {
            getFormattedString(
                R.string.purchase_by_list_delivery_period_price_text,
                FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS_MONEY_VO.getFormatted(),
                BASE_MONEY_VO_ZERO.getFormatted(),
            )
        } doReturn BUTTON_PRICE_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY

        on {
            getFormattedString(
                R.string.purchase_by_list_delivery_period_price_text,
                FIRST_OFFER_TOTAL_PRICE_MONEY_VO.getFormatted(),
                BASE_MONEY_VO_ZERO.getFormatted(),
            )
        } doReturn BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_WITH_PROMOS

        on {
            getString(R.string.delivery_by_courier)
        } doReturn DELIVERY_BY_COURIER

        on {
            getString(R.string.delivery_option_date_free)
        } doReturn DELIVERY_OPTION_DATE_FREE

        on {
            getFormattedString(
                R.string.item_pack_delivery_price,
                DELIVERY_OPTION_PAID
            )
        } doReturn "за $DELIVERY_OPTION_PAID"
    }

    private val moneyFormatter = mock<MoneyFormatter> {
        on {
            formatAsMoneyVo(
                amount = FIRST_OFFER_BASE_PRICE,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn FIRST_OFFER_BASE_PRICE_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = FIRST_SECOND_OFFER_BASE_PRICE_NO_PROMOS,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn FIRST_SECOND_OFFER_BASE_PRICE_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false,
            )
        } doReturn FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS_MONEY_VO

        on {
            formatAsMoneyVo(
                amount = BigDecimal.ZERO,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = true,
            )
        } doReturn BASE_MONEY_VO_ZERO

        on {
            formatAsMoneyVo(
                amount = BigDecimal(99),
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = true,
            )
        } doReturn MoneyVo.ONE_RUBLE.copy(amount = "99")

        on {
            formatDeliveryPriceWithResult(
                Money.Companion.createRub(BigDecimal.ZERO)
            )
        } doReturn MoneyFormatterResult(DELIVERY_OPTION_DATE_FREE, Money.Companion.createRub(BigDecimal.ZERO))

        on {
            formatDeliveryPriceWithResult(
                Money.Companion.createRub(BigDecimal(99))
            )
        } doReturn MoneyFormatterResult(DELIVERY_OPTION_PAID, Money.Companion.createRub(BigDecimal(99)))
    }
    private val buttonWithTextSpannableFormatter = mock<PurchaseByListButtonWithTextSpannableFormatter> {
        on {
            format(
                R.string.bring_here_with_time_and_price,
                IN_ONE_TWO_HOURS.lowercase(),
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY
            )
        } doReturn BUTTON_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_EXPRESS

        on {
            format(
                R.string.bring_here_with_time_and_price,
                IN_ONE_TWO_HOURS.lowercase(),
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY
            )
        } doReturn BUTTON_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY_EXPRESS

        on {
            format(
                R.string.bring_here_with_time_and_price,
                IN_ONE_TWO_HOURS.lowercase(),
                BUTTON_PRICE_TEXT_FEW_OFFERS_FREE_DELIVERY
            )
        } doReturn BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_EXPRESS

        on {
            format(
                R.string.bring_here_with_time_and_price,
                IN_ONE_TWO_HOURS.lowercase(),
                BUTTON_PRICE_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY
            )
        } doReturn BUTTON_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY_EXPRESS

        on {
            format(
                R.string.bring_here_with_time_and_price,
                ARRIVE_TODAY.lowercase(),
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY
            )
        } doReturn BUTTON_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_TODAY

        on {
            format(
                R.string.bring_here_with_time_and_price,
                ARRIVE_TODAY.lowercase(),
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY
            )
        } doReturn BUTTON_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY_TODAY

        on {
            format(
                R.string.bring_here_with_time_and_price,
                IN_ONE_TWO_HOURS.lowercase(),
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_WITH_PROMOS,
            )
        } doReturn BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER_WITH_PROMOS

        on {
            format(
                R.string.checkout_partial_buy,
                "1",
                "2",
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY,
            )
        } doReturn BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER

        on {
            format(
                R.string.checkout_partial_buy,
                "1",
                "2",
                BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_WITH_PROMOS,
            )
        } doReturn BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER_WITH_PROMOS
    }
    private val deliveryPeriodFormatter = mock<DeliveryPeriodToReadableNameFormatter> {
        on {
            format(
                deliveryPeriod = DeliveryPeriod.EXPRESS,
                deliveryPeriodOtherName = R.string.here,
                deliveryPeriodNullName = R.string.here
            )
        } doReturn IN_ONE_TWO_HOURS.lowercase()

        on {
            format(deliveryPeriod = Optional.ofNullable(DeliveryPeriod.EXPRESS).orNull)
        } doReturn IN_ONE_TWO_HOURS.lowercase()

        on {
            format(
                deliveryPeriod = DeliveryPeriod.TODAY,
                deliveryPeriodOtherName = R.string.here,
                deliveryPeriodNullName = R.string.here
            )
        } doReturn ARRIVE_TODAY.lowercase()

        on {
            format(deliveryPeriod = Optional.ofNullable(DeliveryPeriod.TODAY).orNull)
        } doReturn ARRIVE_TODAY.lowercase()
    }
    private val priceWithPromoCodeHelper = mock<PriceWithPromoCodeHelper> {
        on {
            calculateOffersTotalPrice(listOf(FIRST_OFFER_NO_PROMOS, SECOND_OFFER_NO_PROMOS))
        } doReturn FIRST_SECOND_OFFER_BASE_PRICE_NO_PROMOS

        on {
            calculateOffersTotalPrice(listOf(FIRST_OFFER))
        } doReturn FIRST_OFFER_BASE_PRICE

        on {
            calculateMedicineOffersPriceWithPromoCodeAndAmount(
                offersTotalPrice = FIRST_OFFER_BASE_PRICE,
                medicineOffersVo = listOf(FIRST_OFFER)
            )
        } doReturn FIRST_OFFER_TOTAL_PRICE_MONEY_VO

        on {
            calculateMedicineOffersPriceWithPromoCodeAndAmount(
                offersTotalPrice = FIRST_SECOND_OFFER_BASE_PRICE_NO_PROMOS,
                medicineOffersVo = listOf(FIRST_OFFER_NO_PROMOS, SECOND_OFFER_NO_PROMOS)
            )
        } doReturn FIRST_SECOND_OFFER_BASE_PRICE_MONEY_VO

        on {
            calculateOffersTotalPrice(listOf(FIRST_OFFER, SECOND_OFFER))
        } doReturn FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS

        on {
            calculateMedicineOffersPriceWithPromoCodeAndAmount(
                offersTotalPrice = FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS,
                medicineOffersVo = listOf(FIRST_OFFER, SECOND_OFFER)
            )
        } doReturn FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS_MONEY_VO

        on {
            calculateOffersTotalPrice(
                listOf(
                    FIRST_OFFER_NO_PROMOS,
                    SECOND_OFFER_NO_PROMOS.copy(itemsFromCartInStock = 0)
                )
            )
        } doReturn FIRST_OFFER_BASE_PRICE

        on {
            calculateMedicineOffersPriceWithPromoCodeAndAmount(
                offersTotalPrice = FIRST_OFFER_BASE_PRICE,
                medicineOffersVo = listOf(
                    FIRST_OFFER_NO_PROMOS,
                    SECOND_OFFER_NO_PROMOS.copy(itemsFromCartInStock = 0)
                )
            )
        } doReturn BASE_MONEY_VO.copy(amount = FIRST_OFFER_BASE_PRICE.toString())

        on {
            calculateOffersTotalPrice(
                listOf(
                    FIRST_OFFER,
                    SECOND_OFFER.copy(itemsFromCartInStock = 0)
                )
            )
        } doReturn FIRST_OFFER_TOTAL_PRICE_AMOUNT

        on {
            calculateMedicineOffersPriceWithPromoCodeAndAmount(
                offersTotalPrice = FIRST_OFFER_TOTAL_PRICE_AMOUNT,
                medicineOffersVo = listOf(
                    FIRST_OFFER,
                    SECOND_OFFER.copy(itemsFromCartInStock = 0)
                )
            )
        } doReturn BASE_MONEY_VO.copy(amount = FIRST_OFFER_TOTAL_PRICE_AMOUNT.toString())
    }

    private val timeFormatter = mock<TimeFormatter>()
    private val dateFormatter = mock<DateFormatter>()
    private val dateTimeProvider  = mock<DateTimeProvider>()

    private val formatter = MedicineEnrichAddressVoFormatter(
        resourcesManager = resourcesManager,
        moneyFormatter = moneyFormatter,
        buttonWithTextSpannableFormatter = buttonWithTextSpannableFormatter,
        deliveryPeriodFormatter = deliveryPeriodFormatter,
        priceWithPromoCodeHelper = priceWithPromoCodeHelper,
        timeFormatter = timeFormatter,
        dateFormatter = dateFormatter,
        dateTimeProvider = dateTimeProvider,
    )

    /**
     * Выбрана доставка через 1-2 часа
     * с бесплатной доставкой курьером
     * Проверить название попапа: Доставка курьером через 1-2 часа бесплатно
     */
    @Test
    fun `Check express and free popup title`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.title).isEqualTo(EXPRESS_FREE_TITLE)
    }

    /**
     * Выбрана доставка через 1-2 часа
     * с бесплатной доставкой курьером
     * Фича промомеханики в Покупки списком ВКЛЮЧЕНА
     * Проверить название попапа: Доставка курьером через 1-2 часа бесплатно
     */
    @Test
    fun `Check express and free popup title with isPromoEnabled = true`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = true,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.title).isEqualTo(EXPRESS_FREE_TITLE)
    }

    /**
     * Выбрана доставка через 1-2 часа
     * с доставкой за 99 рублей курьером
     * Проверить название попапа: Доставка курьером через 1-2 часа за 99Р
     */
    @Test
    fun `Check express and paid popup title`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER),
            deliveryPrice = BigDecimal(99),
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.title).isEqualTo(EXPRESS_PAID_TITLE)
    }

    /**
     * Выбрана доставка сегодня
     * с бесплатной доставкой курьером
     * Проверить название попапа: Доставка курьером сегодня, бесплатно
     */
    @Test
    fun `Check today and free popup title`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.TODAY,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.title).isEqualTo(TODAY_FREE_TITLE)
    }

    /**
     * Выбрана доставка сегодня
     * с доставкой за 99 рублей курьером
     * Проверить название попапа: Доставка курьером сегодня за 99Р
     */
    @Test
    fun `Check today and paid popup title`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER),
            deliveryPrice = BigDecimal(99),
            deliveryPeriod = DeliveryPeriod.TODAY,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.title).isEqualTo(TODAY_PAID_TITLE)
    }

    /**
     * Полная корзина
     * Оффера без промокодов, то есть не применили в корзине промокод
     * Бесплатная доставка
     * Фича промомеханики в ПС включена
     *
     * На кнопке написано:
     * ```
     * Привезти 'период доставки'
     *    'цена корзины' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Full cart, no promos, free delivery`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER_NO_PROMOS, SECOND_OFFER_NO_PROMOS),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = true,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_EXPRESS)
    }

    /**
     * Полная корзина
     * Оффера с промокодами
     * Бесплатная доставка
     * Фича промомеханики в ПС включена
     *
     * На кнопке написано:
     * ```
     * Привезти 'период доставки'
     *    'цена корзины - промокода' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Full cart, with promos, free delivery`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER, SECOND_OFFER),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = true,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY_EXPRESS)
    }

    /**
     * Полная корзина
     * С промокодом
     * Бесплатная доставка
     * Фича промомеханики в ПС выключена
     *
     * На кнопке написано:
     * ```
     * Привезти 'период доставки'
     *    'цена корзины' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Full cart, with promos, free delivery, promo toggle is off`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER, SECOND_OFFER),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_EXPRESS)
    }

    /**
     * Полная корзина
     * Без промокодов
     * Бесплатная доставка
     * Фича промомеханики в ПС выключена
     *
     * На кнопке написано:
     * ```
     * Привезти 'период доставки'
     *    'цена корзины' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Full cart, no promos, free delivery, promo toggle is off`() {
        val result = formatter.format(
            offers = listOf(FIRST_OFFER_NO_PROMOS, SECOND_OFFER_NO_PROMOS),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_EXPRESS)
    }

    /**
     * Частичная корзина
     * Без промокода
     * Бесплатная доставка
     *
     * На кнопке написано:
     * ```
     * Привезти a и b товаров
     *    'цена корзины' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Partial cart, no promos, free delivery`() {
        val result = formatter.format(
            offers = listOf(
                FIRST_OFFER_NO_PROMOS,
                SECOND_OFFER_NO_PROMOS.copy(itemsFromCartInStock = 0)
            ),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = true,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER)
    }

    /**
     * Частичная корзина
     * С промокодами
     * Бесплатная доставка
     *
     * На кнопке написано:
     * ```
     * Привезти a и b товаров
     *    'цена корзины - промокода' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Partial cart, with promos, free delivery`() {
        val result = formatter.format(
            offers = listOf(
                FIRST_OFFER,
                SECOND_OFFER.copy(itemsFromCartInStock = 0)
            ),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = true,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER_WITH_PROMOS)
    }

    /**
     * Частичная корзина
     * С промокодом
     * Бесплатная доставка
     * Фича промомеханики в ПС выключена
     *
     * На кнопке написано:
     * ```
     * Привезти a и b товаров
     *    'цена корзины' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Partial cart, with promos, free delivery, promo toggle is off`() {
        val result = formatter.format(
            offers = listOf(
                FIRST_OFFER,
                SECOND_OFFER.copy(itemsFromCartInStock = 0)
            ),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER)
    }

    /**
     * Частичная корзина
     * Без промокодов
     * Бесплатная доставка
     * Фича промомеханики в ПС выключена
     *
     * На кнопке написано:
     * ```
     * Привезти a и b товаров
     *    'цена корзины' за 'цена доставки'
     * ```
     */
    @Test
    fun `Check button text - Partial cart, no promos, free delivery, promo toggle is off`() {
        val result = formatter.format(
            offers = listOf(
                FIRST_OFFER_NO_PROMOS,
                SECOND_OFFER_NO_PROMOS.copy(itemsFromCartInStock = 0)
            ),
            deliveryPrice = BigDecimal.ZERO,
            deliveryPeriod = DeliveryPeriod.EXPRESS,
            isPromoEnabled = false,
            isBookingEnabled = false,
            deliveryIntervals = emptyList(),
            deliveryBeginDates = emptyList(),
        )

        assertThat(result.buttonString).isEqualTo(BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER)
    }

    private companion object {
        const val DELIVERY_BY_COURIER = "Доставка курьером"
        const val IN_ONE_TWO_HOURS = "Через 1${Characters.EN_DASH}2 часа"
        const val ARRIVE_TODAY = "сегодня"
        const val DELIVERY_OPTION_DATE_FREE = "бесплатно"
        val DELIVERY_OPTION_PAID = MoneyVo.ONE_RUBLE.copy(amount = "99").getFormatted()

        const val EXPRESS_FREE_TITLE = "$DELIVERY_BY_COURIER $IN_ONE_TWO_HOURS $DELIVERY_OPTION_DATE_FREE"
        val EXPRESS_PAID_TITLE = "$DELIVERY_BY_COURIER $IN_ONE_TWO_HOURS за $DELIVERY_OPTION_PAID"
        const val TODAY_FREE_TITLE = "$DELIVERY_BY_COURIER $ARRIVE_TODAY $DELIVERY_OPTION_DATE_FREE"
        const val TODAY_PAID_TITLE = "$DELIVERY_BY_COURIER $ARRIVE_TODAY за $DELIVERY_OPTION_DATE_FREE"

        val BASE_MONEY_VO = MoneyVo.ONE_RUBLE
        val BASE_MONEY_VO_ZERO = MoneyVo.ONE_RUBLE.copy(amount = "0")
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
        val FIRST_OFFER_TOTAL_PRICE_AMOUNT = FIRST_OFFER_BASE_PRICE - FIRST_OFFER_PROMO_CODE_PRICE
        val FIRST_OFFER_TOTAL_PRICE_MONEY_VO = BASE_MONEY_VO.copy(
            amount = FIRST_OFFER_TOTAL_PRICE_AMOUNT.toString()
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
        val FIRST_OFFER = BASE_MEDICINE_OFFER.copy(
            price = FIRST_OFFER_BASE_PRICE_MONEY_VO,
            promos = FIRST_OFFER_PROMOS,
            marketSku = FIRST_OFFER_MARKET_SKU
        )
        val FIRST_OFFER_NO_PROMOS = BASE_MEDICINE_OFFER.copy(
            price = FIRST_OFFER_BASE_PRICE_MONEY_VO,
            marketSku = FIRST_OFFER_MARKET_SKU
        )

        val SECOND_OFFER_BASE_PRICE = BigDecimal(500)
        val SECOND_OFFER_PROMO_CODE_PRICE = BigDecimal(15)
        val SECOND_OFFER_BASE_PRICE_MONEY_VO = BASE_MONEY_VO.copy(amount = SECOND_OFFER_BASE_PRICE.toString())
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
        val SECOND_OFFER = BASE_MEDICINE_OFFER.copy(
            price = SECOND_OFFER_BASE_PRICE_MONEY_VO,
            promos = SECOND_OFFER_PROMOS,
            marketSku = SECOND_OFFER_MARKET_SKU
        )
        val SECOND_OFFER_NO_PROMOS = BASE_MEDICINE_OFFER.copy(
            price = SECOND_OFFER_BASE_PRICE_MONEY_VO,
            marketSku = SECOND_OFFER_MARKET_SKU
        )

        val FIRST_SECOND_OFFER_BASE_PRICE_NO_PROMOS = FIRST_OFFER_BASE_PRICE + SECOND_OFFER_BASE_PRICE
        val FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS =
            FIRST_SECOND_OFFER_BASE_PRICE_NO_PROMOS - FIRST_OFFER_PROMO_CODE_PRICE - SECOND_OFFER_PROMO_CODE_PRICE
        val FIRST_SECOND_OFFER_BASE_PRICE_MONEY_VO = MoneyVo.ONE_RUBLE.copy(
            amount = FIRST_SECOND_OFFER_BASE_PRICE_NO_PROMOS.toString()
        )
        val FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS_MONEY_VO = MoneyVo.ONE_RUBLE.copy(
            amount = FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS.toString()
        )

        val BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY =
            "${FIRST_OFFER_BASE_PRICE_MONEY_VO.getFormatted()} за ${BASE_MONEY_VO_ZERO.getFormatted()}"

        val BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_WITH_PROMOS =
            "${FIRST_OFFER_TOTAL_PRICE_MONEY_VO.getFormatted()} за ${BASE_MONEY_VO_ZERO.getFormatted()}"

        val BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY =
            "${FIRST_OFFER_BASE_PRICE_MONEY_VO.getFormatted()} за $DELIVERY_OPTION_PAID"

        val BUTTON_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_EXPRESS =
            "Привезти $${IN_ONE_TWO_HOURS.lowercase()}\n$BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY"

        val BUTTON_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_TODAY =
            "Привезти $${ARRIVE_TODAY.lowercase()}\n$BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY"

        val BUTTON_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY_TODAY =
            "Привезти $${ARRIVE_TODAY.lowercase()}\n$BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY"

        val BUTTON_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY_EXPRESS =
            "Привезти $${IN_ONE_TWO_HOURS.lowercase()}\n$BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_PAID_DELIVERY"

        val BUTTON_PRICE_TEXT_FEW_OFFERS_FREE_DELIVERY =
            "${FIRST_SECOND_OFFER_BASE_PRICE_MONEY_VO.getFormatted()} за ${BASE_MONEY_VO_ZERO.getFormatted()}"

        val BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_EXPRESS =
            "Привезти $${IN_ONE_TWO_HOURS.lowercase()}\n$BUTTON_PRICE_TEXT_FEW_OFFERS_FREE_DELIVERY"

        val BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER =
            "Привезти 1 из 2 товаров\n$BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY"

        val BUTTON_TEXT_FEW_OFFERS_FREE_DELIVERY_PARTIAL_FIRST_OFFER_WITH_PROMOS =
            "Привезти 1 из 2 товаров\n$BUTTON_PRICE_TEXT_ONLY_FIRST_OFFER_FREE_DELIVERY_WITH_PROMOS"

        val BUTTON_PRICE_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY =
            "${FIRST_SECOND_OFFER_BASE_PRICE_WITH_PROMOS_MONEY_VO.getFormatted()} за ${BASE_MONEY_VO_ZERO.getFormatted()}"

        val BUTTON_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY_EXPRESS =
            "Привезти $${IN_ONE_TWO_HOURS.lowercase()}\n$BUTTON_PRICE_TEXT_FEW_OFFERS_WITH_PROMOS_FREE_DELIVERY"
    }
}
