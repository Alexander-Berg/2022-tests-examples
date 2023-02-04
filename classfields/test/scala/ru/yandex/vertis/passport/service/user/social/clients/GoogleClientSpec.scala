package ru.yandex.vertis.passport.service.user.social.clients

import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.stream.ActorMaterializer
import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.AkkaSupport
import ru.yandex.vertis.passport.service.user.social.{MultiPlatformOAuth2Config, StandardOAuth2Config}
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.passport.util.http.HttpClientMock

import scala.concurrent.duration._

class GoogleClientSpec extends FreeSpec with SpecBase with AkkaSupport with HttpClientMock {

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(1.second)

  val clientId = "clientId"
  val userId = "12345"
  val email = "crow.fff@gmail.com"
  val secret = "secret"
  val web = StandardOAuth2Config(clientId, secret, Some("http://127.0.0.1:8888"))
  val mobile = StandardOAuth2Config(clientId, secret, Some("http://127.0.0.1:8888"))

  val google = new GoogleClient(MultiPlatformOAuth2Config(web, mobile), http)

  "GoogleClient" - {
    //scalastyle:off
    val token =
      "ya29.GlxSBY5It3GHQoPaP8CxEGYFJ4gD83ZeyP6nG88KFI-AAC0UE2znRp-1irR0iewDtJNhVQt74hPy8UCypygefUowCxMGzL_VCxeTtsbFlS9eGuk31SxW_66qxvSBhw"
    def jwtDecoded(clientId: String) =
      s"""{
         | "azp": "$clientId",
         | "aud": "$clientId",
         | "sub": "$userId",
         | "scope": "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email",
         | "exp": "1517357211",
         | "expires_in": "2329",
         | "email": "$email",
         | "email_verified": "true",
         | "access_type": "online"
         |}
        """.stripMargin
    //scalastyle:on

    def prepare(clientId: String) = onRequest { _ =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(ContentType(MediaTypes.`application/json`), jwtDecoded(clientId).getBytes())
      )
    }

    "validateToken" - {
      "returns userId if token is valid" in {
        prepare(clientId)
        google.validateToken(token).futureValue shouldBe userId
      }

      "fails if token is invalid" in {
        prepare("bad_client")
        google.validateToken(token).failed.futureValue
      }
    }

    "obtain user info from JWT token" in {
      prepare(clientId)
      val user = google.getUserByToken(token).futureValue
      user.id shouldBe userId
      user.emails shouldBe Seq(email)
    }
  }
}
