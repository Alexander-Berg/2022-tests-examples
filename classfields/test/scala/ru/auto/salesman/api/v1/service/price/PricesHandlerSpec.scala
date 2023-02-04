package ru.auto.salesman.api.v1.service.price

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTestTimeout
import ru.auto.api.ApiOfferModel
import ru.auto.api.api_offer_model.Section
import ru.auto.api.price_model.KopeckPrice
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.dealer.ProductsPriceRequestOuterClass.ProductsPriceResponse
import ru.auto.salesman.dealer.ProductsPriceRequestV2.ProductsPriceResponseWithRanges
import ru.auto.salesman.dealer.products_price_request.ProductsPriceRequest
import ru.auto.salesman.dealer.products_price_request.ProductsPriceRequest.{
  Product => ProtoProduct,
  TransportCategory => ProtoTransportCategory
}
import ru.auto.salesman.dealer.products_price_request.ProductsPriceRequest.Product.{
  Placement => ProtoPlacement,
  Service => ProtoService
}

import ru.auto.salesman.dealer.products_price_request_v2.ProductsPriceRequestWithRanges.Product.{
  Service => ProtoRangedService
}
import ru.auto.salesman.dealer.products_price_request_v2.ProductsPriceRequestWithRanges.{
  Product => ProtoRangedProduct,
  TransportCategory => ProtoRangedTransportCategory
}
import ru.auto.salesman.dealer.products_price_request_v2.ProductsPriceRequestWithRanges
import ru.auto.salesman.model.{AutoruDealer, ProductId, RegionId, TransportCategory}
import ru.auto.salesman.service.DealerProductPriceService
import ru.auto.salesman.service.DealerProductPriceService.Price.{
  PlacementPrices,
  ServicePrice,
  ServicePrices
}
import ru.auto.salesman.service.DealerProductPriceService.{
  DealerProduct,
  PlacementPrice,
  PriceWithDiscount,
  ProductPrice,
  ServicePriceForRange
}
import ru.auto.salesman.test.TestException

import scala.concurrent.duration.DurationInt

class PricesHandlerSpec extends RoutingSpec {
  import PricesHandlerSpec._

  def createRoute: (DealerProductPriceService, Route) = {
    val priceService = mock[DealerProductPriceService]
    val route = new PricesHandler(priceService).getPricesRoute
    (priceService, route)
  }

  private val uri = "/prices"

  implicit def default(implicit system: ActorSystem): RouteTestTimeout =
    RouteTestTimeout(5.seconds)

  "POST /price/prices" should {
    "respond ok on service price request" in {
      val (service, route) = createRoute

      val price = ProductPrice(
        ProductId.Badge,
        ServicePrice(PriceWithDiscount(1000L, 500L, 50))
      )

      val products = List(protoServiceProduct(ProductId.alias(ProductId.Badge)))

      val request =
        testRequest(ProtoTransportCategory.CARS, Section.USED, products)

      (service.productsPrices _)
        .expects(
          List(DealerProduct.Service(ProductId.Badge)),
          TransportCategory.Cars,
          ApiOfferModel.Section.USED,
          AutoruDealer(1),
          RegionId(1)
        )
        .returningZ(List(price))

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val res = responseAs[ProductsPriceResponse]
        res.getPricesCount shouldBe 1
        val price = res.getPrices(0).getServicePrice.getPrice
        price.getBasePrice.getKopecks shouldBe 1000L
        price.getFinalPrice.getKopecks shouldBe 500L
        price.getDiscountPercent shouldBe 50
      }
    }

    "respond ok on placement price request" in {
      val (service, route) = createRoute

      val price = ProductPrice(
        ProductId.Placement,
        PlacementPrices(
          List(
            PlacementPrice(
              rangeFrom = 100L,
              placementPrice = PriceWithDiscount(10000L, 5000L, 50),
              prolongPrice = PriceWithDiscount(5000L, 2500L, 50)
            )
          )
        )
      )

      val products = List(protoPlacementProduct(List(100L)))

      val request =
        testRequest(ProtoTransportCategory.CARS, Section.USED, products)

      (service.productsPrices _)
        .expects(
          List(DealerProduct.Placement(List(100L))),
          TransportCategory.Cars,
          ApiOfferModel.Section.USED,
          AutoruDealer(1),
          RegionId(1)
        )
        .returningZ(List(price))

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val res = responseAs[ProductsPriceResponse]
        res.getPricesCount shouldBe 1
        val price = res.getPrices(0).getPlacementPrice.getPriceRanges(0)
        price.getPlacementPrice.getBasePrice.getKopecks shouldBe 10000L
        price.getPlacementPrice.getFinalPrice.getKopecks shouldBe 5000L
        price.getPlacementPrice.getDiscountPercent shouldBe 50
        price.getProlongPrice.getBasePrice.getKopecks shouldBe 5000L
        price.getProlongPrice.getFinalPrice.getKopecks shouldBe 2500L
        price.getProlongPrice.getDiscountPercent shouldBe 50
        price.getRangeFrom.getKopecks shouldBe 100
      }
    }

    "return error for wrong category" in {
      val (_, route) = createRoute

      val request = testRequest(
        ProtoTransportCategory.CATEGORY_UNKNOWN,
        Section.USED,
        List(protoServiceProduct(ProductId.alias(ProductId.Badge)))
      )

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return error on service error" in {
      val (service, route) = createRoute

      (service.productsPrices _)
        .expects(*, *, *, *, *)
        .throwingZ(new TestException())

      val request = testRequest(
        ProtoTransportCategory.CARS,
        Section.SECTION_UNKNOWN,
        List(protoServiceProduct(ProductId.alias(ProductId.Badge)))
      )

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "return error for wrong product" in {
      val (_, route) = createRoute

      val request =
        testRequest(
          ProtoTransportCategory.CARS,
          Section.USED,
          List(protoServiceProduct("bla-bla"))
        )

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "POST /price/prices_with_service_ranges" should {

    def createRangedRoute: (DealerProductPriceService, Route) = {
      val priceService = mock[DealerProductPriceService]
      val route =
        new PricesHandler(priceService).getPricesWithServiceRangesRoute
      (priceService, route)
    }

    "respond ok on service price request" in {
      val uri = "/prices_with_service_ranges"
      val (service, route) = createRangedRoute

      val ranges = Seq(1000L, 2000L)

      val price = ProductPrice(
        ProductId.Badge,
        ServicePrices(
          Seq(
            ServicePriceForRange(1000L, PriceWithDiscount(1000L, 500L, 50)),
            ServicePriceForRange(2000L, PriceWithDiscount(1500L, 750L, 50))
          )
        )
      )

      val products = List(
        protoServiceProductWithRanges(ProductId.alias(ProductId.Badge), ranges)
      )

      val request =
        testRangedPriceRequest(
          ProtoRangedTransportCategory.CARS,
          Section.USED,
          products
        )

      (service.productPricesWithRanges _)
        .expects(
          List(DealerProduct.ServiceWithRanges(ProductId.Badge, ranges)),
          TransportCategory.Cars,
          ApiOfferModel.Section.USED,
          AutoruDealer(1),
          RegionId(1)
        )
        .returningZ(List(price))

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val res = responseAs[ProductsPriceResponseWithRanges]
        res.getPricesCount shouldBe 1
        val prices = res.getPrices(0).getServicePrice
        prices.getPriceRangesCount shouldBe 2
        prices.getPriceRanges(0).getRangeFrom.getKopecks shouldBe 1000L
        prices.getPriceRanges(0).getPrice.getBasePrice.getKopecks shouldBe 1000L
        prices.getPriceRanges(0).getPrice.getFinalPrice.getKopecks shouldBe 500L
      }
    }

  }
}

object PricesHandlerSpec {

  private def testRequest(
      category: ProtoTransportCategory,
      section: Section,
      products: Seq[ProtoProduct]
  ): ProductsPriceRequest =
    ProductsPriceRequest(
      dealerId = 1,
      category = category,
      section = section,
      regionId = 1,
      products = products
    )

  private def testRangedPriceRequest(
      category: ProtoRangedTransportCategory,
      section: Section,
      products: Seq[ProtoRangedProduct]
  ): ProductsPriceRequestWithRanges =
    ProductsPriceRequestWithRanges(
      dealerId = 1,
      category = category,
      section = section,
      regionId = 1,
      products = products
    )

  private def protoServiceProduct(id: String) =
    ProtoProduct(ProtoProduct.Product.Service(ProtoService(id)))

  private def protoServiceProductWithRanges(id: String, ranges: Seq[Long]) =
    ProtoRangedProduct(
      ProtoRangedProduct.Product.Service(
        ProtoRangedService(id, ranges.map(KopeckPrice.apply))
      )
    )

  private def protoPlacementProduct(ranges: Seq[Long]) =
    ProtoProduct(
      ProtoProduct.Product.Placement(
        ProtoPlacement(ranges.map(KopeckPrice.apply))
      )
    )

}
