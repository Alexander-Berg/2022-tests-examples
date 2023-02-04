package ru.yandex.vertis.general.feed.model.testkit

import ru.yandex.vertis.general.common.errors.FeedValidationErrors
import ru.yandex.vertis.general.feed.model.{ErrorLevel, FailedOffer}
import zio.test.Gen
import zio.test.magnolia.DeriveGen

object FailedOfferGen {

  implicit val anyFailedFeerErrorCode: DeriveGen[FeedValidationErrors.Value] = DeriveGen.instance(
    Gen.int(0, FeedValidationErrors.values.size - 1).map(idx => FeedValidationErrors.values.toList(idx))
  )

  val any = DeriveGen[FailedOffer].flatMap { failedOffer =>
    for {
      externalOfferId <- Gen.alphaNumericStringBounded(10, 15).map(_.toLowerCase) // to comply with postgres sorting
    } yield failedOffer.copy(key = failedOffer.key.copy(externalOfferId = externalOfferId))
  }

  val warnings = any.map(_.copy(errorLevel = ErrorLevel.Warning))

  val errors = any.map(_.copy(errorLevel = ErrorLevel.Error))
}
