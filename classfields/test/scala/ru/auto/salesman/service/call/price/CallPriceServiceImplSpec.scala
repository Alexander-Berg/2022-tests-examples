package ru.auto.salesman.service.call.price

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Offer, _}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.salesman.Task
import ru.auto.salesman.client.auctions.AuctionsClient.BidInfo
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.service.DealerMarksService
import ru.auto.salesman.service.auction.AuctionService
import ru.auto.salesman.service.auction.AuctionService.AuctionError.{
  InappropriateOfferError,
  TransportError
}
import ru.auto.salesman.service.auction.AuctionService.HasAuction
import ru.auto.salesman.service.call.cashback.CallCashbackService
import ru.auto.salesman.service.call.cashback.domain.CallId
import ru.auto.salesman.service.call.price.CallPriceService.PriceNotFound
import ru.auto.salesman.service.howmuch.HowMuchService
import ru.auto.salesman.service.howmuch.HowMuchService.HasPrice
import ru.auto.salesman.test.BaseSpec
import zio._

trait CallPriceServiceImplSpec[T1 <: ProductId, T2] extends BaseSpec {
  import CallPriceServiceImplSpec._

  private val auctionService = mock[AuctionService]
  private val howMuchService = mock[HowMuchService]
  private val dealerMarksMock = mock[DealerMarksService]
  private val callCashbackService = mock[CallCashbackService]

  private val dealerMarks = new DealerMarksService {

    def getMarks(
        clientId: ClientId,
        category: Category,
        section: Option[Section]
    ): Task[List[OfferMark]] =
      ZIO.effectSuspend(dealerMarksMock.getMarks(clientId, category, section))

  }

  val service = new CallPriceServiceImpl(
    auctionService,
    howMuchService,
    dealerMarks,
    callCashbackService
  )

  "CallPriceServiceImpl.getCallPrice" should {
    "select call price as maximum from base price and auction bid" in {
      val prices = Table(
        ("price", "bid"),
        (1000, 2000),
        (2000, 1000),
        (1000, 1000)
      )

      forAll(prices) { case (price, bid) =>
        (auctionService
          .getBid(_: Client, _: Offer, _: DateTime)(
            _: HasAuction[T1]
          ))
          .expects(*, *, *, *)
          .returningZ(Some(BidInfo(bid, auctionBlock = false)))

        (howMuchService
          .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
            _: HasPrice[T1, Offer]
          ))
          .expects(*, *, *, *)
          .returningZ(Some(price))

        (howMuchService
          .getPriceForClient(_: Client, _: DateTime, _: T2)(
            _: HasPrice[T1, T2]
          ))
          .expects(*, *, *, *)
          .never()

        service
          .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
          .success
          .value shouldBe Math.max(price, bid)
      }
    }

    "select call price equal to base price if there is no auction bids" in {
      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .returningZ(None)

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      service
        .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
        .success
        .value shouldBe 50000
    }

    "select call price equal to base price if there is auction block" in {
      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(BidInfo(500000, auctionBlock = true)))

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      service
        .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
        .success
        .value shouldBe 50000
    }

    "select call price equal to base price if offer is inappropriate (fallback to no bid)" in {
      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .returning(ZIO.fail(InappropriateOfferError))

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      service
        .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
        .success
        .value shouldBe 50000
    }

    "select call price equal to base client price if there is no offer" in {
      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .never

      (howMuchService
        .getPriceForClient(_: Client, _: DateTime, _: T2)(
          _: HasPrice[T1, T2]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      service
        .getCallPrice(testClient, None, None, None, testCallTime)
        .success
        .value shouldBe 50000
    }

    "fail if there is no price for offer" in {
      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(None)

      service
        .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
        .failure
        .exception shouldBe PriceNotFound
    }

    "fail if some transport error occurred during auction invocation" in {
      val error = new Exception("Some transport error")

      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .returning(ZIO.fail(TransportError(error)))

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      service
        .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
        .failure
        .exception shouldBe error
    }

    "fail if some transport error occurred during howmuch invocation (i.e. unable to load dealer marks for new call)" in {
      val error = new Exception("Some error")

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returning(ZIO.fail(error))

      service
        .getCallPrice(testClient, Some(testOffer(true)), None, None, testCallTime)
        .failure
        .exception shouldBe error
    }

    "decrease price according to withdrawn cashback amount" in {
      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .returningZ(None)

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      (callCashbackService.getWithdrawnCashbackAmount _)
        .expects(testClient, callId)
        .returningZ(Some(30000))

      service
        .getCallPrice(
          testClient,
          Some(testOffer(true)),
          None,
          Some(callId),
          testCallTime
        )
        .success
        .value shouldBe 20000
    }

    "never make price lower than 1 Ruble" in {
      (auctionService
        .getBid(_: Client, _: Offer, _: DateTime)(
          _: HasAuction[T1]
        ))
        .expects(*, *, *, *)
        .returningZ(None)

      (howMuchService
        .getPriceForOffer(_: Client, _: DateTime, _: Offer)(
          _: HasPrice[T1, Offer]
        ))
        .expects(*, *, *, *)
        .returningZ(Some(50000))

      (callCashbackService.getWithdrawnCashbackAmount _)
        .expects(testClient, callId)
        .returningZ(Some(50000))

      service
        .getCallPrice(
          testClient,
          Some(testOffer(true)),
          None,
          Some(callId),
          testCallTime
        )
        .success
        .value shouldBe 100
    }

  }

}

object CallPriceServiceImplSpec {
  private val testCallTime = DateTime.now()
  private val testMark = "BMW"
  private val testModel = "X5"
  private val testPoiId = 18225L
  private val testOfferId = OfferIdentity("123-abcd")
  private val testGeneration = 100500

  private val callId = CallId("id")

  private val testClient =
    Client(
      clientId = 23965,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = true,
      priorityPlacement = true
    )

  private def testOffer(
      withInfo: Boolean,
      section: Section = Section.USED
  ): Offer = {
    val offer = Offer
      .newBuilder()
      .setId(testOfferId.value)
      .setSellerType(SellerType.COMMERCIAL)
      .setCategory(Category.CARS)
      .setSection(section)
      .setSalon(
        Salon
          .newBuilder()
          .setClientId(testClient.clientId.toString)
          .setSalonId(testPoiId)
      )
    if (withInfo)
      offer.setCarInfo(
        CarInfo
          .newBuilder()
          .setMark(testMark)
          .setModel(testModel)
          .setSuperGenId(testGeneration)
      )

    offer.build()
  }
}
