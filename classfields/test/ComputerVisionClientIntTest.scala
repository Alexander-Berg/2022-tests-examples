package auto.carfax.common.clients.cv

import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import java.io.File

@Ignore
class ComputerVisionClientIntTest extends AnyFunSuite {

  val remoteService = new RemoteHttpService(
    "cv",
    new HttpEndpoint("yandex.ru", 80, "http"),
    client = TestHttpUtils.DefaultHttpClient
  )

  implicit val t: Traced = Traced.empty
  val client = new ComputerVisionClient(remoteService)

  test("recognize") {

    val file = new File("/Users/sievmi/Desktop/s1200.jpg")
    client.recognizePlates(file).await

    assert(2 + 2 == 4)
  }

  test("detect_objects") {

    val file = new File("/home/maratvin/Downloads/e8fb75b28ef1388989b2659c8a8a6b6e.png")
    client.detectObjects(file).await

    assert(2 + 2 == 4)
  }

  test("blur") {

    val file = new File("/Users/sievmi/Desktop/s1200.jpg")
    client
      .blurPlates(
        file,
        Array(
          Array(65, 863),
          Array(65, 842),
          Array(147, 841),
          Array(147, 862)
        )
      )
      .await

    assert(2 + 2 == 4)
  }

}
