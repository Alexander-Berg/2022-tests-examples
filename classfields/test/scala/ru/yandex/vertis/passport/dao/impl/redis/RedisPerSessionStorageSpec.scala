package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.{PerSessionStorage, PerSessionStorageSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisPerSessionStorageSpec extends PerSessionStorageSpec with RedisSupport with RedisStartStopSupport {

  override lazy val perSessionStorage: PerSessionStorage = new RedisPerSessionStorage(
    createCache("per-session-storage-test")
  )
}
