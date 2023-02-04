package ru.auto.salesman.api.v1.service.product

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import akka.http.scaladsl.server.Route
import ru.auto.salesman.api.akkahttp.SalesmanExceptionHandler.specificExceptionHandler
import ru.auto.salesman.api.v1.SalesmanApiUtils._
import ru.auto.salesman.api.v1.{HandlerBaseSpec, JdbcProductServices}
import ru.auto.salesman.model.user.ApiModel.{ProductResponse, ProductResponses}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

import scala.collection.JavaConverters._

class HandlerSpec
    extends HandlerBaseSpec
    with SalesmanUserJdbcSpecTemplate
    with UserDaoGenerators
    with JdbcProductServices {

  private val userId = "user:33557799"
  private val anotherUserId = "user:11223344"

  "ProductHandler" should {

    "respond with just one user's vin-history package" in {
      forAll(
        subscriptionCreateRequestGen(userId, OffersHistoryReports(5))
      ) { request =>
        subscriptionDao.insertIfNotExists(request).success
        HttpRequest(
          GET,
          s"/api/1.x/service/autoru/product/offers-history-reports/user/user:33557799"
        )
          .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
          withClue(responseAs[String]) {
            status shouldBe OK
            responseAs[ProductResponse].getCounter shouldBe 5
          }
        }
      }
    }

    "respond with 404 if requested user doesn't have active vin-history package" in {
      forAll(
        subscriptionCreateRequestGen(anotherUserId, OffersHistoryReports(6))
      ) { request =>
        subscriptionDao.insertIfNotExists(request).success
        HttpRequest(
          GET,
          s"/api/1.x/service/autoru/product/offers-history-reports/user/user:33557799"
        )
          .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
          withClue(responseAs[String]) {
            status shouldBe NotFound
          }
        }
      }
    }

    "respond with all user's vin-history packages, so that we can calculate sum of their counters" in {
      forAll(
        subscriptionCreateRequestGen(userId, OffersHistoryReports(4)),
        subscriptionCreateRequestGen(userId, OffersHistoryReports(48))
      ) { (request1, request2) =>
        subscriptionDao.insertIfNotExists(request1).success
        subscriptionDao.insertIfNotExists(request2).success
        HttpRequest(
          GET,
          s"/api/1.x/service/autoru/product/offers-history-reports/user/user:33557799/all"
        )
          .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
          withClue(responseAs[String]) {
            status shouldBe OK
            responseAs[ProductResponses].getProductResponsesList.asScala
              .map(_.getCounter)
              .sum shouldBe 4 + 48
          }
        }
      }
    }

    "respond only with vin-history packages for requested user" in {
      forAll(
        subscriptionCreateRequestGen(userId, OffersHistoryReports(4)),
        subscriptionCreateRequestGen(userId, OffersHistoryReports(48)),
        subscriptionCreateRequestGen(anotherUserId, OffersHistoryReports(9))
      ) { (request1, request2, request3) =>
        subscriptionDao.insertIfNotExists(request1).success
        subscriptionDao.insertIfNotExists(request2).success
        subscriptionDao.insertIfNotExists(request3).success
        HttpRequest(
          GET,
          s"/api/1.x/service/autoru/product/offers-history-reports/user/user:33557799/all"
        )
          .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
          withClue(responseAs[String]) {
            status shouldBe OK
            responseAs[ProductResponses].getProductResponsesList.asScala
              .map(_.getCounter)
              .sum shouldBe 4 + 48
          }
        }
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
