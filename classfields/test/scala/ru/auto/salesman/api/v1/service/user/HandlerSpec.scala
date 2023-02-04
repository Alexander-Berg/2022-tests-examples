package ru.auto.salesman.api.v1.service.user

import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, PUT}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.user.PaymentOuterClass.PaymentPage
import ru.auto.salesman.api.v1.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.api.v1.{HandlerBaseSpec, JdbcProductServices}
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.user.ApiModel.DiscountResponse
import ru.auto.salesman.model.user.Prolongable
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Turbo
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Top
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.Page

trait HandlerSpec extends HandlerBaseSpec with JdbcProductServices {

  "Prolongable handlers" should {

    "put prolongable flag for user offer product" in {
      forAll(AutoruUserGen, AutoruOfferIdGen) { (user, offerId) =>
        val product = Top
        expectChangeFeatureCount()
        expectCreateFeatures()
        addProduct(user.toString, Some(offerId), product, Prolongable(false))
        getProduct(
          user.toString,
          offerId,
          product
        ).prolongable shouldBe Prolongable(false)
        val request =
          HttpRequest(
            PUT,
            s"/api/1.x/service/autoru/user/$user/offer/$offerId/product/$product/prolongable"
          )
            .withSalesmanTestHeader()
        request ~> route ~> check {
          status shouldBe OK
          getProduct(
            user.toString,
            offerId,
            product
          ).prolongable shouldBe Prolongable(true)
        }
      }
    }

    "return 409 when try to put prolongable flag for product activated by bundle" in {
      forAll(AutoruUserGen, AutoruOfferIdGen) { (user, offerId) =>
        val bundle = Turbo
        val productActivatedByBundle = Top // Turbo contains Top
        expectChangeFeatureCount()
        expectCreateFeatures()
        addProduct(user.toString, Some(offerId), bundle, Prolongable(true))
        val request =
          HttpRequest(
            PUT,
            s"/api/1.x/service/autoru/user/$user/offer/$offerId/product/$productActivatedByBundle/prolongable"
          )
            .withSalesmanTestHeader()
        request ~> route ~> check {
          status shouldBe Conflict
          getProduct(
            user.toString,
            offerId,
            productActivatedByBundle
          ).prolongable shouldBe Prolongable(false)
        }
      }
    }

    "delete prolongable flag for user offer product" in {
      forAll(AutoruUserGen, AutoruOfferIdGen) { (user, offerId) =>
        val product = Top
        expectChangeFeatureCount()
        expectCreateFeatures()
        addProduct(user.toString, Some(offerId), product, Prolongable(true))
        getProduct(
          user.toString,
          offerId,
          product
        ).prolongable shouldBe Prolongable(true)
        val request =
          HttpRequest(
            DELETE,
            s"/api/1.x/service/autoru/user/$user/offer/$offerId/product/$product/prolongable"
          )
            .withSalesmanTestHeader()
        request ~> route ~> check {
          status shouldBe OK
          getProduct(
            user.toString,
            offerId,
            product
          ).prolongable shouldBe Prolongable(false)
        }
      }
    }

    "return not found for user offer product" in {
      forAll(
        AutoruUserGen,
        AutoruOfferIdGen,
        AutoProlongableOfferProductGen,
        Gen.oneOf(PUT, DELETE)
      ) { (user, offerId, product, method) =>
        val request =
          HttpRequest(
            method,
            s"/api/1.x/service/autoru/user/$user/offer/$offerId/product/$product/prolongable"
          )
            .withSalesmanTestHeader()
        request ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }

    "return 400 for non-autoprolongable offer product" in {
      forAll(
        AutoruUserGen,
        AutoruOfferIdGen,
        NonAutoProlongableOfferProductGen,
        Gen.oneOf(PUT, DELETE)
      ) { (user, offerId, product, method) =>
        val request =
          HttpRequest(
            method,
            s"/api/1.x/service/autoru/user/$user/offer/$offerId/product/$product/prolongable"
          )
            .withSalesmanTestHeader()
        request ~> route ~> check {
          status shouldBe BadRequest
        }
      }
    }

    "discount in selected category" in {
      forAll(AutoruUserGen) { user =>
        val request =
          HttpRequest(GET, s"/api/1.x/service/autoru/user/$user/discount/cars")
            .withSalesmanTestHeader()
        (periodicalDiscountService.availableDiscount _)
          .expects(user, Some(Category.CARS))
          .returningZ(DiscountResponse.newBuilder().build())
          .noMoreThanOnce()
        request ~> route ~> check {
          status shouldBe OK
        }
      }
    }

    "discount in wild category" in {
      forAll(AutoruUserGen) { user =>
        val request =
          HttpRequest(GET, s"/api/1.x/service/autoru/user/$user/discount/all")
            .withSalesmanTestHeader()
        (periodicalDiscountService.availableDiscount _)
          .expects(user, None)
          .returningZ(DiscountResponse.newBuilder().build())
          .noMoreThanOnce()
        request ~> route ~> check {
          status shouldBe OK
        }
      }
    }

    "return payments" in {
      val user = AutoruUser(123)
      val page = Page(2, 10)
      val request =
        HttpRequest(
          GET,
          s"/api/1.x/service/autoru/user/$user/payments?pageSize=${page.size}&pageNum=${page.number}"
        )
          .withSalesmanTestHeader()
      (paymentHistoryService.list _)
        .expects(user.toString, page)
        .returningZ(PaymentPage.newBuilder().build())
        .noMoreThanOnce()
      request ~> route ~> check {
        status shouldBe OK
      }
    }
  }
}
