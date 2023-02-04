package ru.yandex.vertis.passport.dao.impl.redis

import org.scalatest.BeforeAndAfterAll
import ru.yandex.vertis.passport.dao.{UserEssentialsCache, UserEssentialsCacheSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisUserEssentialsCacheSpec extends UserEssentialsCacheSpec with RedisSupport with RedisStartStopSupport {

  override lazy val userDao: UserEssentialsCache = new RedisUserEssentialsCache(createCache("essentials-test"))
}
