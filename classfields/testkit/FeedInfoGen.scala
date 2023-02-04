package ru.yandex.vertis.general.gost.model.testkit

import general.feed.transformer.RawOffer
import general.gost.feed_api.NamespaceId
import ru.yandex.vertis.general.gost.model.Offer.{ExternalOfferId, FeedInfo}
import zio.random.Random
import zio.test.magnolia.DeriveGen.instance
import zio.test.magnolia._
import zio.test.{Gen, Sized}

object FeedInfoGen {

  implicit private val unknownFieldSetGen: DeriveGen[_root_.scalapb.UnknownFieldSet] =
    DeriveGen.instance(Gen.const(_root_.scalapb.UnknownFieldSet.empty))

  implicit val externalOfferIdGen: DeriveGen[ExternalOfferId] =
    instance(Gen.alphaNumericStringBounded(5, 15).map(ExternalOfferId(_)))

  implicit val NamespaceIdGen: DeriveGen[NamespaceId] =
    instance(Gen.alphaNumericStringBounded(5, 15).map(NamespaceId(_)))

  val anyNamespaceIdGen: Gen[Random with Sized, NamespaceId] = NamespaceIdGen.derive

  val any: Gen[Random with Sized, FeedInfo] = DeriveGen[FeedInfo]

  val anyExternalOfferId: Gen[Random with Sized, ExternalOfferId] =
    externalOfferIdGen.derive

  val anyRawOffer: Gen[Random with Sized, RawOffer] = DeriveGen[RawOffer]
}
