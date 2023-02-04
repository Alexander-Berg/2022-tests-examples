package ru.yandex.vertis.baker

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import play.api.libs.json.Json
import ru.yandex.vertis.baker.components.http.client.HttpClient
import ru.yandex.vertis.baker.components.http.client.config.HttpClientConfig
import ru.yandex.vertis.baker.lifecycle.DefaultApplication
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Await
import scala.concurrent.duration._

object TracedOfferTest extends DefaultApplication {
  private val components = new TestComponents(this)

  private val config = HttpClientConfig.newBuilder.withHostPort("localhost", Some(36240)).withServiceName("vos").build

  import components._

  private val client = HttpClient.newBuilder(config).build

  afterStart {
    implicit val trace: Traced = components.traceCreator.trace
    val req = new HttpGet(
      "/api/v1/offer/cars/1100634230-9d4f81ee?owner=0&include_removed=0&fromvos=0&force_telepony_info=0&with_stats=0"
    )
    val f = client.doRequest("get_offer", req) { res =>
      Json.parse(EntityUtils.toString(res.getEntity))
    }
    val json = Await.result(f, 10.seconds)
    println((json \ "id").as[String])
    println(trace.requestId)
    sys.exit(0)
  }
}
