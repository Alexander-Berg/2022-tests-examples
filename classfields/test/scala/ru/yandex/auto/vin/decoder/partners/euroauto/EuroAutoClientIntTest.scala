package ru.yandex.auto.vin.decoder.partners.euroauto

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.Await
import scala.concurrent.duration._

@Ignore
class EuroAutoClientIntTest extends AnyFunSuite {

  implicit val t = Traced.empty

  val service = new RemoteHttpService(
    "euroauto",
    new HttpEndpoint("euroauto.ru", 443, "https")
  )

  val client = new EuroAutoClient(service, "euroauto.ru")

  test("get index") {
    val res = Await.result(client.getIndex, 3.seconds)

    assert(res.records.nonEmpty)
    assert(res.records.forall(_.ctime > 0))
  }

  test("download file") {
    val res = Await.result(client.getFile("/yml/autoteka/euro_auto_2018_05_3.json"), 60.seconds)

    println(res.getPath)
    assert(2 + 2 === 4)
  }

}
