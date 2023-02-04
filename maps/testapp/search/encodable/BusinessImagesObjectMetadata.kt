@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessImagesObjectMetadata

class BusinessImagesObjectMetadataEncodable(it: BusinessImagesObjectMetadata) {
    class LogoEncodable(it: BusinessImagesObjectMetadata.Logo) {
        val urlTemplate: String = it.urlTemplate
    }

    val logo: LogoEncodable? = it.logo?.let { LogoEncodable(it) }
}
