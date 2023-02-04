package ru.auto.comeback.consumer.test

import ru.auto.api.comeback_model.Comeback.EventType
import ru.auto.comeback.PastEventsBuilder
import ru.auto.comeback.consumer.test.ComebackBuilderSpec.instant
import ru.auto.comeback.model.PastEvents.EventInfo
import ru.auto.comeback.model.testkit.PastEventsGen._
import ru.auto.comeback.model.testkit.{CarfaxHistoryOfferGen, OfferGen}
import zio.test.Assertion._
import zio.test._

object PastEventsBuilderSpec extends DefaultRunnableSpec {

  val now = instant("2020-01-29T12:00:00Z")
  val past1 = instant("2019-08-23T13:00:00Z")
  val past2 = instant("2019-05-29T12:00:00Z")
  val past3 = instant("2018-05-01T00:00:00Z")
  val past4 = instant("2015-04-19T02:03:00Z")
  val past5 = instant("2014-12-11T10:00:00Z")
  val past6 = instant("2018-10-10T23:00:54Z")
  val past7 = instant("2019-07-10T23:00:54Z")

  val past = Gen.oneOf(past1, past2, past3, past4, past5)

  def genPastEvents =
    for {
      externalOffer <- externalOffer(date = past7)
      externalSale <- externalSale(date = past2)
      maintenance <- maintenance(date = past1)
      estimate <- estimate(date = past4)
      saleOfInsurance <- saleOfInsurance(date = past5)
      carfaxReportPurchase <- carfaxReportPurchase(date = past6)
    } yield (externalOffer, externalSale, maintenance, estimate, saleOfInsurance, carfaxReportPurchase)

  override def spec =
    suite("PastEventsBuilder")(
      testM("return empty past offer if offer created before past offer")(
        check(
          OfferGen.offer(activated = past),
          CarfaxHistoryOfferGen.offer(activated = now)
        ) { (offer, pastOffer) =>
          val res = PastEventsBuilder.build(offer, Some(pastOffer), None, None, None, None, None, None)
          assert(res.pastOffer)(isNone)
        }
      ),
      testM("return past offer if offer created after past offer")(
        check(
          OfferGen.offer(activated = now),
          CarfaxHistoryOfferGen.offer(activated = past)
        ) { (offer, pastOffer) =>
          val res = PastEventsBuilder.build(offer, Some(pastOffer), None, None, None, None, None, None)
          assert(res.pastOffer)(isSome(anything))
        }
      ),
      testM("return empty external offer if offer create before past external offer")(
        check(OfferGen.offer(activated = past), externalOffer(date = now)) { (offer, pastExternalOffer) =>
          val res = PastEventsBuilder.build(offer, None, Some(pastExternalOffer), None, None, None, None, None)
          assert(res.pastExternalSale)(isNone)
        }
      ),
      testM("return empty external sale if offer create before past external sale")(
        check(OfferGen.offer(activated = past), externalSale(date = now)) { (offer, pastExternalSale) =>
          val res = PastEventsBuilder.build(offer, None, None, None, Some(pastExternalSale), None, None, None)
          assert(res.pastExternalSale)(isNone)
        }
      ),
      testM("return empty past maintenance if offer created before past maintenance")(
        check(OfferGen.offer(activated = past), maintenance(date = now)) { (offer, pastMaintenance) =>
          val res = PastEventsBuilder.build(offer, None, None, Some(pastMaintenance), None, None, None, None)
          assert(res.pastMaintenance)(isNone)
        }
      ),
      testM("return empty past estimate if offer created before past estimate")(
        check(OfferGen.offer(activated = past), estimate(date = now)) { (offer, pastEstimate) =>
          val res = PastEventsBuilder.build(offer, None, None, None, None, Some(pastEstimate), None, None)
          assert(res.pastEstimate)(isNone)
        }
      ),
      testM("return empty past sale of insurance if offer created before past sale of insurance")(
        check(OfferGen.offer(activated = past), saleOfInsurance(date = now)) { (offer, pastSaleOfInsurance) =>
          val res = PastEventsBuilder.build(offer, None, None, None, None, None, Some(pastSaleOfInsurance), None)
          assert(res.pastInsuranceSale)(isNone)
        }
      ),
      testM("return empty past order if offer created before past order")(
        check(OfferGen.offer(activated = past), carfaxReportPurchase(date = now)) { (offer, carfaxReportPurchase) =>
          val res = PastEventsBuilder.build(offer, None, None, None, None, None, None, Some(carfaxReportPurchase))
          assert(res.pastCarfaxReportPurchase)(isNone)
        }
      ),
      testM("return correct last event info")(
        check(
          OfferGen.offer(activated = now),
          CarfaxHistoryOfferGen.offer(activated = past3),
          genPastEvents
        ) {
          case (
                offer,
                pastOffer,
                (
                  pastExternalOffer,
                  pastExternalSale,
                  pastMaintenance,
                  pastEstimate,
                  pastSaleOfInsurance,
                  pastCarfaxReportPurchase
                )
              ) =>
            val res = PastEventsBuilder.build(
              offer,
              Some(pastOffer),
              Some(pastExternalOffer),
              Some(pastMaintenance),
              Some(pastExternalSale),
              Some(pastEstimate),
              Some(pastSaleOfInsurance),
              Some(pastCarfaxReportPurchase)
            )
            val lastEventInfo = EventInfo(EventType.MAINTENANCE, pastMaintenance.date)
            assert(res.getLastEventInfo)(equalTo(lastEventInfo))
        }
      )
    )
}
