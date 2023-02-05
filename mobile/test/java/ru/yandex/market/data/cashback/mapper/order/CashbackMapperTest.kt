package ru.yandex.market.data.cashback.mapper.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.yandex.market.data.cashback.mapper.CashBackBalanceMapper
import ru.yandex.market.data.cashback.network.dto.order.CashbackDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackOptionTypeDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackRestrictionReasonDto
import ru.yandex.market.data.cashback.network.dto.order.CashbackTypeDto
import ru.yandex.market.data.cashback.network.dto.order.cashbackDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.cashbackOptionDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.cashbackOptionProfileDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.cashbackOptionsDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.paymentSystemCashbackDtoTestInstance
import ru.yandex.market.data.cashback.network.dto.order.welcomeCashbackDtoTestInstance
import ru.yandex.market.data.payment.mapper.PaymentMethodsMapper
import ru.yandex.market.domain.cashback.model.CashBackBalance
import ru.yandex.market.domain.cashback.model.Cashback
import ru.yandex.market.domain.cashback.model.CashbackOption
import ru.yandex.market.domain.cashback.model.CashbackOptionType
import ru.yandex.market.domain.cashback.model.CashbackOptions
import ru.yandex.market.domain.cashback.model.CashbackRestrictionReason
import ru.yandex.market.domain.cashback.model.cashbackOptionProfileTestInstance
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_PaymentSystemCashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_WelcomeCashbackTestInstance
import java.math.BigDecimal
import ru.yandex.market.data.payment.network.dto.PaymentMethod as DataPaymentMethod
import ru.yandex.market.domain.payment.model.PaymentMethod as DomainPaymentMethod

class CashbackMapperTest {

    private val paymentMethodMapper: PaymentMethodsMapper = mock {
        on { map(DataPaymentMethod.YANDEX) } doReturn DomainPaymentMethod.YANDEX
        on { map(DataPaymentMethod.GOOGLE_PAY) } doReturn DomainPaymentMethod.GOOGLE_PAY
    }

    private val mapper = CashbackMapper(
        CashbackOptionMapper(
            CashbackPromoMapper(
                CashbackPromoTagMapper()
            ), CashbackDetailsMapper(
                CashbackPromoTagMapper()
            )
        ),
        CashBackBalanceMapper(),
        CashbackOptionTypeMapper(),
        CashbackOptionProfileMapper(
            CashbackOptionMapper(
                CashbackPromoMapper(
                    CashbackPromoTagMapper()
                ), CashbackDetailsMapper(
                    CashbackPromoTagMapper()
                )
            ),
            CashbackProfileMapper(),
            CashbackOrderMapper(
                CashbackOptionMapper(
                    CashbackPromoMapper(
                        CashbackPromoTagMapper()
                    ), CashbackDetailsMapper(
                        CashbackPromoTagMapper()
                    )
                )
            ),
            paymentMethodMapper,
        ),
        PossibleCashbackOptionsMapper(
            welcomeCashbackPossibleOptionMapper = mock {
                on { map(welcomeCashbackDtoTestInstance()) } doReturn
                    possibleCashbackOption_WelcomeCashbackTestInstance()
            },
            paymentSystemPossibleOptionMapper = mock {
                on { map(paymentSystemCashbackDtoTestInstance(system = "mastercard")) } doReturn
                    possibleCashbackOption_PaymentSystemCashbackTestInstance()
            },
            growingCashbackPossibleOptionMapper = mock(),
            yandexCardCashbackPossibleOptionMapper = mock()
        ),
    )

    @Test
    fun `Test mapping`() {
        val result = mapper.map(createCashbackDto())
        val expected = getExpectedCashback()
        assertThat(result).isEqualTo(expected)
        verify(paymentMethodMapper).map(DataPaymentMethod.GOOGLE_PAY)
        verify(paymentMethodMapper).map(DataPaymentMethod.YANDEX)
        verifyZeroInteractions(paymentMethodMapper)
    }

    private fun createCashbackDto(): CashbackDto {
        @Suppress("UNCHECKED_CAST")
        return cashbackDtoTestInstance(
            cashBackBalance = BigDecimal(1000),
            selectedCashbackOption = CashbackTypeDto.SPEND,
            applicableOptions = cashbackOptionsDtoTestInstance(
                spendOption = cashbackOptionDtoTestInstance(
                    value = BigDecimal(100),
                    restrictionReason = null,
                    promos = emptyList()
                ),
                emitOption = cashbackOptionDtoTestInstance(
                    value = BigDecimal(0),
                    type = CashbackOptionTypeDto.PROHIBITED,
                    restrictionReason = CashbackRestrictionReasonDto.NOT_SUITABLE_CATEGORY,
                    promos = emptyList()
                )
            ),
            cashbackOptionsProfiles = listOf(
                cashbackOptionProfileDtoTestInstance(
                    cashbackTypes = listOf(CashbackTypeDto.SPEND),
                    globalCashback = cashbackOptionsDtoTestInstance(
                        spendOption = cashbackOptionDtoTestInstance(
                            type = CashbackOptionTypeDto.ALLOWED,
                            value = BigDecimal(100),
                            restrictionReason = null,
                            promos = emptyList()
                        ),
                        emitOption = null
                    ),
                    paymentTypes = listOf(
                        DataPaymentMethod.GOOGLE_PAY,
                        null,
                        DataPaymentMethod.YANDEX,
                    ) as List<DataPaymentMethod>,
                    ordersCashback = emptyList()
                ),
                cashbackOptionProfileDtoTestInstance(
                    cashbackTypes = listOf(CashbackTypeDto.SPEND),
                    globalCashback = cashbackOptionsDtoTestInstance(
                        emitOption = cashbackOptionDtoTestInstance(
                            type = CashbackOptionTypeDto.ALLOWED,
                            value = BigDecimal(200),
                            restrictionReason = null,
                            promos = emptyList()
                        ),
                        spendOption = null
                    ),
                    paymentTypes = null,
                    ordersCashback = emptyList()
                ),
                cashbackOptionProfileDtoTestInstance(
                    cashbackTypes = listOf(CashbackTypeDto.SPEND),
                    globalCashback = cashbackOptionsDtoTestInstance(
                        emitOption = cashbackOptionDtoTestInstance(
                            type = CashbackOptionTypeDto.ALLOWED,
                            value = BigDecimal(300),
                            restrictionReason = null,
                            promos = emptyList()
                        ),
                        spendOption = null
                    ),
                    paymentTypes = emptyList(),
                    ordersCashback = emptyList()
                )
            ),
            welcomeCashback = welcomeCashbackDtoTestInstance(),
            paymentSystemCashback = paymentSystemCashbackDtoTestInstance(system = "mastercard")
        )
    }

    private fun getExpectedCashback(): Cashback {
        return cashbackTestInstance(
            cashBackBalance = CashBackBalance(BigDecimal(1000), 0),
            selectedOption = CashbackOptionType.SPEND,
            cashbackOptions = CashbackOptions(
                spendOption = CashbackOption(
                    value = BigDecimal(100),
                    restrictionReason = null,
                    promos = emptyList(),
                    details = null,
                ),
                emitOption = CashbackOption(
                    value = BigDecimal(0),
                    restrictionReason = CashbackRestrictionReason.NOT_SUITABLE_CATEGORY,
                    promos = emptyList(),
                    details = null,
                )
            ),
            cashbackOptionProfiles = listOf(
                cashbackOptionProfileTestInstance(
                    cashbackOptions = CashbackOptions(
                        spendOption = CashbackOption(
                            value = BigDecimal(100),
                            restrictionReason = null,
                            promos = emptyList(),
                            details = null,
                        ),
                        emitOption = CashbackOption(
                            value = BigDecimal(0),
                            restrictionReason = CashbackRestrictionReason.UNKNOWN,
                            promos = emptyList(),
                            details = null,
                        )
                    ),
                    availablePaymentMethods = listOf(DomainPaymentMethod.GOOGLE_PAY, DomainPaymentMethod.YANDEX),
                    orders = emptyList()
                ),
                cashbackOptionProfileTestInstance(
                    cashbackOptions = CashbackOptions(
                        spendOption = CashbackOption(
                            value = BigDecimal(0),
                            restrictionReason = CashbackRestrictionReason.UNKNOWN,
                            promos = emptyList(),
                            details = null,
                        ),
                        emitOption = CashbackOption(
                            value = BigDecimal(200),
                            restrictionReason = null,
                            promos = emptyList(),
                            details = null,
                        )
                    ),
                    availablePaymentMethods = emptyList(),
                    orders = emptyList()
                ),
                cashbackOptionProfileTestInstance(
                    cashbackOptions = CashbackOptions(
                        spendOption = CashbackOption(
                            value = BigDecimal(0),
                            restrictionReason = CashbackRestrictionReason.UNKNOWN,
                            promos = emptyList(),
                            details = null,
                        ),
                        emitOption = CashbackOption(
                            value = BigDecimal(300),
                            restrictionReason = null,
                            promos = emptyList(),
                            details = null,
                        )
                    ),
                    availablePaymentMethods = emptyList(),
                    orders = emptyList()
                )
            ),
            possibleCashbackOptions = listOf(
                possibleCashbackOption_WelcomeCashbackTestInstance(),
                possibleCashbackOption_PaymentSystemCashbackTestInstance()
            ),
        )
    }
}
