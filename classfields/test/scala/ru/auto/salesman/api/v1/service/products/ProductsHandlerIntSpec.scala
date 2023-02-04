package ru.auto.salesman.api.v1.service.products

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import org.joda.time.DateTime
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.dao.ProductDao.CreateProductRequest
import ru.auto.salesman.dao.impl.jdbc.JdbcProductDao
import ru.auto.salesman.model.{ActiveProductNaturalKey => SalesmanKey}
import ru.auto.salesman.products.ProductsOuterClass.{
  Product,
  ProductRequest,
  ProductTariff,
  Products
}
import ru.auto.salesman.service.ProductsService
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

import scala.collection.JavaConverters._
import ru.auto.salesman.environment._
import ru.auto.salesman.model.ProductTariff.ApplicationCreditSingleTariffCarsNew

class ProductsHandlerIntSpec extends RoutingSpec with SalesmanJdbcSpecTemplate {

  private val productDao = new JdbcProductDao(database, database)
  private val productsService = new ProductsService(productDao)
  private val route = new ProductsHandler(productsService).route

  private val applicationCreditAccessRequest = {
    val b = ProductRequest.newBuilder()
    b.getKeyBuilder
      .setDomain("application-credit")
      .setPayer("dealer:20101")
      .setTarget("cars:used")
      .setProductType("access")
    b.build()
  }

  private val gibddHistoryReportRequest = {
    val b = ProductRequest.newBuilder()
    b.getKeyBuilder
      .setDomain("gibdd-history")
      .setPayer("dealer:20101")
      .setTarget("118") // номер заказа
      .setProductType("report")
    b.build()
  }

  private val fullHistoryReportRequest = {
    val b = ProductRequest.newBuilder()
    b.getKeyBuilder
      .setDomain("full-history")
      .setPayer("dealer:20101")
      .setTarget("119") // номер заказа
      .setProductType("report")
    b.build()
  }

  private val applicationCreditSingleCarsNewRequest = {
    val b = ProductRequest.newBuilder()
    b.getKeyBuilder
      .setDomain("application-credit")
      .setPayer("dealer:20101")
      .setTarget("cars:used")
      .setProductType("single")
    b.setTariff(ProductTariff.APPLICATION_CREDIT_SINGLE_TARIFF_CARS_NEW)
    b.build()
  }

  private val applicationCreditSingleNoTariffRequest =
    applicationCreditSingleCarsNewRequest.toBuilder.clearTariff().build()

  private val unknownProductTypeRequest = {
    val b = ProductRequest.newBuilder()
    b.getKeyBuilder
      .setDomain("application-credit")
      .setPayer("dealer:20101")
      .setTarget("cars:used")
      .setProductType("wrong")
    b.build()
  }

  "ProductsHandler" should {

    "create application-credit:access with prolongable = true" in {
      Post("/create", applicationCreditAccessRequest) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe OK
          val created =
            productDao.getWaitingForPayment.success.value.loneElement
          withClue(created) {
            created.prolongable shouldBe true
          }
        }
      }
    }

    "create gibdd-history:report with prolongable = false" in {
      Post("/create", gibddHistoryReportRequest) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe OK
          val created =
            productDao.getWaitingForPayment.success.value.loneElement
          withClue(created) {
            created.prolongable shouldBe false
          }
        }
      }
    }

    "create full-history:report with prolongable = false" in {
      Post("/create", fullHistoryReportRequest) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe OK
          val created =
            productDao.getWaitingForPayment.success.value.loneElement
          withClue(created) {
            created.prolongable shouldBe false
          }
        }
      }
    }

    "create application-credit:single with tariff = application-credit:single:tariff:cars:new" in {
      Post("/create", applicationCreditSingleCarsNewRequest) ~> seal(
        route
      ) ~> check {
        withClue(responseAs[String]) {
          status shouldBe OK
          val created =
            productDao.getWaitingForPayment.success.value.loneElement
          withClue(created) {
            created.tariff.value shouldBe ApplicationCreditSingleTariffCarsNew
          }
        }
      }
    }

    "return 400 if tariff isn't defined in application-credit:single request" in {
      Post("/create", applicationCreditSingleNoTariffRequest) ~> seal(
        route
      ) ~> check {
        withClue(responseAs[String]) {
          status shouldBe BadRequest
        }
      }
    }

    "return 400 on unknown product type" in {
      Post("/create", unknownProductTypeRequest) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe BadRequest
        }
      }
    }

    "return all active by payer and domain" in {
      val pr1 = ProductRequest
        .newBuilder()
        .getKeyBuilder
        .setDomain("application-credit")
        .setPayer("dealer:20101")
        .setTarget("cars:used")
        .setProductType("access")
        .build

      val key1 = SalesmanKey(pr1).asTask.success.value
      val dt1 = DateTime.parse("2020-10-14T10:00:00.000+03:00")
      val createRequest1 = CreateProductRequest(
        key1,
        dt1,
        prolongable = false,
        productTariff = None
      ).right.value

      val pr2 = ProductRequest
        .newBuilder()
        .getKeyBuilder
        .setDomain("application-credit")
        .setPayer("dealer:22222")
        .setTarget("cars:used")
        .setProductType("access")
        .build
      val dt2 = DateTime.parse("2020-10-15T10:00:00.000+03:00")
      val key2 = SalesmanKey(pr2).asTask.success.value
      val createRequest2 = CreateProductRequest(
        key2,
        dt2,
        prolongable = false,
        productTariff = None
      ).right.value

      productDao.create(createRequest1).success
      val id1 =
        productDao.getWaitingForPayment.success.value.loneElement.id
      productDao.activate(id1, dt1.plusDays(2)).success
      productDao.create(createRequest2).success
      val id2 =
        productDao.getWaitingForPayment.success.value.loneElement.id
      productDao.activate(id2, dt2.plusDays(2)).success

      val uri =
        "/domain/application-credit/payer/dealer:20101/active"

      Get(uri) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe OK
          responseAs[
            Products
          ].getProductsList.asScala.loneElement.getKey.getPayer shouldBe "dealer:20101"
        }
      }
    }

    "return created product" in {
      val b = ProductRequest
        .newBuilder()
        .getKeyBuilder
        .setDomain("application-credit")
        .setPayer("dealer:20101")
        .setTarget("cars:used")
        .setProductType("access")
        .build

      val key = SalesmanKey(b).asTask.success.value
      val dt = DateTime.parse("2020-10-14T10:00:00.000+03:00")
      val createRequest =
        CreateProductRequest(
          key,
          dt,
          prolongable = false,
          productTariff = None
        ).right.value

      productDao.create(createRequest).success

      val uri =
        "/domain/application-credit/payer/dealer:20101/target/cars:used/product-type/access"

      Get(uri) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          responseAs[Product].getCreateDate.asDateTime should ~=(dt)
          responseAs[Product].getProlongable shouldBe false
        }
      }
    }

    "return last created product" in {
      val b = ProductRequest
        .newBuilder()
        .getKeyBuilder
        .setDomain("application-credit")
        .setPayer("dealer:20101")
        .setTarget("cars:used")
        .setProductType("access")
        .build

      val key = SalesmanKey(b).asTask.success.value
      val dt1 = DateTime.parse("2020-10-14T10:00:00.000+03:00")
      val createRequest1 = CreateProductRequest(
        key,
        dt1,
        prolongable = false,
        productTariff = None
      ).right.value
      val dt2 = DateTime.parse("2020-10-15T10:00:00.000+03:00")
      val createRequest2 = CreateProductRequest(
        key,
        dt2,
        prolongable = false,
        productTariff = None
      ).right.value

      productDao.create(createRequest1).success

      //Need to do this, because could be only one product in status NEED_PAYMENT or ACTIVE
      val id =
        productDao.getWaitingForPayment.success.value.loneElement.id
      productDao.activate(id, dt1.plusDays(2)).success
      productDao.deactivate(id, "test").success

      productDao.create(createRequest2).success

      val uri =
        "/domain/application-credit/payer/dealer:20101/target/cars:used/product-type/access"

      Get(uri) ~> seal(route) ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          responseAs[Product].getCreateDate.asDateTime should ~=(dt2)
        }
      }
    }
  }
}
