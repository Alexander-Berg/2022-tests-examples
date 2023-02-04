@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.CardHints
import com.yandex.mapkit.search.SerpHints
import com.yandex.mapkit.search.VisualHintsObjectMetadata

class SerpHintsEncodable(it: SerpHints) {
    val showTitle: SerpHints.TitleType = it.showTitle
    val showAddress: SerpHints.AddressType = it.showAddress
    val showCategory: SerpHints.CategoryType = it.showCategory
    val showRating: SerpHints.RatingType = it.showRating
    val showPhoto: SerpHints.PhotoType = it.showPhoto
    val actionButtons: List<SerpHints.ActionButton> = it.actionButtons
    val showWorkHours: Boolean = it.showWorkHours
    val showVerified: Boolean = it.showVerified
    val showDistanceFromTransit: Boolean = it.showDistanceFromTransit
    val showBookmark: Boolean = it.showBookmark
    val showEta: Boolean = it.showEta
    val showGeoproductOffer: Boolean = it.showGeoproductOffer
}

class CardHintsEncodable(it: CardHints) {
    val showClaimOrganization: Boolean = it.showClaimOrganization
    val showTaxiButton: Boolean = it.showTaxiButton
    val showFeedbackButton: Boolean = it.showFeedbackButton
    val showReviews: Boolean = it.showReviews
    val showAddPhotoButton: Boolean = it.showAddPhotoButton
}

class VisualHintsObjectMetadataEncodable(it: VisualHintsObjectMetadata) {
    val serpHints: SerpHintsEncodable? = it.serpHints?.let { SerpHintsEncodable(it) }
    val cardHints: CardHintsEncodable? = it.cardHints?.let { CardHintsEncodable(it) }
}
