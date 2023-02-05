package ru.yandex.market.clean.presentation.feature.lavket.formatter

import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.clean.domain.model.lavka2.LavkaJuridicalInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfoStatus
import ru.yandex.market.clean.domain.model.lavka2.cart.LavkaCart
import ru.yandex.market.clean.domain.model.lavka2.cart.LavkaDeliveryInfo
import ru.yandex.market.clean.presentation.feature.lavka.serviceinfo.DeliveryInformationBarServiceInfoVo
import ru.yandex.market.clean.presentation.feature.lavka.view.DeliveryInformationBarVo
import ru.yandex.market.uikit.model.LavkaJuridicalInfoVo
import java.util.Date

object LavkaServiceInfoFormatterTestEntity {

    private const val SEPARATOR = "\u00A0·\u00A0"

    private val timeFormatter = TimeFormatter()
    val AVAILABLE_DATE = Date(1600000000000)
    val FIRST_DATE = Date(0)
    val SECOND_DATE = Date(1)
    private val AVAILABLE_DATE_SHORT_FORMAT = timeFormatter.formatShort(AVAILABLE_DATE)
    private val FIRST_DATE_SHORT_FORMAT = timeFormatter.formatShort(FIRST_DATE)
    private val SECOND_DATE_SHORT_FORMAT = timeFormatter.formatShort(SECOND_DATE)

    private const val LAVKA_CART_WILL_OPEN_AT = "Откроемся в"
    private const val LAVKA_CART_WILL_OPEN_AT_UNKNOWN = "Лавка закрыта до утра"
    private const val LAVKA_CART_UNAVAILABLE = "Лавка не работает, что-то сломалось"
    private const val LAVKA_SERVICE_INFO_ETA = "Курьер приедет за"
    private const val LAVKA_SERVICE_INFO_AVAILABILITY = "Режим работы:"
    private val LAVKA_CART_WILL_OPEN_AT_AVAILABLE_DATE = "$LAVKA_CART_WILL_OPEN_AT $AVAILABLE_DATE_SHORT_FORMAT"
    private val LAVKA_SERVICE_INFO_AVAILABILITY_WITH_DATES =
        "$LAVKA_SERVICE_INFO_AVAILABILITY $FIRST_DATE_SHORT_FORMAT - $SECOND_DATE_SHORT_FORMAT"

    private const val DELIVERY_TEXT = "Доставка 0 Р"
    private const val ETA = "5–15 мин"
    private const val IS_SURGE = false

    private val REWARD_BLOCKS = emptyList<Pair<String, String>>()

    private const val ADDRESS = "some address"
    private const val PARTNER = "some partner"

    val LAVKA_JURIDICAL_INFO = LavkaJuridicalInfo(
        address = ADDRESS,
        partner = PARTNER
    )

    val LAVKA_JURIDICAL_INFO_VO = LavkaJuridicalInfoVo(
        address = ADDRESS,
        partnerInfo = PARTNER
    )

    val OPEN_LAVKA_SERVICE_INFO = LavkaServiceInfo(
        status = LavkaServiceInfoStatus.OPEN,
        availableAt = AVAILABLE_DATE,
        deliveryText = DELIVERY_TEXT,
        depotId = null,
        eta = ETA,
        isSurge = IS_SURGE,
        availability = Pair(FIRST_DATE, SECOND_DATE),
        rewardBlocks = REWARD_BLOCKS,
        informers = emptyList(),
        isLavkaNewbie = false,
        juridicalInfo = LAVKA_JURIDICAL_INFO,
        lavkaDiscountInfo = null,
    )
    val CLOSED_LAVKA_SERVICE_INFO = OPEN_LAVKA_SERVICE_INFO.copy(status = LavkaServiceInfoStatus.CLOSED)
    val CLOSED_UNTIL_MORNING_LAVKA_SERVICE_INFO = OPEN_LAVKA_SERVICE_INFO.copy(
        status = LavkaServiceInfoStatus.CLOSED,
        availableAt = null
    )
    val UNKNOWN_LAVKA_SERVICE_INFO = OPEN_LAVKA_SERVICE_INFO.copy(status = LavkaServiceInfoStatus.UNKNOWN)

    private val SERVICE_INFO_VO = DeliveryInformationBarServiceInfoVo(
        eta = "$LAVKA_SERVICE_INFO_ETA $ETA",
        availability = LAVKA_SERVICE_INFO_AVAILABILITY_WITH_DATES,
        rewardBlocks = REWARD_BLOCKS,
        juridicalInfoVo = LAVKA_JURIDICAL_INFO_VO
    )

    val OPEN_LAVKA_DELIVERY_INFORMATION_VO = DeliveryInformationBarVo(
        deliveryInfo = listOf(
            DELIVERY_TEXT,
            ETA
        ).joinToString(SEPARATOR),
        closedText = null,
        unavailableText = null,
        isSurge = IS_SURGE,
        serviceInfoVo = SERVICE_INFO_VO,
        deliveryPrice = DELIVERY_TEXT,
        orderPrice = "",
        buttonVo = null,
        courierIconType = DeliveryInformationBarVo.CourierIconType.BIKE,
        time = ETA,
    )
    val CLOSED_LAVKA_DELIVERY_INFORMATION_VO =
        OPEN_LAVKA_DELIVERY_INFORMATION_VO.copy(closedText = LAVKA_CART_WILL_OPEN_AT_AVAILABLE_DATE)
    val CLOSED_UNTIL_MORNING_LAVKA_DELIVERY_INFORMATION_VO =
        OPEN_LAVKA_DELIVERY_INFORMATION_VO.copy(closedText = LAVKA_CART_WILL_OPEN_AT_UNKNOWN)
    val UNKNOWN_LAVKA_DELIVERY_INFORMATION_VO =
        OPEN_LAVKA_DELIVERY_INFORMATION_VO.copy(unavailableText = LAVKA_CART_UNAVAILABLE)

    val LAVKA_CART = LavkaCart(
        cartId = "",
        cartVersion = 142,
        nextIdempotencyToken = "",
        offerId = null,
        depot = null,
        validUntil = null,
        totalPrice = null,
        totalPriceNoDelivery = null,
        orderConditions = null,
        requirements = null,
        items = emptyList(),
        deliveryType = null,
        deliveryInfo = LavkaDeliveryInfo(
            toNextDeliveryWithoutDelivery = null,
            toFreeDeliveryWithDelivery = null,
            toFreeDeliveryWithoutDelivery = null,
            toMinCartWithDelivery = null,
            toMinCartWithoutDelivery = null,
            deliveryPrice = DELIVERY_TEXT
        ),
        isSurge = true,
        availableDeliveryTypes = null,
        orderFlowVersion = null,
        cashbackFlow = null,
        currencySign = null,
        tillNextThreshold = null,
        lavkaInformers = emptyList(),
        totalPriceWithDiscount = null,
        totalPriceWithDiscountNoDelivery = null,
        cashbackAmount = null,
        deliveryAmount = null,
        isOrderAvailable = true,
        totalPriceAmount = null,
        isLavkaAvailable = true,
    )

    const val HYPERLOCAL_ADDRESS_NOT_EXIST = false
    const val HYPERLOCAL_ADDRESS_EXIST = true

}