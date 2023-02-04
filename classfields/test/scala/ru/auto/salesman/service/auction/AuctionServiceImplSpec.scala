package ru.auto.salesman.service.auction

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Category, Offer, Salon, Section, SellerType}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.salesman.client.auctions.AuctionsClient
import ru.auto.salesman.client.auctions.AuctionsClient.BidInfo
import ru.auto.salesman.client.auctions.model.AuctionRequest
import ru.auto.salesman.model.criteria.Criterion
import ru.auto.salesman.model.{
  AutoruDealer,
  CityId,
  Client,
  ClientStatuses,
  ProductId,
  RegionId
}
import ru.auto.salesman.model.criteria.CriteriaContext.CallCarsNewCriteriaContext
import ru.auto.salesman.service.auction.AuctionService.AuctionError._
import ru.auto.salesman.service.auction.AuctionServiceImpl._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{AutoruOfferIdGen, OfferModelGenerators}
import zio.Exit

class AuctionServiceImplSpec extends BaseSpec {
  import AuctionServiceImplSpec._

  private val auctionClient = mock[AuctionsClient]
  private val service = new AuctionServiceImpl(auctionClient)

  "getBidForCall" should {
    "get bid from auction" in {
      val req = AuctionRequest(
        AutoruDealer(testClient.clientId),
        ProductId.Call,
        CallCarsNewCriteriaContext(
          List(
            Criterion("region_id", testClient.regionId.toString),
            Criterion("mark", testMark),
            Criterion("model", testModel)
          )
        )
      )

      (auctionClient
        .bidByDateTime(_: AuctionRequest[ProductId.Call.type], _: DateTime))
        .expects(req, callTime)
        .returningZ(Some(BidInfo(testBid, auctionBlock = false)))

      val result = service
        .getBid[ProductId.Call.type](testClient, testOffer(true), callTime)
        .success
        .value

      result shouldBe Some(BidInfo(testBid, auctionBlock = false))
    }

    "return None if no bid" in {
      (auctionClient
        .bidByDateTime(_: AuctionRequest[ProductId.Call.type], _: DateTime))
        .expects(*, callTime)
        .returningZ(None)

      val result = service
        .getBid[ProductId.Call.type](testClient, testOffer(true), callTime)
        .success
        .value

      result shouldBe None
    }

    "fail with TransportError if auction client failed" in {
      (auctionClient
        .bidByDateTime(_: AuctionRequest[ProductId.Call.type], _: DateTime))
        .expects(*, callTime)
        .throwingZ(new Exception())

      val result = service
        .getBid[ProductId.Call.type](testClient, testOffer(true), callTime)
        .failure

      result shouldBe an[Exit.Failure[TransportError]]
    }

    "fail with InappropriateOfferError if offer has empty car info" in {
      val result = service
        .getBid[ProductId.Call.type](testClient, testOffer(false), callTime)
        .failure

      result shouldBe an[Exit.Failure[InappropriateOfferError.type]]
    }
  }
}

object AuctionServiceImplSpec extends OfferModelGenerators {

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

  private val testMark = "BMW"
  private val testModel = "X5"
  private val callTime = DateTime.now()
  private val testBid = 50000

  private def testOffer(withInfo: Boolean): Offer = {
    val offer = Offer
      .newBuilder()
      .setId(AutoruOfferIdGen.next.value)
      .setSellerType(SellerType.COMMERCIAL)
      .setCategory(Category.CARS)
      .setSection(Section.NEW)
      .setSalon(
        Salon
          .newBuilder()
          .setClientId(testClient.clientId.toString)
      )
    if (withInfo)
      offer.setCarInfo(
        CarInfo
          .newBuilder()
          .setMark(testMark)
          .setModel(testModel)
      )

    offer.build()
  }

}
