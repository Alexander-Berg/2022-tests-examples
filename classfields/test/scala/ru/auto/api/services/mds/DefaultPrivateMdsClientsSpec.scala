package ru.auto.api.services.mds

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.model.ModelGenerators
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._

class DefaultPrivateMdsClientsSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {
  val mdsClient = new DefaultPrivateMdsClient(http, None, "avatars.mdst.yandex.net")

  "DefaultPrivateMdsClient" should {
    "request mds meta" in {
      val id = ModelGenerators.PhotoIDGen.next
      http.expectUrl(url"/getimageinfo-${id.namespace}/${id.groupId}/${id.hash}")
      http.respondWithJsonFrom("/mds/meta.json")

      val result = mdsClient.getMeta(id).futureValue

      result.isFinished shouldBe true
      result.platesList.head.number shouldBe "K888EC199"
      result.platesList.head.confidence shouldBe 0.853
      result.recognitionList.size shouldBe 5
      result.recognitionList.head.confidence shouldBe 248
      result.recognitionList.head.info.getCategory shouldBe Category.CARS
      result.recognitionList.head.info.getMark.getCode shouldBe "MERCEDES"
      result.recognitionList.head.info.getModel.getCode shouldBe "V_KLASSE"
    }
  }
}
