package ru.auto.api.managers.favorite

import java.time.Instant

import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.managers.favorite.FavoritesHelper.FavoriteOffer
import ru.auto.api.model.CategorySelector

trait FavoriteSpecSupport {

  def offerToFavorite(offer: Offer): FavoriteOffer = {
    val create = Instant.now().toEpochMilli
    FavoriteOffer(offer.getId, CategorySelector.from(offer.getCategory), create, create)
  }

  def offerToShortOffer(offer: Offer): Offer = {
    Offer
      .newBuilder()
      .setCategory(offer.getCategory)
      .setId(offer.getId)
      .setIsFavorite(true)
      .build()
  }

  def favoriteToShortOffer(favorite: FavoriteOffer): Offer = {
    Offer
      .newBuilder()
      .setCategory(favorite.category.enum)
      .setId(favorite.id)
      .setIsFavorite(true)
      .build()
  }
}
