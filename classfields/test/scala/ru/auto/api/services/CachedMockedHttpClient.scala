package ru.auto.api.services

import ru.auto.api.BaseSpec
import ru.auto.api.app.redis.{RedisCache, RedisCacheBuilder}
import ru.auto.api.http.CachedHttpClient
import ru.auto.api.testkit.MockHttpClientImpl
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

trait CachedMockedHttpClient extends MockedHttpClient { this: BaseSpec with MockitoSupport =>

  protected val cachedhttp: MockHttpClientImpl with CachedHttpClient = new MockHttpClientImpl with CachedHttpClient {

    val redisCacheBuilder: RedisCacheBuilder = {
      val redisCache = mock[RedisCache]
      val builder = mock[RedisCacheBuilder]

      when(builder.getClient(?)).thenReturn(redisCache)
      when(redisCache.get(?)(?, ?)).thenReturn(Future.successful(None))
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)

      builder
    }
  }
}
