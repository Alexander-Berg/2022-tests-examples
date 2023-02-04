package ru.yandex.auto.vin.decoder.searcher.unification

import auto.carfax.common.storages.redis.cache.HttpCacheLayout
import auto.carfax.common.utils.tracing.Traced
import org.apache.http.HttpResponse
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits

@Ignore
class UnificatorIntTest extends AsyncFunSuite with MockitoSupport {

  implicit val ec = Implicits.global
  implicit val t: Traced = Traced.empty

  val unificatorClient = new Unificator(
    {
      val endpoint: HttpEndpoint = HttpEndpoint("wizard-02-sas.test.vertis.yandex.net", 34320, "http")
      new RemoteHttpService(name = "wizard", endpoint = endpoint)
    },
    new InMemoryAsyncCache[String, HttpResponse](new HttpCacheLayout), {
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(false)
      feature
    }
  )

  test("should return result") {
    unificatorClient.unifyHeadOption("БМВ тройка").map { mm =>
      assert(mm.nonEmpty)
      assert(mm.get.mark == "BMW")
      assert(mm.get.model == "3ER")
    }
  }

  test("several marks") {
    unificatorClient.unify(List("ХЕНДЭ SOLARIS", "X5"), 1).map { result =>
      assert(result.size == 2)
    }
  }
}
