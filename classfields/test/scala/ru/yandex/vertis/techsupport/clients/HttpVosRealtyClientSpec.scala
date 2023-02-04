package ru.yandex.vertis.vsquality.techsupport.clients

import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.Ignore
import com.softwaremill.tagging._
import ru.yandex.realty.api.response.VosInactiveRegionsQuotaResponse
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpVosRealtyClient
import ru.yandex.vertis.vsquality.techsupport.model.UserId.Client
import ru.yandex.vertis.vsquality.techsupport.model.{Tags, Url}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

@Ignore
class HttpVosRealtyClientSpec extends SpecBase {

  private val baseVosUrl: Url =
    "http://realty-vos-api-http.vrts-slb.test.vertis.yandex.net/api/2.0".taggedWith[Tags.Url]
  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  private val client: VosRealtyClient[F] = new HttpVosRealtyClient(baseVosUrl)
  val id = Client.Realty(4091467203L.taggedWith[Tags.RealtyUserId])

  "VosRealtyClient" should {
    "get regional quotas" in {
      client.getQuotaRegions(userId = id).await shouldBe a[VosInactiveRegionsQuotaResponse]
    }
  }
}
