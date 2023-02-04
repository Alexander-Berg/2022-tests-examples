package ru.auto.cabinet.test.gens

import org.scalacheck.Gen
import ru.auto.cabinet.model.ClientId
import ru.auto.cabinet.model.multiposting.{
  ExternalStatisticCounterRecord,
  Source
}
import ru.auto.cabinet.model.offer.OfferId

object MultipostingGens {

  private val offerIdGen: Gen[OfferId] =
    Gen.alphaStr.retryUntil(str => str.nonEmpty && str.length <= 256)

  private val sourceGen: Gen[Source] =
    Gen.alphaStr.retryUntil(str => str.nonEmpty && str.length <= 256)

  private val clientIdGen: Gen[ClientId] = Gen.posNum[ClientId]

  def externalStatisticCounterRecord(): Gen[ExternalStatisticCounterRecord] =
    for {
      offerId <- offerIdGen
      source <- sourceGen
      clientId <- clientIdGen
      views <- Gen.posNum[Long]
      phoneViews <- Gen.posNum[Long]
      favorites <- Gen.posNum[Long]
    } yield ExternalStatisticCounterRecord(
      offerId,
      source,
      clientId,
      views,
      phoneViews,
      favorites)
}
