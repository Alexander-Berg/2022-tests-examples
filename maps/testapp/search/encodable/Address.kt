@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Address

class AddressEncodable(it: Address) {
    class ComponentEncodable(it: Address.Component) {
        val name: String = it.name
        val kinds: List<Address.Component.Kind> = it.kinds
    }

    val formattedAddress: String = it.formattedAddress
    val additionalInfo: String? = it.additionalInfo
    val postalCode: String? = it.postalCode
    val countryCode: String? = it.countryCode
    val components: List<ComponentEncodable> = it.components.map { ComponentEncodable(it) }
}
