@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.*

class PartnerLinkEncodable(it: PartnerLink) {
    val title: String? = it.title
    val image: ImageEncodable? = it.image?.let { ImageEncodable(it) }
    val uri: String = it.uri
}

class PartnerLinksEncodable(it: PartnerLinks) {
    val title: String? = it.title
    val links: List<PartnerLinkEncodable> = it.links.map { PartnerLinkEncodable(it) }
}

class RelatedCollectionsEncodable(it: RelatedCollections) {
    val collections: List<CollectionEncodable> = it.collections.map { CollectionEncodable(it) }
}

class CollectionResultMetadataEncodable(it: CollectionResultMetadata) {
    val collection: CollectionEncodable = CollectionEncodable(it.collection)
    val relatedCollections: RelatedCollectionsEncodable? = it.relatedCollections?.let { RelatedCollectionsEncodable(it) }
    val partnerLinks: PartnerLinksEncodable? = it.partnerLinks?.let { PartnerLinksEncodable(it) }
}

class CollectionEntryLinkEncodable(it: CollectionEntryLink) {
    val title: String? = it.title
    val tags: List<String> = it.tags
    val uri: String = it.uri
}

class CollectionEntryFeatureEncodable(it: CollectionEntryFeature) {
    val type: String = it.type
    val name: String? = it.name
    val value: String? = it.value
}

class CollectionEntryMetadataEncodable(it: CollectionEntryMetadata) {
    val title: String? = it.title
    val annotation: String? = it.annotation
    val description: String? = it.description
    val images: List<ImageEncodable> = it.images.map { ImageEncodable(it) }
    val links: List<CollectionEntryLinkEncodable> = it.links.map { CollectionEntryLinkEncodable(it) }
    val features: List<CollectionEntryFeatureEncodable> = it.features.map { CollectionEntryFeatureEncodable(it) }
    val tags: List<String> = it.tags
}

