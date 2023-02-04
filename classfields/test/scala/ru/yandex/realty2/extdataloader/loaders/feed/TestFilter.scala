package ru.yandex.realty2.extdataloader.loaders.feed

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.feeds.OfferFilters._
import ru.yandex.realty2.extdataloader.loaders.lucene.readers.filters.OfferFilter

class TestFilter(val regionGraphProvider: Provider[RegionGraph]) extends OfferFilter {

  override def validateOffer(offer: Offer): Boolean = {
    validSellOffer(offer) || validRentOffer(offer)
  }

  private def validSellOffer(offer: Offer): Boolean = {
    isApartment(offer) && isSell(offer) &&
    (isSecondaryFlat(offer) || isNewFlat(offer))
  }

  private def validRentOffer(offer: Offer): Boolean = {
    isApartment(offer) &&
    isRent(offer) &&
    isLongPeriod(offer)
  }
}
