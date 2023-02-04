package ru.yandex.vertis.vsquality.callgate.apis.impl

import cats.effect.IO
import com.softwaremill.tagging.Tagger
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.callgate.Globals.OfferIdTag
import ru.yandex.vertis.vsquality.callgate.apis.DefaultApis
import ru.yandex.vertis.vsquality.callgate.apis.config.GeneralGatewayApiConfig
import ru.yandex.vertis.vsquality.utils.http_client_utils.config.HttpClientConfig
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase
import sttp.client3.SttpBackend

@Ignore
class HttpGeneralGatewayApiClientImplSpec extends SpecBase with DefaultApis {
  implicit val sttp: SttpBackend[F, Any] = getSttpBackend()

  private val config =
    GeneralGatewayApiConfig(
      useStub = false,
      httpClient = HttpClientConfig(
        url = "http://general-gateway-api.vrts-slb.test.vertis.yandex.net"
      )
    )

  private val client = new HttpGeneralGatewayApiClientImpl[IO](config)

  "HttpGeneralGatewayApiClientImpl" should {
    "requests for redirect of existing offer" in {
      val offerId = "108217445354049536".taggedWith[OfferIdTag]
      val ret = client.getRedirectContacts(offerId).unsafeRunSync()
      println(ret)
    }

    "requests for redirect of non-existing offer" in {
      val offerId = "non-existing".taggedWith[OfferIdTag]
      val ret = client.getRedirectContacts(offerId).unsafeRunSync()
      println(ret)
    }
  }
}
