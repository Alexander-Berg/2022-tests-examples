package ru.auto.api.services.bigbrother

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.managers.TestRequest
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Request
import ru.yandex.proto.crypta.Profile
import ru.yandex.vertis.mockito.MockitoSupport

class DefaultBigBrotherClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest
  with MockitoSupport {

  private val client = new DefaultBigBrotherClient(http)

  implicit override def request: Request = super.request

  import DefaultBigBrotherClient._

  "BigBrotherClient" should {
    "returns crypta profile" in {
      val yandexUid = "1234567890"
      http.expectUrl(
        s"/bigb?$FormatParam=protobuf&$BigBrotherUidParam=$yandexUid&$ClientParam=autoru-api"
      )
      http.respondWithProtoFrom[Profile]("/bigbrother/profile.json")
      val result = client.getProfile(BigBrotherSearchParams.apply(yandexUid = Some(yandexUid))).futureValue
      result.getItems(0).getUintValues(0) shouldBe 12345678
    }
  }
}
