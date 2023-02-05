// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.enums.Currency

class CurrencyInitializer private constructor() {
    companion object {
        private const val currenciesFieldName = "currencies"

        @JvmStatic
        fun injectTestDataInStaticFields() {
            val field = Currency::class.java.getDeclaredField(currenciesFieldName)
            field.isAccessible = true
            field.set(null, ApiSampleData.currency.map { it.code to it }.toMap())
        }
    }
}