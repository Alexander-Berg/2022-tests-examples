package ru.yandex.auto.vin.decoder.api.util

import akka.http.scaladsl.model.HttpMethods
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.utils.enumerations.VinUpdatePart
import ru.yandex.auto.vin.decoder.utils.enumerations.VinUpdatePart.VinUpdatePart
import ru.yandex.vertis.commons.http.client.HttpClient.UnitResponseFormat
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, HttpRoute, RemoteHttpService}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

@Ignore
class ManualVinUpdateCurlTest extends AnyFunSuite {

  private val httpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("localhost", 3000, "http")
    new RemoteHttpService(name = "vin-decoder-api", endpoint = endpoint)
  }

  val updateRoute: HttpRoute = httpService.newRoute(
    routeName = "update",
    method = HttpMethods.POST,
    pathPattern = "/api/v1/admin/update"
  )

  def update(vin: String, updatePart: VinUpdatePart): Future[Unit] = {
    updateRoute
      .newRequest()
      .addQueryParam("vin", vin)
      .addQueryParam("context", updatePart.toString)
      .handle200(UnitResponseFormat)
      .execute()
  }

  test("update from file") {
    val vins = Source.fromFile("/Users/knkmx/work/temp/uremon_vin.txt").getLines()
    vins.foreach { vin =>
      Await.result(update(vin, VinUpdatePart.UREMONT), 10.seconds)
    }
  }

}
