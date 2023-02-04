@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.Closed
import com.yandex.mapkit.search.Precision
import com.yandex.mapkit.search.Properties

class PropertiesEncodable(it: Properties) {
    class ItemEncodable(it: Properties.Item) {
        val key: String = it.key
        val value: String = it.value
    }

    val items: List<ItemEncodable> = it.items.map { ItemEncodable(it) }
}

class BusinessObjectMetadataEncodable(it: BusinessObjectMetadata) {
    val oid: String = it.oid
    val name: String = it.name
    val address: AddressEncodable = AddressEncodable(it.address)
    val categories: List<CategoryEncodable> = it.categories.map { CategoryEncodable(it) }
    val advertisement: AdvertisementEncodable? = it.advertisement?.let { AdvertisementEncodable(it) }
    val phones: List<PhoneEncodable> = it.phones.map { PhoneEncodable(it) }
    val workingHours: WorkingHoursEncodable? = it.workingHours?.let { WorkingHoursEncodable(it) }
    val precision: Precision? = it.precision
    val features: List<FeatureEncodable> = it.features.map { FeatureEncodable(it) }
    val importantFeatures: FeatureSetEncodable? = it.importantFeatures?.let { FeatureSetEncodable(it) }
    val links: List<LinkEncodable> = it.links.map { LinkEncodable(it) }
    val distance: LocalizedValueEncodable? = it.distance?.let { LocalizedValueEncodable(it) }
    val chains: List<ChainEncodable> = it.chains.map { ChainEncodable(it) }
    val closed: Closed? = it.closed
    val unreliable: Boolean? = it.unreliable
    val seoname: String? = it.seoname
    val shortName: String? = it.shortName
    val properties: PropertiesEncodable? = it.properties?.let { PropertiesEncodable(it) }
    val featureGroups: List<FeatureGroupEncodable> = it.featureGroups.map { FeatureGroupEncodable(it) }
    val indoorLevel: String? = it.indoorLevel
}
