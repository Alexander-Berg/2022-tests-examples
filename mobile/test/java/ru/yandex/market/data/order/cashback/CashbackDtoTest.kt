package ru.yandex.market.data.order.cashback

import ru.yandex.market.data.cashback.network.dto.growingcashback.growingCashbackOptionDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.CashbackDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackOptionDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackOptionPreconditionDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackOptionProfileDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackOptionTypeDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackOptionsDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackPromoDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackRestrictionReasonDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackTypeDto
import ru.yandex.market.data.cashback.network.dto.order.OrderCashbackDto
import ru.yandex.market.data.cashback.network.dto.order.OrderItemCashBackDto
import ru.yandex.market.data.cashback.network.dto.order.paymentSystemCashbackDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.welcomeCashbackDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.yandexCardCashbackDtoTestInstance
import ru.yandex.market.data.delivery.dto.DeliveryTypeDto
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.testcase.JsonSerializationTestCase
import java.lang.reflect.Type
import java.math.BigDecimal

class CashbackDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance: CashbackDto
            get() {
                return CashbackDto(
                    selectedCashbackOption = CashbackTypeDto.EMIT,
                    cashBackBalance = BigDecimal(199),
                    cashbackOptionsProfiles = listOf(
                        CashbackOptionProfileDto(
                            cashbackTypes = listOf(CashbackTypeDto.EMIT, CashbackTypeDto.SPEND),
                            preconditions = listOf(
                                CashbackOptionPreconditionDto.PAYMENT,
                                CashbackOptionPreconditionDto.DELIVERY
                            ),
                            paymentTypes = listOf(
                                PaymentMethod.CASH_ON_DELIVERY,
                                PaymentMethod.CARD_ON_DELIVERY,
                                PaymentMethod.YANDEX,
                                PaymentMethod.APPLE_PAY,
                                PaymentMethod.CREDIT,
                                PaymentMethod.GOOGLE_PAY
                            ),
                            deliveryTypes = listOf(
                                DeliveryTypeDto.PICKUP,
                                DeliveryTypeDto.DELIVERY
                            ),
                            globalCashback = CashbackOptionsDto(
                                emitOption = CashbackOptionDto(
                                    id = null,
                                    type = CashbackOptionTypeDto.ALLOWED,
                                    value = BigDecimal(100),
                                    restrictionReason = CashbackRestrictionReasonDto.NOT_YA_PLUS_SUBSCRIBER,
                                    uiPromoFlags = emptyList(),
                                    promos = null,
                                    cashbackDetails = null,
                                ),
                                spendOption = CashbackOptionDto(
                                    id = null,
                                    type = CashbackOptionTypeDto.RESTRICTED,
                                    value = BigDecimal(499),
                                    restrictionReason = CashbackRestrictionReasonDto.CASHBACK_DISABLED,
                                    uiPromoFlags = emptyList(),
                                    promos = null,
                                    cashbackDetails = null,
                                )
                            ),
                            ordersCashback = listOf(
                                OrderCashbackDto(
                                    cartId = "334455",
                                    orderId = "112233",
                                    cashbackOptionsDto = CashbackOptionsDto(
                                        emitOption = CashbackOptionDto(
                                            id = null,
                                            type = CashbackOptionTypeDto.ALLOWED,
                                            value = BigDecimal(100),
                                            restrictionReason = null,
                                            uiPromoFlags = emptyList(),
                                            promos = null,
                                            cashbackDetails = null,
                                        ),
                                        spendOption = CashbackOptionDto(
                                            id = null,
                                            type = CashbackOptionTypeDto.ALLOWED,
                                            value = BigDecimal(499),
                                            restrictionReason = null,
                                            uiPromoFlags = emptyList(),
                                            promos = null,
                                            cashbackDetails = null,
                                        )
                                    ),
                                    orderItems = listOf(
                                        OrderItemCashBackDto(
                                            feedId = 12345,
                                            offerId = "334455",
                                            cashbackOptionsDto = CashbackOptionsDto(
                                                emitOption = CashbackOptionDto(
                                                    id = null,
                                                    type = CashbackOptionTypeDto.ALLOWED,
                                                    value = BigDecimal(10),
                                                    restrictionReason = null,
                                                    uiPromoFlags = emptyList(),
                                                    promos = null,
                                                    cashbackDetails = null,
                                                ),
                                                spendOption = CashbackOptionDto(
                                                    id = null,
                                                    type = CashbackOptionTypeDto.PROHIBITED,
                                                    value = BigDecimal(399),
                                                    restrictionReason = CashbackRestrictionReasonDto.NOT_SUITABLE_CATEGORY,
                                                    uiPromoFlags = emptyList(),
                                                    promos = null,
                                                    cashbackDetails = null,
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    applicableOptions = CashbackOptionsDto(
                        emitOption = CashbackOptionDto(
                            id = null,
                            type = CashbackOptionTypeDto.ALLOWED,
                            value = BigDecimal(54321),
                            restrictionReason = null,
                            uiPromoFlags = emptyList(),
                            promos = listOf(
                                CashbackPromoDto(
                                    value = BigDecimal(123),
                                    nominal = BigDecimal(456),
                                    promoKey = "promoKey",
                                    uiPromoFlags = listOf("promoFlag")
                                )
                            ),
                            cashbackDetails = null,
                        ),
                        spendOption = CashbackOptionDto(
                            id = null,
                            type = CashbackOptionTypeDto.RESTRICTED,
                            value = BigDecimal(12345),
                            restrictionReason = CashbackRestrictionReasonDto.NOT_SUITABLE_CATEGORY,
                            uiPromoFlags = emptyList(),
                            promos = null,
                            cashbackDetails = null,
                        )
                    ),
                    welcomeCashback = welcomeCashbackDtoTestInstance(
                        remainingMultiCartTotal = 100,
                        minMultiCartTotal = 3500,
                        amount = 500,
                        agitationPriority = 1
                    ),
                    paymentSystemCashback = paymentSystemCashbackDtoTestInstance(
                        amount = BigDecimal(100),
                        promoKey = "paymentSystemCashback",
                        system = "mastercard",
                        cashbackPercent = 5,
                        agitationPriority = 2
                    ),
                    growingCashback = growingCashbackOptionDtoTestInstance(
                        remainingMultiCartTotal = 500,
                        minMultiCartTotal = 3500,
                        amount = 350,
                        promoKey = "growingCashback",
                        agitationPriority = 3
                    ),
                    yandexCardCashback = yandexCardCashbackDtoTestInstance(
                        amount = BigDecimal(456),
                        promoKey = "yandexCard",
                        cashbackPercent = 15,
                        maxOrderTotal = BigDecimal(15000),
                        agitationPriority = 4
                    )
                )
            }

        override val type: Type = CashbackDto::class.java

        override val jsonSource: JsonSource = file("CashbackDto.json")

    }

}
