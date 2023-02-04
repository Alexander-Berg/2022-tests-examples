package ru.yandex.vertis.moderation.service

import ru.yandex.extdata.core.gens.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.PhoneRedirectInfoClient
import ru.yandex.vertis.moderation.client.PhoneRedirectInfoClient.Contacts
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.HoboSignalSourceGen
import ru.yandex.vertis.moderation.proto.Model.HoboCheckType
import ru.yandex.vertis.moderation.service.PhoneRedirectSignalEnrichmentServiceImplSpec.{enrichedPayload, payload}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PhoneRedirectSignalEnrichmentServiceImplSpec extends SpecBase {
  val client = mock[PhoneRedirectInfoClient]
  private val service = new PhoneRedirectSignalEnrichmentServiceImpl(client)

  "SignalEnricher" should {
    "enrich signal with correct label" in {
      val redirectedId = "redirectedId"
      val redirected =
        HoboSignalSourceGen.next.copy(
          snapshotPayload = Some(payload(redirectedId)),
          labels = Set("general_actuality_to_cc"),
          `type` = HoboCheckType.CALLGATE_OFFERS_CALL
        )

      doReturn(Future.successful(Contacts("redirected", "original", true)))
        .when(client)
        .getRedirectContacts(redirectedId)

      val res = service.enrichHoboSignal(redirected).futureValue

      res.snapshotPayload.get shouldBe enrichedPayload
    }

    "correctly not change signals without redirected phone" in {
      val unredirectedId = "unredirectedId"
      val unredirected =
        HoboSignalSourceGen.next.copy(
          snapshotPayload = Some(payload(unredirectedId)),
          labels = Set("general_actuality_to_cc")
        )
      doReturn(Future.successful(Contacts("phone", "", false))).when(client).getRedirectContacts(unredirectedId)

      val res = service.enrichHoboSignal(unredirected).futureValue

      res shouldBe unredirected
    }

    "ignore other hobo signals" in {
      val other =
        HoboSignalSourceGen.next.copy(
          labels = Set("other")
        )

      val res = service.enrichHoboSignal(other).futureValue

      res shouldBe other
    }
  }
}

object PhoneRedirectSignalEnrichmentServiceImplSpec {
  def payload(offerId: String) =
    s"""|{
        |  "originalPhone" : "phone",
        |  "process":"offer_actuality_check",
        |  "userId": "userId",
        |  "offerId": "$offerId",
        |  "url": "url",
        |  "origin": "origin",
        |  "phone": "phone",
        |  "regionName": "regionName",
        |  "categoryId": "categoryId",
        |  "fullCategoryName": "fullCategoryName"
        | }
        |""".stripMargin

  val enrichedPayload =
    s"""|{
        |  "redirectPhone" : "redirected",
        |  "originalPhone" : "original",
        |  "phone" : "redirected",
        |  "process" : "offer_actuality_check",
        |  "userId" : "userId",
        |  "offerId" : "redirectedId",
        |  "url" : "url",
        |  "origin" : "origin",
        |  "regionName" : "regionName",
        |  "categoryId" : "categoryId",
        |  "fullCategoryName" : "fullCategoryName"
        |}""".stripMargin
}
