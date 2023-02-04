package ru.yandex.vertis.parsing.util.http.cache.ehcache

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.util.http.cache.{HttpCache, HttpCacheBuildersAware}

import scala.concurrent.duration.Duration

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedEhcacheBuilders extends HttpCacheBuildersAware with MockitoSupport {

  override def ehcache(cacheName: String, maxHeapEntries: Long, maxDiskSizeMB: Long, ttl: Duration): HttpCache = {
    mock[HttpCache]
  }
}
