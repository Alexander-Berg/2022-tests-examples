package ru.yandex.vertis.feedprocessor.services.telepony

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.feedprocessor.BaseSpec
import ru.yandex.vertis.feedprocessor.services.telepony.TeleponyClient._
import ru.yandex.vertis.feedprocessor.util.MockHttpClient

class HttpTeleponyClientSpec extends BaseSpec with BeforeAndAfter {

  implicit val scheduler = ActorSystem("test").scheduler

  val http = new MockHttpClient
  val teleponyClient: HttpTeleponyClient = new HttpTeleponyClient(http)

  before {
    http.reset()
  }

  "HttpTeleponyClient: getOrCreate" should {

    "return redirect" in {
      http.expect(HttpMethods.POST, "/api/2.x/auto-dealers/redirect/getOrCreate/dealer-10767")
      http.respondWithJsonFrom("/telepony_get_or_create_redirect.json")

      val request = CreateRequest(
        target = "+79000000000",
        phoneType = None,
        geoId = None,
        ttl = None,
        antifraud = None,
        tag = Some("test-tag"),
        options = RedirectOptions(allowRedirectUnsuccessful = None),
        voxUsername = None
      ).fold(throw _, identity)

      val redirect = teleponyClient
        .getOrCreate(Domains.AutoDealers, "dealer-10767", request)
        .futureValue(Timeout(Span(5, Seconds)))

      val expectedRedirect = Redirect(
        id = "mO1Bnu1fJVY",
        objectId = "dealer-10767",
        createTime = OffsetDateTime.parse("2021-02-10T11:53:20.673+03:00"),
        source = "+74994270054",
        target = "+79299855246",
        options = None,
        deadline = None,
        tag = None
      )

      assert(redirect == expectedRedirect)
    }

    "no redirect phones available" in {
      http.expect(HttpMethods.POST, "/api/2.x/auto-dealers/redirect/getOrCreate/dealer-10767")
      http.respondWithStatus(status = 449)

      val request = CreateRequest(
        target = "+79000000000",
        phoneType = None,
        geoId = None,
        ttl = None,
        antifraud = None,
        tag = Some("test-tag"),
        options = RedirectOptions(allowRedirectUnsuccessful = None),
        voxUsername = None
      ).fold(throw _, identity)

      assertThrows[NoAvailableRedirectPhones] {
        teleponyClient
          .getOrCreate(Domains.AutoDealers, "dealer-10767", request)
          .failed
      }
    }
  }

}
