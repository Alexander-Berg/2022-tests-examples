package ru.yandex.auto.vin.decoder.geo

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class DefaultGeobaseClientTest extends AnyFunSuite {

  implicit val t: Traced = Traced.empty

  val service = new RemoteHttpService(
    name = "geobase",
    endpoint = new HttpEndpoint("geobase-test.qloud.yandex.ru", 80, "http")
  )

  val client = new DefaultGeobaseClient(service)

  test("get region name by id") {
    val regionId = 213L
    val res = client.getRegionName(regionId).await

    assert(res.nonEmpty)
    assert(res.get == "Москва")
  }

  test("get region id by ip") {
    val res = client.regionIdByIp("91.144.140.0").await

    assert(res.nonEmpty)
    assert(res.get == 43L)
  }

  test("get timezone by region id") {
    val res = client.timezoneByRid(43L).await

    assert(res.nonEmpty)
    assert(res.get == "Europe/Moscow")
  }

}
