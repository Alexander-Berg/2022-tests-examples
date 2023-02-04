package ru.auto.api.services.cabinet

import java.time.LocalDate

import org.apache.http.client.config.RequestConfig
import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.exceptions.AgentAccessForbidden
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.{AutoruDealer, AutoruUser, DealerUserRoles, RequestParams}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.cabinet.BalanceTestData._
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.cabinet.Redemption.RedemptionForm
import ru.auto.cabinet.Redemption.RedemptionForm.{CarDescription, ClientInfo, DealerInfo, SuggestedCar}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class DefaultCabinetApiClientIntTest extends HttpClientSuite with MockitoSupport with OptionValues {

  override protected def config: HttpClientConfig =
    HttpClientConfig(
      "http",
      "autoru-cabinet-api-http.vrts-slb.test.vertis.yandex.net",
      80,
      RequestConfig
        .custom()
        .setConnectionRequestTimeout(10000)
        .setConnectTimeout(300)
        .setSocketTimeout(3000)
        .build()
    )

  val cabinetApiClient = new DefaultCabinetApiClient(http)
  implicit override val trace: Traced = mock[Traced]

  var requestId: String = Gen.identifier.filter(_.nonEmpty).next
  when(trace.toString).thenReturn(requestId)
  when(trace.requestId).thenReturn(requestId)

  implicit val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  test("successful check client") {
    requestId = Gen.identifier.filter(_.nonEmpty).next
    val agent = AutoruUser(906442)
    val client = AutoruDealer(564)
    val response = cabinetApiClient.checkAccess(agent, client).futureValue
    response.message shouldBe "OK"
    response.role shouldBe DealerUserRoles.Client
  }

  test("successful check agency") {
    requestId = Gen.identifier.filter(_.nonEmpty).next
    val agent = AutoruUser(14439810)
    val client = AutoruDealer(12268)
    val response = cabinetApiClient.checkAccess(agent, client).futureValue
    response.message shouldBe "OK"
    response.role shouldBe DealerUserRoles.Agency
  }

  test("successful check company") {
    pending
    requestId = Gen.identifier.filter(_.nonEmpty).next
    val agent = AutoruUser(15498345)
    val client = AutoruDealer(25870)
    val response = cabinetApiClient.checkAccess(agent, client).futureValue
    response.message shouldBe "OK"
    response.role shouldBe DealerUserRoles.Company
  }

  test("unsuccessful check") {
    requestId = Gen.identifier.filter(_.nonEmpty).next
    val agent = AutoruUser(14439810)
    val client = AutoruDealer(88005553535L)
    val error = cabinetApiClient.checkAccess(agent, client).failed.futureValue
    error shouldBe a[AgentAccessForbidden]
  }

  test("redemption successfully sent") {
    val mark = "MARK"
    val model = "MODEL"
    val km = 10
    val year = 10
    val price = 1000000
    val userName = "Vasiya"
    val telNumber = "+123"
    val dealerProfileUri = "http://hello-world.ru/123"
    val dealer = AutoruDealer(20101)
    val form = RedemptionForm
      .newBuilder()
      .setClient(
        ClientInfo
          .newBuilder()
          .setName(userName)
          .setPhoneNumber(telNumber)
      )
      .setDealerInfo(
        DealerInfo
          .newBuilder()
          .setProfileUrl(dealerProfileUri)
      )
      .setSuggestedCar(
        SuggestedCar
          .newBuilder()
          .setDescription(
            CarDescription
              .newBuilder()
              .setMark(mark)
              .setModel(model)
              .setPrice(price)
              .setYear(year)
              .setMileage(km)
          )
      )
      .build()

    cabinetApiClient.submitRedemption(dealer, form).futureValue
  }

  test("get balance client") {
    val balanceClient = cabinetApiClient.getBalanceClient(dealer)(dealerRequest).futureValue
    balanceClient.balanceClientId shouldBe dealerBalanceId
    balanceClient.balanceAgencyId shouldBe None
  }

  test("get expenses report") {
    val dealer = AutoruDealer(20101)
    val fromDate = LocalDate.parse("2017-12-06")
    val toDate = LocalDate.parse("2018-12-05")
    val response = cabinetApiClient.expensesReport(dealer, fromDate, toDate, 1)(dealerRequest).futureValue
    response.hasProcessStatus shouldBe true
  }

  test("get balance agency client") {
    val balanceClient = cabinetApiClient.getBalanceClient(agencyDealer)(agencyDealerRequest).futureValue
    balanceClient.balanceClientId shouldBe agencyDealerBalanceId
    balanceClient.balanceAgencyId.value shouldBe agencyBalanceId
  }

  test("get dealer account") {
    val response = cabinetApiClient.getDealerAccount(dealerId, DealerUserRoles.Manager)(dealerRequest).futureValue
    response.getBalanceClientId shouldBe dealerBalanceId.value
  }

  test("throw forbidden") {
    val ex = cabinetApiClient.getBalanceClient(agencyDealer)(wrongUserRequest).failed.futureValue
    ex shouldBe an[AgentAccessForbidden]
  }
}
