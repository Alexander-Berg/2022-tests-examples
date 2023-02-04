package ru.yandex.vertis.billing.settings

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt

class CallCentersSettingsSpec extends AnyWordSpec with Matchers {

  private val CallCenterId = "beeper"

  private val CallCenterCampaignToUrl = Map(
    "1" -> "best.call.center.api.ever.seen",
    "2" -> "ordinary.call.center.api.ever.seen",
    "6" -> "worst.call.center.api.ever.seen"
  )
  private val CallCenterClientConnectionTimeout = 5.seconds
  private val CallCenterClientRequestTimeout = 10.seconds
  private val CallCenterClientNumRetries = 3
  private val CallCenterClientMaxConnections = 16
  private val CallCenterClientProxyHost = "best.proxy.host.ever.seen"
  private val CallCenterClientProxyPort = 3535

  private val CallCenterCampaignToUrlConfigPart = {
    CallCenterCampaignToUrl
      .map { case (k, v) =>
        s"$k = $v"
      }
      .mkString("\n")
  }

  private val CallCenterConfigStr =
    s"""
       beeper {
         campaigns = {
           $CallCenterCampaignToUrlConfigPart
         }
       }
       client {
         connection-timeout = $CallCenterClientConnectionTimeout
         request-timeout = $CallCenterClientRequestTimeout
         num-retries = $CallCenterClientNumRetries
         max-connections = $CallCenterClientMaxConnections
         proxy = {
           host = "$CallCenterClientProxyHost"
           port = $CallCenterClientProxyPort
         }
       }
      """

  private val CallCentersConfig = ConfigFactory.parseString(CallCenterConfigStr)

  "CallCentersSettings" should {
    "" in {
      val callCentersSettings = CallCentersSettings.apply(CallCentersConfig)
      callCentersSettings.settings.size shouldBe 1
      val settings = callCentersSettings.settings.head

      settings.id.toString shouldBe CallCenterId
      settings.campaignsToUrl should contain theSameElementsAs CallCenterCampaignToUrl
      callCentersSettings.client.connectionTimeout shouldBe CallCenterClientConnectionTimeout
      callCentersSettings.client.requestTimeout shouldBe CallCenterClientRequestTimeout
      callCentersSettings.client.numRetries shouldBe CallCenterClientNumRetries
      callCentersSettings.client.maxConnections shouldBe CallCenterClientMaxConnections
      callCentersSettings.client.proxy.map(_.host) shouldBe Some(CallCenterClientProxyHost)
      callCentersSettings.client.proxy.map(_.port) shouldBe Some(CallCenterClientProxyPort)
    }
  }

}
