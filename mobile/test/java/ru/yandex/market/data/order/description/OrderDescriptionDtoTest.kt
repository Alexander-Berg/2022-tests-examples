package ru.yandex.market.data.order.description

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.clean.data.model.dto.offerIdDtoTestInstance
import ru.yandex.market.clean.data.model.dto.offerPriceDtoTestInstance
import ru.yandex.market.clean.domain.model.selectedInstallmentsDtoTestInstance
import ru.yandex.market.clean.domain.model.selectedInstallmentsDto_MonthlyPaymentTestInstance
import ru.yandex.market.clean.domain.model.selectedInstallmentsDto_OptionsItemTestInstance
import ru.yandex.market.data.cashback.network.dto.order.CashbackTypeDto
import ru.yandex.market.data.cashback.network.dto.order.cashbackDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.cashbackOptionDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.cashbackOptionsDtoTestInstance
import ru.yandex.market.data.order.shopOfferIdDtoTestInstance
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.data.payment.network.dto.selectedCardInfoDtoTestInstance
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.testcase.JsonSerializationTestCase
import java.math.BigDecimal

@RunWith(Enclosed::class)
class OrderDescriptionDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = orderDescriptionRequestModelTestInstance(
            paymentMethod = PaymentMethod.CARD_ON_DELIVERY,
            paymentOptions = listOf(PaymentMethod.YANDEX),
            appliedCoinIds = null,
            currency = Currency.RUR,
            summary = null,
            cashbackType = CashbackTypeDto.EMIT,
            cashback = cashbackDtoTestInstance(
                selectedCashbackOption = CashbackTypeDto.SPEND,
                cashBackBalance = BigDecimal.ONE,
                cashbackOptionsProfiles = listOf(),
                applicableOptions = cashbackOptionsDtoTestInstance(
                    emitOption = cashbackOptionDtoTestInstance(),
                    spendOption = cashbackOptionDtoTestInstance().copy(
                        cashbackDetails = null
                    )
                ),
                welcomeCashback = null,
                paymentSystemCashback = null,
                growingCashback = null,
                yandexCardCashback = null
            ),
            additionalOffersData = listOf(
                additionalOfferDataDtoTestInstance(
                    offerId = offerIdDtoTestInstance(
                        wareMd5 = "wareMd5_123",
                        feeShow = "feeShow_123"
                    ),
                    shopOfferId = shopOfferIdDtoTestInstance(
                        feedId = 12345,
                        offerId = "offerId_123"
                    ),
                    offerPrice = offerPriceDtoTestInstance(value = "6789", discountType = null),
                    sku = "sku_123",
                    shopSku = "shopSku_123",
                    supplierId = 1346
                )
            ),
            useInternalPromoCode = false,
            installmentsInformation = selectedInstallmentsDtoTestInstance(
                selectedInstallmentsDto_OptionsItemTestInstance(
                    selectedInstallmentsDto_MonthlyPaymentTestInstance(
                        Currency.RUR,
                        "123"
                    ),
                    "12"
                )
            ),
            creditInformation = selectedInstallmentsDtoTestInstance(
                selectedInstallmentsDto_OptionsItemTestInstance(
                    selectedInstallmentsDto_MonthlyPaymentTestInstance(
                        Currency.RUR,
                        "123"
                    ),
                    "12"
                )
            ),
            paymentSystem = "paymentSystem",
            selectedCardInfo = selectedCardInfoDtoTestInstance(
                isYandexCard = true,
                selectedCardId = "selectedCard"
            )
        )

        override val type = OrderDescriptionRequestModel::class.java

        override val jsonSource = file("OrderDescriptionDtoTest.json")
    }
}
