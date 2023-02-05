package ru.yandex.yandexbus.inhouse.organization.card

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.uri.Uri
import com.yandex.mapkit.uri.UriObjectMetadata
import com.yandex.runtime.any.Collection
import org.mockito.Mockito
import ru.yandex.yandexbus.inhouse.eq
import ru.yandex.yandexbus.inhouse.geometry.MapkitPoint
import ru.yandex.yandexbus.inhouse.whenever

object OrganizationGeoObjectTestFactory {
    fun mockGeoObject(name: String, position: MapkitPoint, uri: String): GeoObject {
        val geoObject = Mockito.mock(GeoObject::class.java)
        
        whenever(geoObject.name).thenReturn(name)

        val geometry = Mockito.mock(Geometry::class.java)
        whenever(geometry.point).thenReturn(position)
        whenever(geoObject.geometry).thenReturn(mutableListOf(geometry))

        val uriMeta = Mockito.mock(UriObjectMetadata::class.java)
        whenever(uriMeta.uris).thenReturn(mutableListOf(Uri(uri)))

        val collection = Mockito.mock(Collection::class.java)
        whenever(collection.getItem(eq(UriObjectMetadata::class.java))).thenReturn(uriMeta)

        whenever(geoObject.metadataContainer).thenReturn(collection)

        return geoObject
    }
}