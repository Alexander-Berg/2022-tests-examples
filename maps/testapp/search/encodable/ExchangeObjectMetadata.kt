@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.CurrencyExchangeMetadata
import com.yandex.mapkit.search.CurrencyExchangeType

class CurrencyExchangeTypeEncodable(it: CurrencyExchangeType) {
    val name: String? = it.name
    val buy: MoneyEncodable? = it.buy?.let { MoneyEncodable(it) }
    val sell: MoneyEncodable? = it.sell?.let { MoneyEncodable(it) }
}

class CurrencyExchangeMetadataEncodable(it: CurrencyExchangeMetadata) {
    val currencies: List<CurrencyExchangeTypeEncodable> = it.currencies.map { CurrencyExchangeTypeEncodable(it) }
}

