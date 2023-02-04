package ru.yandex.vertis.general.gost.storage.testkit

import ru.yandex.vertis.general.gost.model.Offer.OfferId
import ru.yandex.vertis.general.gost.storage.FeedIdsMappingDao.FeedIdsMapping
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

object FeedIdsMappingGen {

  import ru.yandex.vertis.general.gost.model.testkit.FeedInfoGen._

  implicit val anyOfferIdGen: DeriveGen.Typeclass[OfferId] = DeriveGen.instance(Gen.anyString.map(OfferId(_)))

  val anyFeedIdsMapping: Gen[Random with Sized, FeedIdsMapping] = DeriveGen[FeedIdsMapping]

}
