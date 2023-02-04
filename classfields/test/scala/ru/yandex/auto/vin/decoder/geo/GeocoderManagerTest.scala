package ru.yandex.auto.vin.decoder.geo

import auto.carfax.common.storages.redis.cache.HttpCacheLayout
import auto.carfax.common.utils.config.Environment
import auto.carfax.common.utils.tracing.Traced
import org.apache.http.HttpResponse
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.auto.vin.decoder.extdata.S3DataService
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegionType, Tree}
import ru.yandex.auto.vin.decoder.tvm.{DefaultTvmConfig, DefaultTvmTicketsProvider, TvmTicketsProvider}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class GeocoderManagerTest extends AnyFunSuite with BeforeAndAfterAll {

  implicit val t: Traced = Traced.empty

  lazy val tvmTicketsProvider: TvmTicketsProvider = DefaultTvmTicketsProvider(
    DefaultTvmConfig(Environment.config.getConfig("auto-vin-decoder.tvm"))
  )

  lazy val tree = new Tree(Iterable.empty)

  lazy val basePath = "search/stable/yandsearch"

  lazy val service = new RemoteHttpService(
    name = "geocoder",
    endpoint = new HttpEndpoint("addrs-testing.search.yandex.net", 80, "http")
  )

  lazy val cache = new InMemoryAsyncCache[String, HttpResponse](new HttpCacheLayout)

  lazy val client = new DefaultGeocoderClient(service, basePath, cache, tvmTicketsProvider)

  lazy val geocoderManager = new GeocoderManager(client, tree)

  test("get unify name by simple name") {
    val res = geocoderManager.findRegionBy("Москва, ул. Кастанаевская").await

    assert(res.nonEmpty)
    assert(res.get.name == "Москва")
  }

  test("get city name by LatLon") {
    val res = geocoderManager.getCityByLatLon(LatLon("50.801337", "42.004282")).await

    assert(res.nonEmpty)
    assert(res.get == "Урюпинск")
  }

  test("get GeoObject by simple name") {
    val res = geocoderManager
      .findGeoObjectBy("Алтуфьево")
      .await

    assert(res.nonEmpty)
    assert(res.get.toponymOpt.map(_.geoId).get == 20378L)
  }

  test("listUpByTree should list appropriate regions") {

    val villageId = 11296L

    assert(geocoderManager.listUpByTree(villageId, List.empty).isEmpty)

    val regionTypes = List(
      GeoRegionType.SUBJECT_OF_THE_FEDERATION,
      GeoRegionType.REGION,
      GeoRegionType.RURAL_SETTLEMENT,
      GeoRegionType.CITY,
      GeoRegionType.VILLAGE
    )

    val res = geocoderManager.listUpByTree(11296L, regionTypes)
    assert(res.size == 3)
    assert(
      List(
        GeoRegionType.SUBJECT_OF_THE_FEDERATION,
        GeoRegionType.VILLAGE,
        GeoRegionType.RURAL_SETTLEMENT
      ).forall(res.contains)
    )
  }
}
