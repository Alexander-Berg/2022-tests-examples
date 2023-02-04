package ru.yandex.vertis.billing

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.model_core.EventStat.{
  CommonRawEventDetail,
  Details,
  DetailsConversionException,
  ItemDetails,
  RawEventDetails,
  RevenueDetails
}
import ru.yandex.vertis.billing.model_core.{OrderTransactions, Withdraw2}
import ru.yandex.vertis.billing.model_core.gens.{
  orderTransactionGen,
  rawEventDetailsGen,
  AsOffer,
  AsPartlyRevenue,
  AsRandomCall,
  AsRevenue,
  OrderTransactionGenParams,
  RawEventDetailState
}

class RawEventDetailsToOldDetailsConversionSpec
  extends AnyWordSpec
  with Matchers
  with ScalaCheckPropertyChecks
  with DetailsHelper {

  private def genForState(state: RawEventDetailState): Gen[Withdraw2] = {
    for {
      wraped <- orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Withdraw))
      d <- rawEventDetailsGen(state)
      unwraped = wraped match {
        case w: Withdraw2 => w.copy(details = Some(d))
        case _ => throw new IllegalArgumentException("unexpected")
      }
    } yield unwraped
  }

  private def mapDetails(w: Withdraw2, mapper: RawEventDetails => Details): Withdraw2 = {
    w.details match {
      case Some(r: RawEventDetails) => w.copy(details = Some(mapper(r)))
      case _ => w
    }
  }

  "RawEventDetailsToOldDetailsConversionSpec" should {
    "work correct with revenue details" in {
      forAll(genForState(AsRevenue)) { r =>
        val map = r.details.map {
          case RawEventDetails(elements) =>
            elements
              .map {
                case CommonRawEventDetail(_, Some(offerId), Some(revenue)) =>
                  offerId.value -> revenue
                case other =>
                  throw new IllegalArgumentException(s"unexpected $other")
              }
              .groupBy(_._1)
          case _ =>
            throw new IllegalArgumentException("unexpected")
        }

        val actual = RevenueDetails(map.get.view.mapValues(_.map(_._2).sum).toMap)
        val expected = mapDetails(r, toRevenueDetails)
        actual should be(expected.details.get)
      }
    }
    "work correct with item details" in {
      forAll(genForState(AsOffer)) { r =>
        val items = r.details.map {
          case RawEventDetails(elements) =>
            elements.map {
              case CommonRawEventDetail(_, Some(offerId), None) =>
                offerId.value
              case other =>
                throw new IllegalArgumentException(s"unexpected $other")
            }
          case _ =>
            throw new IllegalArgumentException("unexpected")
        }
        val actual = ItemDetails(items.get)
        val expected = mapDetails(r, toItemDetails)
        (actual should be).equals(expected.details.get)
      }
    }
    "work correct with partly revenue details" in {
      forAll(genForState(AsPartlyRevenue)) { r =>
        intercept[DetailsConversionException] {
          mapDetails(r, toRevenueDetails)
        }
      }
    }
    "work correct with call details" in {
      forAll(genForState(AsRandomCall)) { r =>
        intercept[DetailsConversionException] {
          mapDetails(r, toRevenueDetails)
        }
      }
    }
  }

}
