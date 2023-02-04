package ru.yandex.vertis.akka.ning.client

import akka.http.scaladsl.client.RequestBuilding
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks

import scala.concurrent.duration._

/**
  * @author neron
  */
@RunWith(classOf[JUnitRunner])
class HttpRequestSupportSettingsSpec
    extends WordSpecLike
      with Matchers
      with BeforeAndAfterEach
      with RequestBuilding
      with PropertyChecks {

  import HttpRequestSupport._
  import HttpRequestSupportSettingsSpec._

  val HttpRequestSettingsGen = for {
    requestTimeout <- Gen.option(Gen.choose(0, Int.MaxValue).map(_.millis))
    followRedirect <- Gen.option(Gen.oneOf(false, true))
  } yield HttpRequestSettings(requestTimeout, followRedirect)

  "HttpRequestSettings" should {
    "be carried on the http request" in {
      forAll(HttpRequestSettingsGen) { expectedSettings =>
        val expectedRequest = Get(SamplePath)
        val richRequest = expectedRequest.withSettings(expectedSettings)
        val (actualSettings, actualRequest) = richRequest.extractSettings
        actualSettings should ===(expectedSettings)
        actualRequest should ===(expectedRequest)
      }
    }
  }

}

object HttpRequestSupportSettingsSpec {
  private val Path = "path"
  private val Host = "localhost"
  private val Port = 34328
  val SamplePath = s"http://$Host:$Port/$Path"
}
