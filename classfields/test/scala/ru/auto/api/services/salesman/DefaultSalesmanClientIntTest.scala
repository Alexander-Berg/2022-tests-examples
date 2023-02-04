package ru.auto.api.services.salesman

import java.time.LocalDate
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.billing.AutostrategyModel.AutostrategyId
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.autostrategies._
import ru.auto.api.model.gen.SalesmanModelGenerators
import ru.auto.api.model.{AutoruDealer, MetricProduct, OfferID}
import ru.auto.api.services.HttpClientSuite
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm._

import scala.jdk.CollectionConverters._

class DefaultSalesmanClientIntTest
  extends HttpClientSuite
  with ScalaCheckPropertyChecks
  with SalesmanModelGenerators
  with TestRequest {

  override protected def config: HttpClientConfig =
    HttpClientConfig("salesman-tasks-01-sas.test.vertis.yandex.net", 1030)

  private val client = new DefaultSalesmanClient(http)

  test("create, get, delete and unable to get autostrategy") {
    val allAutostrategies = Gen.nonEmptyListOf(AutostrategyGen).next
    // need to filter redundant generated autostrategies
    val autostrategies = allAutostrategies
      .groupBy(autostrategy => (autostrategy.getOfferId, autostrategy.getPayloadCase))
      .values
      .map(_.minBy(_.getOfferId))
      .toList
      .sortBy(_.getOfferId)
    client.putAutostrategies(autostrategies).futureValue
    val offerIds = autostrategies.map(_.getOfferId).map(OfferID.parse).distinct
    val offerAutostrategies = client.getAutostrategies(offerIds).futureValue
    val gotAutostrategies = offerAutostrategies.flatMap(_.getAutostrategiesList.asScala)
    gotAutostrategies should contain theSameElementsAs autostrategies
    val ids = autostrategies.map { autostrategy =>
      AutostrategyId
        .newBuilder()
        .setOfferId(autostrategy.getOfferId)
        .setAutostrategyType(autostrategy.getType)
        .build()
    }
    client.deleteAutostrategies(ids).futureValue
    val noAutostrategies = client.getAutostrategies(offerIds).futureValue
    noAutostrategies.flatMap(_.getAutostrategiesList.asScala) shouldBe empty
  }

  test("get campaigns") {
    client.getCampaigns(AutoruDealer(20101), includeDisabled = false).futureValue should not be empty
  }

  test("get disabled campaigns too") {
    client.getCampaigns(AutoruDealer(20101), includeDisabled = true).futureValue should not be empty
  }

  test("get call info") {
    val info = client.getCallInfo(AutoruDealer(20101)).futureValue
    info.callCost should be > 0L
    info.depositCoefficient shouldBe 3
  }

  test("trade-in request successfully sent") {
    val userOfferMark = "Kia"
    val userOfferModel = "Rio"
    val userOfferMileage = 500000
    val userOfferYear = 2010
    val userOfferPrice = 100000
    val userName = "Vasiya"
    val phoneNumber = "+79175586983"
    val clientOfferMark = "Porche"
    val clientOfferModel = "r9"
    val clientOfferMileage = 1000
    val clientOfferYear = 2019
    val clientOfferPrice = 9000000

    val form = TradeInRequestForm
      .newBuilder()
      .setUserInfo(
        UserInfo
          .newBuilder()
          .setName(userName)
          .setPhoneNumber(phoneNumber)
      )
      .setClientInfo(
        ClientInfo
          .newBuilder()
          .setClientId(123)
      )
      .setClientOfferInfo(
        OfferInfo
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.NEW)
          .setOfferId("12345-1234")
          .setDescription(
            OfferDescription
              .newBuilder()
              .setMark(clientOfferMark)
              .setModel(clientOfferModel)
              .setPrice(clientOfferPrice)
              .setYear(clientOfferYear)
              .setMileage(clientOfferMileage)
          )
      )
      .setUserOfferInfo(
        OfferInfo
          .newBuilder()
          .setOfferId("1234-321")
          .setCategory(Category.CARS)
          .setDescription(
            OfferDescription
              .newBuilder()
              .setMark(userOfferMark)
              .setModel(userOfferModel)
              .setPrice(userOfferPrice)
              .setYear(userOfferYear)
              .setMileage(userOfferMileage)
          )
      )
      .build()

    client.putTradeInRequest(form).futureValue
  }

  test("get trade-in requests") {
    val dealerId = 20101
    val section = None //no filter by section
    val fromDate = LocalDate.of(2019, 1, 1)
    val toDate = None
    val pageNum = 1
    val pageSize = 10

    client.getTradeInRequests(dealerId, section, fromDate, toDate, pageNum, pageSize).futureValue
  }

  test("get offers with paid services") {
    val from = LocalDate.of(2019, 1, 1)
    val to = LocalDate.of(2019, 1, 4)
    val productsFilter = List(MetricProduct.Placement, MetricProduct.FreshOffers)
    val dealer = AutoruDealer(20101)
    val pageSize = 10
    val pageNum = 0
    client.getOffersWithPaidProductsIdentities(dealer, from, to, productsFilter, pageSize, pageNum).futureValue
  }
}
