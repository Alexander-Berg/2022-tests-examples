package ru.yandex.realty.rent.clients.mango

import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.jwt.JwtBasedClient.ActualTokens
import ru.yandex.realty.rent.clients.mango.model.{
  CancelFlatPolicyResult,
  CancelFlatPolicySubRequest,
  FlatInsurancePolicyRequest,
  FlatInsurancePolicySubscription
}

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class MangoClientSpec extends SpecBase with AsyncSpecBase with PropertyChecks with RequestAware with HttpClientMock {
  import MangoClientSpec._

  private val client = new MangoClientImpl(httpService, tokens, partnerCode, promoCode)

  "MangoClient" should {
    "create flat insurance policy" in {
      httpClient.expect(HttpMethods.POST, "/api/partner/v1/product/flat/realty/issue")
      httpClient.expectHeader("Authorization", s"Bearer ${tokens.accessToken}")
      httpClient.expectJson(Json.toJson(insurancePolicyReq).toString)

      httpClient.respondWith(
        StatusCodes.OK,
        Json.toJson(subscriptionRes).toString
      )

      client.createFlatInsurancePolicy(insurancePolicyReq).futureValue equals subscriptionRes
    }

    "cancel insurance subscription correctly" in {
      httpClient.expect(HttpMethods.POST, "/api/partner/v1/product/flat/disable_renewing")
      httpClient.expectHeader("Authorization", s"Bearer ${tokens.accessToken}")
      httpClient.expectJson(Json.toJson(cancelSubReq).toString)

      httpClient.respondWith(
        StatusCodes.OK,
        Json.toJson(cancelSubRes).toString
      )

      client
        .cancelFlatPolicySubscription(subscriptionRes.tenantSubscriptionId, cancelSubReason)
        .futureValue equals cancelSubRes
    }

  }
}

object MangoClientSpec {
  private val tokens = ActualTokens(Random.nextString(30), Random.nextString(30))
  private val partnerCode = Random.nextString(5)
  private val promoCode = Random.nextString(5)

  private val insurancePolicyReq = FlatInsurancePolicyRequest(
    Random.nextString(15),
    Random.nextString(15),
    "landlord@test.ru",
    "+79998887766",
    Random.nextInt(30000).toString,
    "2020-01-01",
    Random.nextString(15),
    "+79998887755",
    "tenant@test.ru",
    partnerCode,
    promoCode
  )

  private val subscriptionRes = FlatInsurancePolicySubscription(Random.nextString(36), Random.nextString(36))

  val cancelSubReason = "My test reason."
  val cancelSubReq = CancelFlatPolicySubRequest(subscriptionRes.tenantSubscriptionId, cancelSubReason)
  val cancelSubRes = CancelFlatPolicyResult(subscriptionRes.tenantSubscriptionId, true)
}
