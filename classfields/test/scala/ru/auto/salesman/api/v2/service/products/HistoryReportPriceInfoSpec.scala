package ru.auto.salesman.api.v2.service.products

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Route
import org.scalatest.Inspectors
import ru.auto.salesman.api.akkahttp.SalesmanExceptionHandler.specificExceptionHandler
import ru.auto.salesman.api.v1.{HandlerBaseSpec, JdbcProductServices}
import ru.auto.salesman.api.v2.SalesmanApiUtils._
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.user.ApiModel.ProductPrice
import ru.auto.salesman.model.user.ApiModel.ProductPriceInfo.OptionalQuotaLeftCase
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest.VinHistory
import ru.auto.salesman.model.user.Prolongable
import ru.auto.salesman.model.user.periodical_discount_exclusion.User.NoActiveDiscount
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, DeprecatedDomains, RegionId}
import ru.auto.salesman.service.impl.user.AutoruPriceService
import ru.auto.salesman.service.impl.user.autoru.price.service._
import ru.auto.salesman.service.user.PriceService.{
  EnrichedPriceRequestContext,
  EnrichedProduct,
  UserContext
}
import ru.auto.salesman.service.user.autoru.price.service.{
  PaymentInputCalculator,
  UserContextCollector
}
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.service.ProductDescriptionService
import ru.auto.salesman.service.geoservice.RegionService
import ru.auto.salesman.service.user.autoru.price.service.sale.UserPeriodicalDiscountService
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class HistoryReportPriceInfoSpec
    extends HandlerBaseSpec
    with JdbcProductServices
    with ServiceModelGenerators {

  private lazy val userContextCollector = stub[UserContextCollector]
  private lazy val vosClient = stub[VosClient]
  private lazy val regionService = stub[RegionService]

  private lazy val quotaLeftCalculator =
    new QuotaLeftCalculatorImpl(subscriptionDao)
  private lazy val featureService = stub[UserFeatureService]

  private lazy val userPeriodicalDiscountService =
    stub[UserPeriodicalDiscountService]

  private lazy val contextEnricher = new ContextEnricherImpl(
    vosClient,
    regionService,
    quotaLeftCalculator,
    userPeriodicalDiscountService,
    featureService
  )

  private lazy val productDescriptonService = stub[ProductDescriptionService]
  private lazy val priceCalculator = stub[PriceCalculator]

  private lazy val productPriceInfoCalculator =
    new ProductPriceInfoCalculatorImpl(
      productDescriptonService,
      priceCalculator
    )

  private lazy val productEnricher = stub[ProductEnricher]
  private lazy val paymentInputCalculator = stub[PaymentInputCalculator]

  private lazy val productPriceCalculator = new ProductPriceCalculatorImpl(
    productEnricher,
    paymentInputCalculator,
    productPriceInfoCalculator
  )

  override def priceService = new AutoruPriceService(
    userContextCollector,
    contextEnricher,
    productPriceCalculator
  )

  private val autoruUserId = "user:56795117"

  private val defaultPriceRequest =
    PriceRequest
      .newBuilder()
      .setVinHistory(VinHistory.getDefaultInstance)
      .build()
      .toByteArray

  (userContextCollector
    .getUserContextOpt(_: Option[AutoruUser], _: Option[RegionId], _: Boolean))
    .when(*, *, *)
    .returningZ(
      Some(
        UserContext(
          allUserExperiments = None,
          geoIds = Nil,
          userModerationStatus = None,
          features = Nil,
          moneyFeature = List.empty
        )
      )
    )

  (userPeriodicalDiscountService.getActiveDiscountFor _)
    .when(*)
    .returningZ(NoActiveDiscount)

  for (size <- List(1, 10, 50)) {
    val product = OffersHistoryReports(size)
    val enriched = enrichedProduct(product).next
    (productEnricher.enrichProduct _)
      .when(product, *)
      .returningZ(enriched)

    (paymentInputCalculator
      .calculatePaymentInput(
        _: EnrichedProduct,
        _: EnrichedPriceRequestContext
      ))
      .when(enriched, *)
      .returningZ(productPriceGen(product = product).next)
  }

  (priceCalculator.calculateAutoApplyPrice _)
    .when(*, *)
    .returningZ(None)
    .anyNumberOfTimes()

  (productDescriptonService.userDescription _)
    .when(*)
    .returningZ(None)
    .anyNumberOfTimes()

  expectChangeFeatureCount()

  "/products/prices" should {

    "return purchase_forbidden=true if user already has enough of active vin-history quota" in {
      addProduct(autoruUserId, OffersHistoryReports(24), Prolongable(false))
      val result = getPrices(
        Some(autoruUserId),
        List("offers-history-reports-10", "offers-history-reports-50")
      )
      val reports10Price = result.find(_.getCounter == 10).value
      reports10Price.getProductPriceInfo.getPurchaseForbidden shouldBe true
    }

    "return purchase_forbidden=false if user doesn't have enough of active vin-history quota" in {
      addProduct(autoruUserId, OffersHistoryReports(24), Prolongable(false))
      val result = getPrices(
        Some(autoruUserId),
        List("offers-history-reports-10", "offers-history-reports-50")
      )
      val reports50Price = result.find(_.getCounter == 50).value
      reports50Price.getProductPriceInfo.getPurchaseForbidden shouldBe false
    }

    "return purchase_forbidden=false for anonymous" in {
      val result =
        getPrices(
          user = None,
          List("offers-history-reports-10")
        ).loneElement.getProductPriceInfo.getPurchaseForbidden
      result shouldBe false
    }

    "return purchase_forbidden=false for a single vin-history if user doesn't have active vin-history quota" in {
      val result =
        getPrices(Some(autoruUserId), List("offers-history-reports-1"))
      val reportPrice = result.find(_.getCounter == 1).value
      reportPrice.getProductPriceInfo.getPurchaseForbidden shouldBe false
    }

    "return quota_left=0 if user doesn't have active vin-history quota" in {
      val result =
        getPrices(
          Some(autoruUserId),
          List("offers-history-reports-10")
        ).loneElement.getProductPriceInfo
      result.getOptionalQuotaLeftCase match {
        case OptionalQuotaLeftCase.QUOTA_LEFT =>
          result.getQuotaLeft shouldBe 0
        case OptionalQuotaLeftCase.OPTIONALQUOTALEFT_NOT_SET =>
          fail("quota_left wasn't set")
      }
    }

    "return quota_left for all products if user has active vin-history quota" in {
      addProduct(autoruUserId, OffersHistoryReports(15), Prolongable(false))
      val quotaLefts =
        getPrices(
          Some(autoruUserId),
          List("offers-history-reports-10", "offers-history-reports-50")
        )
          .map(_.getProductPriceInfo.getQuotaLeft)
      Inspectors.forEvery(quotaLefts)(_ shouldBe 15)
    }
  }

  private def getPrices(user: Option[String], products: List[String]) = {
    val userParam = user.map("user" -> _).toList
    val productsParams = products.map("product" -> _)
    val allParams = userParam ::: productsParams
    val uri = Uri("/api/2.x/service/autoru/products/prices")
      .withQuery(Query(allParams: _*))
    Post(uri)
      .withEntity(defaultPriceRequest)
      .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
      withClue(responseAs[String]) {
        status shouldBe OK
        responseAs[Seq[ProductPrice]]
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
