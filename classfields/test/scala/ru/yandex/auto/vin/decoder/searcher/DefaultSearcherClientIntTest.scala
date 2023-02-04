package ru.yandex.auto.vin.decoder.searcher

import auto.carfax.common.storages.redis.cache.HttpCacheLayout
import auto.carfax.common.utils.tracing.Traced
import org.apache.http.HttpResponse
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

@Ignore
class DefaultSearcherClientIntTest extends AsyncFunSuite with BeforeAndAfterAll with MockitoSupport {

  implicit val t: Traced = Traced.empty

  val commonHttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with TracedHttpClient
      with RetryHttpClient
      with CachedHttpClient

  private val searcherHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("auto-searcher-01-sas.test.vertis.yandex.net", 34389, "http")
    new RemoteHttpService(name = "searcher", endpoint = endpoint, client = commonHttpClient)
  }

  val cache = new InMemoryAsyncCache[String, HttpResponse](new HttpCacheLayout)
  val feature = mock[Feature[Boolean]]
  when(feature.value).thenReturn(true)

  private val searcherClient = new DefaultSearcherClient(
    searcherHttpService,
    cache,
    feature
  )

  test("getCarMarkModelFilters") {
    searcherClient.getCarMarkModelFilters("MERCEDES", "C_KLASSE", 2307688L).map { response =>
      assert(response.getHandledOffers > 0)
      assert(response.getMarkEntriesCount > 0)
    }
  }
}
