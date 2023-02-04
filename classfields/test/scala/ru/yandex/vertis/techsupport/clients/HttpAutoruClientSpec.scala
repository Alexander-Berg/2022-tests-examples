package ru.yandex.vertis.vsquality.techsupport.clients

import com.softwaremill.tagging._
import org.scalatest.Ignore
import ru.auto.api.response_model.VinResolutionResponse
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpAutoruClient
import ru.yandex.vertis.vsquality.techsupport.model.{Tags, Url, UserId}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.SttpBackend

@Ignore
class HttpAutoruClientSpec extends SpecBase {

  val autoruPublicApibaseUrl: Url =
    "http://autoru-api-server-int.vrts-slb.test.vertis.yandex.net/1.0".taggedWith[Tags.Url]
  val authToken = "Vertis swagger"
  implicit val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  val client: AutoruClient[F] = new HttpAutoruClient(autoruPublicApibaseUrl, authToken)

  val user = UserId.Client.Autoru.PrivatePerson(39284566L.taggedWith[Tags.AutoruPrivatePersonId])
  val offerId = "1097507597-4b743a9f".taggedWith[Tags.OfferId]

  "AutoruClient.offers" should {
    "reload vin resolution for ok offer should return forbidden error response" in {
      val expected =
        VinResolutionResponse(
          None,
          None,
          None,
          None,
          Some(ru.auto.api.response_model.ErrorCode.VIN_RESOLUTION_RELOAD_FORBIDDEN),
          Some(ru.auto.api.response_model.ResponseStatus.ERROR),
          Some("Reload not allowed"),
          None,
          None
        )
      client.reloadVinResolution(user, offerId).await shouldBe expected
    }
  }
}
