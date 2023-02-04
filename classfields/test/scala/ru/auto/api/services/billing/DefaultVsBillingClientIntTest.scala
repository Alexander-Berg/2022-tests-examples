package ru.auto.api.services.billing

import org.scalatest.OptionValues
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.model.billing.vsbilling.{Balance2, Order}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.billing.VsBillingTestUtils.{dealerOrderId, _}

class DefaultVsBillingClientIntTest extends HttpClientSuite with OptionValues with TestRequestWithId {

  override protected def config: HttpClientConfig =
    HttpClientConfig("http", "back-rt-01-sas.test.vertis.yandex.net", 34100)

  val client = new DefaultVsBillingClient(http)

  test("get dealer orders") {
    val res = client.getOrders(dealerBalanceId, agencyBalanceId = None).futureValue
    res.total shouldBe 1
    val page = res.page
    page.size shouldBe 10
    page.number shouldBe 0
    res.values should matchPattern {
      case List(Order(`dealerOrderId`, Balance2(_))) =>
    }
  }

  test("get agency orders") {
    val res = client.getOrders(agencyDealerBalanceId, Some(agencyBalanceId)).futureValue
    res.total shouldBe 1
    val page = res.page
    page.size shouldBe 10
    page.number shouldBe 0
    res.values should matchPattern {
      case List(Order(`agencyOrderId`, Balance2(_))) =>
    }
  }

  test("get short dealer order transactions") {
    val res =
      client
        .getOrderTransactions(dealerBalanceId, agencyBalanceId = None, dealerOrderId, intTestShortDealerParams)
        .futureValue
    val page = res.page
    page.size shouldBe 10
    page.number shouldBe 0
  }

  test("get short agency order transactions") {
    val res =
      client
        .getOrderTransactions(agencyDealerBalanceId, Some(agencyBalanceId), agencyOrderId, intTestShortAgencyParams)
        .futureValue
    val page = res.page
    page.size shouldBe 10
    page.number shouldBe 0
  }

  test("get full dealer order transactions") {
    val res =
      client
        .getOrderTransactions(intTestFullBalanceId, Some(intTestFullAgencyId), intTestFullOrderId, intTestFullParams)
        .futureValue
    val page = res.page
    page.size shouldBe fullPageSize
    page.number shouldBe fullPageNumber
  }
}
