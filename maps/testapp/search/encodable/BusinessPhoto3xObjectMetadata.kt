package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessPhoto3xObjectMetadata

class BusinessPhoto3xObjectMetadataEncodable(it: BusinessPhoto3xObjectMetadata) {
    class PhotoEncodable(it: BusinessPhoto3xObjectMetadata.Photo) {
        class LinkInfoEncodable(it: BusinessPhoto3xObjectMetadata.Photo.LinkInfo) {
            val type: String? = it.type
            val uri: String = it.uri
        }

        val image: ImageEncodable = ImageEncodable(it.image)
        val links: List<LinkInfoEncodable> = it.links.map { LinkInfoEncodable(it) }
    }

    class GroupEncodable(it: BusinessPhoto3xObjectMetadata.Group) {
        val id: String = it.id
        val count: Int? = it.count
        val name: String? = it.name
        val photos: List<PhotoEncodable> = it.photos.map { PhotoEncodable(it) }
    }

    val groups: List<GroupEncodable> = it.groups.map { GroupEncodable(it) }
}

