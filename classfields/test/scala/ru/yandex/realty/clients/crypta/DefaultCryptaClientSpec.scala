package ru.yandex.realty.clients.crypta

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.model.user.{AuthGenerators, WebUser}
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.vertis.generators.ProducerProvider

/**
  * Specs on HTTP [[CryptaClient]].
  *
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class DefaultCryptaClientSpec
  extends AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with UserProfileGenerators
  with ProducerProvider {

  private val client = new DefaultCryptaClient(httpService)
  "DefaultCryptaClient" should {

    "successfully getProfile" in {
      forAll(AuthGenerators.yandexUidGen) { yandexUid =>
        forAll(userProfileGen) { userProfile =>
          expectGetProfile(yandexUid)
          httpClient.respondWith(userProfile)
          client.getProfile(Seq(WebUser(yandexUid))).futureValue shouldBe Some(userProfile)
        }
      }
    }

    "handle 404 in getProfile" in {
      val yandexUid = AuthGenerators.yandexUidGen.next
      expectGetProfile(yandexUid)
      httpClient.respond(StatusCodes.NotFound)
      client.getProfile(Seq(WebUser(yandexUid))).futureValue shouldBe None
    }

    "handle 400 in getProfile" in {
      val yandexUid = AuthGenerators.yandexUidGen.next
      expectGetProfile(yandexUid)
      httpClient.respond(StatusCodes.BadRequest)
      interceptCause[IllegalArgumentException] {
        client.getProfile(Seq(WebUser(yandexUid))).futureValue
      }
    }

    "successfully getProfileSync" in {
      forAll(AuthGenerators.yandexUidGen) { yandexUid =>
        forAll(userProfileGen) { userProfile =>
          expectGetProfile(yandexUid)
          httpClient.respondWith(userProfile)
          client.getProfileSync(Seq(WebUser(yandexUid))).get shouldBe Some(userProfile)
        }
      }
    }
  }

  private def expectGetProfile(yandexUid: String): Unit = {
    httpClient.expect(GET, s"/bigb?bigb-uid=$yandexUid&format=protobuf&client=realty-backend")
  }
}
