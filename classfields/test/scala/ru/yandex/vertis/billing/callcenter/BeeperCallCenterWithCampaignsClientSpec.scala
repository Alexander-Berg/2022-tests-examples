package ru.yandex.vertis.billing.callcenter

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.callcenter.BeeperCallCenterWithCampaignsClientSpec._
import ru.yandex.vertis.billing.callcenter.client.impl.BeeperCallCenterWithCampaignsClient
import ru.yandex.vertis.billing.model_core.Phone
import ru.yandex.vertis.billing.model_core.callcenter.CallCenterIds
import ru.yandex.vertis.billing.settings.CallCenterWithCampaignsSettings
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}
import sttp.client3.testing.SttpBackendStub
import sttp.monad._

import scala.concurrent.Future

class BeeperCallCenterWithCampaignsClientSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  private val callCenterId = CallCenterIds.Beeper
  private val callCenterCampaignIds = Iterable("3535")
  private val baseUrl = "http://autorucalls.beeper.ru"

  private val callCenterCampaignIdToUrl = callCenterCampaignIds.map { campaignId =>
    campaignId -> baseUrl
  }.toMap

  private val settings = CallCenterWithCampaignsSettings(
    callCenterId,
    callCenterCampaignIdToUrl
  )

  implicit val testingBackend = SttpBackendStub.asynchronousFuture
    .whenRequestMatches(_.uri.toString.startsWith(baseUrl))
    .thenRespond(ExpectedCallStr)

  implicit val monad: FutureMonad = new FutureMonad()

  private val client = new BeeperCallCenterWithCampaignsClient[Future](settings)

  "CallCenterWithCampaignsClient" should {
    "correctly fetch and parse call center calls" in {
      val actualCalls = client.calls(DateTimeInterval.currentDay).futureValue
      actualCalls.size shouldBe 1
      val actualCall = actualCalls.head
      actualCall.id shouldBe s"$CallCenterId-$CallCenterCallId"
      actualCall.callCenterName shouldBe CallCenterId
      actualCall.callCenterCampaignId shouldBe Some(CallCenterCampaignId)
      actualCall.callCenterCallId shouldBe CallCenterCallId
      actualCall.calleeNumber shouldBe s"+$CallCenterCallClientNumber"
      actualCall.callerNumber shouldBe extractPhone(CallCenterCallDealerNumber)
      actualCall.timestamp shouldBe parseTimestamp(CallCenterCallTransferStartDateTimeUtc)
    }
  }

}

object BeeperCallCenterWithCampaignsClientSpec {

  private val CallCenterResponseDateTimeFormatter =
    DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZone(DateTimeZone.UTC)

  private def parseTimestamp(timestampStr: String): DateTime = {
    CallCenterResponseDateTimeFormatter.parseDateTime(timestampStr).withZone(DateTimeUtils.TimeZone)
  }

  private def extractPhone(number: String): Phone = {
    val code = number.take(1)
    val country = number.slice(1, 4)
    val phone = number.drop(4)
    Phone(code, country, phone)
  }

  private val CallCenterId = CallCenterIds.Beeper
  private val CallCenterCallId = "1530643469282"
  private val CallCenterCalStartTime = "29.01.2020 15:33:26"
  private val CallCenterCampaignId = "5154"
  private val CallCenterCallClientNumber = "79272874935"
  private val CallCenterCallDealerNumber = "74954100319"
  private val CallCenterCallTransferStartDateTimeUtc = "29.01.2020 15:34:24"

  private val ExpectedCallStr =
    s"""
    [
      {
          "CallCentreName": "$CallCenterId",
          "CallId": "$CallCenterCallId",
          "CallStart": "$CallCenterCalStartTime",
          "CampaignId": $CallCenterCampaignId,
          "ClientNumber": "$CallCenterCallClientNumber",
          "DealerNumber": "$CallCenterCallDealerNumber",
          "TransferStartDateTimeUtc": "$CallCenterCallTransferStartDateTimeUtc"
      }
    ]
    """

}
