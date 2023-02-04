package ru.auto.salesman.api.v1.service

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.auto.salesman.api.RoutingSpec

class HandlerSpec extends RoutingSpec {

  private val quotaHandlerRoute: Route = complete(StatusCodes.OK, "quota")
  private val tariffHandlerRoute: Route = complete(StatusCodes.OK, "tariff")
  private val adsHandlerRoute: Route = complete(StatusCodes.OK, "ads")
  private val goodsHandlerRoute: Route = complete(StatusCodes.OK, "goods")
  private val campaignHandlerRoute: Route = complete(StatusCodes.OK, "campaign")

  private val autostrategiesHandlerRoute: Route =
    complete(StatusCodes.OK, "autostrageties")

  private val billingCampaignHandlerRoute: Route =
    complete(StatusCodes.OK, "billing")
  private val productHandlerRoute: Route = complete(StatusCodes.OK, "product")

  private val schedulesHandlerRoute: Route =
    complete(StatusCodes.OK, "schedules")
  private val tradeInHandlerRoute: Route = complete(StatusCodes.OK, "trade-in")
  private val offersHandlerRoute: Route = complete(StatusCodes.OK, "offers")
  private val featuresHandlerRoute: Route = complete(StatusCodes.OK, "features")

  private val configurationsHandlerRoute: Route =
    complete(StatusCodes.OK, "configurations")
  private val teleponyHandlerRoute: Route = complete(StatusCodes.OK, "telepony")
  private val cashbackHandlerRoute: Route = complete(StatusCodes.OK, "cashback")

  private val matchApplicationHandlerRoute: Route =
    complete(StatusCodes.OK, "match-applications")

  private val productsHandlerRoute: Route =
    complete(StatusCodes.OK, "products")

  private val amoyakSyncRequestsHandlerRoute: Route =
    complete(StatusCodes.Created, "sync-requests")

  private val route = new Handler {
    override protected def quotaHandler: Route = quotaHandlerRoute

    override protected def tariffHandler: Route = tariffHandlerRoute

    override protected def goodsHandler: Route = goodsHandlerRoute

    override protected def adsHandler: Route = adsHandlerRoute

    override protected def campaignHandler: Route = campaignHandlerRoute

    override protected def autostrategiesHandler: Route =
      autostrategiesHandlerRoute

    override protected def billingCampaignHandler: Route =
      billingCampaignHandlerRoute

    override protected def productHandler: Route = productHandlerRoute

    override protected def schedulesHandler: Route = schedulesHandlerRoute

    override protected def tradeInHandler: Route = tradeInHandlerRoute

    override protected def offersHandler: Route = offersHandlerRoute

    override protected def featuresHandler: Route = featuresHandlerRoute

    override protected def teleponyHandler: Route = teleponyHandlerRoute

    override protected def cashbackHandler: Route = cashbackHandlerRoute

    override protected def matchApplicationHandler: Route =
      matchApplicationHandlerRoute

    override protected def configurationsHandler: Route =
      configurationsHandlerRoute

    override protected def productsHandler: Route = productsHandlerRoute

    override protected def amoyakHandler: Route =
      amoyakSyncRequestsHandlerRoute

    override protected def creditTariffHandler: Route = creditTariffRoute

    override protected def pricesHandler: Route = priceRoute

    override protected def promocodesHandler: Route = promocodesRoute
  }.route

  "ANY /quota" should {
    "route to user handler" in {
      Get("/quota") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "quota"
      }
    }
  }

  "ANY /offers" should {
    "route to offers handler" in {
      Get("/offers") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "offers"
      }
    }
  }

  "ANY /features" should {
    "route to features handler" in {
      Get("/features") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "features"
      }
    }
  }

  "ANY /match-applications" should {
    "route to match applications handler" in {
      Get("/match-applications") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "match-applications"
      }
    }
  }

  "ANY /products" should {
    "route to products handler" in {
      Get("/products") ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "products"
      }
    }
  }
}
