package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.{ApiTokenCache, ApiTokenCacheSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisApiTokenCacheSpec extends ApiTokenCacheSpec with RedisSupport with RedisStartStopSupport {
  override lazy val tokenCache: ApiTokenCache = new RedisApiTokenCache(createCache("token-cache-test"))
}
