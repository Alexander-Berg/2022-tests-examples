package ru.yandex.vertis.general.gost.model.testkit

import general.feed.model.FeedSourceEnum.FeedSource
import general.feed.transformer.RawOffer
import ru.yandex.vertis.general.gost.model.Offer._
import ru.yandex.vertis.general.gost.model.OfferUpdate
import ru.yandex.vertis.general.gost.model.feed.{ErrorInfo, ErrorLevel, FeedOffer, FeedOfferError, UnificationErrorInfo}
import zio.random.Random
import zio.test.{Gen, Sized}

object FeedOfferGen {

  private def errorInfo =
    for {
      message <- Gen.alphaNumericStringBounded(5, 15)
      description <- Gen.alphaNumericStringBounded(5, 15)
      descriptionCode <- Gen.anyInt
    } yield UnificationErrorInfo(message, description, descriptionCode)

  private def normalFeedOffer(
      externalOfferId: Gen[Random with Sized, ExternalOfferId] = FeedInfoGen.anyExternalOfferId,
      offerUpdate: Gen[Random with Sized, OfferUpdate] = OfferUpdateGen.feedOfferUpdate,
      rawOffer: Gen[Random with Sized, RawOffer] = FeedInfoGen.anyRawOffer,
      errorInfo: Gen[Random with Sized, ErrorInfo] = errorInfo) =
    for {
      externalOfferId <- externalOfferId
      update <- offerUpdate
      errorInfos <- Gen.listOfBounded(0, 3)(errorInfo)
      errors = errorInfos.map { info =>
        FeedOfferError(
          title = update.title,
          errorLevel = ErrorLevel.Warning,
          errorInfo = info
        )
      }
      rawOffer <- rawOffer
    } yield FeedOffer(
      externalOfferId,
      Some(update),
      rawOffer,
      errors,
      FeedSource.FEED
    )

  val normalFeedOffer: Gen[Random with Sized, FeedOffer] = normalFeedOffer()

  val feedOfferWithPhoto: Gen[Random with Sized, FeedOffer] =
    normalFeedOffer(offerUpdate = OfferUpdateGen.anyOfferUpdate(photos = Gen.listOf1(OfferGen.anyPhoto)))

  val failedFeedOffer: Gen[Random with Sized, FeedOffer] =
    for {
      offer <- normalFeedOffer
      info <- errorInfo
      updatedOffer = offer.copy(
        offer = None,
        errors = offer.errors :+ FeedOfferError(title = offer.offer.get.title, errorLevel = ErrorLevel.Error, info)
      )
    } yield updatedOffer
}
