package ru.auto.testdata

import android.graphics.Color
import ru.auto.feature.loans.common.model.AmountRange
import ru.auto.feature.loans.common.model.Bank
import ru.auto.feature.loans.common.model.CreditProduct
import ru.auto.feature.loans.common.model.CreditProductType
import ru.auto.feature.loans.common.model.InterestRange
import ru.auto.feature.loans.common.model.LoanCalculatorParams
import ru.auto.feature.loans.common.model.PeriodRange


val CREDIT_PRODUCT_GENERIC = CreditProduct(
    id = "any_id",
    bank = Bank(
        id = "bank",
        bankName = "",
        bankLogoUrl = "",
        bankDarkLogoUrl = "",
        bankLogoSmall = "",
        bankDarkLogoSmall = "",
        bankLogoRound = "",
        bankColor = Color.WHITE
    ),
    amountRange = AmountRange(100_000L, 2_000_000L),
    interestRange = InterestRange(from = 0.099, to = 0.12),
    periodRange = PeriodRange(12, 32),
    productType = CreditProductType.CONSUMER,
    downPaymentRate = 0.1,
    isActive = true
)

val CALCULATOR_PARAMS_GENERIC = LoanCalculatorParams(
    amountRange = AmountRange(100_000L, 2_000_000L),
    interestRange = InterestRange(from = 0.099, to = 0.12),
    periodRange = PeriodRange(12, 32),
    downPaymentRate = 0.1,
)
