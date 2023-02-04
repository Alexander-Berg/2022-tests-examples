package ru.yandex.vertis.billing.callcenter

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.callcenter.client.impl.TelecontactCallCenterWithCampaignsClient
import ru.yandex.vertis.billing.model_core.Phone
import ru.yandex.vertis.billing.model_core.callcenter.{CallCenterCall, CallCenterIds}
import ru.yandex.vertis.billing.settings.CallCenterWithCampaignsSettings
import ru.yandex.vertis.billing.util.DateTimeInterval
import spray.json.enrichString
import sttp.client3.testing.SttpBackendStub
import sttp.monad._

import scala.concurrent.Future
import scala.io.Source

/**
  * @author ruslansd
  */
class TelecontactCallCenterWithCampaignsClientSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  private val callCenterId = CallCenterIds.TK
  private val campaign1 = "1"
  private val campaign2 = "2"

  private val baseUrl =
    "https://tc-msk-app2.telecontact.ru/projects/yandex/sc200114_0458_yandex_vertical_predictive_test/Api/External/ServiceExtended.php"
  private val response = Source.fromURL(getClass.getResource("/telecontact_response.json")).getLines().mkString
  private val emptyResponse = """{"response":{"status":{"code":0}}}"""
  private val errorResponse = """{"response":{"status":{"code":1}}}"""

  private val campaign1Call = CallCenterCall(
    s"${CallCenterIds.TK}-342",
    "+79192046424",
    Phone("7", "499", "9566620"),
    new DateTime("2020-02-19T11:47:14.000+0300"),
    CallCenterIds.TK,
    Some("1"),
    "342",
    """{"callId":"342","campaignid":"1","dateTimeCall":"2020-02-19T11:41:46+0300","phoneTransfer":"+74999566620","callcentrename":"TK","dateTimeTransfer":"2020-02-19T11:47:14+0300","phoneTransferClient":"+79192046424"}""".parseJson
  )

  private val campaign2Call = CallCenterCall(
    s"${CallCenterIds.TK}-343",
    "+79192046424",
    Phone("7", "499", "9566620"),
    new DateTime("2020-02-19T11:47:14.000+0300"),
    CallCenterIds.TK,
    Some("2"),
    "343",
    """{"callId":"343","campaignid":"2","dateTimeCall":"2020-02-19T11:41:46+0300","phoneTransfer":"+74999566620","callcentrename":"TK","dateTimeTransfer":"2020-02-19T11:47:14+0300","phoneTransferClient":"+79192046424"}""".parseJson
  )

  implicit val monad = new FutureMonad()

  def client(
      settings: CallCenterWithCampaignsSettings,
      response: String): TelecontactCallCenterWithCampaignsClient[Future] =
    new TelecontactCallCenterWithCampaignsClient(settings)(testingBackend(response), monad)

  def testingBackend(response: String) =
    SttpBackendStub.asynchronousFuture
      .whenRequestMatches(_.uri.toString.startsWith(baseUrl))
      .thenRespond(response)

  private def settings(campaignIds: Iterable[String]) = {
    val campaignIdToUrl = campaignIds.map { id =>
      id -> baseUrl
    }.toMap

    CallCenterWithCampaignsSettings(
      callCenterId,
      campaignIdToUrl
    )
  }

  "TelecontactCallCenterWithCampaignsClient" should {
    "read nothing on empty response" in {
      val s = settings(Iterable(campaign2, campaign1))
      val c = client(s, emptyResponse)

      c.calls(DateTimeInterval.currentDay).futureValue shouldBe empty
    }

    "throw error on error response" in {
      val s = settings(Iterable(campaign2, campaign1))
      val c = client(s, errorResponse)

      intercept[IllegalStateException] {
        c.calls(DateTimeInterval.currentDay).await
      }
    }

    "successfully get calls" in {
      val s = settings(Iterable(campaign2, campaign1))
      val c = client(s, response)

      val expectedCalls = Iterable(campaign1Call, campaign2Call)
      val responseCall = c.calls(DateTimeInterval.currentDay).futureValue.toSet

      responseCall should contain theSameElementsAs expectedCalls
    }

    "successfully get calls by specified campaign" in {
      val s = settings(Iterable(campaign1))
      val c = client(s, response)

      val expectedCalls = Iterable(campaign1Call)
      val responseCall = c.calls(DateTimeInterval.currentDay).futureValue.toSet

      responseCall should contain theSameElementsAs expectedCalls
    }

  }

}
