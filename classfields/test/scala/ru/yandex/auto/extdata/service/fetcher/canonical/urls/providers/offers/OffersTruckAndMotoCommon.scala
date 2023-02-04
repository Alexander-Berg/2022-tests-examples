package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.offers

trait OffersTruckAndMotoCommon {
  protected def autoruId(l: Long) = s"autoru-$l"
  protected val OfferId1 = 1
  protected val OfferId2 = 2
  protected val OfferHash1 = "hash1"
  protected val OfferHash2 = "hash2"

  protected val Model1 = "model1"
  protected val Mark1 = "mark1"

  protected val Category = "some_category"

  protected val StateKeyNew = "NEW"
  protected val StateKeyBeaten = "BEATEN"
}
