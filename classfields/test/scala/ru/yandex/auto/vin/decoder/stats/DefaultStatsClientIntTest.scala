package ru.yandex.auto.vin.decoder.stats

import auto.carfax.common.storages.redis.cache.HttpCacheLayout
import auto.carfax.common.utils.tracing.Traced
import org.apache.http.HttpResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._

@Ignore
class DefaultStatsClientIntTest
  extends AnyFunSuite
  with BeforeAndAfterAll
  with ScalaFutures
  with Matchers
  with MockitoSupport {

  implicit val t: Traced = Traced.empty

  private val commonHttpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with MeteredHttpClient
      with CachedHttpClient
      with RetryHttpClient {
      override def prometheus: PrometheusRegistry = TestOperationalSupport.prometheusRegistry
    }

  val client: StatsClient = {
    val statsHttpService = {
      val endpoint: HttpEndpoint = HttpEndpoint("autoru-stats-api.vrts-slb.test.vertis.yandex.net", 80, "http")
      new RemoteHttpService(name = "stats", endpoint = endpoint, client = commonHttpClient)
    }
    new DefaultStatsClient(
      statsHttpService,
      new InMemoryAsyncCache[String, HttpResponse](new HttpCacheLayout), {
        val feature = mock[Feature[Boolean]]
        when(feature.value).thenReturn(true)
        feature
      }
    )
  }

  test("get model stats") {
    val params = StatsSummaryParams(
      Map(
        "mark" -> List("FORD"),
        "rid" -> List("213"),
        "model" -> List("FOCUS")
      )
    )
    val res = Await.result(client.getSummary(params), 10.second)
    assert(res.nonEmpty)
  }

  test("get mark stats") {
    val params = StatsSummaryParams(
      Map(
        "mark" -> List("FORD"),
        "rid" -> List("213")
      )
    )

    val res = Await.result(client.getSummary(params), 10.second)
    assert(res.nonEmpty)
  }

  test("get stats bad request") {
    val params = StatsSummaryParams(
      Map(
        "rid" -> List("213"),
        "model" -> List("FOCUS")
      )
    )
    val res = Await.result(client.getSummary(params), 10.seconds)
    assert(res.isEmpty)
  }
}
