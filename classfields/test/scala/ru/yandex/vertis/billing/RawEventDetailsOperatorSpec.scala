package ru.yandex.vertis.billing

import org.scalacheck.{Gen, Shrink}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.DetailsOperator.{ItemDetailsOperator, RawEventDetailsOperator, RevenueDetailsOperator}
import ru.yandex.vertis.billing.model_core.Baggage
import ru.yandex.vertis.billing.model_core.BaggageObjectId.OfferObjectId
import ru.yandex.vertis.billing.util.BaggageSpecBase
import ru.yandex.vertis.billing.model_core.EventStat.{
  CallRawEventDetail,
  CommonRawEventDetail,
  DetailsConversionException,
  ItemDetails,
  RawEventDetails,
  RevenueDetails
}
import ru.yandex.vertis.billing.model_core.gens.{
  BaggageGen,
  CallWithStatusBaggagePayloadGen,
  OfferObjectIdGen,
  Producer,
  RevenueBaggagePayloadsGenParams
}

import scala.util.Random

class RawEventDetailsOperatorSpec extends BaggageSpecBase with ScalaCheckPropertyChecks with DetailsHelper {

  implicit def shrinkAny[T]: Shrink[T] = Shrink.shrinkAny

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(workers = 5)

  val BadBaggagesGen = for {
    rs <- revenueBaggagesGen()
    os <- OfferBaggagesGen
    badBaggage = Random.shuffle(rs ++ os)
  } yield badBaggage

  "work with revenue" in {
    val params = RevenueBaggagePayloadsGenParams().withoutCallWithStatusBaggagePayload
    forAll(revenueBaggagesGen(params)) { baggages =>
      val actualExtracted = baggages.map { baggage =>
        RawEventDetailsOperator.get(baggage)
      }
      val actual = actualExtracted.reduce(RawEventDetailsOperator.fold)
      val expectedExtracted = baggages.map { baggage =>
        RevenueDetailsOperator.get(baggage)
      }
      val expected = expectedExtracted.reduce(RevenueDetailsOperator.fold)

      (actual, expected) match {
        case (Some(ra: RawEventDetails), Some(b: RevenueDetails)) =>
          val a = toRevenueDetails(ra)
          a.items should contain theSameElementsAs b.items
        case other => fail(s"Unexpected $other")
      }
    }
  }

  "work with offers" in {
    forAll(OfferBaggagesGen) { baggages =>
      val actual = baggages
        .map { baggage =>
          RawEventDetailsOperator.get(baggage)
        }
        .reduce(RawEventDetailsOperator.fold)
      val expected = baggages
        .map { baggage =>
          ItemDetailsOperator.get(baggage)
        }
        .reduce(ItemDetailsOperator.fold)

      (actual, expected) match {
        case (Some(ra: RawEventDetails), Some(b: ItemDetails)) =>
          val a = toItemDetails(ra)
          a.items should contain theSameElementsAs b.items
        case other => fail(s"Unexpected $other")
      }
    }
  }

  "not convert to RawEventDetails and ItemDetails" in {
    forAll(BadBaggagesGen) { baggages =>
      val result = baggages
        .map { baggage =>
          RawEventDetailsOperator.get(baggage)
        }
        .reduce(RawEventDetailsOperator.fold)

      result match {
        case Some(s: RawEventDetails) =>
          intercept[DetailsConversionException] {
            toRevenueDetails(s)
          }
          intercept[DetailsConversionException] {
            toItemDetails(s)
          }
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

  "map baggage with CallWithStatusBaggagePayload correctly" in {
    val params = RevenueBaggagePayloadsGenParams.empty.withCallWithStatusBaggagePayload
    forAll(revenueBaggagesGen(params)) { baggages =>
      val actual = baggages
        .map { baggage =>
          RawEventDetailsOperator.get(baggage)
        }
        .reduce(RawEventDetailsOperator.fold)
        .get

      actual match {
        case RawEventDetails(items) =>
          val commonRawEventDetail = items.exists {
            case _: CommonRawEventDetail => true
            case _ => false
          }
          commonRawEventDetail shouldBe false
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

}
