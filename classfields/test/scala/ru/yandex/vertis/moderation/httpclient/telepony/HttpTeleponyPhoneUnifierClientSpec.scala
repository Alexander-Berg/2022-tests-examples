package ru.yandex.vertis.moderation.httpclient.telepony

import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.exceptions.TestFailedException
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.http.HttpCodeException
import ru.yandex.vertis.moderation.httpclient.telepony.TeleponyPhoneUnifierClient.UnifiedPhone
import ru.yandex.vertis.moderation.httpclient.telepony.impl.http.HttpTeleponyPhoneUnifierClient

import scala.concurrent.ExecutionContext

/**
  * Specs for [[HttpTeleponyPhoneUnifierClient]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpTeleponyPhoneUnifierClientSpec extends SpecBase {

  import ExecutionContext.Implicits.global
  private val httpClient = new DefaultAsyncHttpClient()
  lazy val client =
    new HttpTeleponyPhoneUnifierClient("http://hydra-01-myt.test.vertis.yandex.net:35530/api/1.x", httpClient)

  "HttpTeleponyPhoneUnifierClient" should {

    val ParsablePhones =
      Seq(
        ("+7-921-3882934", UnifiedPhone("+79213882934", 2, "Mobile", 10174)),
        ("8-921-3882934", UnifiedPhone("+79213882934", 2, "Mobile", 10174)),
        ("921-3882934", UnifiedPhone("+79213882934", 2, "Mobile", 10174)),
        ("9213882934", UnifiedPhone("+79213882934", 2, "Mobile", 10174)),
        ("+7 812 633-36-00", UnifiedPhone("+78126333600", 2, "Local", 2)),
        ("812633-36-00", UnifiedPhone("+78126333600", 2, "Local", 2)),
        ("7 495 739-70-00", UnifiedPhone("+74957397000", 213, "Local", 213)),
        ("8 800 250-96-39", UnifiedPhone("+78002509639", 225, "Local", 225)),
        ("+7 843 524-71-71", UnifiedPhone("+78435247171", 43, "Local", 120894))
      )

    ParsablePhones.foreach { case (phone, expectedResult) =>
      s"provide unified for $phone" in {
        val actualResult = client.unify(phone).futureValue
        actualResult shouldBe expectedResult
      }
    }

    val UnparsablePhones =
      Seq(
        "3882934",
        "1",
        "-1",
        "aaaaa",
        "1237213786127378231732",
        "1234567890",
        "+1 617 398 7870",
        "+90212386-87-60",
        "+375 17 328-19-61"
      )

    UnparsablePhones.foreach { phone =>
      s"fail on parsing $phone" in {
        try {
          client.unify(phone).futureValue
          fail("Unexpected done")
        } catch {
          case t: TestFailedException =>
            t.cause match {
              case Some(HttpCodeException(400, _)) => ()
              case other                           => fail(s"Unexpected $other")
            }
        }
      }
    }

  }
}
