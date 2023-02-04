package ru.yandex.vertis.vsquality.techsupport.clients

import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.Ignore
import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.techsupport.clients.PushnoyClient.DeviceInfo
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpPushnoyClient
import ru.yandex.vertis.vsquality.techsupport.model.{Domain, Tags, Url}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

/**
  * @author devreggs
  */
@Ignore
class HttpPushnoyClientSpec extends SpecBase {

  private val basePushnoyUrl: Url =
    "http://pushnoy-test-int.slb.vertis.yandex.net/api/v1".taggedWith[Tags.Url]
  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  private val client: PushnoyClient[F] = new HttpPushnoyClient(basePushnoyUrl)

  "PushnoyClient.deviceInfo" should {
    "get device info" in {
      client
        .deviceInfo("g5d2ede0f4fcdmklgu5nqdjc8h3btkdo.51936c4edfe1109afa70b44a24145c70", Domain.Autoru)
        .await shouldBe a[DeviceInfo]
    }

    "get empty device info " in {
      client
        .deviceInfo("non_exist_device", Domain.Autoru)
        .await shouldBe PushnoyClient.DeviceInfo.Empty
    }
  }
}
