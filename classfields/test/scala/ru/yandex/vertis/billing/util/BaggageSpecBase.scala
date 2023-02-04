package ru.yandex.vertis.billing.util

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.BaggageObjectId.OfferObjectId
import ru.yandex.vertis.billing.model_core.gens.{
  chargeableBaggagePayloadGen,
  BaggageGen,
  OfferObjectIdGen,
  Producer,
  RevenueBaggagePayloadsGenParams
}
import ru.yandex.vertis.billing.model_core.{Baggage, BaggagePayload, ChargeableBaggagePayload}

trait BaggageSpecBase extends AnyWordSpec with Matchers {

  def revenueBaggage(b: Baggage) =
    (b.payload, b.objectId) match {
      case (_: ChargeableBaggagePayload, _: OfferObjectId) => true
      case _ => false
    }

  def getOfferId(b: Baggage) = b.objectId match {
    case OfferObjectId(offerId) => offerId
    case other => fail(s"Unexpected $other")
  }

  def getRevenue(b: Baggage) = b.payload match {
    case c: ChargeableBaggagePayload => c.revenue
    case other => fail(s"Unexpected $other")
  }

  def revenueBaggagesGen(
      params: RevenueBaggagePayloadsGenParams = RevenueBaggagePayloadsGenParams()): Gen[Seq[Baggage]] = {
    def baggage(ids: Seq[OfferObjectId]): Gen[Baggage] =
      for {
        id <- Gen.oneOf(ids)
        source <- BaggageGen
        payload <- chargeableBaggagePayloadGen(params)
      } yield source.copy(payload = payload, objectId = id)
    for {
      ids <- Gen.listOfN(5, OfferObjectIdGen)
      bs <- Gen.const(baggage(ids).next(50))
    } yield bs.toSeq
  }

  val OfferBaggagesGen: Gen[Seq[Baggage]] = {
    def baggage(ids: Seq[OfferObjectId]): Gen[Baggage] =
      for {
        id <- Gen.oneOf(ids)
        source <- BaggageGen
        payload = BaggagePayload.Empty
      } yield source.copy(payload = payload, objectId = id)
    for {
      ids <- Gen.listOfN(5, OfferObjectIdGen)
      bs <- Gen.const(baggage(ids).next(50))
    } yield bs.toSeq
  }

  def revenueByOffer(bs: Iterable[Baggage]) =
    bs.groupBy(_.objectId).collect { case (OfferObjectId(offerId), group) =>
      val revenues = group.collect { case Baggage(_, _, _, _, _, c: ChargeableBaggagePayload, _, _, _, _) =>
        c.revenue
      }
      offerId.value -> revenues.sum
    }

}
