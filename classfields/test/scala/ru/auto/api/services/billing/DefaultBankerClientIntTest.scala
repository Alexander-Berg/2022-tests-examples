package ru.auto.api.services.billing

import ru.auto.api.auth.Application.{moderation, swagger}
import ru.auto.api.auth.{Application, Grants}
import ru.auto.api.exceptions.AccountNotFoundException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.ModelGenerators.ReadableStringGen
import ru.auto.api.model.banker.AccountConsumeRequest
import ru.auto.api.model.gen.BankerModelGenerators
import ru.auto.api.model.{AutoruUser, RateLimit, RequestParams}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.billing.BankerClient.PaymentMethodFilter
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.banker.model.ApiModel._

import scala.concurrent.Future

/**
  * @author ruslansd
  */
class DefaultBankerClientIntTest extends HttpClientSuite with BankerModelGenerators {

  override protected def config: HttpClientConfig =
    HttpClientConfig("banker-api-http-api.vrts-slb.test.vertis.yandex.net", 80)

  private val user = AutoruUser(1234)
  private val client = new DefaultBankerClientImpl(http)
  private val accountId = "public-api-unit-test"
  private val account = Account.newBuilder().setId(accountId).setUser(user.toPlain).build()
  private val yandexPassportId = Some("12345")

  implicit val request: Request = {
    val r = new RequestImpl
    r.setApplication(swagger)
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(user)
    r
  }

  val moderationRequest: Request = {
    val r = new RequestImpl
    r.setApplication(moderation)
    r
  }

  private val maxPoster = Application.external(
    "maxposter",
    RateLimit.PerApplication(300),
    Grants.Breadcrumbs,
    Grants.Catalog,
    Grants.PassportLogin,
    Grants.Search
  )

  val maxPosterRequest: Request = {
    val r = new RequestImpl
    r.setApplication(maxPoster)
    r.setUser(AutoruUser(9999))
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  test("get info") {
    client.getAccountInfo(user, accountId).futureValue
  }

  test("get info for moderation") {
    client.getAccountInfo(user, accountId)(moderationRequest).futureValue
  }

  test("not get info for non-max-poster account on max-poster request") {
    pending
    client.getAccountInfo(user, accountId)(maxPosterRequest).failed.futureValue shouldBe a[AccountNotFoundException]
  }

  test("get account") {
    val accounts = client.getAccounts(user).futureValue
    if (accounts.nonEmpty) {
      accounts.map(_.getUser).toSet shouldBe Set(user.toPlain)
    }
  }

  test("create account") {
    val properties = AccountPropertiesGen.next
    val accountSource = account.toBuilder.setProperties(properties).build()
    client.createAccount(user, accountSource).futureValue shouldBe accountSource
  }

  test("update account") {
    val patch = AccountPatchGen.next
    val updatedAccount = client.updateAccount(account, patch).futureValue
    if (patch.hasEmail) {
      updatedAccount.getProperties.getEmail shouldBe patch.getEmail.getValue
    }
    if (patch.hasPhone) {
      updatedAccount.getProperties.getPhone shouldBe patch.getPhone.getValue
    }
  }

  test("request payment methods") {
    val source = PaymentRequest.Source
      .newBuilder()
      .setAccount(accountId)
      .setAmount(100)
      .setContext(PaymentRequest.Context.newBuilder().setTarget(Target.WALLET))
      .build()

    val res = client.requestPaymentMethods(user, source).futureValue
    val ykMethods = res.filter(_.getPsId == PaymentSystemId.YANDEXKASSA)
    ykMethods should not be empty
  }

  test("request payment") {
    val psId = PaymentSystemId.FREEOFCHARGE
    val method = "free"
    val source = PaymentRequest.Source
      .newBuilder()
      .setAccount(accountId)
      .setAmount(100)
      .setContext(PaymentRequest.Context.newBuilder().setTarget(Target.WALLET))
      .build()
    client.requestPayment(user, psId, method, source, yandexPassportId = yandexPassportId).futureValue
  }

  test("get payment methods") {
    val psId = PaymentSystemId.YANDEXKASSA
    val methodId = "AC"

    val all = client.getPaymentMethods(user, PaymentMethodFilter.All, yandexPassportId = yandexPassportId).futureValue

    val forPs =
      client
        .getPaymentMethods(user, PaymentMethodFilter.ForPaymentSystem(psId), yandexPassportId = yandexPassportId)
        .futureValue
    forPs.foreach(method => method.getPsId shouldBe psId)
    val forName = client
      .getPaymentMethods(user, PaymentMethodFilter.ForMethodName(psId, methodId), yandexPassportId = yandexPassportId)
      .futureValue
    forName.foreach { method =>
      method.getPsId shouldBe psId
      method.getId should be(methodId).or(startWith(methodId))
    }

    all should not be empty
    forPs should not be empty
    forName should not be empty
  }

  test("delete payment method") {
    val psId = PaymentSystemId.YANDEXKASSA
    client.deleteMethod(user, psId, "AC#444444|4448").futureValue
  }

  test("add funds to account, wait for actual adding and then pay by account") {
    pending
    val psId = PaymentSystemId.FREEOFCHARGE
    val method = "free"
    val amount = 100
    val source = PaymentRequest.Source
      .newBuilder()
      .setAccount(accountId)
      .setAmount(amount)
      .setContext(PaymentRequest.Context.newBuilder().setTarget(Target.WALLET))
      .build()

    def getInfoAfterPayment: Future[AccountInfo] = {
      client.getAccountInfo(user, accountId).flatMap { info =>
        if (info.getBalance < amount) {
          getInfoAfterPayment
        } else {
          Future.successful(info)
        }
      }
    }

    getInfoAfterPayment.futureValue
    client.requestPayment(user, psId, method, source, yandexPassportId = None).futureValue
    val paymentId = ReadableStringGen.next
    val payload = PayloadGen.next
    val receiptData = receiptDataGen(amount).next
    val consumeRequest = AccountConsumeRequest(paymentId, payload, accountId, amount, receiptData)
    val transaction = client.payByAccount(user, accountId, consumeRequest).futureValue
    transaction.getId.getId shouldBe paymentId
    transaction.getId.getType shouldBe TransactionType.WITHDRAW
    transaction.getAccount shouldBe accountId
    transaction.getPayload shouldBe payload
    transaction.getReceipt shouldBe receiptData
  }
}
