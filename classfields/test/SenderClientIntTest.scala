package auto.carfax.common.clients.sender.test

import auto.carfax.common.clients.sender.{DefaultSenderClient, SenderClient}
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsString, Json}
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client._

import scala.concurrent.Await
import scala.concurrent.duration._

class SenderClientIntTest extends AnyFunSuite {

  implicit val t: Traced = Traced.empty

  val commonHttpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  private val senderHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("email-sender-proxy-api.vrts-slb.test.vertis.yandex.net", 80, "http")
    new RemoteHttpService(name = "sender", endpoint = endpoint, client = commonHttpClient)
  }

  val senderClient: SenderClient = new DefaultSenderClient(senderHttpService, "35737570d429418884a40e469265b951")

  ignore("send letteer") {

    val params = Json.toJson(
      Map(
        "mark_model" -> JsString("BMW X5"),
        "vin" -> JsString("TEST VIN"),
        "auto_report_blocks" -> Json.toJson(Seq("Владельцы по ПТС", "Юридические ограничения")),
        "image" -> JsString(
          "https://avatars.mds.yandex.net/get-autoru-vos/2163740/261a973d764cad46fbf9afaba7086c93/832x624"
        )
      )
    )
    Await.result(senderClient.sendLetterWithJsonParams("vin_report_wait", "sievmi@yandex-team.ru", params), 10.seconds)
  }

}
