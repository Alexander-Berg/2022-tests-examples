package ru.yandex.vertis.billing

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.DetailsOperator.RevenueDetailsOperator
import ru.yandex.vertis.billing.model_core.EventStat.RevenueDetails
import ru.yandex.vertis.billing.model_core.gens.{
  chargeableBaggagePayloadGen,
  BaggageGen,
  NonChargeableBaggagePayloadGen,
  OfferObjectIdGen
}
import ru.yandex.vertis.billing.model_core.{Baggage, BaggageObjectId}
import ru.yandex.vertis.billing.util.BaggageSpecBase

/**
  * Runnable specs on [[ru.yandex.vertis.billing.DetailsOperator.RevenueDetailsOperator]]
  *
  * @author alex-kovalenko
  */
class RevenueDetailsOperatorSpec extends BaggageSpecBase with ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(workers = 10)

  "RevenueDetailsOperator" should {
    "get None for unsupported Baggage" in {
      val nonRevenueBaggageGen: Gen[Baggage] = for {
        source <- BaggageGen
        (idGen, payloadGen) <- Gen.oneOf(
          (OfferObjectIdGen, NonChargeableBaggagePayloadGen),
          (Gen.const(BaggageObjectId.Empty), chargeableBaggagePayloadGen())
        )
        id <- idGen
        payload <- payloadGen
      } yield source.copy(objectId = id, payload = payload)

      forAll(nonRevenueBaggageGen) { b =>
        RevenueDetailsOperator.get(b) shouldBe None
      }
    }

    "get Some for supported Baggage" in {
      val revenueBaggageGen: Gen[Baggage] = for {
        source <- BaggageGen
        id <- OfferObjectIdGen
        payload <- chargeableBaggagePayloadGen()
      } yield source.copy(objectId = id, payload = payload)
      forAll(revenueBaggageGen) { b =>
        RevenueDetailsOperator.get(b) match {
          case Some(RevenueDetails(items)) =>
            items should have size 1
            items(getOfferId(b).value) shouldBe getRevenue(b)
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "correctly fold baggages with repeated offer ids" in {
      forAll(revenueBaggagesGen()) { bs =>
        bs.map(RevenueDetailsOperator.get(_)).reduce(RevenueDetailsOperator.fold) match {
          case Some(RevenueDetails(items)) =>
            val expected = revenueByOffer(bs)
            items should contain theSameElementsAs expected
          case other => fail(s"Unexpected $other")
        }
      }
    }
  }
}
