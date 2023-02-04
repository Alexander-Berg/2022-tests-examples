@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessPhotoObjectMetadata

class BusinessPhotoObjectMetadataEncodable(it: BusinessPhotoObjectMetadata) {
    class PhotoEncodable(it: BusinessPhotoObjectMetadata.Photo) {
        class PhotoLinkEncodable(it: BusinessPhotoObjectMetadata.Photo.PhotoLink) {
            val type: String? = it.type
            val uri: String = it.uri
        }

        val id: String = it.id
        val links: List<PhotoLinkEncodable> = it.links.map { PhotoLinkEncodable(it) }
    }

    val count: Int = it.count
    val photos: List<PhotoEncodable> = it.photos.map { PhotoEncodable(it) }
}

