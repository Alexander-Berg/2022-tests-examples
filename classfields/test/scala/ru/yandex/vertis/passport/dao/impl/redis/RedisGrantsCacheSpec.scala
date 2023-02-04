package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.{GrantsCache, GrantsCacheSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisGrantsCacheSpec extends GrantsCacheSpec with RedisSupport with RedisStartStopSupport {
  override lazy val grantsCache: GrantsCache = new RedisGrantsCache(createCache("grants-test"))
}
