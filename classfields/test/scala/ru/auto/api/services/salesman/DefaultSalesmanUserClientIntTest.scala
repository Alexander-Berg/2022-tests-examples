package ru.auto.api.services.salesman

import com.google.protobuf.util.Durations
import org.scalatest.Inspectors
import ru.auto.api.ApiOfferModel
import ru.auto.api.auth.Application
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.ModelGenerators.{OfferIDGen, PrivateUserRefGen}
import ru.auto.api.model.salesman.TransactionId
import ru.auto.api.model.{AutoruProduct, AutoruUser, RequestParams}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.billing.util.SalesmanTicketId
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain.AutoruSalesmanDomain
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.salesman.model.user.ApiModel._
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest.VinHistory
import ru.yandex.vertis.feature.model.Feature

import scala.jdk.CollectionConverters._

/**
  * @author alex-kovalenko
  */
class DefaultSalesmanUserClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("back-rt-01-sas.test.vertis.yandex.net", 1050)

  private val salesmanUserClient = new DefaultSalesmanUserClient(http)

  val user = AutoruUser(1)

  implicit val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.web)
    r.setUser(user)
    r
  }
  private val domain = AutoruSalesmanDomain

  test("get user billing") {
    val userRef = PrivateUserRefGen.next
    salesmanUserClient.getUserBilling(userRef).futureValue
  }

  val productPrice = ProductPrice
    .newBuilder()
    .setProduct("placement")
    .setDuration(Durations.fromSeconds(60 * 60 * 24 * 7))
    .build()
  val offer = OfferIDGen.next

  val productRequest = ProductRequest
    .newBuilder()
    .setProduct("placement")
    .setOffer(offer.toPlain)
    .setAmount(100L)
    .setContext(
      ProductContext
        .newBuilder()
        .setGoods(
          ProductContext.GoodsContext
            .newBuilder()
            .setProductPrice(productPrice)
            .build()
        )
        .build()
    )
    .build()

  val transactionRequest = TransactionRequest
    .newBuilder()
    .setUser(user.toPlain)
    .setAmount(100L)
    .addPayload(productRequest)
    .build()

  test("create, get and process transaction") {
    val created = salesmanUserClient.createTransaction(transactionRequest, domain).futureValue

    val got = salesmanUserClient
      .getTransaction(user, SalesmanTicketId(created.getTransactionId, AutoruSalesmanDomain))
      .futureValue
    got.getStatus shouldBe Transaction.Status.NEW
    got.getUser shouldBe user.toPlain
    got.getAmount shouldBe 100L
    got.getPayloadList should have size 1

    salesmanUserClient.processTransaction(SalesmanTicketId(created.getTransactionId, AutoruSalesmanDomain)).futureValue

    val process = salesmanUserClient
      .getTransaction(user, SalesmanTicketId(created.getTransactionId, AutoruSalesmanDomain))
      .futureValue
    process.getStatus shouldBe Transaction.Status.PROCESS
  }

  test("create, get and process transaction by primary key id") {
    val created = salesmanUserClient.createTransaction(transactionRequest, domain).futureValue

    val got = salesmanUserClient
      .getTransaction(user, SalesmanTicketId(created.getTransactionId, AutoruSalesmanDomain))
      .futureValue
    got.getStatus shouldBe Transaction.Status.NEW
    got.getUser shouldBe user.toPlain
    got.getAmount shouldBe 100L
    got.getPayloadList should have size 1

    salesmanUserClient.processTransaction(SalesmanTicketId(created.getTransactionId, AutoruSalesmanDomain)).futureValue

    val process = salesmanUserClient
      .getTransaction(user, SalesmanTicketId(created.getTransactionId, AutoruSalesmanDomain))
      .futureValue
    process.getStatus shouldBe Transaction.Status.PROCESS
  }

  test("create and get account refill request") {
    val accountRefill = AccountRefillRequest
      .newBuilder()
      .setTransactionId("12345-test")
      .setPaymentRequestId("1212322323")
      .build()

    val noTransaction = salesmanUserClient.getAccountRefill("no-transaction-test").futureValue
    noTransaction shouldBe None

    salesmanUserClient.accountRefill(accountRefill).futureValue
    val result = salesmanUserClient.getAccountRefill(accountRefill.getTransactionId).futureValue
    result shouldBe Some(accountRefill)
  }

  test("set and unset prolongable for transaction") {
    // placement isn't prolongable, use top in test
    val prolongableProductRequest = productRequest.toBuilder
      .setProduct("top")
      .setProlongable(false)
      .build()
    val builder = transactionRequest.toBuilder
    builder.clearPayload().addPayload(prolongableProductRequest)
    val created = salesmanUserClient.createTransaction(builder.build(), domain).futureValue
    salesmanUserClient.putProlongable(domain, TransactionId(created.getTransactionId)).futureValue
    val ticketId = SalesmanTicketId(created.getTransactionId, domain)
    val updated = salesmanUserClient.getTransaction(user, ticketId).futureValue
    Inspectors.forEvery(updated.getPayloadList.asScala)(_.getProlongable shouldBe true)
    salesmanUserClient.deleteProlongable(domain, TransactionId(created.getTransactionId)).futureValue
    val cleared = salesmanUserClient.getTransaction(user, ticketId).futureValue
    Inspectors.forEvery(cleared.getPayloadList.asScala)(_.getProlongable shouldBe false)
  }

  test("get subscription price via price request") {
    val vin = "JN1GANR35U0101949"
    val pr = PriceRequest
      .newBuilder()
      .setVinHistory(VinHistory.newBuilder().setVin(vin))
    val result = salesmanUserClient
      .getConcreteSubscriptionPrice(
        AutoruSalesmanDomain,
        None,
        AutoruProduct.OffersHistoryReports,
        pr.build(),
        applyMoneyFeature = false
      )
      .futureValue
    result.getPrice.getEffectivePrice shouldNot be(0)
  }

  test("get multiple offers prices") {
    val offer = OfferGen.next
    val offers = ApiOfferModel.OffersList.newBuilder().addAllOffers(Seq(offer).asJava).build
    val disabledShowInStories = new Feature[Boolean] {
      override def name: String = ""
      override def value: Boolean = false
    }
    val result = salesmanUserClient
      .getMultipleOffersPrices(
        AutoruProduct.autoruUserProducts(disabledShowInStories),
        applyMoneyFeature = false,
        offers
      )
      .futureValue
    result.head.getOfferId shouldBe offer.getId
  }

}
