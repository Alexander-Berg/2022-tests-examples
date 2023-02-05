package ru.yandex.market.data.cashback.mapper.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.data.cashback.network.dto.order.PaymentSystemCashbackDto
import ru.yandex.market.data.payment.mapper.PaymentSystemMapper
import ru.yandex.market.domain.cashback.model.PossibleCashbackOption
import ru.yandex.market.domain.payment.model.PaymentSystem
import ru.yandex.market.safe.Safe
import java.math.BigDecimal

@RunWith(Enclosed::class)
class PaymentSystemPossibleOptionMapperTest {

    @RunWith(Parameterized::class)
    class MapFromDtoTest(
        private val input: PaymentSystemCashbackDto,
        private val expectedResult: PossibleCashbackOption.PaymentSystemCashback?
    ) {

        private val paymentSystemMapper = mock<PaymentSystemMapper> {
            on { map(MASTER_PAYMENT_SYSTEM_DTO) } doReturn Safe.value(PaymentSystem.MASTERCARD)
            on { map(null) } doReturn Safe.error(NullPointerException())
            on { map(INVALID_PAYMENT_SYSTEM_DTO) } doReturn Safe.error(IllegalArgumentException())
        }

        private val mapper = PaymentSystemPossibleOptionMapper(paymentSystemMapper)

        @Test
        fun testMap() {
            val actual = mapper.map(input)
            assertThat(actual).isEqualTo(expectedResult)
        }

        companion object {

            private const val MASTER_PAYMENT_SYSTEM_DTO = "mastercard"
            private const val INVALID_PAYMENT_SYSTEM_DTO = "fantiki"
            private val MASTER_PAYMENT_SYSTEM = PaymentSystem.MASTERCARD

            @Parameterized.Parameters(name = "{index}: {0} -> {2}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                //0
                arrayOf(
                    PaymentSystemCashbackDto(
                        amount = null,
                        promoKey = null,
                        system = null,
                        cashbackPercent = null,
                        agitationPriority = null
                    ),
                    null
                ),
                //1
                arrayOf(
                    PaymentSystemCashbackDto(
                        amount = BigDecimal.TEN,
                        promoKey = null,
                        system = null,
                        cashbackPercent = null,
                        agitationPriority = null
                    ),
                    null
                ),
                //2
                arrayOf(
                    PaymentSystemCashbackDto(
                        amount = BigDecimal.TEN,
                        promoKey = null,
                        system = MASTER_PAYMENT_SYSTEM_DTO,
                        cashbackPercent = null,
                        agitationPriority = null
                    ),
                    null
                ),
                //3
                arrayOf(
                    PaymentSystemCashbackDto(
                        amount = BigDecimal.TEN,
                        promoKey = null,
                        system = MASTER_PAYMENT_SYSTEM_DTO,
                        cashbackPercent = 10,
                        agitationPriority = null
                    ),
                    PossibleCashbackOption.PaymentSystemCashback(
                        paymentSystem = MASTER_PAYMENT_SYSTEM,
                        percentValue = 10,
                        cashbackAmount = BigDecimal.TEN,
                        agitationPriority = 0
                    )
                ),
                //4
                arrayOf(
                    PaymentSystemCashbackDto(
                        amount = BigDecimal(200),
                        promoKey = "promoKey",
                        system = MASTER_PAYMENT_SYSTEM_DTO,
                        cashbackPercent = 7,
                        agitationPriority = 22
                    ),
                    PossibleCashbackOption.PaymentSystemCashback(
                        paymentSystem = MASTER_PAYMENT_SYSTEM,
                        percentValue = 7,
                        cashbackAmount = BigDecimal(200),
                        agitationPriority = 22
                    )
                ),
                //5
                arrayOf(
                    PaymentSystemCashbackDto(
                        amount = BigDecimal(200),
                        promoKey = "promoKey",
                        system = INVALID_PAYMENT_SYSTEM_DTO,
                        cashbackPercent = 7,
                        agitationPriority = 23
                    ),
                    null
                ),
            )
        }
    }

    class MatToDtoTest {

        private val mapper = PaymentSystemPossibleOptionMapper(mock())

        @Test
        fun testMap() {
            val actual = mapper.mapToDto(
                PossibleCashbackOption.PaymentSystemCashback(
                    paymentSystem = PaymentSystem.MASTERCARD,
                    percentValue = 15,
                    cashbackAmount = BigDecimal(123),
                    agitationPriority = 42
                )
            )
            val expected = PaymentSystemCashbackDto(
                amount = BigDecimal(123),
                promoKey = null,
                system = "MASTERCARD",
                cashbackPercent = 15,
                agitationPriority = 42
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
