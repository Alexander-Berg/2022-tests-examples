package ru.yandex.auto.vin.decoder.publicapi

import auto.carfax.common.clients.public_api.PublicApiClient
import auto.carfax.common.storages.redis.cache.HttpCacheLayout
import auto.carfax.common.utils.tracing.Traced
import org.apache.http.HttpResponse
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.StatsModel.PredictRequest
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class PublicApiCientIntTest extends AnyFunSuite with MockitoSupport {

  val remoteService = new RemoteHttpService(
    "public-api",
    new HttpEndpoint("autoru-api-server.vrts-slb.test.vertis.yandex.net", 80, "http")
  )

  implicit val t: Traced = Traced.empty

  val client = new PublicApiCarfax(
    new PublicApiClient(remoteService, "Vertis carfax-cf473a037ac973a988a354e25e3d4ed41993fad6\""),
    new InMemoryAsyncCache[String, HttpResponse](new HttpCacheLayout), {
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(false)
      feature
    }
  )

  test("get offer") {
    val res = client.getOffer("1102885835-bf58e6ff").await

    assert(res.nonEmpty)
  }

  test("get predict") {
    val request = PredictRequest
      .newBuilder()
      .setRid(213)
      .setTechParamId(21134259)
      .setKmAge(25000)
      .setColor("FAFBFB")
      .setOwningTime(22)
      .setOwnersCount(2)
      .setYear(2019)
      .build()

    val res = client.getPredict(request).await

    assert(res.nonEmpty)
  }

}
