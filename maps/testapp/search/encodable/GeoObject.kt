@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.*
import com.yandex.maps.testapp.search.metadata

class GeoObjectEncodable(it: GeoObject) {
    val name: String? = it.name
    val description: String? = it.descriptionText

    val encyclopediaMetadata = it
        .metadata<EncyclopediaObjectMetadata>()
        ?.let { EncyclopediaObjectMetadataEncodable(it) }
    val visualHintsMetadata = it
        .metadata<VisualHintsObjectMetadata>()
        ?.let { VisualHintsObjectMetadataEncodable(it) }
    val businessMetadata = it
        .metadata<BusinessObjectMetadata>()
        ?.let { BusinessObjectMetadataEncodable(it)}
    val businessImagesMetadata = it
        .metadata<BusinessImagesObjectMetadata>()
        ?.let { BusinessImagesObjectMetadataEncodable(it) }
    val businessPhotoMetadata = it
        .metadata<BusinessPhotoObjectMetadata>()
        ?.let { BusinessPhotoObjectMetadataEncodable(it) }
    val businessPhoto3xMetadata = it
        .metadata<BusinessPhoto3xObjectMetadata>()
        ?.let { BusinessPhoto3xObjectMetadataEncodable(it) }
    val businessRating1xMetadata = it
        .metadata<BusinessRating1xObjectMetadata>()
        ?.let { BusinessRating1xObjectMetadataEncodable(it) }
    val discovery2xMetadata = it
        .metadata<Discovery2xObjectMetadata>()
        ?.let { Discovery2xObjectMetadataEncodable(it) }
    val exchangeMetadata = it
        .metadata<CurrencyExchangeMetadata>()
        ?.let { CurrencyExchangeMetadataEncodable(it) }
    val experimentalMetadata = it
        .metadata<ExperimentalMetadata>()
        ?.let { ExperimentalMetadataEncodable(it) }
    val fuelMetadata = it
        .metadata<FuelMetadata>()
        ?.let { FuelMetadataEncodable(it) }
    val goods1xMetadata = it
        .metadata<Goods1xObjectMetadata>()
        ?.let { Goods1xObjectMetadataEncodable(it) }
    val masstransit1xMetadata = it
        .metadata<MassTransit1xObjectMetadata>()
        ?.let { MassTransit1xObjectMetadataEncodable(it) }
    val masstransit2xMetadata = it
        .metadata<MassTransit2xObjectMetadata>()
        ?.let { MassTransit2xObjectMetadataEncodable(it) }
    val metrikaMetadata = it
        .metadata<MetrikaObjectMetadata>()
        ?.let { MetrikaObjectMetadataEncodable(it) }
    val panoramasMetadata = it
        .metadata<PanoramasObjectMetadata>()
        ?.let { PanoramasObjectMetadataEncodable(it) }
    val referencesMetadata = it
        .metadata<ReferencesObjectMetadata>()
        ?.let { ReferencesObjectMetadataEncodable(it) }
    val relatedAdvertsMetadata = it
        .metadata<RelatedAdvertsObjectMetadata>()
        ?.let { RelatedAdvertsObjectMetadataEncodable(it) }
    val relatedPlacesMetadata = it
        .metadata<RelatedPlacesObjectMetadata>()
        ?.let { RelatedPlacesObjectMetadataEncodable(it) }
    val routeDistancesMetadata = it
        .metadata<RouteDistancesObjectMetadata>()
        ?.let { RouteDistancesObjectMetadataEncodable(it) }
    val routePointMetadata = it
        .metadata<RoutePointMetadata>()
        ?.let { RoutePointMetadataEncodable(it) }
    val searchMetadata = it
        .metadata<SearchObjectMetadata>()
        ?.let { SearchObjectMetadataEncodable(it) }
    val showtimesMetadata = it
        .metadata<ShowtimesObjectMetadata>()
        ?.let { ShowtimesObjectMetadataEncodable(it) }
    val subtitleMetadata = it
        .metadata<SubtitleMetadata>()
        ?.let { SubtitleMetadataEncodable(it) }
    val toponymMetadata = it
        .metadata<ToponymObjectMetadata>()
        ?.let { ToponymObjectMetadataEncodable(it) }
    val transitMetadata = it
        .metadata<TransitObjectMetadata>()
        ?.let { TransitObjectMetadataEncodable(it) }
}
