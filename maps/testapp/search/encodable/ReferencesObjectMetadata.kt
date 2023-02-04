@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.ReferenceType
import com.yandex.mapkit.search.ReferencesObjectMetadata

class ReferenceTypeEncodable(it: ReferenceType) {
    val id: String = it.id
    val scope: String = it.scope
}

class ReferencesObjectMetadataEncodable(it: ReferencesObjectMetadata) {
    val references: List<ReferenceTypeEncodable> = it.references.map { ReferenceTypeEncodable(it) }
}
