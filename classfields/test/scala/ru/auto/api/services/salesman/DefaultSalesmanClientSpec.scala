package ru.auto.api.services.salesman

import java.time.LocalDate
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.apache.http.HttpStatus
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.api.billing.AutostrategyModel.OfferAutostrategies
import ru.auto.api.exceptions.{ClientNotFound, SalesmanBadRequest, UnableToActivateOfferException}
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.gen.SalesmanModelGenerators
import ru.auto.api.model.salesman.Campaign
import ru.auto.api.model.salesman.PaymentModel.{Calls, Quota, Single}
import ru.auto.api.model.{AutoruDealer, AutoruProduct, MetricProduct, OfferID, Paging}
import ru.auto.api.services.salesman.DefaultSalesmanClient._
import ru.auto.api.services.salesman.SalesmanClient.Good
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.FutureMatchers.failWith
import ru.auto.api.util.Protobuf
import ru.auto.salesman.offers.FreePlacement.FreePlacementRequest

import scala.jdk.CollectionConverters._

//noinspection ScalaUnnecessaryParentheses
class DefaultSalesmanClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with SalesmanModelGenerators
  with TestRequest {

  val client = new DefaultSalesmanClient(http)

  "Salesman client" should {

    "get autostrategies" in {
      forAll(Gen.nonEmptyListOf(OfferIDGen), Gen.listOf(AutostrategyGen)) { (offerIds, autostrategies) =>
        val params = offerIds.map(_.toPlain).map("offerId=" + _).mkString("&")
        http.expectUrl(GET, s"$BaseAutostrategiesUri?$params")
        http.expectHeader(`X-Salesman-User`)
        val offerAutostrategies = autostrategies
          .groupBy(_.getOfferId)
          .map {
            case (offerId, foundOfferAutostrategies) =>
              OfferAutostrategies
                .newBuilder()
                .setOfferId(offerId)
                .addAllAutostrategies(foundOfferAutostrategies.asJava)
                .build()
          }
          .toList
        val response = Protobuf.toJsonArray(offerAutostrategies, snakeCase = false)
        http.respondWith(OK, response)
        val result = client.getAutostrategies(offerIds).futureValue
        result shouldBe offerAutostrategies
      }
    }

    "put autostrategies" in {
      forAll(Gen.listOf(AutostrategyGen)) { autostrategies =>
        http.expectUrl(PUT, BaseAutostrategiesUri)
        http.expectHeader(`X-Salesman-User`)
        val body = Protobuf.toJsonArray(autostrategies, snakeCase = false)
        http.expectJson(body)
        http.respondWith(OK, unitResponse)
        client.putAutostrategies(autostrategies).futureValue shouldBe (())
      }
    }

    "delete autostrategies" in {
      forAll(Gen.listOf(AutostrategyIdGen)) { ids =>
        http.expectUrl(PUT, DeleteAutostrategiesUri)
        http.expectHeader(`X-Salesman-User`)
        val body = Protobuf.toJsonArray(ids, snakeCase = false)
        http.expectJson(body)
        http.respondWith(OK, unitResponse)
        client.deleteAutostrategies(ids).futureValue shouldBe (())
      }
    }

    "get campaigns" in {
      forAll(Gen.listOf(CampaignGen).map(_.distinct), DealerUserRefGen) { (campaigns, dealer) =>
        http.expectUrl(GET, BaseClientCampaignUri + dealer.clientId + "?include_disabled=false")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Json.toJson(campaigns).toString())
        client.getCampaigns(dealer, includeDisabled = false).futureValue should contain theSameElementsAs campaigns
      }
    }

    "get campaigns including disabled" in {
      forAll(Gen.listOf(CampaignGen).map(_.distinct), DealerUserRefGen) { (campaigns, dealer) =>
        http.expectUrl(GET, BaseClientCampaignUri + dealer.clientId + "?include_disabled=true")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Json.toJson(campaigns).toString())
        client.getCampaigns(dealer, includeDisabled = true).futureValue should contain theSameElementsAs campaigns
      }
    }

    "get campaigns from file" in {
      forAll(DealerUserRefGen) { dealer =>
        http.expectUrl(GET, BaseClientCampaignUri + dealer.clientId + "?include_disabled=false")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJsonFrom("/salesman/campaigns_response.json")
        val campaigns = client.getCampaigns(dealer, includeDisabled = false).futureValue
        campaigns shouldBe Set(
          Campaign(
            Single,
            "commercial",
            "commercial",
            List(
              "bus",
              "autoloader",
              "lcv",
              "trailer",
              "bulldozers",
              "agricultural",
              "crane_hydraulics",
              "swapbody",
              "dredge",
              "municipal",
              "artic",
              "crane",
              "trucks",
              "construction"
            ),
            List("used", "new"),
            2147483647,
            enabled = true
          ),
          Campaign(Single, "cars:used", "cars", Nil, List("used"), 2147483647, enabled = true),
          Campaign(
            Quota,
            "quota:placement:moto",
            "moto",
            List(
              "motorcycle",
              "amphibious",
              "atv",
              "scooters",
              "baggi",
              "carting",
              "snowmobile"
            ),
            List("new", "used"),
            25,
            enabled = true
          ),
          Campaign(Quota, "quota:placement:cars:new", "cars", Nil, List("new"), 2147483647, enabled = true)
        )
      }
    }

    "get campaigns including disabled from file" in {
      forAll(DealerUserRefGen) { dealer =>
        http.expectUrl(GET, BaseClientCampaignUri + dealer.clientId + "?include_disabled=false")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJsonFrom("/salesman/all_campaigns_response.json")
        val campaigns = client.getCampaigns(dealer, includeDisabled = false).futureValue
        campaigns shouldBe Set(
          Campaign(
            Quota,
            "quota:placement:moto",
            "moto",
            List(
              "motorcycle",
              "amphibious",
              "atv",
              "scooters",
              "baggi",
              "carting",
              "snowmobile"
            ),
            List("new", "used"),
            1500,
            enabled = true
          ),
          Campaign(
            Quota,
            "quota:placement:moto",
            "moto",
            List(
              "motorcycle",
              "amphibious",
              "atv",
              "scooters",
              "baggi",
              "carting",
              "snowmobile"
            ),
            List("new", "used"),
            25,
            enabled = false
          ),
          Campaign(
            Single,
            "commercial",
            "commercial",
            List(
              "bus",
              "autoloader",
              "lcv",
              "trailer",
              "bulldozers",
              "agricultural",
              "crane_hydraulics",
              "swapbody",
              "dredge",
              "municipal",
              "artic",
              "crane",
              "trucks",
              "construction"
            ),
            List("used", "new"),
            2147483647,
            enabled = true
          ),
          Campaign(Single, "cars:used", "cars", Nil, List("used"), 2147483647, enabled = false),
          Campaign(
            Quota,
            "quota:placement:moto",
            "moto",
            List(
              "motorcycle",
              "amphibious",
              "atv",
              "scooters",
              "baggi",
              "carting",
              "snowmobile"
            ),
            List("new", "used"),
            125,
            enabled = false
          ),
          Campaign(Calls, "call", "cars", Nil, List("new"), 2147483647, enabled = false)
        )
      }
    }

    "throw client not found while getting campaigns" in {
      forAll(DealerUserRefGen) { dealer =>
        http.expectUrl(GET, BaseClientCampaignUri + dealer.clientId + "?include_disabled=false")
        http.expectHeader(`X-Salesman-User`)
        http.respondWith(NotFound, "Client not found")
        client.getCampaigns(dealer, includeDisabled = false).failed.futureValue shouldBe a[ClientNotFound]
      }
    }

    "throw bad request while getting campaigns" in {
      forAll(DealerUserRefGen) { dealer =>
        http.expectUrl(GET, BaseClientCampaignUri + dealer.clientId + "?include_disabled=false")
        http.expectHeader(`X-Salesman-User`)
        http.respondWith(BadRequest, "Bad request")
        client.getCampaigns(dealer, includeDisabled = false).failed.futureValue shouldBe a[SalesmanBadRequest]
      }
    }

    "get call info" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/billing/campaign/call/client/20101/info")
      http.expectHeader(`X-Salesman-User`)
      http.respondWith("""{"callCost": 50000, "depositCoefficient": 3}""")
      val info = client.getCallInfo(AutoruDealer(20101)).futureValue
      info.callCost shouldBe 50000
      info.depositCoefficient shouldBe 3
    }

    "get schedules" in {
      forAll(scheduleResponseGen(), DealerUserRefGen) { (schedules, dealer) =>
        http.expectUrl(GET, BaseSchedulesUri + "/client/" + dealer.clientId)
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Protobuf.toJson(schedules))
        val result = client.getSchedules(dealer, products = Nil, offerIds = Nil).futureValue
        result shouldBe schedules
      }
    }

    "get schedules with product and offers" in {
      forAll(scheduleResponseGen(), DealerUserRefGen) { (schedules, dealer) =>
        http.expectUrl(GET, BaseSchedulesUri + "/client/" + dealer.clientId + "?product=boost&offerId=1-a&offerId=2-b")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Protobuf.toJson(schedules))
        val products = List(Boost)
        val offerIds = List(OfferID(1, Some("a")), OfferID(2, Some("b")))
        val result = client.getSchedules(dealer, products, offerIds).futureValue
        result shouldBe schedules
      }
    }

    "put schedules" in {
      forAll(scheduleRequestGen, DealerUserRefGen) { (scheduleRequest, dealer) =>
        http.expectUrl(PUT, BaseSchedulesUri + "/client/" + dealer.clientId + "/product/boost?offerId=111&offerId=222")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Protobuf.toJson(successResponse))
        val offerIds = List(OfferID(111, hash = None), OfferID(222, hash = None))
        val result = client.putSchedules(dealer, Boost, offerIds, scheduleRequest).futureValue
        result shouldBe successResponse
      }
    }

    "delete schedules" in {
      forAll(DealerUserRefGen) { (dealer) =>
        http.expectUrl(
          DELETE,
          BaseSchedulesUri + "/client/" + dealer.clientId + "?product=boost&offerId=111&offerId=222"
        )
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Protobuf.toJson(successResponse))
        val products = List(Boost)
        val offerIds = List(OfferID(111, hash = None), OfferID(222, hash = None))
        val result = client.deleteSchedules(dealer, products, offerIds).futureValue
        result shouldBe successResponse
      }
    }

    "get trade-in requests" in {
      forAll(DealerUserRefGen, tradeInRequestsGen) { (dealer, tradeIn) =>
        http.expectUrl(GET, BaseTradeInUri + dealer.clientId + "?from=2019-01-01&pageNum=1&pageSize=10")
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Json.toJson(tradeIn).toString())
        val result = client.getTradeInRequests(dealer.clientId, None, LocalDate.of(2019, 1, 1), None, 1, 10).futureValue
        result shouldBe tradeIn
      }
    }

    "get offers with paid services ids" in {
      forAll(DealerUserRefGen, offersListingGen) { (dealer, offersListing) =>
        http.expectUrl(
          GET,
          BaseOffersUri + "/client/" + dealer.clientId + "?from=2019-01-01&to=2019-01-04" +
            "&pageSize=10&pageNum=0&product=placement&product=fresh-offers"
        )
        http.expectHeader(`X-Salesman-User`)
        http.respondWithJson(Json.toJson(offersListing).toString())
        val from = LocalDate.of(2019, 1, 1)
        val to = LocalDate.of(2019, 1, 4)
        val productsFilter = List(MetricProduct.Placement, MetricProduct.FreshOffers)
        val pageSize = 10
        val pageNum = 0
        val result =
          client.getOffersWithPaidProductsIdentities(dealer, from, to, productsFilter, pageSize, pageNum).futureValue
        result shouldBe (offersListing)
      }
    }

    "get goods" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/goods/category/cars?offer=1082691044")
      http.expectHeader(`X-Salesman-User`)
      http.respondWithJsonFrom("/salesman/goods.json")
      val goods = client.getGoods(OfferID.parse("1082691044-09f456bf"), CARS).futureValue
      goods shouldBe List(Good(Highlighting), Good(SpecialOffer), Good(TopList), Good(PackageTurbo))
    }

    "get truck goods" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/goods/category/commercial?offer=1082691044")
      http.expectHeader(`X-Salesman-User`)
      http.respondWithJsonFrom("/salesman/goods.json")
      val goods = client.getGoods(OfferID.parse("1082691044-09f456bf"), TRUCKS).futureValue
      goods shouldBe List(Good(Highlighting), Good(SpecialOffer), Good(TopList), Good(PackageTurbo))
    }

    "get moto goods" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/goods/category/moto?offer=1082691044")
      http.expectHeader(`X-Salesman-User`)
      http.respondWithJsonFrom("/salesman/goods.json")
      val goods = client.getGoods(OfferID.parse("1082691044-09f456bf"), MOTO).futureValue
      goods shouldBe List(Good(Highlighting), Good(SpecialOffer), Good(TopList), Good(PackageTurbo))
    }

    "get client vin history bought reports" in {
      forAll(DealerUserRefGen) { (dealer) =>
        val expectedPath = s"/api/1.x/service/autoru/product/vin-history/client/${dealer.clientId}/reports"
        val expectedSearchParams = "vin=XXX123&offerId=123-fff&onlyActive=false&pageNum=2&pageSize=20"

        http.expectUrl(GET, expectedPath + "?" + expectedSearchParams)
        http.expectHeader(`X-Salesman-User`)
        http.respondWithStatus(OK)

        client.getBoughtVinHistoryReports(
          dealer,
          vin = Some("XXX123"),
          offerId = Some(OfferID.parse("123-fff")),
          createdFrom = None,
          createdTo = None,
          onlyActive = false,
          Paging(page = 2, pageSize = 20)
        )
      }
    }

    "get offer placement days" in {
      forAll(HashedOfferIDGen) { (offerId) =>
        val expectedPath = s"/api/1.x/service/autoru/offers/category/commercial/$offerId/placement"
        http.expectUrl(GET, expectedPath)
        http.expectHeader(`X-Salesman-User`)
        http.respondWithStatus(OK)
        client.productActiveDays(offerId.toString, Category.TRUCKS, AutoruProduct.Placement)
      }
    }

    "validate offer activation" in {
      forAll(OfferGen) { offer =>
        val dealer = AutoruDealer(100500)
        http.expectUrl(POST, s"$BaseOffersUri/client/100500/validation/product/placement")
        http.expectProto(offer)
        http.respondWithStatus(OK)

        client.validateActivation(dealer, offer, AutoruProduct.Placement).futureValue
      }
    }

    "validate offer activation should not fail on 500" in {
      forAll(OfferGen) { offer =>
        val dealer = AutoruDealer(100500)
        http.expectUrl(POST, s"$BaseOffersUri/client/100500/validation/product/placement")
        http.expectProto(offer)
        http.respondWithStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)

        client.validateActivation(dealer, offer, AutoruProduct.Placement).futureValue
      }
    }

    "validate offer activation should fail on 402" in {
      forAll(OfferGen) { offer =>
        val dealer = AutoruDealer(100500)
        http.expectUrl(POST, s"$BaseOffersUri/client/100500/validation/product/placement")
        http.expectProto(offer)
        http.respondWithStatus(HttpStatus.SC_PAYMENT_REQUIRED)

        client.validateActivation(dealer, offer, AutoruProduct.Placement) should failWith[
          UnableToActivateOfferException
        ]
      }
    }

    "allow free placement" in {
      forAll(HashedOfferIDGen) { offerId =>
        val rq = FreePlacementRequest.newBuilder().setOfferId(offerId.toString).build()
        val expectedPath = s"/api/1.x/service/autoru/offers/free/placement"

        http.expectUrl(PUT, expectedPath)
        http.expectHeader(`X-Salesman-User`)
        http.expectProto(rq)
        http.respondWithStatus(OK)

        client.allowFreePlacement(rq).futureValue
      }
    }
  }

  private val unitResponse = """{"message": "OK"}"""

  private val successResponse = SuccessResponse
    .newBuilder()
    .setStatus(ResponseStatus.SUCCESS)
    .build()
}
