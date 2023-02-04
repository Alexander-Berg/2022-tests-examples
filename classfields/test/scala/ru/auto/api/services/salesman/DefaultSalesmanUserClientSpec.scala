package ru.auto.api.services.salesman

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.PromocodeModel.PromocodeListing
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.api.exceptions.{ProductNotFound, TransactionNotFound}
import ru.auto.api.http._
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.{AutoruUser, OfferID, Paging}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.services.billing.util.SalesmanTicketId
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain.AutoruSalesmanDomain
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanOffer
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.user.PaymentOuterClass.PaymentPage
import ru.auto.api.util.StringUtils.UrlInterpolation
import ru.auto.salesman.model.user.ApiModel.{DiscountResponse, ProductResponses}
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest.VinHistory

import scala.jdk.CollectionConverters._

/**
  * @author alex-kovalenko
  */
class DefaultSalesmanUserClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {

  implicit val global = scala.concurrent.ExecutionContext.Implicits.global
  val salesmanUserClient = new DefaultSalesmanUserClient(http)
  private val domain = AutoruSalesmanDomain
  private val successResponse = SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()

  "DefaultSalesmanUser" should {
    "get paysys for user" in {
      forAll(PrivateUserRefGen, UserPaysysGen) { (user, paysys) =>
        http.expectUrl(GET, url"/api/1.x/service/autoru/user/$user/paysys")

        http.respondWith(StatusCodes.OK, paysys)

        val result = salesmanUserClient.getUserBilling(user).futureValue
        result shouldBe paysys
      }
    }

    "get transaction" in {
      forAll(PrivateUserRefGen, ReadableStringGen, TransactionGen) { (user, transactionId, transaction) =>
        http.expectUrl(GET, url"/api/1.x/service/autoru/transaction/$transactionId")

        http.respondWith(transaction)

        val result =
          salesmanUserClient.getTransaction(user, SalesmanTicketId(transactionId, AutoruSalesmanDomain)).futureValue
        result shouldBe transaction
      }
    }

    "create transaction" in {
      forAll(TransactionRequestGen, TransactionGen) { (req, transaction) =>
        http.expectUrl(POST, "/api/1.x/service/autoru/transaction")
        http.expectProto(req)

        http.respondWith(transaction)

        val result = salesmanUserClient.createTransaction(req, domain).futureValue
        result.getTransactionId shouldBe transaction.getTransactionId
      }
    }

    "process transaction" in {
      forAll(readableString) { transactionId =>
        http.expectUrl(PUT, s"/api/1.x/service/autoru/transaction/$transactionId/process")
        http.respondWithStatus(StatusCodes.OK)

        salesmanUserClient.processTransaction(SalesmanTicketId(transactionId, AutoruSalesmanDomain)).futureValue
      }
    }

    "do get prices" in {
      forAll(PrivateUserRefGen, OfferIDGen, ProductGen, Gen.listOf(ProductPriceGen), bool) {
        (user, offerId, product, prices, applyMoneyFeature) =>
          val url = url"/api/1.x/service/autoru/products/prices?user=${user.toPlain}".value +
            url"&offerId=${offerId.toPlain}&product=${product.name}&applyMoneyFeature=$applyMoneyFeature".value
          http.expectUrl(GET, url)
          http.expectHeader(PreferProtobufAcceptHeader)

          http.respondWithMany(prices)

          val result = salesmanUserClient
            .getPrices(user, SalesmanOffer(offerId), Seq(product), geoId = None, applyMoneyFeature)
            .futureValue
          result shouldBe prices
      }
    }

    "do get price for multiple offers" in {
      forAll(ProductPricesGen, ProductGen, bool, UserOffersGen) {
        (productPrices, product, applyMoneyFeature, userOffers) =>
          val url = url"/api/1.x/service/autoru/products/multipleOffersPrices?product=${product.salesName}".value +
            url"&applyMoneyFeature=$applyMoneyFeature".value
          http.expectUrl(POST, url)
          http.expectHeader(PreferProtobufAcceptHeader)
          http.expectProto(userOffers)

          http.respondWithMany(Iterable(productPrices))

          val result =
            salesmanUserClient.getMultipleOffersPrices(Seq(product), applyMoneyFeature, userOffers).futureValue
          result shouldBe Iterable(productPrices)
      }
    }

    "do get active products" in {
      forAll(OfferIDGen, Gen.listOf(productResponseGen())) { (offerId, response) =>
        val url = s"/api/1.x/service/autoru/products?offer=${offerId.toPlain}"
        http.expectUrl(GET, url)
        http.expectHeader(PreferProtobufAcceptHeader)

        http.respondWithMany(response)

        val result = salesmanUserClient.getActiveProducts(List(SalesmanOffer(offerId))).futureValue
        result shouldBe response
      }
    }

    "get all active products" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/product/offers-history-reports/user/user:56795118/all")
      http.expectHeader(PreferProtobufAcceptHeader)

      http.respondWithProtoFrom[ProductResponses]("/salesman/all_active_offers_history_reports.json")

      val result = salesmanUserClient
        .getAllActiveProducts(AutoruSalesmanDomain, AutoruUser(56795118), OffersHistoryReports)
        .futureValue
      // проверяем только каунтеры, т.к. в потребителях клиента используются только они
      result.getProductResponsesList.asScala
        .map(_.getCounter)
        .sum shouldBe 7 + 10 // см. поле counter в файле all_active_offers_history_reports.json
    }

    "put prolongable for product" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, privateRequestGen) { (domain, offerId, product, request) =>
        val url = url"/api/1.x/service/$domain/user/${request.user.userRef}/offer/$offerId/product/$product/prolongable"
        http.expectUrl(PUT, url)
        http.respondWithStatus(StatusCodes.OK)
        salesmanUserClient.putProlongable(domain, offerId, product)(request).futureValue shouldBe (())
      }
    }

    "throw ProductNotFound on putting prolongable for product if salesman respond with 404" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, privateRequestGen) { (domain, offerId, product, request) =>
        val url = url"/api/1.x/service/$domain/user/${request.user.userRef}/offer/$offerId/product/$product/prolongable"
        http.expectUrl(PUT, url)
        http.respondWithStatus(StatusCodes.NotFound)
        val ex = salesmanUserClient.putProlongable(domain, offerId, product)(request).failed.futureValue
        ex shouldBe a[ProductNotFound]
      }
    }

    "delete prolongable for product" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, privateRequestGen) { (domain, offerId, product, request) =>
        val url = url"/api/1.x/service/$domain/user/${request.user.userRef}/offer/$offerId/product/$product/prolongable"
        http.expectUrl(DELETE, url)
        http.respondWithStatus(StatusCodes.OK)
        salesmanUserClient.deleteProlongable(domain, offerId, product)(request).futureValue shouldBe (())
      }
    }

    "throw ProductNotFound on deleting prolongable for product if salesman respond with 404" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, privateRequestGen) { (domain, offerId, product, request) =>
        val url = url"/api/1.x/service/$domain/user/${request.user.userRef}/offer/$offerId/product/$product/prolongable"
        http.expectUrl(DELETE, url)
        http.respondWithStatus(StatusCodes.NotFound)
        val ex = salesmanUserClient.deleteProlongable(domain, offerId, product)(request).failed.futureValue
        ex shouldBe a[ProductNotFound]
      }
    }

    "put prolongable for transaction" in {
      forAll(SalesmanDomainGen, TransactionIdGen) { (domain, transactionId) =>
        val url = s"/api/1.x/service/$domain/transaction/${transactionId.value}/prolongable"
        http.expectUrl(PUT, url)
        http.respondWithStatus(StatusCodes.OK)
        salesmanUserClient.putProlongable(domain, transactionId)(request).futureValue shouldBe (())
      }
    }

    "throw TransactionNotFound on putting prolongable for transaction if salesman respond with 404" in {
      forAll(SalesmanDomainGen, TransactionIdGen) { (domain, transactionId) =>
        val url = s"/api/1.x/service/$domain/transaction/${transactionId.value}/prolongable"
        http.expectUrl(PUT, url)
        http.respondWithStatus(StatusCodes.NotFound)
        val ex = salesmanUserClient.putProlongable(domain, transactionId)(request).failed.futureValue
        ex shouldBe a[TransactionNotFound]
      }
    }

    "delete prolongable for transaction" in {
      forAll(SalesmanDomainGen, TransactionIdGen) { (domain, transactionId) =>
        val url = s"/api/1.x/service/$domain/transaction/${transactionId.value}/prolongable"
        http.expectUrl(DELETE, url)
        http.respondWithStatus(StatusCodes.OK)
        salesmanUserClient.deleteProlongable(domain, transactionId)(request).futureValue shouldBe (())
      }
    }

    "throw TransactionNotFound on deleting prolongable for transaction if salesman respond with 404" in {
      forAll(SalesmanDomainGen, TransactionIdGen) { (domain, transactionId) =>
        val url = s"/api/1.x/service/$domain/transaction/${transactionId.value}/prolongable"
        http.expectUrl(DELETE, url)
        http.respondWithStatus(StatusCodes.NotFound)
        val ex = salesmanUserClient.deleteProlongable(domain, transactionId)(request).failed.futureValue
        ex shouldBe a[TransactionNotFound]
      }
    }

    "get schedules" in {
      forAll(scheduleResponseGen(), SalesmanDomainGen) { (schedules, domain) =>
        val userRef = request.user.userRef
        val url = url"/api/1.x/service/$domain/schedules/user/$userRef"
        http.expectUrl(GET, url)
        http.respondWith(schedules)
        val result = salesmanUserClient.getSchedules(domain, userRef.asPrivate, products = Nil, offerIds = Nil)
        result.futureValue shouldBe schedules
      }
    }

    "get schedules with product and offers" in {
      forAll(scheduleResponseGen(), SalesmanDomainGen) { (schedules, domain) =>
        val userRef = request.user.userRef
        val url = url"/api/1.x/service/$domain/schedules/user/$userRef?product=boost&offerId=1-a&offerId=2-b"
        http.expectUrl(GET, url)
        http.respondWith(schedules)
        val products = List(Boost)
        val offerIds = List(OfferID(1, Some("a")), OfferID(2, Some("b")))
        val result = salesmanUserClient.getSchedules(domain, userRef.asPrivate, products, offerIds)
        result.futureValue shouldBe schedules
      }
    }

    "put schedules" in {
      forAll(scheduleRequestGen, SalesmanDomainGen) { (scheduleRequest, domain) =>
        val userRef = request.user.userRef
        val url = url"/api/1.x/service/$domain/schedules/user/$userRef/product/boost?offerId=111&offerId=222"
        http.expectUrl(PUT, url)
        http.respondWith(successResponse)
        val offerIds = List(OfferID(111, hash = None), OfferID(222, hash = None))
        val result = salesmanUserClient.putSchedules(domain, userRef.asPrivate, Boost, offerIds, scheduleRequest)
        result.futureValue shouldBe successResponse
      }
    }

    "delete schedules" in {
      forAll(SalesmanDomainGen) { (domain) =>
        val userRef = request.user.userRef
        val url = url"/api/1.x/service/$domain/schedules/user/$userRef?product=boost&offerId=111&offerId=222"
        http.expectUrl(DELETE, url)
        http.respondWith(successResponse)
        val products = List(Boost)
        val offerIds = List(OfferID(111, hash = None), OfferID(222, hash = None))
        val result = salesmanUserClient.deleteSchedules(domain, userRef.asPrivate, products, offerIds)
        result.futureValue shouldBe successResponse
      }
    }

    "get user bought vin history reports" in {
      forAll(SalesmanDomainGen, PrivateUserRefGen, VinHistoryBoughtReportGen) { (domain, user, report) =>
        val url =
          url"/api/1.x/service/$domain/vin-history/user/${user.toPlain}/reports?".value +
            url"vin=XXX123&offerId=123-fff&onlyActive=true&pageNum=1&pageSize=100".value

        http.expectUrl(GET, url)
        http.expectHeader(PreferProtobufAcceptHeader)
        http.respondWith(StatusCodes.OK, report)

        salesmanUserClient
          .getBoughtVinHistoryReports(
            domain,
            user,
            vin = Some("XXX123"),
            offerId = Some(OfferID.parse("123-fff")),
            createdFrom = None,
            createdTo = None,
            onlyActive = true,
            Paging(page = 1, pageSize = 100)
          )
          .futureValue
      }
    }

    "save user bought vin history report" in {
      forAll(SalesmanDomainGen, PrivateUserRefGen, VinHistoryBoughtReportGen) {
        (domain, user, vinHistoryBoughtReport) =>
          val url = url"/api/1.x/service/$domain/vin-history/user/${user.toPlain}".value +
            url"/report?count=1&decrement_count=true&tag=tag-1&tag=tag-2".value

          val decrementCount = 1
          val tags = Set("tag-1", "tag-2")

          http.expectUrl(POST, url)
          http.expectHeader(PreferProtobufAcceptHeader)
          http.expectProto(vinHistoryBoughtReport)
          http.respondWith(StatusCodes.OK, successResponse)

          salesmanUserClient
            .saveBoughtVinHistoryReport(
              domain,
              user,
              Some(decrementCount),
              true,
              tags,
              vinHistoryBoughtReport
            )
            .futureValue
      }
    }

    "save user bought vin history report without decrement counter" in {
      forAll(SalesmanDomainGen, PrivateUserRefGen, VinHistoryBoughtReportGen) {
        (domain, user, vinHistoryBoughtReport) =>
          val url = url"/api/1.x/service/$domain/vin-history/user/${user.toPlain}/report?decrement_count=false"

          http.expectUrl(POST, url)
          http.expectHeader(PreferProtobufAcceptHeader)
          http.expectProto(vinHistoryBoughtReport)
          http.respondWith(StatusCodes.OK, successResponse)

          salesmanUserClient
            .saveBoughtVinHistoryReport(
              domain,
              user,
              counter = None,
              false,
              tags = Set.empty,
              vinHistoryBoughtReport
            )
            .futureValue
      }
    }

    "get subscription price" in {
      forAll(SalesmanDomainGen, PrivateUserRefGen, bool, ProductPriceGen) {
        (domain, user, applyMoneyFeature, productPrice) =>
          val expectedPath = s"/api/2.x/service/$domain/products/prices"

          val expectedParams = List(
            url"user=${user.toPlain}",
            "product=offers-history-reports-1",
            url"applyMoneyFeature=$applyMoneyFeature"
          ).mkString("&")

          val url = expectedPath + "?" + expectedParams

          http.expectUrl(POST, url)
          http.expectHeader(PreferProtobufAcceptHeader)
          http.respondWithMany(List(productPrice))

          val priceRequest =
            PriceRequest
              .newBuilder()
              .setVinHistory {
                VinHistory
                  .newBuilder()
                  .setVin("XXX123")
              }
              .build()

          http.expectProto(priceRequest)

          salesmanUserClient
            .getConcreteSubscriptionPrice(
              domain,
              Some(user),
              AutoruProductWithCount(OffersHistoryReports),
              priceRequest,
              applyMoneyFeature
            )
            .futureValue
      }
    }

    "get discount for user" in {
      forAll(PrivateUserRefGen, SelectorGen) { (user, categorySelector) =>
        val expectedPath = url"/api/1.x/service/$domain/user/${user.toPlain}/discount/${categorySelector.code}"
        http.expectUrl(GET, expectedPath)
        http.expectHeader(PreferProtobufAcceptHeader)
        http.respondWith(StatusCodes.OK, DiscountResponse.getDefaultInstance)

        salesmanUserClient.getUserDiscount(user, categorySelector).futureValue
      }
    }

    "get promocode listing" in {
      val user = AutoruUser(33221100)
      val expectedPath = "/api/1.x/service/autoru/promocodes/user/user%3A33221100"
      http.expectUrl(GET, expectedPath)
      http.expectHeader(PreferProtobufAcceptHeader)
      http.respondWith(StatusCodes.OK, PromocodeListing.getDefaultInstance)

      salesmanUserClient.getPromocodeListing(user).futureValue
    }

    "get payments history" in {
      val user = AutoruUser(33221100)
      val paging = Paging(1, 10)
      val expectedPath =
        url"/api/1.x/service/$domain/user/user%3A33221100/payments?pageNum=${paging.page}&pageSize=${paging.pageSize}"
      http.expectUrl(GET, expectedPath)
      http.expectHeader(PreferProtobufAcceptHeader)
      http.respondWith(StatusCodes.OK, PaymentPage.getDefaultInstance)

      salesmanUserClient.getPaymentsHistory(user, domain, paging).futureValue
    }
  }
}
