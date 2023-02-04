package auto.carfax.common.clients.pushnoy

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import ru.yandex.auto.vin.decoder.model.AutoruUser
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.pushnoy.PushRequestModel.SendPushTemplateRequest
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client._

@Ignore
class PushnoyClientIntTest extends AsyncFunSuite {

  implicit val t: Traced = Traced.empty

  val commonHttpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  val pushnoyHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("pushnoy-test-int.slb.vertis.yandex.net", 80, "http")
    new RemoteHttpService(name = "pushnoy", endpoint = endpoint, client = commonHttpClient)
  }

  val pushnoyClient: PushnoyClient = new PushnoyClient(pushnoyHttpService)

  val entity: SendPushTemplateRequest = {
    val entity = SendPushTemplateRequest.newBuilder()

    entity.getVinReportReadyBuilder
      .setMarkName("test mark")
      .setModelName("test model")
      .setVin("vin")

    entity.build()
  }

  test("send push to user") {
    pushnoyClient.sendPushes("user:50172708", entity).map { cnt =>
      assert(cnt > 0)
    }
  }

  test("send push to unknown user") {
    pushnoyClient.sendPushes("user:50172-08", entity).map { cnt =>
      assert(cnt == 0)
    }
  }

  test("send push to unknown device") {
    pushnoyClient
      .sendPush("-", entity)
      .map(cnt => assert(cnt.contains("0")))
  }

  test("get user devices") {
    pushnoyClient.getDevices(AutoruUser(5)).map { devices =>
      assert(devices.nonEmpty)
    }
  }

  test("check user without devices") {
    pushnoyClient.getDevices(AutoruUser(1)).map { devices =>
      assert(devices.isEmpty)
    }
  }
}
