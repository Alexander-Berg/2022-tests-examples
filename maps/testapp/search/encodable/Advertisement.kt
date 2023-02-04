@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Action
import com.yandex.mapkit.search.AdvertImage
import com.yandex.mapkit.search.Advertisement

class AdvertImageEncodable(it: AdvertImage) {
    val url: String = it.url
    val tags: List<String> = it.tags
}

class AdvertisementEncodable(it: Advertisement) {
    class LinkEncodable(it: Advertisement.Link) {
        val type: String? = it.type
        val uri: String = it.uri
    }

    class PromoEncodable(it: Advertisement.Promo) {
        val title: String? = it.title
        val details: String? = it.details
        val disclaimers: List<String> = it.disclaimers
        val url: String? = it.url
        val banner: AdvertImageEncodable? = it.banner?.let { AdvertImageEncodable(it) }
        val fullDisclaimer: String? = it.fullDisclaimer
    }

    class ProductEncodable(it: Advertisement.Product) {
        val title: String? = it.title
        val url: String? = it.url
        val photo: AdvertImageEncodable? = it.photo?.let { AdvertImageEncodable(it) }
        val price: MoneyEncodable? = it.price?.let { MoneyEncodable(it) }
    }

    class TextDataEncodable(it: Advertisement.TextData) {
        val title: String? = it.title
        val text: String? = it.text
        val disclaimers: List<String> = it.disclaimers
        val url: String? = it.url
    }

    val textData: TextDataEncodable? = it.textData?.let { TextDataEncodable(it) }
    val promo: PromoEncodable? = it.promo?.let { PromoEncodable(it) }
    val products: List<ProductEncodable> = it.products.map { ProductEncodable(it) }
    val about: String? = it.about
    val logo: AdvertImageEncodable? = it.logo?.let { AdvertImageEncodable(it) }
    val photo: AdvertImageEncodable? = it.photo?.let { AdvertImageEncodable(it) }
    val images: List<ImageEncodable> = it.images.map { ImageEncodable(it) }
    val actions: List<Action> = it.actions
    val logId: String? = it.logId
    val properties: List<KeyValuePairEncodable> = it.properties.map { KeyValuePairEncodable(it) }
    val highlighted: Boolean = it.highlighted
}

