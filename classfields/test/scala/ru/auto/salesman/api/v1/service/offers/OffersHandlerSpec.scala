package ru.auto.salesman.api.v1.service.offers

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes.{
  BadRequest,
  InternalServerError,
  NotFound,
  OK,
  PaymentRequired
}
import akka.http.scaladsl.testkit.RouteTestTimeout
import org.joda.time.LocalDate
import ru.auto.api.ApiOfferModel.{Category, Offer, Section, SellerType}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.view.offers.OfferProductActiveDaysView
import ru.auto.salesman.model.{
  ClientId,
  OfferCategories,
  OfferProductActiveDays,
  OffersWithPaidProducts,
  Paging,
  ProductId
}
import ru.auto.salesman.offers.FreePlacement.FreePlacementRequest
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.{
  AdsRequestForbidden,
  NotEnoughFunds
}
import ru.auto.salesman.service.GoodsDecider.NoActionReason.OfferResolutionError
import ru.auto.salesman.service.async.{AsyncOfferInfoService, AsyncOffersResolver}
import ru.auto.salesman.service.placement.free.FreePlacementService
import ru.auto.salesman.service.placement.validation.DealerGoodsPreparingService
import ru.auto.salesman.service.placement.validation.domain.{
  Allowed,
  Forbidden,
  TemporaryError
}
import ru.auto.salesman.test.model.gens.AutoruOfferIdGen

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class OffersHandlerSpec extends RoutingSpec {

  import OffersHandlerSpec._

  private val asyncOffersResolver = mock[AsyncOffersResolver]
  private val asyncOfferInfoService = mock[AsyncOfferInfoService]

  private val goodsPreparingService =
    mock[DealerGoodsPreparingService]

  private val freePlacementService = mock[FreePlacementService]

  private val route =
    new OffersHandler(
      asyncOffersResolver,
      asyncOfferInfoService,
      freePlacementService,
      goodsPreparingService
    ).route

  "GET /client/{clientId}" should {
    "return single offer id with filters " in {
      val expectingFrom = LocalDate.parse("2017-09-06")
      val expectingTo = LocalDate.parse("2019-02-01")
      (asyncOffersResolver.getOffersWithPaidProducts _)
        .expects(
          expectingFrom,
          expectingTo,
          13869L,
          List("all_sale_fresh", "all_sale_activate"),
          Paging(0, 20)
        )
        .returningF(OffersWithPaidProducts(Nil, 1))
      Get(
        s"/client/13869?from=2017-09-06&to=2019-02-01&product=boost&product=placement&pageNum=0&pageSize=20"
      ) ~>
        seal(route) ~> check {
          println(response)
          status shouldBe OK
        }
    }

    "return single offer id without filters " in {
      val expectingFrom = LocalDate.parse("2017-09-06")
      val expectingTo = LocalDate.parse("2019-02-01")
      (asyncOffersResolver.getOffersWithPaidProducts _)
        .expects(expectingFrom, expectingTo, 13869L, Nil, Paging(0, 20))
        .returning(Future.successful(OffersWithPaidProducts(Nil, 1)))
      Get(
        s"/client/13869?from=2017-09-06&to=2019-02-01&pageNum=0&pageSize=20"
      ) ~>
        seal(route) ~> check {
          status shouldBe OK
        }
    }

    "bad request if date and time present" in {
      Get(
        s"/client/13869?from=2017-09-06&to=2019-02-01T23-59-49&pageNum=0&pageSize=20"
      ) ~>
        seal(route) ~> check {
          status shouldBe BadRequest
        }
    }

    "bad request if old product format given" in {
      Get(
        s"/client/13869?from=2017-09-06&to=2019-02-01&product=all_sale_fresh&product=placement&pageNum=0&pageSize=20"
      ) ~>
        seal(route) ~> check {
          status shouldBe BadRequest
        }
    }
  }

  "GET /category/{categoryId}/{offerId}/{productId}" should {

    "return offer product duration days" in {
      forAll(AutoruOfferIdGen) { offerId =>
        val expectedResult = OfferProductActiveDays(Some(17))
        (asyncOfferInfoService.productActivityDaysDuration _)
          .expects(OfferCategories.Cars, offerId, ProductId.Fresh)
          .returningF(expectedResult)

        Get(s"/category/cars/$offerId/${ProductId.alias(ProductId.Fresh)}") ~>
          seal(route) ~> check {
            status shouldBe OK
            responseAs[
              OfferProductActiveDaysView
            ] shouldBe OfferProductActiveDaysView
              .asView(expectedResult)
          }
      }
    }

    "return 404 for invalid product" in {
      forAll(AutoruOfferIdGen) { offerId =>
        (asyncOfferInfoService.productActivityDaysDuration _)
          .expects(*, *, *)
          .never()

        Get(s"/category/cars/$offerId/wswwdasd") ~>
          seal(route) ~> check {
            status shouldBe NotFound
          }
      }
    }
  }

  "POST /client/{clientId}/validation/product/{productId}" should {

    "return OK if validation was successful" in {
      implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.seconds)
      (goodsPreparingService
        .prepare(_: Offer, _: ProductId, _: ClientId))
        .expects(offer, ProductId.Fresh, 13869L)
        .returningZ(Allowed)

      val request =
        Post(s"/client/13869/validation/product/boost")
          .withEntity(HttpEntity(offer.toByteArray))
          .withHeaders(RequestIdentityHeaders)

      request ~> seal(route) ~> check(status shouldBe OK)
    }

    "return Bad Request if offer cant be activated" in {
      (goodsPreparingService
        .prepare(_: Offer, _: ProductId, _: ClientId))
        .expects(offer, ProductId.Fresh, 13869L)
        .returningZ(Forbidden(AdsRequestForbidden))

      val request =
        Post(s"/client/13869/validation/product/boost")
          .withEntity(HttpEntity(offer.toByteArray))
          .withHeaders(RequestIdentityHeaders)

      request ~> seal(route) ~> check(status shouldBe BadRequest)
    }

    "return Payment Required if insufficient funds been thrown" in {
      (goodsPreparingService
        .prepare(_: Offer, _: ProductId, _: ClientId))
        .expects(offer, ProductId.Fresh, 13869L)
        .returningZ(Forbidden(NotEnoughFunds))

      val request =
        Post(s"/client/13869/validation/product/boost")
          .withEntity(HttpEntity(offer.toByteArray))
          .withHeaders(RequestIdentityHeaders)

      request ~> seal(route) ~> check(status shouldBe PaymentRequired)
    }

    "return Internal Error in other cases" in {
      (goodsPreparingService
        .prepare(_: Offer, _: ProductId, _: ClientId))
        .expects(offer, ProductId.Fresh, 13869L)
        .returningZ(
          TemporaryError(OfferResolutionError(someException))
        )

      val request =
        Post(s"/client/13869/validation/product/boost")
          .withEntity(HttpEntity(offer.toByteArray))
          .withHeaders(RequestIdentityHeaders)

      request ~> seal(route) ~> check(status shouldBe InternalServerError)
    }

  }

  "PUT /free/placement" should {

    "return OK if free placement allowed" in {
      (freePlacementService
        .allowFreePlacement(_: FreePlacementRequest))
        .expects(FreePlacementRequest.getDefaultInstance)
        .returningZ(())

      val request =
        Put("/free/placement")
          .withEntity(HttpEntity(FreePlacementRequest.getDefaultInstance.toByteArray))
          .withHeaders(RequestIdentityHeaders)

      request ~> seal(route) ~> check(status shouldBe OK)
    }

  }

  def actorRefFactory: ActorRefFactory = system
}

object OffersHandlerSpec {

  private val offer = Offer
    .newBuilder()
    .setId("123")
    .setSellerType(SellerType.COMMERCIAL)
    .setCategory(Category.CARS)
    .setSection(Section.NEW)
    .setCarInfo(CarInfo.newBuilder().setMark("BMW"))
    .build()

  private val someException = new Exception("some message")

}
