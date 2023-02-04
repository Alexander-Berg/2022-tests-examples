package auto.carfax.common.clients.cv

import auto.carfax.common.clients.avatars.DefaultAvatarsClient
import auto.carfax.common.clients.cv.{ComputerVisionClient, LicensePlatesBlurManager}
import auto.carfax.common.utils.avatars.PhotoInfoId
import auto.carfax.common.utils.config.Environment
import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.tvm.{DefaultTvmConfig, DefaultTvmTicketsProvider}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class LicensePlatesBlurManagerIntTest extends AnyFunSuite with MockitoSupport {

  implicit val t: Traced = Traced.empty

  private lazy val avatarsClient = {
    val remoteService = new RemoteHttpService(
      "avatars",
      new HttpEndpoint("avatars-int.mdst.yandex.net", 13000, "http"),
      client = TestHttpUtils.DefaultHttpClient
    )

    lazy val tickerProvider = DefaultTvmTicketsProvider(
      DefaultTvmConfig(Environment.config.getConfig("auto-vin-decoder.tvm"))
    )

    new DefaultAvatarsClient(
      avatarsHttpService = remoteService,
      tvmTicketsProvider = tickerProvider,
      "images.mds-proxy.test.avto.ru"
    )
  }

  private lazy val cvClient = {
    val remoteService = new RemoteHttpService(
      "cv",
      new HttpEndpoint("yandex.ru", 80, "http"),
      client = TestHttpUtils.DefaultHttpClient
    )

    new ComputerVisionClient(remoteService)
  }

  private lazy val manager = new LicensePlatesBlurManager(cvClient, avatarsClient)

  test("upload image") {
    val url = "http://img02.avto-nomer.ru/011/o/ru3016137.jpg"
    avatarsClient.putImageFromUrl("autoru-carfax", url, None).await
    // 1397950
    // rlRSkWYDbN5ilfirD1oQaA4KQldD6W3uu

    assert(2 + 2 == 4)
  }

  test("get image") {
    avatarsClient.getOrigImage("autoru-carfax", 1397950, "rlRSkWYDbN5ilfirD1oQaA4KQldD6W3uu").await
    assert(2 + 2 == 4)
  }

  test("blur image") {

    val orig = PhotoInfoId(1397950, "rlRSkWYDbN5ilfirD1oQaA4KQldD6W3uu", "autoru-carfax")

    manager.blur(orig).await

    assert(2 + 2 == 4)
  }

}
