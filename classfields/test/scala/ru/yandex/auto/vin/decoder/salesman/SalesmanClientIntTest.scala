package ru.yandex.auto.vin.decoder.salesman

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.model.AutoruUser
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.duration._

@Ignore
class SalesmanClientIntTest extends AsyncFunSuite {

  implicit val t = Traced.empty

  val endpoint: HttpEndpoint = HttpEndpoint(
    "back-rt-01-sas.test.vertis.yandex.net",
    1050,
    "http"
  )

  val remoteService = new RemoteHttpService("salesman", endpoint)
  val client = new SalesmanClient(remoteService)

  test("get bought reports") {
    client.getBoughtReports(AutoruUser(43973966), Some("1093315372-c7442209"), None, Some(200.days)).map { response =>
      assert(response.getReportsCount == 1)
    }
  }
}
