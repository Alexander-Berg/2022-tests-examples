package ru.yandex.auto.vin.decoder.utils

import akka.http.scaladsl.model.HttpMethods
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.decode.data.VinBatch
import ru.yandex.vertis.commons.http.client.HttpClient.UnitResponseFormat
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

@Ignore
class AddVinToBuildTest extends AnyFunSuite {

  private val testingHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("auto-vin-decoder-api.vrts-slb.test.vertis.yandex.net", 80, "http")
    // val endpoint: HttpEndpoint = HttpEndpoint("localhost", 3000, "http")
    new RemoteHttpService(name = "testing", endpoint = endpoint)
  }

  private val buildQueueRoute = testingHttpService.newRoute(
    routeName = "add_vin_to_build_queu",
    method = HttpMethods.POST,
    pathPattern = "/api/v1/admin/build_queue",
    timeout = 10.seconds
  )

  private def addVin(vins: Seq[String]) = {
    val req = VinBatch(vins)
    buildQueueRoute
      .newRequest()
      .setJsonEntity(req)
      .handle200(UnitResponseFormat)
      .execute()
  }

  test("enqueue vins") {
    val vins = Source.fromFile("/Users/knkmx/work/temp/vin_codes_updates1.csv").getLines().toList

    val btach: Seq[Seq[String]] = vins.grouped(1000).toList
    btach.foreach { vinsBatch =>
      Await.result(addVin(vinsBatch), 10.seconds)
      Thread.sleep(1000)
    }
  }
}
