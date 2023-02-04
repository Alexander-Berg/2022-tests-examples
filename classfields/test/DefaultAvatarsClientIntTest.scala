package auto.carfax.common.clients.avatars

import auto.carfax.common.clients.avatars.DefaultAvatarsClient
import auto.carfax.common.utils.config.Environment
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.tvm.{DefaultTvmConfig, DefaultTvmTicketsProvider}
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Ignore
class DefaultAvatarsClientIntTest extends AsyncFunSuite with MockitoSugar with BeforeAndAfterAll {

  implicit val t: Traced = Traced.empty

  lazy val remoteService = new RemoteHttpService(
    "avatars",
    new HttpEndpoint("avatars-int.mdst.yandex.net", 13000, "http")
  )

  lazy val tickerProvider = DefaultTvmTicketsProvider(
    DefaultTvmConfig(Environment.config.getConfig("auto-vin-decoder.tvm"))
  )

  lazy val client = new DefaultAvatarsClient(
    avatarsHttpService = remoteService,
    tvmTicketsProvider = tickerProvider,
    "images.mds-proxy.test.avto.ru"
  )

  test("sample put") {
    client.putImageFromUrl("autoru-carfax", "https://www.migtorg.com/media/photo/2539734.jpg", None).map { response =>
      assert(response.getMdsPhotoInfo.getName != "")
    }
  }

  test("sample delete") {
    val res = client.removeImage("autoru-carfax", 1397950, "nRBans0wdWfEmZQRUddlqu502MxAX729A")
    Await.result(res, 1.minute)
    assert(true)
  }
}
